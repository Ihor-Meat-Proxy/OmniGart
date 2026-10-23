package com.omnigart.treadmill.android

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
import android.content.Context
import android.content.SharedPreferences
import com.omnigart.treadmill.ble.TreadmillBleUuids
import com.omnigart.treadmill.diagnostics.TreadmillDiagnosticsLog
import com.omnigart.treadmill.ftms.FtmsControlPoint
import com.omnigart.treadmill.ftms.FtmsTreadmillDataParser
import com.omnigart.treadmill.iconsole.IConsoleProtocol
import com.omnigart.treadmill.model.TreadmillLiveMetrics
import com.omnigart.treadmill.model.TreadmillProtocol
import com.omnigart.treadmill.rsc.RscMeasurementParser
import com.omnigart.treadmill.session.TreadmillMetricsMerger
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.min
import kotlin.math.pow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Android BLE central for FTMS / RSC / iConsole treadmills.
 * Requires BLUETOOTH_SCAN + BLUETOOTH_CONNECT (API 31+).
 */
class TreadmillBleClient(
    context: Context,
    private val scope: CoroutineScope,
    private val hrMax: Int = 190
) {
    data class Discovered(val address: String, val name: String)

    enum class State { Idle, Scanning, Connecting, Connected, Reconnecting, Failed }

    private val appContext = context.applicationContext
    private val prefs: SharedPreferences =
        appContext.getSharedPreferences("omnigart.treadmill", Context.MODE_PRIVATE)
    private val adapter: BluetoothAdapter? =
        (appContext.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

    private val _state = MutableStateFlow(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()
    private val _discovered = MutableStateFlow<List<Discovered>>(emptyList())
    val discovered: StateFlow<List<Discovered>> = _discovered.asStateFlow()
    private val _metrics = MutableStateFlow(TreadmillLiveMetrics())
    val metrics: StateFlow<TreadmillLiveMetrics> = _metrics.asStateFlow()
    val diagnostics = TreadmillDiagnosticsLog()

    var allowRemoteControl: Boolean = false
    var watchHeartRateBpm: Int? = null

    private var gatt: BluetoothGatt? = null
    private var controlPoint: BluetoothGattCharacteristic? = null
    private var iconsoleRx: BluetoothGattCharacteristic? = null
    private var preferredAddress: String? = prefs.getString(KEY_REMEMBERED, null)
    private var reconnectAttempt = 0
    private var reconnectJob: Job? = null
    private var iconsolePollJob: Job? = null
    private val scanning = AtomicBoolean(false)

    private val scanCallback =
        object : ScanCallback() {
            @SuppressLint("MissingPermission")
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.device ?: return
                val name = result.scanRecord?.deviceName ?: device.name ?: "Treadmill"
                val entry = Discovered(device.address, name)
                val current = _discovered.value
                if (current.none { it.address == entry.address }) {
                    _discovered.value = current + entry
                    diagnostics.info("discovered $name ${device.address}")
                }
            }
        }

    private val gattCallback =
        object : BluetoothGattCallback() {
            @SuppressLint("MissingPermission")
            override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    _state.value = State.Connected
                    reconnectAttempt = 0
                    diagnostics.info("connected")
                    g.discoverServices()
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    diagnostics.info("disconnected")
                    iconsolePollJob?.cancel()
                    _metrics.value = _metrics.value.copy(connected = false, protocol = TreadmillProtocol.NONE)
                    if (preferredAddress != null) scheduleReconnect() else _state.value = State.Idle
                }
            }

            @SuppressLint("MissingPermission")
            override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
                for (service in g.services) {
                    diagnostics.service(service.uuid.toString())
                    for (ch in service.characteristics) {
                        diagnostics.characteristic(
                            service.uuid.toString(),
                            ch.uuid.toString(),
                            ch.properties.toString()
                        )
                        val u = ch.uuid.toString().lowercase()
                        when {
                            u.contains("2acd") || u.contains("2a53") -> enableNotify(g, ch)
                            u.contains("2ad9") -> controlPoint = ch
                            u == TreadmillBleUuids.ICONSOLE_UART_TX -> enableNotify(g, ch)
                            u == TreadmillBleUuids.ICONSOLE_UART_RX -> {
                                iconsoleRx = ch
                                startIConsolePoll(g, ch)
                            }
                        }
                    }
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onCharacteristicChanged(g: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
                @Suppress("DEPRECATION")
                val value = characteristic.value ?: return
                handleBytes(characteristic.uuid.toString(), value)
            }

            override fun onCharacteristicChanged(
                g: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                value: ByteArray
            ) {
                handleBytes(characteristic.uuid.toString(), value)
            }
        }

    @SuppressLint("MissingPermission")
    fun startScan() {
        val scanner = adapter?.bluetoothLeScanner
        if (scanner == null) {
            _state.value = State.Failed
            return
        }
        _discovered.value = emptyList()
        _state.value = State.Scanning
        scanning.set(true)
        diagnostics.info("scan start")
        scanner.startScan(scanCallback)
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        if (scanning.getAndSet(false)) {
            adapter?.bluetoothLeScanner?.stopScan(scanCallback)
        }
        if (_state.value == State.Scanning) _state.value = State.Idle
    }

    @SuppressLint("MissingPermission")
    fun connect(address: String) {
        stopScan()
        preferredAddress = address
        prefs.edit().putString(KEY_REMEMBERED, address).apply()
        _state.value = State.Connecting
        val device: BluetoothDevice =
            try {
                adapter?.getRemoteDevice(address)
            } catch (_: IllegalArgumentException) {
                null
            } ?: run {
                _state.value = State.Failed
                return
            }
        gatt?.close()
        gatt = device.connectGatt(appContext, false, gattCallback)
    }

    fun connectRemembered() {
        preferredAddress?.let { connect(it) }
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        reconnectJob?.cancel()
        iconsolePollJob?.cancel()
        preferredAddress = null
        gatt?.disconnect()
        gatt?.close()
        gatt = null
        _state.value = State.Idle
        _metrics.value = TreadmillLiveMetrics()
    }

    @SuppressLint("MissingPermission")
    fun setTargetSpeedKmh(speed: Double) {
        if (!allowRemoteControl) return
        val g = gatt ?: return
        val cp = controlPoint ?: return
        write(g, cp, FtmsControlPoint.requestControl())
        write(g, cp, FtmsControlPoint.setTargetSpeedKmh(speed))
        diagnostics.info("control set speed $speed")
    }

    @SuppressLint("MissingPermission")
    fun setTargetInclinePercent(incline: Double) {
        if (!allowRemoteControl) return
        val g = gatt ?: return
        val cp = controlPoint ?: return
        write(g, cp, FtmsControlPoint.requestControl())
        write(g, cp, FtmsControlPoint.setTargetInclinePercent(incline))
        diagnostics.info("control set incline $incline")
    }

    private fun handleBytes(uuid: String, value: ByteArray) {
        diagnostics.rawBytes(uuid, value)
        val lower = uuid.lowercase()
        val merged =
            when {
                lower.contains("2acd") ->
                    FtmsTreadmillDataParser.parse(value).getOrNull()?.let {
                        TreadmillMetricsMerger.fromFtms(it, watchHeartRateBpm, hrMax)
                    }
                lower.contains("2a53") ->
                    RscMeasurementParser.parse(value).getOrNull()?.let {
                        TreadmillMetricsMerger.fromRsc(it, watchHeartRateBpm, hrMax)
                    }
                lower == TreadmillBleUuids.ICONSOLE_UART_TX ->
                    IConsoleProtocol.parseTelemetry(value).getOrNull()?.let {
                        TreadmillMetricsMerger.fromIConsole(it, watchHeartRateBpm, hrMax)
                    }
                else -> null
            }
        if (merged != null) _metrics.value = merged
    }

    @SuppressLint("MissingPermission")
    private fun enableNotify(g: BluetoothGatt, ch: BluetoothGattCharacteristic) {
        g.setCharacteristicNotification(ch, true)
        val cccd = ch.getDescriptor(UUID.fromString(TreadmillBleUuids.CCCD)) ?: return
        @Suppress("DEPRECATION")
        cccd.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        @Suppress("DEPRECATION")
        g.writeDescriptor(cccd)
    }

    @SuppressLint("MissingPermission")
    private fun write(g: BluetoothGatt, ch: BluetoothGattCharacteristic, bytes: ByteArray) {
        @Suppress("DEPRECATION")
        ch.value = bytes
        @Suppress("DEPRECATION")
        g.writeCharacteristic(ch)
    }

    @SuppressLint("MissingPermission")
    private fun startIConsolePoll(g: BluetoothGatt, rx: BluetoothGattCharacteristic) {
        iconsolePollJob?.cancel()
        iconsolePollJob =
            scope.launch(Dispatchers.IO) {
                write(g, rx, IConsoleProtocol.ping())
                while (isActive) {
                    write(g, rx, IConsoleProtocol.read())
                    delay(500)
                    write(g, rx, IConsoleProtocol.ping())
                }
            }
    }

    private fun scheduleReconnect() {
        val address = preferredAddress ?: return
        reconnectJob?.cancel()
        _state.value = State.Reconnecting
        val attempt = reconnectAttempt++
        val delayMs = (min(60.0, 2.0.pow(min(attempt, 5).toDouble())) * 1000).toLong()
        reconnectJob =
            scope.launch {
                delay(delayMs)
                connect(address)
            }
    }

    companion object {
        private const val KEY_REMEMBERED = "remembered_address"
    }
}
