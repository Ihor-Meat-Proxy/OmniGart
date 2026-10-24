package com.omnigart.shared.insights

import kotlin.math.sqrt

/**
 * Banister-style fitness (CTL), fatigue (ATL) and form (TSB) wellness estimates.
 * See docs/INSIGHTS_METRICS.md.
 */
object FitnessBanister {
    const val CTL_TAU_DAYS: Double = 42.0
    const val ATL_TAU_DAYS: Double = 7.0
    const val DEFAULT_PROJECT_DAYS: Int = 14

    data class DayPoint(
        /** Epoch day index (arbitrary origin; must be contiguous after fill). */
        val dayIndex: Long,
        val dailyLoad: Double,
        val ctl: Double,
        val atl: Double,
        val tsb: Double,
        val ctlLow: Double,
        val ctlHigh: Double,
        val projected: Boolean = false
    )

    data class SeriesResult(val points: List<DayPoint>, val currentCtl: Double, val currentAtl: Double, val currentTsb: Double)

    /**
     * @param dailyLoadsNewestLast map of dayIndex → load for days that have data;
     *        missing days inside the span are treated as 0.
     */
    fun compute(dailyLoadsByDayIndex: Map<Long, Double>, projectDays: Int = DEFAULT_PROJECT_DAYS): SeriesResult {
        if (dailyLoadsByDayIndex.isEmpty()) {
            return SeriesResult(emptyList(), 0.0, 0.0, 0.0)
        }
        val first = dailyLoadsByDayIndex.keys.minOrNull()!!
        val last = dailyLoadsByDayIndex.keys.maxOrNull()!!
        var ctl = 0.0
        var atl = 0.0
        val residuals = ArrayDeque<Double>(42)
        val points = mutableListOf<DayPoint>()

        fun step(day: Long, load: Double, projected: Boolean) {
            ctl += (load - ctl) / CTL_TAU_DAYS
            atl += (load - atl) / ATL_TAU_DAYS
            val residual = load - ctl
            residuals.addLast(residual)
            if (residuals.size > 42) residuals.removeFirst()
            val band = residualBand(residuals)
            points +=
                DayPoint(
                    dayIndex = day,
                    dailyLoad = load,
                    ctl = ctl,
                    atl = atl,
                    tsb = ctl - atl,
                    ctlLow = (ctl - band).coerceAtLeast(0.0),
                    ctlHigh = ctl + band,
                    projected = projected
                )
        }

        for (day in first..last) {
            step(day, dailyLoadsByDayIndex[day] ?: 0.0, projected = false)
        }
        val endCtl = ctl
        val endAtl = atl
        val endTsb = ctl - atl
        if (projectDays > 0) {
            for (offset in 1..projectDays) {
                step(last + offset, 0.0, projected = true)
            }
            // Restore "current" to last non-projected values.
            return SeriesResult(points, endCtl, endAtl, endTsb)
        }
        return SeriesResult(points, endCtl, endAtl, endTsb)
    }

    private fun residualBand(residuals: Collection<Double>): Double {
        if (residuals.size < 3) return 5.0
        val mean = residuals.sum() / residuals.size
        val variance = residuals.sumOf {
            val d = it - mean
            d * d
        } / residuals.size
        return sqrt(variance).coerceAtLeast(2.0)
    }
}
