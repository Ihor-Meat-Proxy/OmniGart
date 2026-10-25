package com.omnigart.sensors.android.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import com.omnigart.sensors.api.HeartRateSensorManager
import com.omnigart.sensors.api.PolarSensorFeatures
import com.omnigart.sensors.android.permission.BlePermissionRequirements
import com.omnigart.sensors.android.store.RememberedDevicesStore
import com.omnigart.sensors.ble.BatteryLevelParser
import com.omnigart.sensors.ble.BleUuids
import com.omnigart.sensors.ble.BodySensorLocation
import com.omnigart.sensors.ble.DeviceInformationParser
import com.omnigart.sensors.ble.HeartRateMeasurementParser
import com.omnigart.sensors.model.BleHrFailure
import com.omnigart.sensors.model.BleHrResult
import com.omnigart.sensors.model.DeviceInformation
import com.omnigart.sensors.model.HeartRateMeasurement
import com.omnigart.sensors.model.SensorConnectionState
import com.omnigart.sensors.model.SensorDevice
import com.omnigart.sensors.model.SensorKind
import com.omnigart.sensors.model.SensorSessionState
import com.omnigart.sensors.reconnect.ReconnectBackoff
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Standard Bluetooth Heart Rate Profile central (service 0x180D).
 *
 * Battery-friendly behaviour: scan uses low-power mode, auto-reconnect uses
 * [ReconnectBackoff] with jitter, and GATT is closed when disconnected.
 */
@SuppressLint("MissingPermission")
class StandardBleHeartRateManager(
    context: Context,
    private val rememberedStore: RememberedDevicesStore = RememberedDevicesStore(context),
    private val backoff: ReconnectBackoff = ReconnectBackoff(),
    private val polarFeaturesProvider: (() -> PolarSensorFeatures?)? = null,
) : HeartRateSensorManager {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val bluetoothManager =
        appContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val adapter: BluetoothAdapter? get() = bluetoothManager.adapter

    private val _session = MutableStateFlow(
        SensorSessionState(device = null, connection = SensorConnectionState.Disconnected)
    )
    override val sessionState: StateFlow<SensorSessionState> = _session.asStateFlow()

    private val discovered = ConcurrentHashMap<String, SensorDevice>()
    private val _discovered = MutableStateFlow<List<SensorDevice>>(emptyList())
    override val discoveredDevices: StateFlow<List<SensorDevice>> = _discovered.asStateFlow()

    private val _measurements = MutableSharedFlow<HeartRateMeasurement>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    override val measurements: SharedFlow<HeartRateMeasurement> = _measurements.asSharedFlow()

    override val rememberedDevices: StateFlow<List<SensorDevice>> = rememberedStore.devices

    private var gatt: BluetoothGatt? = null
    private var scanCallback: ScanCallback? = null
    private var autoReconnect = true
    private var reconnectJob: Job? = null
    private var preferredDeviceId: String? = null
    private var deviceInfo = DeviceInformation()

    override fun polarFeatures(): PolarSensorFeatures? = polarFeaturesProvider?.invoke()

    override suspend fun startScan(): BleHrResult = withContext(Dispatchers.Main) {
        readinessFailure()?.let { return@withContext failResult(it) }
        val scanner = adapter?.bluetoothLeScanner
        if (scanner == null) {
            return@withContext failResult(BleHrFailure.ScannerUnavailable)
        }
        stopScanInternal()
        discovered.clear()
        _discovered.value = emptyList()
        updateSession(connection = SensorConnectionState.Scanning)

        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(UUID.fromString(BleUuids.HEART_RATE_SERVICE)))
            .build()
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
            .build()
        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.toSensorDevice()
                discovered[device.id] = device
                _discovered.value = discovered.values.sortedBy { it.name ?: it.id }
            }

            override fun onScanFailed(errorCode: Int) {
                failSession(BleHrFailure.ScanFailed(errorCode))
            }
        }
        scanCallback = callback
        scanner.startScan(listOf(filter), settings, callback)
        BleHrResult.success()
    }

    override suspend fun stopScan() = withContext(Dispatchers.Main) {
        stopScanInternal()
        if (_session.value.connection == SensorConnectionState.Scanning) {
            updateSession(connection = SensorConnectionState.Disconnected)
        }
    }

    override suspend fun connect(deviceId: String): BleHrResult = withContext(Dispatchers.Main) {
        readinessFailure()?.let { return@withContext failResult(it) }
        stopScanInternal()
        reconnectJob?.cancel()
        preferredDeviceId = deviceId
        val remote =
            try {
                adapter?.getRemoteDevice(deviceId)
            } catch (_: IllegalArgumentException) {
                null
            }
        if (remote == null) {
            return@withContext failResult(BleHrFailure.DeviceNotFound(deviceId))
        }
        updateSession(
            device = discovered[deviceId] ?: SensorDevice(deviceId, remote.name, SensorKind.Unknown),
            connection = SensorConnectionState.Connecting,
        )
        gatt?.close()
        gatt = remote.connectGatt(appContext, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        BleHrResult.success()
    }

    override suspend fun disconnect() = withContext(Dispatchers.Main) {
        autoReconnect = false
        reconnectJob?.cancel()
        preferredDeviceId = null
        gatt?.disconnect()
        gatt?.close()
        gatt = null
        updateSession(connection = SensorConnectionState.Disconnected)
    }

    override suspend fun rememberDevice(device: SensorDevice) {
        rememberedStore.remember(device)
    }

    override suspend fun forgetDevice(deviceId: String) {
        rememberedStore.forget(deviceId)
    }

    override fun setAutoReconnect(enabled: Boolean) {
        autoReconnect = enabled
        if (!enabled) reconnectJob?.cancel()
    }

    fun close() {
        scope.cancel()
        gatt?.close()
        gatt = null
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            scope.launch {
                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        if (status != BluetoothGatt.GATT_SUCCESS) {
                            gatt.close()
                            this@StandardBleHeartRateManager.gatt = null
                            failSession(BleHrFailure.GattError(status, "connect"))
                            maybeReconnect()
                            return@launch
                        }
                        updateSession(connection = SensorConnectionState.Connected)
                        gatt.discoverServices()
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        gatt.close()
                        this@StandardBleHeartRateManager.gatt = null
                        if (status != BluetoothGatt.GATT_SUCCESS) {
                            failSession(BleHrFailure.GattError(status, "disconnect"))
                        } else {
                            updateSession(connection = SensorConnectionState.Disconnected)
                        }
                        maybeReconnect()
                    }
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                scope.launch {
                    failSession(BleHrFailure.GattError(status, "discoverServices"))
                }
                return
            }
            scope.launch {
                enableHrNotifications(gatt)
                readOptionalCharacteristics(gatt)
                updateSession(connection = SensorConnectionState.Ready)
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
        ) {
            if (characteristic.uuid == UUID.fromString(BleUuids.HEART_RATE_MEASUREMENT)) {
                HeartRateMeasurementParser.parse(value).onSuccess { measurement ->
                    _measurements.tryEmit(measurement)
                }
            }
        }

        @Deprecated("Deprecated in API 33")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
        ) {
            val value = characteristic.value ?: return
            onCharacteristicChanged(gatt, characteristic, value)
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int,
        ) {
            if (status != BluetoothGatt.GATT_SUCCESS) return
            scope.launch { handleRead(characteristic.uuid, value) }
        }

        @Deprecated("Deprecated in API 33")
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int,
        ) {
            val value = characteristic.value ?: return
            onCharacteristicRead(gatt, characteristic, value, status)
        }
    }

    private fun enableHrNotifications(gatt: BluetoothGatt) {
        val service = gatt.getService(UUID.fromString(BleUuids.HEART_RATE_SERVICE)) ?: return
        val hr = service.getCharacteristic(UUID.fromString(BleUuids.HEART_RATE_MEASUREMENT)) ?: return
        gatt.setCharacteristicNotification(hr, true)
        val cccd = hr.getDescriptor(UUID.fromString(BleUuids.CCCD)) ?: return
        cccd.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
        gatt.writeDescriptor(cccd)
    }

    private fun readOptionalCharacteristics(gatt: BluetoothGatt) {
        gatt.getService(UUID.fromString(BleUuids.BATTERY_SERVICE))
            ?.getCharacteristic(UUID.fromString(BleUuids.BATTERY_LEVEL))
            ?.let { gatt.readCharacteristic(it) }

        gatt.getService(UUID.fromString(BleUuids.HEART_RATE_SERVICE))
            ?.getCharacteristic(UUID.fromString(BleUuids.BODY_SENSOR_LOCATION))
            ?.let { gatt.readCharacteristic(it) }

        val dis = gatt.getService(UUID.fromString(BleUuids.DEVICE_INFORMATION_SERVICE))
        listOf(
            BleUuids.MANUFACTURER_NAME,
            BleUuids.MODEL_NUMBER,
            BleUuids.SERIAL_NUMBER,
            BleUuids.FIRMWARE_REVISION,
            BleUuids.HARDWARE_REVISION,
            BleUuids.SOFTWARE_REVISION,
        ).forEach { uuid ->
            dis?.getCharacteristic(UUID.fromString(uuid))?.let { gatt.readCharacteristic(it) }
        }
    }

    private fun handleRead(uuid: UUID, value: ByteArray) {
        when (uuid) {
            UUID.fromString(BleUuids.BATTERY_LEVEL) -> {
                BatteryLevelParser.parse(value).onSuccess { level ->
                    updateSession(battery = level)
                }
            }
            UUID.fromString(BleUuids.BODY_SENSOR_LOCATION) -> {
                val loc = value.firstOrNull()?.let { BodySensorLocation.fromByte(it.toInt()) }
                updateSession(location = loc?.name)
            }
            UUID.fromString(BleUuids.MANUFACTURER_NAME) -> {
                deviceInfo = DeviceInformationParser.merge(deviceInfo, manufacturerName = value)
                updateSession(info = deviceInfo)
            }
            UUID.fromString(BleUuids.MODEL_NUMBER) -> {
                deviceInfo = DeviceInformationParser.merge(deviceInfo, modelNumber = value)
                updateSession(info = deviceInfo)
            }
            UUID.fromString(BleUuids.SERIAL_NUMBER) -> {
                deviceInfo = DeviceInformationParser.merge(deviceInfo, serialNumber = value)
                updateSession(info = deviceInfo)
            }
            UUID.fromString(BleUuids.FIRMWARE_REVISION) -> {
                deviceInfo = DeviceInformationParser.merge(deviceInfo, firmwareRevision = value)
                updateSession(info = deviceInfo)
            }
            UUID.fromString(BleUuids.HARDWARE_REVISION) -> {
                deviceInfo = DeviceInformationParser.merge(deviceInfo, hardwareRevision = value)
                updateSession(info = deviceInfo)
            }
            UUID.fromString(BleUuids.SOFTWARE_REVISION) -> {
                deviceInfo = DeviceInformationParser.merge(deviceInfo, softwareRevision = value)
                updateSession(info = deviceInfo)
            }
        }
    }

    private fun maybeReconnect() {
        val id = preferredDeviceId ?: return
        if (!autoReconnect) return
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            var attempt = 0
            while (coroutineContext.isActive) {
                updateSession(connection = SensorConnectionState.Reconnecting)
                val delayMs = backoff.delayMsForAttempt(attempt)
                if (delayMs < 0) {
                    failSession(BleHrFailure.ReconnectExhausted)
                    return@launch
                }
                delay(delayMs)
                attempt += 1
                // connect() replaces session; exit loop and let disconnect restart if needed
                connect(id)
                return@launch
            }
        }
    }

    private fun stopScanInternal() {
        val callback = scanCallback ?: return
        runCatching { adapter?.bluetoothLeScanner?.stopScan(callback) }
        scanCallback = null
    }

    /** Expected BLE precondition failures — never throw into the sensors UI. */
    private fun readinessFailure(): BleHrFailure? {
        if (!BlePermissionRequirements.hasAll(appContext)) {
            return BleHrFailure.MissingPermissions(
                BlePermissionRequirements.requiredPermissions().toList(),
            )
        }
        if (adapter?.isEnabled != true) {
            return BleHrFailure.AdapterDisabled
        }
        return null
    }

    private fun failSession(failure: BleHrFailure) {
        updateSession(
            connection = SensorConnectionState.Failed,
            error = failure.message,
            failure = failure,
        )
    }

    private fun failResult(failure: BleHrFailure): BleHrResult {
        failSession(failure)
        return BleHrResult.failure(failure)
    }

    private fun updateSession(
        device: SensorDevice? = _session.value.device,
        connection: SensorConnectionState = _session.value.connection,
        battery: Int? = _session.value.batteryPercent,
        location: String? = _session.value.bodySensorLocation,
        info: DeviceInformation? = _session.value.deviceInformation,
        error: String? = null,
        failure: BleHrFailure? = null,
    ) {
        _session.value = SensorSessionState(
            device = device,
            connection = connection,
            batteryPercent = battery,
            bodySensorLocation = location,
            deviceInformation = info,
            lastError = error,
            lastFailure = failure,
        )
    }

    private fun ScanResult.toSensorDevice(): SensorDevice {
        val name = scanRecord?.deviceName ?: device.name
        val kind = when {
            name?.contains("H10", ignoreCase = true) == true -> SensorKind.PolarH10
            name?.contains("Sense", ignoreCase = true) == true -> SensorKind.PolarVeritySense
            name?.contains("Polar", ignoreCase = true) == true -> SensorKind.PolarOther
            else -> SensorKind.StandardHeartRateProfile
        }
        val services = scanRecord?.serviceUuids?.map { it.uuid.toString() }.orEmpty()
        return SensorDevice(
            id = device.address,
            name = name,
            kind = kind,
            rssi = rssi,
            advertisedServiceUuids = services,
        )
    }
}
