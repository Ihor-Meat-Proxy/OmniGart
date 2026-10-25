package com.omnigart.sensors.api

import com.omnigart.sensors.model.AccSample
import com.omnigart.sensors.model.BleHrResult
import com.omnigart.sensors.model.EcgSample
import com.omnigart.sensors.model.HeartRateMeasurement
import com.omnigart.sensors.model.OfflineRecordingEntry
import com.omnigart.sensors.model.OfflineRecordingPayload
import com.omnigart.sensors.model.SensorDevice
import com.omnigart.sensors.model.SensorSessionState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Platform-agnostic heart-rate sensor façade.
 *
 * Android (`android/sensors`) and iOS (`ios/Packages/OmniGartSensors`) provide
 * implementations. Standard BLE Heart Rate Profile devices and Polar devices both
 * surface through this API; Polar-only capabilities hang off [PolarSensorFeatures].
 *
 * Scan / connect return [BleHrResult] for expected BLE failures (permissions off,
 * adapter off, device lost, GATT errors) instead of throwing.
 */
interface HeartRateSensorManager {
    val sessionState: StateFlow<SensorSessionState>
    val discoveredDevices: StateFlow<List<SensorDevice>>
    val measurements: Flow<HeartRateMeasurement>
    val rememberedDevices: StateFlow<List<SensorDevice>>

    /** Scan filtered to Heart Rate Service (0x180D) and known Polar advertisements. */
    suspend fun startScan(): BleHrResult
    suspend fun stopScan()

    suspend fun connect(deviceId: String): BleHrResult
    suspend fun disconnect()

    /** Persist pairing preference for auto-reconnect. */
    suspend fun rememberDevice(device: SensorDevice)
    suspend fun forgetDevice(deviceId: String)

    /** Enable / disable auto-reconnect with exponential backoff (platform applies policy). */
    fun setAutoReconnect(enabled: Boolean)

    fun polarFeatures(): PolarSensorFeatures? = null
}

/**
 * Polar-specific streams and offline recording. Null from [HeartRateSensorManager.polarFeatures]
 * when the connected device is a generic HR strap.
 */
interface PolarSensorFeatures {
    val ecg: Flow<EcgSample>
    val acc: Flow<AccSample>

    suspend fun startHrStream()
    suspend fun stopHrStream()
    suspend fun startEcgStream()
    suspend fun stopEcgStream()
    suspend fun startAccStream(sampleRateHz: Int = 50)
    suspend fun stopAccStream()

    /**
     * H10 internal exercise recording / Verity Sense SDK offline recording.
     * See docs/SENSORS.md for device matrix.
     */
    suspend fun startOfflineRecording(types: Set<OfflineRecordingRequest>)
    suspend fun stopOfflineRecording()
    suspend fun listOfflineRecordings(): List<OfflineRecordingEntry>
    suspend fun downloadOfflineRecording(entry: OfflineRecordingEntry): OfflineRecordingPayload
    suspend fun deleteOfflineRecording(entry: OfflineRecordingEntry)
}

enum class OfflineRecordingRequest {
    HeartRate,
    RrInterval,
    Ecg,
    Acc,
    Ppi,
}
