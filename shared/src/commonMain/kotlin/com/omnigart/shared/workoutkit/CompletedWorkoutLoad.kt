package com.omnigart.shared.workoutkit

import com.omnigart.shared.health.HealthWorkout
import com.omnigart.shared.health.WorkoutActivityType
import kotlin.math.max
import kotlin.time.Instant
import kotlinx.serialization.Serializable

/**
 * Lightweight load / recovery signal derived from a completed HealthKit workout.
 *
 * Not a medical TRIMP implementation — a wellness proxy for planner readiness hooks:
 * duration-weighted by a coarse intensity factor from average HR when present.
 */
@Serializable
data class WorkoutLoadContribution(
    val workoutId: String,
    val activityType: WorkoutActivityType,
    val start: Instant,
    val end: Instant,
    val durationSeconds: Long,
    /** Dimensionless load proxy; higher = more session stress. */
    val loadScore: Double,
    val distanceMetres: Double? = null,
    val averageHeartRateBpm: Double? = null,
)

object CompletedWorkoutLoad {
    fun fromHealthWorkout(
        workout: HealthWorkout,
        restingHrBpm: Double? = null,
        maxHrBpm: Double? = null,
    ): WorkoutLoadContribution {
        val durationMs =
            (workout.end.toEpochMilliseconds() - workout.start.toEpochMilliseconds())
                .coerceAtLeast(0L)
        val durationSeconds = durationMs / 1000L
        val intensity = intensityFactor(
            averageHr = workout.averageHeartRateBpm,
            restingHr = restingHrBpm,
            maxHr = maxHrBpm,
        )
        val minutes = durationSeconds / 60.0
        val loadScore = minutes * intensity
        return WorkoutLoadContribution(
            workoutId = workout.id,
            activityType = workout.activityType,
            start = workout.start,
            end = workout.end,
            durationSeconds = durationSeconds,
            loadScore = loadScore,
            distanceMetres = workout.distanceMetres,
            averageHeartRateBpm = workout.averageHeartRateBpm,
        )
    }

    /**
     * Maps average HR into ~0.5–1.5 intensity. When HR or zone anchors are missing,
     * falls back to 1.0 (duration-only).
     */
    fun intensityFactor(
        averageHr: Double?,
        restingHr: Double?,
        maxHr: Double?,
    ): Double {
        if (averageHr == null || averageHr <= 0) return 1.0
        val rest = restingHr?.takeIf { it > 0 }
        val max = maxHr?.takeIf { it > rest ?: 0.0 }
        if (rest != null && max != null && max > rest) {
            val reserve = ((averageHr - rest) / (max - rest)).coerceIn(0.0, 1.5)
            return 0.5 + reserve
        }
        // Crude %HRmax-style fallback when only average HR is known (assume 190 max).
        val assumedMax = max ?: 190.0
        return (0.5 + (averageHr / assumedMax)).coerceIn(0.5, 1.5)
    }

    fun totalLoad(contributions: Collection<WorkoutLoadContribution>): Double =
        contributions.sumOf { max(0.0, it.loadScore) }
}
