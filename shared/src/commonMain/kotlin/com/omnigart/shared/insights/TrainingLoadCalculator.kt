package com.omnigart.shared.insights

import com.omnigart.shared.health.store.LocalHealthWorkout
import com.omnigart.shared.health.store.LocalHeartRatePoint
import kotlin.math.round

/**
 * Session training-load wellness estimates.
 *
 * Priority: HR zone TRIMP → calorie provider / stored active kcal → duration × RPE.
 * See docs/INSIGHTS_METRICS.md.
 */
object TrainingLoadCalculator {
    const val DEFAULT_SESSION_RPE: Double = 5.0

    data class HrAnchors(val restingBpm: Double, val maxBpm: Double) {
        init {
            require(restingBpm > 0 && maxBpm > restingBpm)
        }
    }

    data class SessionLoadResult(
        val load: Double,
        val source: LoadSource,
        val anaerobicMinutes: Double,
        val highAerobicMinutes: Double,
        val lowAerobicMinutes: Double
    )

    enum class LoadSource {
        HEART_RATE_TRIMP,
        CALORIE_MODEL,
        DURATION_RPE
    }

    fun zoneForHrr(hrr: Double): Int = when {
        hrr < 0.60 -> 1
        hrr < 0.70 -> 2
        hrr < 0.80 -> 3
        hrr < 0.90 -> 4
        else -> 5
    }

    fun zoneWeight(zone: Int): Int = zone.coerceIn(1, 5)

    fun heartRateReserve(hr: Double, anchors: HrAnchors): Double =
        ((hr - anchors.restingBpm) / (anchors.maxBpm - anchors.restingBpm)).coerceIn(0.0, 1.5)

    fun fromHeartRateSeries(series: List<LocalHeartRatePoint>, anchors: HrAnchors, fallbackDurationSeconds: Long): SessionLoadResult? {
        if (series.size < 2) return null
        val ordered = series.sortedBy { it.recordedAt.toEpochMilliseconds() }
        var trimp = 0.0
        var anaerobic = 0.0
        var highAerobic = 0.0
        var lowAerobic = 0.0
        for (i in 0 until ordered.lastIndex) {
            val a = ordered[i]
            val b = ordered[i + 1]
            val dtMin =
                (
                    (b.recordedAt.toEpochMilliseconds() - a.recordedAt.toEpochMilliseconds())
                        .coerceAtLeast(0L) / 60_000.0
                    )
                    .coerceAtMost(5.0)
            if (dtMin <= 0) continue
            val hrr = heartRateReserve(a.beatsPerMinute, anchors)
            val zone = zoneForHrr(hrr)
            trimp += zoneWeight(zone) * dtMin
            when (zone) {
                4, 5 -> anaerobic += dtMin
                3 -> highAerobic += dtMin
                else -> lowAerobic += dtMin
            }
        }
        if (trimp <= 0.0 && fallbackDurationSeconds > 0) {
            // Sparse series: treat whole duration as low aerobic with weight 1.
            val minutes = fallbackDurationSeconds / 60.0
            return SessionLoadResult(
                load = round2(minutes),
                source = LoadSource.HEART_RATE_TRIMP,
                anaerobicMinutes = 0.0,
                highAerobicMinutes = 0.0,
                lowAerobicMinutes = round2(minutes)
            )
        }
        if (trimp <= 0.0) return null
        return SessionLoadResult(
            load = round2(trimp),
            source = LoadSource.HEART_RATE_TRIMP,
            anaerobicMinutes = round2(anaerobic),
            highAerobicMinutes = round2(highAerobic),
            lowAerobicMinutes = round2(lowAerobic)
        )
    }

    fun fromWorkout(
        workout: LocalHealthWorkout,
        anchors: HrAnchors?,
        calorieProvider: CalorieEnergyProvider,
        weightKg: Double?,
        sessionRpe: Double = DEFAULT_SESSION_RPE
    ): SessionLoadResult {
        val durationSec = workout.durationSeconds().coerceAtLeast(0L)
        if (anchors != null && workout.heartRateSeries.isNotEmpty()) {
            fromHeartRateSeries(workout.heartRateSeries, anchors, durationSec)?.let { return it }
        }
        // Average-HR single-point approximation when series missing.
        if (anchors != null) {
            val avg = workout.averageHeartRateBpm
            if (avg != null && avg > 0 && durationSec > 0) {
                val minutes = durationSec / 60.0
                val zone = zoneForHrr(heartRateReserve(avg, anchors))
                val load = zoneWeight(zone) * minutes
                val buckets = when (zone) {
                    4, 5 -> Triple(minutes, 0.0, 0.0)
                    3 -> Triple(0.0, minutes, 0.0)
                    else -> Triple(0.0, 0.0, minutes)
                }
                return SessionLoadResult(
                    load = round2(load),
                    source = LoadSource.HEART_RATE_TRIMP,
                    anaerobicMinutes = round2(buckets.first),
                    highAerobicMinutes = round2(buckets.second),
                    lowAerobicMinutes = round2(buckets.third)
                )
            }
        }

        val storedKcal = workout.activeEnergyKcal?.takeIf { it > 0 }
        val estimatedKcal =
            calorieProvider.estimateActiveKcal(
                activityKey = workout.activityType.name,
                durationSeconds = durationSec,
                weightKg = weightKg,
                averageHeartRateBpm = workout.averageHeartRateBpm
            )
        val kcal = storedKcal ?: estimatedKcal
        if (kcal != null && kcal > 0) {
            val minutes = durationSec / 60.0
            return SessionLoadResult(
                load = round2(kcal / 10.0),
                source = LoadSource.CALORIE_MODEL,
                anaerobicMinutes = 0.0,
                highAerobicMinutes = 0.0,
                lowAerobicMinutes = round2(minutes)
            )
        }

        val minutes = durationSec / 60.0
        val rpe = sessionRpe.coerceIn(0.0, 10.0)
        return SessionLoadResult(
            load = round2(minutes * rpe),
            source = LoadSource.DURATION_RPE,
            anaerobicMinutes = 0.0,
            highAerobicMinutes = 0.0,
            lowAerobicMinutes = round2(minutes)
        )
    }

    private fun round2(v: Double): Double = round(v * 100.0) / 100.0
}
