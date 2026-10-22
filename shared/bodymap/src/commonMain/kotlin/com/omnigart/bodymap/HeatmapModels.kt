package com.omnigart.bodymap

import com.omnigart.content.Muscle
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class BiologicalSex {
    @SerialName("female") FEMALE,
    @SerialName("male") MALE,
    @SerialName("unspecified") UNSPECIFIED,
}

@Serializable
enum class BodyMapView {
    @SerialName("front") FRONT,
    @SerialName("back") BACK,
}

/**
 * Discrete intensity band for colouring a muscle region.
 * Levels 1–5 map to the colourblind-safe sequential scale in
 * `design/body-map/mapping/intensity-scale.json` (HR-zone Okabe–Ito tokens).
 */
@Serializable
enum class IntensityLevel(val level: Int) {
    IDLE(0),
    L1(1),
    L2(2),
    L3(3),
    L4(4),
    L5(5);

    companion object {
        fun fromLevel(level: Int): IntensityLevel = when (level.coerceIn(0, 5)) {
            0 -> IDLE
            1 -> L1
            2 -> L2
            3 -> L3
            4 -> L4
            5 -> L5
            else -> IDLE
        }
    }
}

@Serializable
data class CompletedSet(
    val exerciseId: String,
    val primaryMuscles: List<Muscle>,
    val secondaryMuscles: List<Muscle> = emptyList(),
    /** True when the set counts as a hard / effective set for volume. */
    val hardSet: Boolean = true,
    val setCount: Int = 1,
)

@Serializable
data class CompletedWorkout(
    val id: String,
    val sets: List<CompletedSet>,
)

@Serializable
data class MuscleLoad(
    val muscle: Muscle,
    /** Effective / fractional hard sets for this muscle. */
    val effectiveSets: Double,
    val intensity: IntensityLevel,
)

@Serializable
data class WorkoutHeatmap(
    val workoutId: String,
    val sex: BiologicalSex,
    val loads: List<MuscleLoad>,
    val maxEffectiveSets: Double,
    val weights: InvolvementWeights,
)

@Serializable
data class WeeklyMuscleVolume(
    val muscle: Muscle,
    val effectiveSets: Double,
    val landmarkTarget: Int?,
    /** effectiveSets / landmark midpoint, or null when no landmark. */
    val ratioToLandmark: Double?,
    val intensity: IntensityLevel,
)

@Serializable
data class WeeklyVolumeHeatmap(
    val sex: BiologicalSex,
    val muscles: List<WeeklyMuscleVolume>,
    val weights: InvolvementWeights,
)

@Serializable
data class ExerciseHighlight(
    val exerciseId: String,
    val primaryMuscles: List<Muscle>,
    val secondaryMuscles: List<Muscle>,
    val views: List<BodyMapView>,
)
