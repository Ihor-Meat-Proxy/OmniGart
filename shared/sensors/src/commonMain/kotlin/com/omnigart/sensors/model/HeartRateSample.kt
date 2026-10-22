package com.omnigart.sensors.model

/**
 * A single heart-rate sample in a unified timeline (epoch millis, UTC).
 */
data class HeartRateSample(
    val epochMillis: Long,
    val bpm: Int,
    val source: HeartRateSource,
    /** Optional RR intervals (ms) associated with this sample. */
    val rrIntervalsMs: List<Double> = emptyList(),
)

enum class HeartRateSource {
    /** Live BLE Heart Rate Service (0x180D). */
    BLE_HRS,
    /** Polar SDK live stream. */
    POLAR_LIVE,
    /** Polar H10 / Verity Sense offline recording download. */
    POLAR_OFFLINE,
    /** Watch / phone workout recording. */
    WATCH_WORKOUT,
    /** Already-merged / exported series. */
    MERGED,
}
