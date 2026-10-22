package com.omnigart.sensors.polar

/**
 * Platform-implemented Polar BLE SDK façade.
 *
 * iOS (`ios/Packages/Sensors`) and Android (`android/sensors`) provide
 * adapters over Polar BLE SDK **8.4.0**. Shared code never imports the SDK.
 *
 * Suspend / callback style is intentionally simple so either Rx (SDK native)
 * or structured concurrency wrappers can implement it.
 */
interface PolarDeviceClient {
    suspend fun searchDevices(): List<PolarDeviceInfo>

    suspend fun connect(deviceId: String)

    suspend fun disconnect(deviceId: String)

    suspend fun deviceInfo(deviceId: String): PolarDeviceInfo

    /** Start live HR (+ RR when available). Emissions delivered via [listener]. */
    fun startHrStreaming(deviceId: String, listener: (PolarHrSample) -> Unit)

    fun stopHrStreaming(deviceId: String)

    /**
     * ECG stream — H10 only (≈130 Hz). Returns false if unsupported.
     */
    fun startEcgStreaming(deviceId: String, listener: (PolarEcgSample) -> Unit): Boolean

    fun stopEcgStreaming(deviceId: String)

    // --- H10 offline exercise recording (PolarH10OfflineExerciseApi) ---

    suspend fun startH10Recording(
        deviceId: String,
        exerciseId: String,
        sampleType: H10SampleType,
        interval: H10RecordingInterval = H10RecordingInterval.INTERVAL_1_S,
    )

    suspend fun stopH10Recording(deviceId: String)

    suspend fun h10RecordingStatus(deviceId: String): H10RecordingStatus

    suspend fun listH10Recordings(deviceId: String): List<OfflineRecordingEntry>

    suspend fun downloadH10Recording(
        deviceId: String,
        entry: OfflineRecordingEntry,
    ): OfflineHrRecording

    suspend fun deleteH10Recording(deviceId: String, entry: OfflineRecordingEntry)

    // --- Verity Sense / watches: PolarOfflineRecordingApi (optional) ---

    suspend fun listOfflineRecordings(deviceId: String): List<OfflineRecordingEntry> =
        emptyList()
}

/**
 * Stub client for shared-module tests and hosts without Polar hardware.
 */
class StubPolarDeviceClient(
    private val devices: List<PolarDeviceInfo> = emptyList(),
    private var recordings: MutableList<OfflineHrRecording> = mutableListOf(),
) : PolarDeviceClient {
    var connectedId: String? = null
        private set
    var recordingOngoing: Boolean = false
    private var lastExerciseId: String? = null

    override suspend fun searchDevices(): List<PolarDeviceInfo> = devices

    override suspend fun connect(deviceId: String) {
        connectedId = deviceId
    }

    override suspend fun disconnect(deviceId: String) {
        if (connectedId == deviceId) connectedId = null
    }

    override suspend fun deviceInfo(deviceId: String): PolarDeviceInfo =
        devices.firstOrNull { it.deviceId == deviceId }
            ?: PolarDeviceInfo(deviceId, null, null, PolarDeviceType.OTHER)

    override fun startHrStreaming(deviceId: String, listener: (PolarHrSample) -> Unit) = Unit

    override fun stopHrStreaming(deviceId: String) = Unit

    override fun startEcgStreaming(deviceId: String, listener: (PolarEcgSample) -> Unit): Boolean =
        devices.any { it.deviceId == deviceId && it.type == PolarDeviceType.H10 }

    override fun stopEcgStreaming(deviceId: String) = Unit

    override suspend fun startH10Recording(
        deviceId: String,
        exerciseId: String,
        sampleType: H10SampleType,
        interval: H10RecordingInterval,
    ) {
        require(recordings.isEmpty() || !recordingOngoing) {
            "H10 supports only one recording; stop/delete existing first"
        }
        recordingOngoing = true
        lastExerciseId = exerciseId
    }

    override suspend fun stopH10Recording(deviceId: String) {
        recordingOngoing = false
    }

    override suspend fun h10RecordingStatus(deviceId: String): H10RecordingStatus =
        H10RecordingStatus(recordingOngoing, lastExerciseId)

    override suspend fun listH10Recordings(deviceId: String): List<OfflineRecordingEntry> =
        recordings.map { it.entry }

    override suspend fun downloadH10Recording(
        deviceId: String,
        entry: OfflineRecordingEntry,
    ): OfflineHrRecording =
        recordings.first { it.entry.id == entry.id }

    override suspend fun deleteH10Recording(deviceId: String, entry: OfflineRecordingEntry) {
        recordings.removeAll { it.entry.id == entry.id }
    }

    fun seedRecording(recording: OfflineHrRecording) {
        recordings += recording
    }
}
