package com.omnigart.sensors.android.treadmill

import com.omnigart.sensors.treadmill.TreadmillBridge
import com.omnigart.sensors.treadmill.TreadmillSampleDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** In-process FTMS treadmill for Android emulators (no real BLE). */
class SimulatedTreadmillController {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _sample = MutableStateFlow<TreadmillSampleDto?>(null)
    val sample: StateFlow<TreadmillSampleDto?> = _sample.asStateFlow()
    private var tickJob: Job? = null
    private var controlArmed = false

    fun reset() {
        tickJob?.cancel()
        TreadmillBridge.simulatorReset()
        controlArmed = false
        _sample.value = null
    }

    fun armControl(): Boolean {
        val response = TreadmillBridge.simulatorControl(TreadmillBridge.controlRequestControl())
        controlArmed = response.size >= 3 && (response[2].toInt() and 0xFF) == 1
        return controlArmed
    }

    fun start(): Boolean {
        if (!controlArmed) return false
        val response = TreadmillBridge.simulatorControl(TreadmillBridge.controlStart())
        val ok = response.size >= 3 && (response[2].toInt() and 0xFF) == 1
        if (ok) startTicks()
        return ok
    }

    fun stop(): Boolean {
        tickJob?.cancel()
        val response = TreadmillBridge.simulatorControl(TreadmillBridge.controlStop())
        return response.size >= 3 && (response[2].toInt() and 0xFF) == 1
    }

    fun applyPreset(id: String): Boolean = TreadmillBridge.simulatorApplyPreset(id)

    private fun startTicks() {
        tickJob?.cancel()
        tickJob =
            scope.launch {
                while (isActive) {
                    _sample.value = TreadmillBridge.simulatorTick()
                    delay(1_000)
                }
            }
    }
}
