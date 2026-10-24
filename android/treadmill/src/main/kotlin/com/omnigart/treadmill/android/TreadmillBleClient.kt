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
import com.omnigart.treadmill.fitshow.FitShowStatusParser
import com.omnigart.treadmill.ftms.FtmsControlPoint
import com.omnigart.treadmill.ftms.FtmsTreadmillDataParser
import com.omnigart.treadmill.iconsole.IConsoleProtocol
import com.omnigart.treadmill.model.TreadmillLiveMetrics
import com.omnigart.treadmill.model.TreadmillProtocol
import com.omnigart.treadmill.rsc.RscMeasurementParser
import com.omnigart.treadmill.session.TreadmillMetricsMerger
import com.omnigart.treadmill.sim.SimulatedTreadmill
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
 * Android BLE central for FTMS / FitShow / RSC / iConsole treadmills.
 * Discovery preference: FTMS → FitShow → RSC → iConsole.
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
    private var fitShowWrite: BluetoothGattCharacteristic? = null
    private var iconsoleRx: BluetoothGattCharacteristic? = null
    private var preferredAddress: String? = prefs.getString(KEY_REMEMBERED, null)
    private var reconnectAttempt = 0
    private var reconnectJob: Job? = null
    private var iconsolePollJob: Job? = null
    private var fitShowPollJob: Job? = null
    private var activeProtocol: TreadmillProtocol = TreadmillProtocol.NONE
    private val scanning = AtomicBoolean(false)
    private var simulator: SimulatedTreadmill? = null
    private var simulatorTickJob: Job? = null
    /** True when live metrics come from [SimulatedTreadmill] instead of GATT. */
    val usingSimulator: Boolean get() = simulator != null

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
                    fitShowPollJob?.cancel()
                    activeProtocol = TreadmillProtocol.NONE
                    _metrics.value = _metrics.value.copy(connected = false, protocol = TreadmillProtocol.NONE)
                    if (preferredAddress != null) scheduleReconnect() else _state.value = State.Idle
                }
            }

            @SuppressLint("MissingPermission")
            override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
                var hasFtms = false
                var hasFitShow = false
                var hasRsc = false
                var hasIConsole = false
                var fitShowNotify: BluetoothGattCharacteristic? = null
                for (service in g.services) {
                    diagnostics.service(service.uuid.toString())
                    for (ch in service.characteristics) {
                        diagnostics.characteristic(
                            service.uuid.toString(),
                            ch.uuid.toString(),
                            ch.properties.toString()
                        )
                        val uuid = ch.uuid.toString().lowercase()
                        when {
                            uuid.contains("2acd") -> {
                                hasFtms = true
                                enableNotify(g, ch)
                            }
                            uuid.contains("2ad9") -> controlPoint = ch
                            uuid == TreadmillBleUuids.FITSHOW_NOTIFY || uuid.contains("fff1") -> {
                                hasFitShow = true
                                fitShowNotify = ch
                            }
                            uuid == TreadmillBleUuids.FITSHOW_WRITE || uuid.contains("fff2") ->
                                fitShowWrite = ch
                            uuid.contains("2a53") -> hasRsc = true
                            uuid == TreadmillBleUuids.ICONSOLE_UART_TX -> {
                                hasIConsole = true
                                enableNotify(g, ch)
                            }
                            uuid == TreadmillBleUuids.ICONSOLE_UART_RX -> iconsoleRx = ch
                        }
                    }
                }
                when {
                    hasFtms -> {
                        activeProtocol = TreadmillProtocol.FTMS
                    }
                    hasFitShow -> {
                        activeProtocol = TreadmillProtocol.FITSHOW
                        fitShowNotify?.let { enableNotify(g, it) }
                        fitShowWrite?.let { startFitShowPoll(g, it) }
                    }
                    hasRsc -> {
                        activeProtocol = TreadmillProtocol.RSC
                        for (service in g.services) {
                            for (ch in service.characteristics) {
                                if (ch.uuid.toString().lowercase().contains("2a53")) {
                                    enableNotify(g, ch)
                                }
                            }
                        }
                    }
                    hasIConsole -> {
                        activeProtocol = TreadmillProtocol.ICONSOLE
                        iconsoleRx?.let { startIConsolePoll(g, it) }
                    }
                    else -> activeProtocol = TreadmillProtocol.NONE
                }
                diagnostics.info("active protocol $activeProtocol")
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

    /**
     * Attach the in-process FTMS simulator (DEBUG / emulator / BLE unavailable).
     * Arms control and marks the client connected with [TreadmillProtocol.SIMULATED].
     */
    fun useSimulator() {
        stopScan()
        reconnectJob?.cancel()
        iconsolePollJob?.cancel()
        fitShowPollJob?.cancel()
        simulatorTickJob?.cancel()
        gatt?.close()
        gatt = null
        val sim = SimulatedTreadmill(hrMax = hrMax)
        sim.handleControlPoint(FtmsControlPoint.requestControl())
        simulator = sim
        activeProtocol = TreadmillProtocol.SIMULATED
        _state.value = State.Connected
        _discovered.value = listOf(Discovered(sim.deviceId, sim.deviceName))
        _metrics.value = sim.currentMetrics(watchHeartRateBpm)
        diagnostics.clear()
        diagnostics.protocolGuess = TreadmillProtocol.SIMULATED.name
        diagnostics.upsertDevice(sim.deviceId, sim.deviceName, listOf(TreadmillBleUuids.FITNESS_MACHINE_SERVICE))
        diagnostics.info("simulator attached")
    }

    fun startSimulatorBelt(): Boolean {
        val sim = simulator ?: return false
        val response = sim.handleControlPoint(FtmsControlPoint.startOrResume())
        val ok =
            response.size >= 3 &&
                (response[2].toInt() and 0xFF) == FtmsControlPoint.ResultCode.SUCCESS.code
        if (ok) startSimulatorTicks()
        return ok
    }

    fun stopSimulatorBelt(): Boolean {
        simulatorTickJob?.cancel()
        val sim = simulator ?: return false
        val response =
            sim.handleControlPoint(FtmsControlPoint.stopOrPause(FtmsControlPoint.StopPause.STOP))
        _metrics.value = sim.currentMetrics(watchHeartRateBpm)
        return response.size >= 3 &&
            (response[2].toInt() and 0xFF) == FtmsControlPoint.ResultCode.SUCCESS.code
    }

    @SuppressLint("MissingPermission")
    fun startScan() {
        clearSimulator()
        val scanner = adapter?.bluetoothLeScanner
        if (scanner == null) {
            _state.value = State.Failed
            diagnostics.info("BLE scanner unavailable")
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
        if (address == SIMULATOR_DEVICE_ID || simulator?.deviceId == address) {
            useSimulator()
            return
        }
        clearSimulator()
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
        diagnostics.upsertDevice(address, device.name)
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
        fitShowPollJob?.cancel()
        clearSimulator()
        preferredAddress = null
        activeProtocol = TreadmillProtocol.NONE
        gatt?.disconnect()
        gatt?.close()
        gatt = null
        _state.value = State.Idle
        _metrics.value = TreadmillLiveMetrics()
    }

    /** True when FTMS, FitShow, or the in-process simulator can accept remote writes. */
    fun controlAvailable(): Boolean =
        allowRemoteControl &&
            (
                usingSimulator ||
                    (activeProtocol == TreadmillProtocol.FTMS && controlPoint != null) ||
                    (activeProtocol == TreadmillProtocol.FITSHOW && fitShowWrite != null)
            )

    @SuppressLint("MissingPermission")
    fun setTargetSpeedKmh(speed: Double) {
        if (!allowRemoteControl) return
        val sim = simulator
        if (sim != null) {
            sim.handleControlPoint(FtmsControlPoint.setTargetSpeedKmh(speed))
            diagnostics.info("sim control set speed $speed")
            return
        }
        val g = gatt ?: return
        when (activeProtocol) {
            TreadmillProtocol.FTMS -> {
                val cp = controlPoint ?: return
                write(g, cp, FtmsControlPoint.requestControl())
                write(g, cp, FtmsControlPoint.setTargetSpeedKmh(speed))
            }
            TreadmillProtocol.FITSHOW -> {
                val tx = fitShowWrite ?: return
                val incline = _metrics.value.inclinePercent ?: 0.0
                write(g, tx, FitShowStatusParser.buildSetTarget(speed, incline))
            }
            else -> return
        }
        diagnostics.info("control set speed $speed")
    }

    @SuppressLint("MissingPermission")
    fun setTargetInclinePercent(incline: Double) {
        if (!allowRemoteControl) return
        val sim = simulator
        if (sim != null) {
            sim.handleControlPoint(FtmsControlPoint.setTargetInclinePercent(incline))
            diagnostics.info("sim control set incline $incline")
            return
        }
        val g = gatt ?: return
        when (activeProtocol) {
            TreadmillProtocol.FTMS -> {
                val cp = controlPoint ?: return
                write(g, cp, FtmsControlPoint.requestControl())
                write(g, cp, FtmsControlPoint.setTargetInclinePercent(incline))
            }
            TreadmillProtocol.FITSHOW -> {
                val tx = fitShowWrite ?: return
                val speed = _metrics.value.speedKmh ?: 0.0
                write(g, tx, FitShowStatusParser.buildSetTarget(speed, incline))
            }
            else -> return
        }
        diagnostics.info("control set incline $incline")
    }

    private fun startSimulatorTicks() {
        val sim = simulator ?: return
        simulatorTickJob?.cancel()
        simulatorTickJob =
            scope.launch {
                while (isActive) {
                    val bytes = sim.tick()
                    _metrics.value = sim.parseOwnNotification(bytes, watchHeartRateBpm)
                    delay(1_000)
                }
            }
    }

    private fun clearSimulator() {
        simulatorTickJob?.cancel()
        simulatorTickJob = null
        simulator = null
    }

    private fun handleBytes(uuid: String, value: ByteArray) {
        diagnostics.rawBytes(uuid, value)
        val lower = uuid.lowercase()
        val merged =
            when {
                lower.contains("2acd") && activeProtocol == TreadmillProtocol.FTMS ->
                    FtmsTreadmillDataParser.parse(value).getOrNull()?.let {
                        TreadmillMetricsMerger.fromFtms(it, watchHeartRateBpm, hrMax)
                    }
                (lower == TreadmillBleUuids.FITSHOW_NOTIFY || lower.contains("fff1")) &&
                    activeProtocol == TreadmillProtocol.FITSHOW ->
                    FitShowStatusParser.parseNotification(value).getOrNull()?.let {
                        TreadmillMetricsMerger.fromFitShow(it, watchHeartRateBpm, hrMax)
                    }
                lower.contains("2a53") && activeProtocol == TreadmillProtocol.RSC ->
                    RscMeasurementParser.parse(value).getOrNull()?.let {
                        TreadmillMetricsMerger.fromRsc(it, watchHeartRateBpm, hrMax)
                    }
                lower == TreadmillBleUuids.ICONSOLE_UART_TX &&
                    activeProtocol == TreadmillProtocol.ICONSOLE ->
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

    @SuppressLint("MissingPermission")
    private fun startFitShowPoll(g: BluetoothGatt, writeChar: BluetoothGattCharacteristic) {
        fitShowPollJob?.cancel()
        fitShowPollJob =
            scope.launch(Dispatchers.IO) {
                while (isActive) {
                    write(g, writeChar, FitShowStatusParser.buildPollStatus())
                    delay(1000)
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
        const val SIMULATOR_DEVICE_ID = "sim-treadmill-ftms"
    }
}
