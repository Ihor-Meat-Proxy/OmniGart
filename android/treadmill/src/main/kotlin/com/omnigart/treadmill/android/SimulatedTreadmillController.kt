package com.omnigart.treadmill.android

import com.omnigart.treadmill.ftms.FtmsControlPoint
import com.omnigart.treadmill.model.TreadmillLiveMetrics
import com.omnigart.treadmill.sim.SimulatedTreadmill
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** In-process FTMS treadmill for Android emulators / DEBUG when BLE is unavailable. */
class SimulatedTreadmillController(
    private val scope: CoroutineScope,
    private val hrMax: Int = 190
) {
    private val sim = SimulatedTreadmill(hrMax = hrMax)
    private val _metrics = MutableStateFlow(TreadmillLiveMetrics(connected = false))
    val metrics: StateFlow<TreadmillLiveMetrics> = _metrics.asStateFlow()
    private var tickJob: Job? = null
    var watchHeartRateBpm: Int? = null

    val deviceId: String get() = sim.deviceId
    val deviceName: String get() = sim.deviceName
    val controlArmed: Boolean get() = sim.controlGranted
    val running: Boolean get() = sim.running

    fun reset() {
        tickJob?.cancel()
        tickJob = null
        sim.resetSession()
        _metrics.value = TreadmillLiveMetrics(connected = false)
    }

    fun attach(): Boolean {
        sim.resetSession()
        val ok = armControl()
        if (ok) {
            _metrics.value = sim.currentMetrics(watchHeartRateBpm)
        }
        return ok
    }

    fun armControl(): Boolean {
        val response = sim.handleControlPoint(FtmsControlPoint.requestControl())
        return response.size >= 3 &&
            (response[2].toInt() and 0xFF) == FtmsControlPoint.ResultCode.SUCCESS.code
    }

    fun startBelt(): Boolean {
        if (!sim.controlGranted) return false
        val response = sim.handleControlPoint(FtmsControlPoint.startOrResume())
        val ok =
            response.size >= 3 &&
                (response[2].toInt() and 0xFF) == FtmsControlPoint.ResultCode.SUCCESS.code
        if (ok) startTicks()
        return ok
    }

    fun stopBelt(): Boolean {
        tickJob?.cancel()
        tickJob = null
        val response =
            sim.handleControlPoint(FtmsControlPoint.stopOrPause(FtmsControlPoint.StopPause.STOP))
        _metrics.value = sim.currentMetrics(watchHeartRateBpm)
        return response.size >= 3 &&
            (response[2].toInt() and 0xFF) == FtmsControlPoint.ResultCode.SUCCESS.code
    }

    fun setTargetSpeedKmh(speed: Double) {
        if (!sim.controlGranted) return
        sim.handleControlPoint(FtmsControlPoint.setTargetSpeedKmh(speed))
    }

    fun setTargetInclinePercent(incline: Double) {
        if (!sim.controlGranted) return
        sim.handleControlPoint(FtmsControlPoint.setTargetInclinePercent(incline))
    }

    fun applyProgram(id: String): Boolean = sim.applyProgram(id)

    fun encodeCurrentNotification(): ByteArray = sim.encodeCurrentNotification()

    private fun startTicks() {
        tickJob?.cancel()
        tickJob =
            scope.launch {
                while (isActive) {
                    val bytes = sim.tick()
                    _metrics.value = sim.parseOwnNotification(bytes, watchHeartRateBpm)
                    delay(1_000)
                }
            }
    }
}
