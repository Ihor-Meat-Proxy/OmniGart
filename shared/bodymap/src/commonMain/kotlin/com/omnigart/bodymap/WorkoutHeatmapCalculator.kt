package com.omnigart.bodymap

import com.omnigart.content.Level
import com.omnigart.content.Muscle
import com.omnigart.content.TrainingGoal
import com.omnigart.planning.VolumeLandmarks

/**
 * Computes per-muscle training load for a completed workout (or week of workouts)
 * using configurable primary/secondary involvement weights.
 */
object WorkoutHeatmapCalculator {

    fun accumulate(
        workouts: List<CompletedWorkout>,
        weights: InvolvementWeights = InvolvementWeights.FractionalPelland2025,
    ): Map<Muscle, Double> {
        val loads = mutableMapOf<Muscle, Double>()
        for (workout in workouts) {
            for (set in workout.sets) {
                if (!set.hardSet || set.setCount <= 0) continue
                val n = set.setCount.toDouble()
                val primaryDelta = n * weights.primary
                if (primaryDelta > 0.0) {
                    for (m in set.primaryMuscles) {
                        // FULL_BODY is retained as its own bucket; region expansion is render-time.
                        loads[m] = (loads[m] ?: 0.0) + primaryDelta
                    }
                }
                val secondaryDelta = n * weights.secondary
                if (secondaryDelta > 0.0) {
                    for (m in set.secondaryMuscles) {
                        loads[m] = (loads[m] ?: 0.0) + secondaryDelta
                    }
                }
            }
        }
        return loads
    }

    fun workoutHeatmap(
        workout: CompletedWorkout,
        sex: BiologicalSex,
        weights: InvolvementWeights = InvolvementWeights.FractionalPelland2025,
    ): WorkoutHeatmap {
        val loads = accumulate(listOf(workout), weights)
        val max = loads.values.maxOrNull() ?: 0.0
        val muscleLoads = loads.entries
            .sortedByDescending { it.value }
            .map { (muscle, value) ->
                MuscleLoad(
                    muscle = muscle,
                    effectiveSets = value,
                    intensity = IntensityNormalizer.levelFor(value, max),
                )
            }
        return WorkoutHeatmap(
            workoutId = workout.id,
            sex = sex,
            loads = muscleLoads,
            maxEffectiveSets = max,
            weights = weights,
        )
    }

    fun weeklyVolumeHeatmap(
        workouts: List<CompletedWorkout>,
        sex: BiologicalSex,
        level: Level,
        goal: TrainingGoal,
        weights: InvolvementWeights = InvolvementWeights.FractionalPelland2025,
    ): WeeklyVolumeHeatmap {
        val loads = accumulate(workouts, weights)
        val landmarks = VolumeLandmarks.defaultTargets(level, goal)
            .associate { it.muscle to it.hardSets }
        val muscles = (loads.keys + landmarks.keys).toSet().sortedBy { it.name }
        val rows = muscles.map { muscle ->
            val effective = loads[muscle] ?: 0.0
            val target = landmarks[muscle]
            val ratio = if (target != null && target > 0) effective / target.toDouble() else null
            WeeklyMuscleVolume(
                muscle = muscle,
                effectiveSets = effective,
                landmarkTarget = target,
                ratioToLandmark = ratio,
                intensity = if (ratio != null) {
                    IntensityNormalizer.levelForLandmarkRatio(ratio)
                } else {
                    val max = loads.values.maxOrNull() ?: 0.0
                    IntensityNormalizer.levelFor(effective, max)
                },
            )
        }
        return WeeklyVolumeHeatmap(sex = sex, muscles = rows, weights = weights)
    }
}
