package com.omnigart.sensors.android

import android.content.Context
import com.omnigart.sensors.android.ble.StandardBleHeartRateManager
import com.omnigart.sensors.android.polar.PolarBleSensorAdapter
import com.omnigart.sensors.android.store.RememberedDevicesStore
import com.omnigart.sensors.api.HeartRateSensorManager
import com.omnigart.sensors.api.PolarSensorFeatures
import com.omnigart.sensors.model.BleHrResult
import com.omnigart.sensors.model.HeartRateMeasurement
import com.omnigart.sensors.model.SensorDevice
import com.omnigart.sensors.model.SensorKind
import com.omnigart.sensors.model.SensorSessionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch

/**
 * Chooses standard BLE HR Profile vs Polar SDK based on remembered / selected device kind.
 * App shells should depend on this façade rather than Polar types directly.
 */
class OmniGartSensorFacade(
    context: Context,
) : HeartRateSensorManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val store = RememberedDevicesStore(context)
    private val standard = StandardBleHeartRateManager(context, store) { polar }
    private val polar = PolarBleSensorAdapter(context)
    private val mergedMeasurements = MutableSharedFlow<HeartRateMeasurement>(extraBufferCapacity = 64)

    init {
        scope.launch {
            merge(standard.measurements, polar.heartRateMeasurements).collect {
                mergedMeasurements.emit(it)
            }
        }
    }

    override val sessionState: StateFlow<SensorSessionState> = standard.sessionState
    override val discoveredDevices: StateFlow<List<SensorDevice>> = standard.discoveredDevices
    override val measurements: Flow<HeartRateMeasurement> = mergedMeasurements
    override val rememberedDevices: StateFlow<List<SensorDevice>> = store.devices

    override suspend fun startScan(): BleHrResult = standard.startScan()
    override suspend fun stopScan() = standard.stopScan()

    override suspend fun connect(deviceId: String): BleHrResult {
        val kind = discoveredDevices.value.find { it.id == deviceId }?.kind
            ?: rememberedDevices.value.find { it.id == deviceId }?.kind
            ?: SensorKind.Unknown
        return when (kind) {
            SensorKind.PolarH10,
            SensorKind.PolarVeritySense,
            SensorKind.PolarOther,
            -> {
                // Polar device IDs are often the 8-char sensor id; BLE address also accepted by SDK.
                polar.connect(deviceId)
                polar.startHrStream()
                BleHrResult.success()
            }
            else -> standard.connect(deviceId)
        }
    }

    override suspend fun disconnect() {
        polar.disconnect()
        standard.disconnect()
    }

    override suspend fun rememberDevice(device: SensorDevice) = standard.rememberDevice(device)
    override suspend fun forgetDevice(deviceId: String) = standard.forgetDevice(deviceId)
    override fun setAutoReconnect(enabled: Boolean) = standard.setAutoReconnect(enabled)
    override fun polarFeatures(): PolarSensorFeatures = polar

    fun close() {
        scope.cancel()
        standard.close()
        polar.close()
    }
}
