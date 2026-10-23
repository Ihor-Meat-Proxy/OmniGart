package com.omnigart.sensors.android.treadmill

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
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import com.omnigart.sensors.ble.BleUuids
import com.omnigart.sensors.model.SensorConnectionState
import com.omnigart.sensors.model.SensorDevice
import com.omnigart.sensors.model.SensorKind
import com.omnigart.sensors.reconnect.ReconnectBackoff
import com.omnigart.sensors.treadmill.TreadmillBridge
import com.omnigart.sensors.treadmill.TreadmillSampleDto
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Android BLE central for treadmill protocols. Tries FTMS first at discovery time;
 * FitShow FFF0 and iConsole UART are also accepted. Control writes are caller-gated.
 */
@SuppressLint("MissingPermission")
class TreadmillBleManager(
    context: Context,
    private val backoff: ReconnectBackoff = ReconnectBackoff(),
) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val bluetoothManager =
        appContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val adapter: BluetoothAdapter? get() = bluetoothManager.adapter

    private val _connection = MutableStateFlow(SensorConnectionState.Disconnected)
    val connection: StateFlow<SensorConnectionState> = _connection.asStateFlow()

    private val discovered = ConcurrentHashMap<String, SensorDevice>()
    private val _discovered = MutableStateFlow<List<SensorDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<SensorDevice>> = _discovered.asStateFlow()

    private val _sample = MutableStateFlow<TreadmillSampleDto?>(null)
    val sample: StateFlow<TreadmillSampleDto?> = _sample.asStateFlow()

    private val _protocol = MutableStateFlow("Unknown")
    val protocolName: StateFlow<String> = _protocol.asStateFlow()

    private var gatt: BluetoothGatt? = null
    private var scanCallback: ScanCallback? = null
    private var controlCharacteristic: BluetoothGattCharacteristic? = null
    private var writeCharacteristic: BluetoothGattCharacteristic? = null
    private var autoReconnect = true
    private var preferredDeviceId: String? = null
    private var reconnectJob: Job? = null
    private var reconnectAttempt = 0
    private val serviceUuidBuffer = mutableListOf<String>()

    fun startScan() {
        val scanner = adapter?.bluetoothLeScanner ?: return
        stopScan()
        discovered.clear()
        _discovered.value = emptyList()
        _connection.value = SensorConnectionState.Scanning
        val callback =
            object : ScanCallback() {
                override fun onScanResult(callbackType: Int, result: ScanResult) {
                    val id = result.device.address
                    val name = result.device.name ?: result.scanRecord?.deviceName
                    discovered[id] =
                        SensorDevice(
                            id = id,
                            name = name,
                            kind = SensorKind.Unknown,
                            rssi = result.rssi,
                            advertisedServiceUuids =
                            result.scanRecord?.serviceUuids?.map { it.toString() }.orEmpty(),
                        )
                    _discovered.value = discovered.values.toList()
                }
            }
        scanCallback = callback
        scanner.startScan(
            null,
            ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_POWER).build(),
            callback,
        )
    }

    fun stopScan() {
        scanCallback?.let { adapter?.bluetoothLeScanner?.stopScan(it) }
        scanCallback = null
        if (_connection.value == SensorConnectionState.Scanning) {
            _connection.value = SensorConnectionState.Disconnected
        }
    }

    fun connect(deviceId: String) {
        val device = adapter?.getRemoteDevice(deviceId) ?: return
        stopScan()
        preferredDeviceId = deviceId
        reconnectJob?.cancel()
        _connection.value = SensorConnectionState.Connecting
        gatt = device.connectGatt(appContext, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
    }

    fun disconnect() {
        autoReconnect = false
        reconnectJob?.cancel()
        preferredDeviceId = null
        gatt?.close()
        gatt = null
        controlCharacteristic = null
        writeCharacteristic = null
        _connection.value = SensorConnectionState.Disconnected
    }

    fun setAutoReconnect(enabled: Boolean) {
        autoReconnect = enabled
    }

    fun writeControl(payload: ByteArray) {
        val gattLocal = gatt ?: return
        val characteristic = controlCharacteristic ?: writeCharacteristic ?: return
        @Suppress("DEPRECATION")
        characteristic.value = payload
        characteristic.writeType =
            if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0) {
                BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            } else {
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            }
        @Suppress("DEPRECATION")
        gattLocal.writeCharacteristic(characteristic)
    }

    private val gattCallback =
        object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    _connection.value = SensorConnectionState.Connected
                    reconnectAttempt = 0
                    serviceUuidBuffer.clear()
                    gatt.discoverServices()
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    controlCharacteristic = null
                    writeCharacteristic = null
                    if (autoReconnect) {
                        scheduleReconnect()
                    } else {
                        _connection.value = SensorConnectionState.Disconnected
                    }
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                if (status != BluetoothGatt.GATT_SUCCESS) return
                serviceUuidBuffer.clear()
                for (service in gatt.services) {
                    val su = service.uuid.toString().lowercase()
                    serviceUuidBuffer += su
                    for (characteristic in service.characteristics) {
                        val cu = characteristic.uuid.toString().lowercase()
                        when {
                            cu.contains("2ad9") -> {
                                controlCharacteristic = characteristic
                                enableNotify(gatt, characteristic)
                            }
                            cu.contains("2acd") || cu.contains("2ada") || cu.contains("fff1") ||
                                cu.contains("1e4d4bd9") -> {
                                enableNotify(gatt, characteristic)
                            }
                            cu.contains("fff2") || cu.contains("884143f4") -> {
                                writeCharacteristic = characteristic
                            }
                            cu.contains("2acc") || cu.contains("2ad4") || cu.contains("2ad5") -> {
                                gatt.readCharacteristic(characteristic)
                            }
                        }
                    }
                }
                _protocol.value = TreadmillBridge.detectProtocol(serviceUuidBuffer.toTypedArray())
                _connection.value = SensorConnectionState.Ready
            }

            @Deprecated("Deprecated in Java")
            override fun onCharacteristicChanged(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
            ) {
                @Suppress("DEPRECATION")
                val payload = characteristic.value ?: return
                ingest(characteristic.uuid.toString(), payload)
            }

            @Deprecated("Deprecated in Java")
            override fun onCharacteristicRead(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                status: Int,
            ) {
                if (status != BluetoothGatt.GATT_SUCCESS) return
                @Suppress("DEPRECATION")
                val payload = characteristic.value ?: return
                ingest(characteristic.uuid.toString(), payload)
            }
        }

    private fun ingest(characteristicUuid: String, payload: ByteArray) {
        val dto =
            TreadmillBridge.parseNotification(
                _protocol.value,
                characteristicUuid,
                payload,
            )
        if (dto != null) {
            _sample.value = dto
        }
    }

    private fun enableNotify(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        gatt.setCharacteristicNotification(characteristic, true)
        val cccd = characteristic.getDescriptor(UUID.fromString(BleUuids.CCCD)) ?: return
        @Suppress("DEPRECATION")
        cccd.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        @Suppress("DEPRECATION")
        gatt.writeDescriptor(cccd)
    }

    private fun scheduleReconnect() {
        val deviceId = preferredDeviceId ?: return
        _connection.value = SensorConnectionState.Reconnecting
        reconnectJob?.cancel()
        val delayMs = backoff.delayMsForAttempt(reconnectAttempt)
        reconnectAttempt += 1
        if (delayMs < 0L) return
        reconnectJob =
            scope.launch {
                delay(delayMs)
                connect(deviceId)
            }
    }
}
