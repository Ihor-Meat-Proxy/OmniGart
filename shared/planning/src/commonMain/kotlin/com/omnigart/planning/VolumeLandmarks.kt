package com.omnigart.planning

import com.omnigart.content.Level
import com.omnigart.content.Muscle
import com.omnigart.content.TrainingGoal
import kotlin.math.roundToInt

object VolumeLandmarks {
    private val hypertrophyMuscles = listOf(
        Muscle.QUADRICEPS,
        Muscle.HAMSTRINGS,
        Muscle.GLUTEUS_MAXIMUS,
        Muscle.PECTORALIS_MAJOR,
        Muscle.LATISSIMUS_DORSI,
        Muscle.FRONT_DELTOID,
        Muscle.LATERAL_DELTOID,
        Muscle.REAR_DELTOID,
        Muscle.BICEPS,
        Muscle.TRICEPS,
        Muscle.ERECTOR_SPINAE,
        Muscle.RECTUS_ABDOMINIS
    )

    fun weeklyHardSetTarget(level: Level, goal: TrainingGoal): IntRange = when (goal) {
        TrainingGoal.HYPERTROPHY, TrainingGoal.FAT_LOSS_SUPPORT -> when (level) {
            Level.BEGINNER -> 8..12
            Level.INTERMEDIATE -> 10..16
            Level.ADVANCED -> 12..20
        }
        TrainingGoal.STRENGTH -> when (level) {
            Level.BEGINNER -> 6..10
            Level.INTERMEDIATE -> 8..14
            Level.ADVANCED -> 10..16
        }
        else -> when (level) {
            Level.BEGINNER -> 6..10
            Level.INTERMEDIATE -> 8..12
            Level.ADVANCED -> 8..14
        }
    }

    fun defaultTargets(level: Level, goal: TrainingGoal): List<WeeklyVolumeTarget> {
        val band = weeklyHardSetTarget(level, goal)
        val midpoint = ((band.first + band.last) / 2.0).roundToInt()
        return hypertrophyMuscles.map { WeeklyVolumeTarget(it, midpoint) }
    }

    fun applyReadiness(sets: Int, readiness: ReadinessBand): Int = when (readiness) {
        ReadinessBand.HIGH -> (sets * 1.05).roundToInt()
        ReadinessBand.NORMAL -> sets
        ReadinessBand.LOW -> (sets * 0.8).roundToInt().coerceAtLeast(1)
        ReadinessBand.VERY_LOW -> (sets * 0.5).roundToInt().coerceAtLeast(0)
    }

    fun applyDeload(sets: Int, deload: Boolean): Int = if (deload) (sets * 0.55).roundToInt().coerceAtLeast(1) else sets
}
