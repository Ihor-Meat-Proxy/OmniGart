package com.omnigart.sensors.model

import kotlinx.datetime.Instant

enum class OfflineRecordingDataType {
    HeartRate,
    RrInterval,
    Ecg,
    Acc,
    Ppi,
    Ppg,
    Gyro,
    Magnetometer,
    Unknown,
}

data class OfflineRecordingEntry(
    val id: String,
    val deviceId: String,
    val path: String? = null,
    val sizeBytes: Long? = null,
    val date: Instant? = null,
    val dataTypes: Set<OfflineRecordingDataType> = emptySet(),
)

data class OfflineRecordingPayload(
    val entry: OfflineRecordingEntry,
    val heartRateBpm: List<Pair<Instant, Int>> = emptyList(),
    val rrIntervalsMs: List<Pair<Instant?, Double>> = emptyList(),
    val ecg: List<EcgSample> = emptyList(),
    val acc: List<AccSample> = emptyList(),
)

/**
 * Watch/phone swim workout markers used to time-align an offline HR recording.
 */
data class SwimWorkoutMarkers(
    val workoutId: String,
    val startedAt: Instant,
    val endedAt: Instant,
    val lapEnds: List<Instant> = emptyList(),
)

data class MergedWorkoutHeartRate(
    val series: WorkoutHeartRateSeries,
    val alignmentOffsetMs: Long,
    val matchedSampleCount: Int,
    val notes: List<String> = emptyList(),
)
