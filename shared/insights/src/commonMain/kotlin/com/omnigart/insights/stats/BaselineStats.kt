package com.omnigart.insights.stats

import kotlin.math.sqrt

data class RollingBaseline(
    val mean: Double,
    val sd: Double,
    val n: Int,
    val windowDays: Int,
)

object BaselineStats {
    fun mean(values: List<Double>): Double {
        require(values.isNotEmpty()) { "values must not be empty" }
        return values.sum() / values.size
    }

    fun sampleSd(values: List<Double>): Double {
        if (values.size < 2) return 0.0
        val m = mean(values)
        val varSum = values.sumOf { (it - m) * (it - m) }
        return sqrt(varSum / (values.size - 1))
    }

    fun rollingBaseline(valuesNewestFirst: List<Double>, windowDays: Int): RollingBaseline? {
        val window = valuesNewestFirst.take(windowDays).filter { it.isFinite() }
        if (window.isEmpty()) return null
        return RollingBaseline(
            mean = mean(window),
            sd = sampleSd(window),
            n = window.size,
            windowDays = windowDays,
        )
    }

    /** Signed z vs baseline; 0 when sd is ~0. */
    fun zScore(value: Double, baseline: RollingBaseline): Double {
        if (baseline.sd < 1e-9) return 0.0
        return (value - baseline.mean) / baseline.sd
    }
}
