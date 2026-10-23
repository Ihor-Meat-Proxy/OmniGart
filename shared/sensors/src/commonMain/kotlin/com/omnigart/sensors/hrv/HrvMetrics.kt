package com.omnigart.sensors.hrv

import kotlin.math.ln
import kotlin.math.sqrt

/**
 * Time-domain HRV metrics on NN (normal-to-normal) intervals in milliseconds.
 *
 * References:
 * - Task Force of the ESC and NASPE. Heart rate variability: standards of measurement,
 *   physiological interpretation and clinical use. Circulation. 1996;93(5):1043–1065.
 *   DOI: [10.1161/01.CIR.93.5.1043](https://doi.org/10.1161/01.CIR.93.5.1043)
 * - Shaffer F, Ginsberg JP. An overview of heart rate variability metrics and norms.
 *   Front Public Health. 2017;5:258. DOI: [10.3389/fpubh.2017.00258](https://doi.org/10.3389/fpubh.2017.00258)
 */
data class HrvTimeDomain(
    val meanRrMs: Double,
    val sdnnMs: Double,
    val rmssdMs: Double,
    val lnRmssd: Double,
    val pnn50Percent: Double,
    val nnCount: Int
)

object HrvMetrics {
    fun timeDomain(nnMs: List<Double>): HrvTimeDomain {
        require(nnMs.size >= 2) { "Need at least 2 NN intervals" }
        require(nnMs.all { it > 0.0 }) { "NN intervals must be positive" }

        val mean = nnMs.average()
        val variance = nnMs.sumOf { (it - mean) * (it - mean) } / (nnMs.size - 1)
        val sdnn = sqrt(variance)

        val successive = nnMs.zipWithNext { a, b -> b - a }
        val rmssd = sqrt(successive.sumOf { it * it } / successive.size)
        val lnRmssd = ln(rmssd.coerceAtLeast(1e-9))
        val pnn50 = 100.0 * successive.count { kotlin.math.abs(it) > 50.0 } / successive.size

        return HrvTimeDomain(
            meanRrMs = mean,
            sdnnMs = sdnn,
            rmssdMs = rmssd,
            lnRmssd = lnRmssd,
            pnn50Percent = pnn50,
            nnCount = nnMs.size
        )
    }
}
