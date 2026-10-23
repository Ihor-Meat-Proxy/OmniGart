package com.omnigart.activity.run

import kotlin.math.roundToInt

/**
 * Running pace, kilometre/mile splits, and cadence.
 *
 * Pace is elapsed time per unit distance. Cadence is steps per minute
 * (SPM). Splits partition a distance–time series into fixed distance bins.
 */
data class DistancePoint(
    /** Metres from session start. */
    val distanceMetres: Double,
    /** Elapsed seconds from session start. */
    val elapsedSeconds: Double,
)

data class Pace(
    /** Seconds per kilometre. */
    val secondsPerKm: Double,
    /** Seconds per mile (1609.344 m). */
    val secondsPerMile: Double,
) {
    val minutesPerKm: Double get() = secondsPerKm / 60.0
    val minutesPerMile: Double get() = secondsPerMile / 60.0

    fun formatMetric(): String = formatPace(secondsPerKm)
    fun formatImperial(): String = formatPace(secondsPerMile)

    companion object {
        const val METRES_PER_MILE = 1609.344

        fun fromSpeedMetresPerSecond(speedMs: Double): Pace? {
            if (speedMs <= 0.0 || !speedMs.isFinite()) return null
            val secPerKm = 1000.0 / speedMs
            val secPerMile = METRES_PER_MILE / speedMs
            return Pace(secPerKm, secPerMile)
        }

        fun fromDistanceAndTime(distanceMetres: Double, elapsedSeconds: Double): Pace? {
            if (distanceMetres <= 0.0 || elapsedSeconds <= 0.0) return null
            return fromSpeedMetresPerSecond(distanceMetres / elapsedSeconds)
        }

        fun formatPace(secondsPerUnit: Double): String {
            val total = secondsPerUnit.roundToInt().coerceAtLeast(0)
            val m = total / 60
            val s = total % 60
            val ss = if (s < 10) "0$s" else "$s"
            return "$m:$ss"
        }
    }
}

data class Split(
    val index: Int,
    val startMetres: Double,
    val endMetres: Double,
    val durationSeconds: Double,
    val pace: Pace,
)

object PaceCalculator {
    fun overallPace(points: List<DistancePoint>): Pace? {
        if (points.size < 2) return null
        val first = points.first()
        val last = points.last()
        return Pace.fromDistanceAndTime(
            last.distanceMetres - first.distanceMetres,
            last.elapsedSeconds - first.elapsedSeconds,
        )
    }

    /**
     * Fixed-distance splits (default 1000 m). Partial final split included when
     * [includePartial] is true and residual distance > 0.
     */
    fun splits(
        points: List<DistancePoint>,
        splitDistanceMetres: Double = 1000.0,
        includePartial: Boolean = true,
    ): List<Split> {
        require(splitDistanceMetres > 0.0)
        if (points.size < 2) return emptyList()

        val result = mutableListOf<Split>()
        var splitIndex = 0
        var cursor = 0
        var target = points.first().distanceMetres + splitDistanceMetres

        while (cursor < points.lastIndex) {
            val start = points[cursor]
            val endIdx = points.indexOfFirst { it.distanceMetres >= target }
            if (endIdx < 0) break
            val end = interpolateAtDistance(points, cursor, endIdx, target)
            val duration = end.elapsedSeconds - start.elapsedSeconds
            val pace = Pace.fromDistanceAndTime(splitDistanceMetres, duration) ?: break
            result += Split(
                index = splitIndex++,
                startMetres = start.distanceMetres,
                endMetres = target,
                durationSeconds = duration,
                pace = pace,
            )
            // Advance cursor to the point at/after target.
            cursor = if (points[endIdx].distanceMetres == target) endIdx else endIdx
            // Re-seat start at interpolated target for next split continuity.
            target += splitDistanceMetres
        }

        if (includePartial) {
            val last = points.last()
            val covered = result.lastOrNull()?.endMetres ?: points.first().distanceMetres
            val residual = last.distanceMetres - covered
            if (residual > 1.0) {
                val startTime = if (result.isEmpty()) {
                    points.first().elapsedSeconds
                } else {
                    interpolateAtDistance(
                        points,
                        0,
                        points.lastIndex,
                        covered,
                    ).elapsedSeconds
                }
                val duration = last.elapsedSeconds - startTime
                val pace = Pace.fromDistanceAndTime(residual, duration)
                if (pace != null) {
                    result += Split(
                        index = splitIndex,
                        startMetres = covered,
                        endMetres = last.distanceMetres,
                        durationSeconds = duration,
                        pace = pace,
                    )
                }
            }
        }
        return result
    }

    private fun interpolateAtDistance(
        points: List<DistancePoint>,
        fromIdx: Int,
        toIdx: Int,
        distance: Double,
    ): DistancePoint {
        val a = points[(toIdx - 1).coerceAtLeast(fromIdx)]
        val b = points[toIdx]
        if (b.distanceMetres == a.distanceMetres) return b
        val t = ((distance - a.distanceMetres) / (b.distanceMetres - a.distanceMetres))
            .coerceIn(0.0, 1.0)
        return DistancePoint(
            distanceMetres = distance,
            elapsedSeconds = a.elapsedSeconds + t * (b.elapsedSeconds - a.elapsedSeconds),
        )
    }
}

object CadenceCalculator {
    /**
     * Cadence (steps/min) from step event timestamps in epoch millis or
     * session-relative millis.
     */
    fun fromStepTimestamps(
        stepTimestampsMillis: List<Long>,
        windowMillis: Long = 10_000L,
    ): Double? {
        if (stepTimestampsMillis.size < 2) return null
        val sorted = stepTimestampsMillis.sorted()
        val end = sorted.last()
        val start = end - windowMillis
        val inWindow = sorted.filter { it >= start }
        if (inWindow.size < 2) return null
        val span = (inWindow.last() - inWindow.first()).toDouble()
        if (span <= 0.0) return null
        val steps = inWindow.size - 1
        return steps * 60_000.0 / span
    }

    /** Cadence from total steps over elapsed seconds. */
    fun fromStepCount(stepCount: Int, elapsedSeconds: Double): Double? {
        if (stepCount <= 0 || elapsedSeconds <= 0.0) return null
        return stepCount * 60.0 / elapsedSeconds
    }
}
