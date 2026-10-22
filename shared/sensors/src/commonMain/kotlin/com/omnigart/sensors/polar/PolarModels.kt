package com.omnigart.sensors.polar

/**
 * Device / streaming / offline-recording models shared across Polar adapters.
 *
 * Platform packages (iOS SwiftPM, Android library) wrap Polar BLE SDK 8.4.0
 * behind [PolarDeviceClient]. Pure logic (merge, clock offset) lives here.
 */

enum class PolarDeviceType {
    H10,
    VERITY_SENSE,
    OTHER,
}

data class PolarDeviceInfo(
    val deviceId: String,
    val address: String?,
    val name: String?,
    val type: PolarDeviceType,
    val rssi: Int? = null,
    val firmwareVersion: String? = null,
    val batteryPercent: Int? = null,
)

enum class PolarStreamFeature {
    HR,
    RR,
    ECG,
    ACC,
    PPG,
    PPI,
}

data class PolarHrSample(
    val epochMillis: Long,
    val bpm: Int,
    val rrMs: List<Double> = emptyList(),
    /** Contact status when reported by the SDK / HRS flags. */
    val contact: Boolean? = null,
)

data class PolarEcgSample(
    val epochMillis: Long,
    /** Microvolts at the device sample rate (H10 ≈ 130 Hz). */
    val microvolts: Int,
)

/**
 * H10 internal exercise recording sample type.
 *
 * Per Polar BLE SDK [PolarH10OfflineExerciseApi]:
 * - [HR] — heart rate; recording interval applies (typically 1 s).
 * - [RR] — RR intervals; interval parameter is ignored.
 *
 * Product docs emphasise HR at 1 s; the API also exposes RR. The H10 stores
 * **only one** recording at a time.
 */
enum class H10SampleType {
    HR,
    RR,
}

enum class H10RecordingInterval(val seconds: Int) {
    INTERVAL_1_S(1),
    INTERVAL_5_S(5),
    INTERVAL_15_S(15),
    INTERVAL_60_S(60),
}

data class H10RecordingStatus(
    val ongoing: Boolean,
    val entryId: String?,
)

data class OfflineRecordingEntry(
    val id: String,
    val path: String?,
    val sizeBytes: Long?,
    val startedEpochMillis: Long?,
    val sampleType: H10SampleType?,
    val deviceType: PolarDeviceType,
)

data class OfflineHrRecording(
    val entry: OfflineRecordingEntry,
    val samples: List<PolarHrSample>,
)

/**
 * Documented offline storage limits (see docs/sensors/POLAR_OFFLINE.md).
 */
object PolarOfflineLimits {
    /** H10: a single exercise recording slot. */
    const val H10_MAX_RECORDINGS = 1

    /** H10 HR recording sample period emphasised by Polar product docs. */
    const val H10_HR_SAMPLE_PERIOD_SECONDS = 1

    /** Verity Sense offline recording memory limit 1 (bytes). */
    const val VERITY_SENSE_MEMORY_LIMIT_1_BYTES = 2 * 1024 * 1024

    /** Verity Sense offline recording memory limit 2 (bytes). */
    const val VERITY_SENSE_MEMORY_LIMIT_2_BYTES = 300 * 1024

    /** Verity Sense minimum firmware for offline recording API. */
    const val VERITY_SENSE_MIN_FW_OFFLINE = "2.1.0"
}
