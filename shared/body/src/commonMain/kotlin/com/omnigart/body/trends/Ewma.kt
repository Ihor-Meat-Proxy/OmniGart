package com.omnigart.body.trends

/**
 * Exponentially weighted moving average for body-metric time series.
 *
 * [alpha] close to 1.0 tracks recent values tightly; close to 0.0 smooths more.
 * Default 0.3 balances day-to-day noise (weight, girths) against responsiveness.
 */
object Ewma {
    const val DEFAULT_ALPHA = 0.3

    data class Point(
        val epochMs: Long,
        val value: Double,
    )

    data class SmoothedPoint(
        val epochMs: Long,
        val raw: Double,
        val smoothed: Double,
    )

    fun smooth(
        points: List<Point>,
        alpha: Double = DEFAULT_ALPHA,
    ): List<SmoothedPoint> {
        require(alpha in 0.0..1.0) { "alpha must be in [0, 1]" }
        if (points.isEmpty()) return emptyList()
        val ordered = points.sortedBy { it.epochMs }
        val out = ArrayList<SmoothedPoint>(ordered.size)
        var prev = ordered.first().value
        for (p in ordered) {
            val s = alpha * p.value + (1.0 - alpha) * prev
            out += SmoothedPoint(p.epochMs, p.value, s)
            prev = s
        }
        return out
    }

    /** One-step update when streaming new samples. */
    fun next(previousSmoothed: Double?, observation: Double, alpha: Double = DEFAULT_ALPHA): Double {
        require(alpha in 0.0..1.0)
        return if (previousSmoothed == null) observation
        else alpha * observation + (1.0 - alpha) * previousSmoothed
    }
}
