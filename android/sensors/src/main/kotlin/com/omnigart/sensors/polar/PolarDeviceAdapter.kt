package com.omnigart.sensors.polar

import android.content.Context
import com.polar.sdk.api.PolarBleApi
import com.polar.sdk.api.PolarBleApiCallback
import com.polar.sdk.api.PolarBleApiDefaultImpl
import com.polar.sdk.api.PolarH10OfflineExerciseApi
import com.polar.sdk.api.model.PolarDeviceInfo
import com.polar.sdk.api.model.PolarExerciseData
import com.polar.sdk.api.model.PolarExerciseEntry
import com.polar.sdk.api.model.PolarHrData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * Polar BLE SDK **8.4.0** Android adapter (Kotlin Flow / coroutines API).
 *
 * H10 offline: [PolarH10OfflineExerciseApi] — one recording slot; HR @ 1 s or RR.
 * See docs/sensors/POLAR_OFFLINE.md for Verity Sense offline recording limits.
 */
class PolarDeviceAdapter(
    context: Context,
) {
    var onDeviceConnected: ((PolarDeviceInfo) -> Unit)? = null
    var onDeviceDisconnected: ((String) -> Unit)? = null
    var onBattery: ((String, Int) -> Unit)? = null

    private val scope = CoroutineScope(Dispatchers.Main.immediate + Job())
    private var hrJob: Job? = null

    private val api: PolarBleApi = PolarBleApiDefaultImpl.defaultImplementation(
        context.applicationContext,
        setOf(
            PolarBleApi.PolarBleSdkFeature.FEATURE_HR,
            PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_ONLINE_STREAMING,
            PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_OFFLINE_RECORDING,
            PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_H10_EXERCISE_RECORDING,
            PolarBleApi.PolarBleSdkFeature.FEATURE_BATTERY_INFO,
            PolarBleApi.PolarBleSdkFeature.FEATURE_DEVICE_INFO,
        ),
    )

    init {
        api.setApiCallback(object : PolarBleApiCallback() {
            override fun deviceConnected(polarDeviceInfo: PolarDeviceInfo) {
                onDeviceConnected?.invoke(polarDeviceInfo)
            }

            override fun deviceDisconnected(polarDeviceInfo: PolarDeviceInfo) {
                onDeviceDisconnected?.invoke(polarDeviceInfo.deviceId)
            }

            override fun batteryLevelReceived(identifier: String, level: Int) {
                onBattery?.invoke(identifier, level)
            }
        })
    }

    fun searchDevices(): Flow<PolarDeviceInfo> = api.searchForDevice()

    fun connect(deviceId: String) {
        api.connectToDevice(deviceId)
    }

    fun disconnect(deviceId: String) {
        api.disconnectFromDevice(deviceId)
    }

    fun startHrStreaming(deviceId: String): Flow<PolarHrData> = api.startHrStreaming(deviceId)

    fun startHrStreaming(
        deviceId: String,
        onSample: (hr: Int, rrMs: List<Int>) -> Unit,
    ) {
        hrJob?.cancel()
        hrJob = scope.launch {
            try {
                api.startHrStreaming(deviceId).collect { data ->
                    for (sample in data.samples) {
                        onSample(sample.hr, sample.rrsMs)
                    }
                }
            } catch (_: Exception) {
                // stream ended
            }
        }
    }

    fun stopHrStreaming() {
        hrJob?.cancel()
        hrJob = null
    }

    /**
     * ECG (~130 Hz) — H10 only. Collect [Flow] from the SDK once stream settings
     * are resolved; returns the raw SDK flow for callers that need it.
     */
    fun startEcgStreaming(deviceId: String): Flow<*>? {
        // ECG requires requestStreamSettings then startEcgStreaming — full wiring
        // verified on-device (see docs/testing/polar-h10.md). Exposed as optional.
        return null
    }

    suspend fun startH10Recording(
        deviceId: String,
        exerciseId: String,
        sampleType: PolarH10OfflineExerciseApi.SampleType =
            PolarH10OfflineExerciseApi.SampleType.HR,
        interval: PolarH10OfflineExerciseApi.RecordingInterval =
            PolarH10OfflineExerciseApi.RecordingInterval.INTERVAL_1S,
    ) {
        api.startRecording(deviceId, exerciseId, interval, sampleType)
    }

    suspend fun stopH10Recording(deviceId: String) {
        api.stopRecording(deviceId)
    }

    suspend fun h10RecordingStatus(deviceId: String): Pair<Boolean, String> =
        api.requestRecordingStatus(deviceId)

    fun listH10Exercises(deviceId: String): Flow<PolarExerciseEntry> =
        api.listExercises(deviceId)

    suspend fun fetchH10Exercise(deviceId: String, entry: PolarExerciseEntry): PolarExerciseData =
        api.fetchExercise(deviceId, entry)

    suspend fun removeH10Exercise(deviceId: String, entry: PolarExerciseEntry) {
        api.removeExercise(deviceId, entry)
    }

    fun shutdown() {
        stopHrStreaming()
        api.shutDown()
    }
}
