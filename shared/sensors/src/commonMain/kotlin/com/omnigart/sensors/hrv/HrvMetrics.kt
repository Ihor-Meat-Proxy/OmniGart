package com.omnigart.sensors.hrv

import kotlin.math.ln
import kotlin.math.sqrt

/**
 * Time-domain HRV metrics from an RR-interval series (milliseconds).
 *
 * - **SDNN** — standard deviation of NN (normalised RR) intervals.
 * - **rMSSD** — root mean square of successive differences.
 * - **ln rMSSD** — natural log of rMSSD (common readiness / parasympathetic index).
 *
 * Population SD (divide by N) is used for SDNN to match common wearable /
 * Kubios “SDNN” reporting on short windows; successive diffs use N−1 pairs.
 */
data class HrvMetrics(
    val sampleCount: Int,
    val meanRrMs: Double,
    val sdnnMs: Double,
    val rmssdMs: Double,
    val lnRmssd: Double,
    val meanHrBpm: Double,
) {
    companion object {
        fun compute(rrMs: List<Double>): HrvMetrics? {
            if (rrMs.size < 2) return null
            require(rrMs.all { it.isFinite() && it > 0.0 }) {
                "RR intervals must be finite and positive"
            }

            val n = rrMs.size
            val mean = rrMs.sum() / n
            val variance = rrMs.sumOf { (it - mean) * (it - mean) } / n
            val sdnn = sqrt(variance)

            val diffs = (1 until n).map { rrMs[it] - rrMs[it - 1] }
            val rmssd = sqrt(diffs.sumOf { it * it } / diffs.size)
            val lnRmssd = ln(rmssd)

            val meanHr = 60_000.0 / mean

            return HrvMetrics(
                sampleCount = n,
                meanRrMs = mean,
                sdnnMs = sdnn,
                rmssdMs = rmssd,
                lnRmssd = lnRmssd,
                meanHrBpm = meanHr,
            )
        }
    }
}
