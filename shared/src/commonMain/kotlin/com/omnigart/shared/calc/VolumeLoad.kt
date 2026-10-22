package com.omnigart.shared.calc

import kotlin.math.max
import kotlin.time.Instant

/**
 * Pure volume-load maths for workout summaries and period stats.
 *
 * Volume load = Σ (effective_load_kg × reps × side_multiplier) over included sets.
 * Canonical mass is kilograms; convert with [MassUnits] for display.
 */
data class VolumeExerciseMeta(
    val exerciseId: String,
    val unilateral: Boolean,
    /** Stable muscle keys matching the exercise content schema (e.g. `pectoralis_major`). */
    val primaryMuscles: List<String>,
    val secondaryMuscles: List<String> = emptyList(),
    /**
     * When true, base load is `bodyweightKg × loadCoefficient` plus optional [VolumeSetInput.weightKg]
     * as added external mass (weighted dips / pull-ups).
     */
    val usesBodyweightLoad: Boolean = false,
    /** Override; null → [BodyweightLoadCoefficients.forExerciseId]. */
    val loadCoefficient: Double? = null
)

/**
 * One logged set for volume calculation. Independent of persistence so Phase 1 UI / SQL
 * can map into this shape without coupling the calc layer to schema churn.
 */
data class VolumeSetInput(
    val id: String = "",
    val exerciseId: String,
    val reps: Int,
    /** External load, or added weight for bodyweight movements (kg). */
    val weightKg: Double? = null,
    val isWarmup: Boolean = false,
    val completed: Boolean = true,
    /** Assistance mass subtracted from effective load (kg). */
    val assistanceKg: Double? = null,
    /**
     * When true and the exercise is unilateral, the set is treated as one side logged
     * per hand/leg and volume counts both sides (×2).
     */
    val loggedPerHand: Boolean = false,
    val rir: Double? = null
) {
    init {
        require(reps >= 0) { "reps must be non-negative" }
        weightKg?.let { require(it >= 0.0) { "weightKg must be non-negative" } }
        assistanceKg?.let { require(it >= 0.0) { "assistanceKg must be non-negative" } }
        rir?.let { require(it in 0.0..10.0) { "rir must be between 0 and 10" } }
    }
}

data class VolumeSessionInput(val id: String, val workoutId: String? = null, val startedAt: Instant, val sets: List<VolumeSetInput>)

data class VolumeLoadOptions(
    val bodyweightKg: Double,
    /** Exclude warm-up sets unless true. */
    val includeWarmups: Boolean = false,
    val displayUnit: MassUnit = MassUnit.KG
) {
    init {
        require(bodyweightKg > 0.0) { "bodyweightKg must be positive" }
    }
}

data class EffectiveLoad(
    val loadKg: Double,
    /** True when bodyweight × coefficient contributed (estimate). */
    val estimated: Boolean
)

data class SetVolumeContribution(
    val setId: String,
    val exerciseId: String,
    val included: Boolean,
    val isHardSet: Boolean,
    val effectiveLoadKg: Double,
    val volumeKg: Double,
    val estimated: Boolean,
    val sideMultiplier: Int
)

data class ExerciseVolume(val exerciseId: String, val volumeKg: Double, val hardSets: Int, val estimated: Boolean)

data class WorkoutVolumeSummary(
    val totalVolumeKg: Double,
    val totalVolumeDisplay: Double,
    val displayUnit: MassUnit,
    val hardSets: Int,
    val estimatedVolumeKg: Double,
    val perExercise: List<ExerciseVolume>,
    /** Full set volume attributed to each primary muscle key (heatmap feed). */
    val perMuscleVolumeKg: Map<String, Double>,
    val setContributions: List<SetVolumeContribution>
)

data class SessionVolumeComparison(
    val currentVolumeKg: Double,
    val previousVolumeKg: Double,
    val volumeDeltaKg: Double,
    val currentHardSets: Int,
    val previousHardSets: Int,
    val hardSetsDelta: Int
)

enum class PersonalRecordKind {
    BEST_E1RM,
    BEST_SET_VOLUME,
    BEST_SESSION_VOLUME
}

data class PersonalRecord(
    val exerciseId: String,
    val kind: PersonalRecordKind,
    val valueKg: Double,
    val sessionId: String,
    val setId: String? = null
)

data class PeriodVolumeStats(
    val totalVolumeKg: Double,
    val hardSets: Int,
    val sessionCount: Int,
    val perExerciseVolumeKg: Map<String, Double>,
    val perMuscleVolumeKg: Map<String, Double>,
    val estimatedVolumeKg: Double
)

object VolumeLoad {
    fun loadCoefficient(meta: VolumeExerciseMeta): Double =
        meta.loadCoefficient ?: BodyweightLoadCoefficients.forExerciseId(meta.exerciseId)

    /**
     * Effective load for one set before the unilateral side multiplier.
     * Assisted: `max(0, base − assistance)`. Bodyweight base is flagged estimated.
     */
    fun effectiveLoadKg(set: VolumeSetInput, meta: VolumeExerciseMeta, bodyweightKg: Double): EffectiveLoad {
        require(bodyweightKg > 0.0)
        val added = set.weightKg ?: 0.0
        val (base, estimated) =
            if (meta.usesBodyweightLoad) {
                bodyweightKg * loadCoefficient(meta) + added to true
            } else {
                added to false
            }
        val assisted = set.assistanceKg?.let { max(0.0, base - it) } ?: base
        return EffectiveLoad(loadKg = assisted, estimated = estimated)
    }

    fun sideMultiplier(set: VolumeSetInput, meta: VolumeExerciseMeta): Int = if (meta.unilateral && set.loggedPerHand) 2 else 1

    /** Hard set: completed, non-warmup, reps ≥ 1. */
    fun isHardSet(set: VolumeSetInput): Boolean = set.completed && !set.isWarmup && set.reps >= 1

    fun shouldInclude(set: VolumeSetInput, includeWarmups: Boolean): Boolean {
        if (!set.completed || set.reps < 1) return false
        if (set.isWarmup && !includeWarmups) return false
        return true
    }

    fun setContribution(set: VolumeSetInput, meta: VolumeExerciseMeta, options: VolumeLoadOptions): SetVolumeContribution {
        val include = shouldInclude(set, options.includeWarmups)
        val sides = sideMultiplier(set, meta)
        if (!include) {
            return SetVolumeContribution(
                setId = set.id,
                exerciseId = set.exerciseId,
                included = false,
                isHardSet = isHardSet(set),
                effectiveLoadKg = 0.0,
                volumeKg = 0.0,
                estimated = false,
                sideMultiplier = sides
            )
        }
        val load = effectiveLoadKg(set, meta, options.bodyweightKg)
        return SetVolumeContribution(
            setId = set.id,
            exerciseId = set.exerciseId,
            included = true,
            isHardSet = isHardSet(set),
            effectiveLoadKg = load.loadKg,
            volumeKg = load.loadKg * set.reps * sides,
            estimated = load.estimated,
            sideMultiplier = sides
        )
    }

    fun summarize(
        sets: List<VolumeSetInput>,
        exercises: Map<String, VolumeExerciseMeta>,
        options: VolumeLoadOptions
    ): WorkoutVolumeSummary {
        val contributions =
            sets.map { set ->
                val meta =
                    exercises[set.exerciseId]
                        ?: error("Missing VolumeExerciseMeta for ${set.exerciseId}")
                setContribution(set, meta, options)
            }

        val perExercise =
            contributions
                .groupBy { it.exerciseId }
                .map { (exerciseId, rows) ->
                    ExerciseVolume(
                        exerciseId = exerciseId,
                        volumeKg = rows.sumOf { it.volumeKg },
                        hardSets = rows.count { it.isHardSet },
                        estimated = rows.any { it.included && it.estimated }
                    )
                }
                .sortedBy { it.exerciseId }

        val perMuscle = linkedMapOf<String, Double>()
        contributions.filter { it.included }.forEach { row ->
            val meta = exercises.getValue(row.exerciseId)
            meta.primaryMuscles.forEach { muscle ->
                perMuscle[muscle] = (perMuscle[muscle] ?: 0.0) + row.volumeKg
            }
        }

        val total = contributions.sumOf { it.volumeKg }
        val estimated = contributions.filter { it.included && it.estimated }.sumOf { it.volumeKg }

        return WorkoutVolumeSummary(
            totalVolumeKg = total,
            totalVolumeDisplay = MassUnits.kgToDisplay(total, options.displayUnit),
            displayUnit = options.displayUnit,
            hardSets = contributions.count { it.isHardSet },
            estimatedVolumeKg = estimated,
            perExercise = perExercise,
            perMuscleVolumeKg = perMuscle,
            setContributions = contributions
        )
    }
}
