package com.omnigart.shared.calc

import com.omnigart.shared.domain.ExerciseLoadSemantics
import com.omnigart.shared.domain.LoadModel
import kotlin.math.max
import kotlin.time.Instant

/**
 * Pure volume-load maths for workout summaries and period stats.
 *
 * Effective load per rep =
 * `bodyweightKg × bodyweightFraction + addedKg − assistanceKg` (clamped ≥ 0)
 * when the exercise [LoadModel] includes body mass; otherwise external load only.
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
    val loadSemantics: ExerciseLoadSemantics = ExerciseLoadSemantics.EXTERNAL_ONLY
) {
    val usesBodyweightLoad: Boolean get() = loadSemantics.usesBodyweight
}

/**
 * One logged set for volume calculation. Independent of persistence so Phase 1 UI / SQL
 * can map into this shape without coupling the calc layer to schema churn.
 */
data class VolumeSetInput(
    val id: String = "",
    val exerciseId: String,
    val reps: Int,
    /** Added external mass (+kg) for BW+external, or implement load for external-only. */
    val addedWeightKg: Double? = null,
    val isWarmup: Boolean = false,
    val completed: Boolean = true,
    /** Direct assistance (−kg), or sum of selected bands. */
    val assistanceKg: Double? = null,
    /** Selected band ids from [com.omnigart.shared.domain.BandEquipmentProfile]. */
    val selectedBandIds: List<String> = emptyList(),
    /**
     * When true and the exercise is unilateral, the set is treated as one side logged
     * per hand/leg and volume counts both sides (×2).
     */
    val loggedPerHand: Boolean = false,
    val rir: Double? = null
) {
    init {
        require(reps >= 0) { "reps must be non-negative" }
        addedWeightKg?.let { require(it >= 0.0) { "addedWeightKg must be non-negative" } }
        assistanceKg?.let { require(it >= 0.0) { "assistanceKg must be non-negative" } }
        rir?.let { require(it in 0.0..10.0) { "rir must be between 0 and 10" } }
    }

    /** Alias used by external-only logging (barbell / DB). */
    val weightKg: Double? get() = addedWeightKg
}

data class VolumeSessionInput(val id: String, val workoutId: String? = null, val startedAt: Instant, val sets: List<VolumeSetInput>)

data class VolumeLoadOptions(
    /** Resolved body mass; null when missing — BW movements then contribute 0 and flag missing. */
    val bodyweightKg: Double? = null,
    /** Exclude warm-up sets unless true. */
    val includeWarmups: Boolean = false,
    val displayUnit: MassUnit = MassUnit.KG,
    /** Band id → estimated assistance kg (from equipment profile). */
    val bandAssistanceKgById: Map<String, Double> = emptyMap()
) {
    init {
        bodyweightKg?.let { require(it > 0.0) { "bodyweightKg must be positive when set" } }
    }
}

data class EffectiveLoad(
    val loadKg: Double,
    /** True when bodyweight × fraction contributed, or fraction basis is estimate. */
    val estimated: Boolean,
    /** True when a BW model needed body mass but none was available. */
    val missingBodyweight: Boolean = false,
    val bodyweightShareKg: Double = 0.0,
    val addedKg: Double = 0.0,
    val assistanceKg: Double = 0.0
)

data class SetVolumeContribution(
    val setId: String,
    val exerciseId: String,
    val included: Boolean,
    val isHardSet: Boolean,
    val effectiveLoadKg: Double,
    val volumeKg: Double,
    val estimated: Boolean,
    val missingBodyweight: Boolean,
    val sideMultiplier: Int
)

data class ExerciseVolume(
    val exerciseId: String,
    val volumeKg: Double,
    val hardSets: Int,
    val estimated: Boolean,
    val missingBodyweight: Boolean = false
)

data class WorkoutVolumeSummary(
    val totalVolumeKg: Double,
    val totalVolumeDisplay: Double,
    val displayUnit: MassUnit,
    val hardSets: Int,
    val estimatedVolumeKg: Double,
    val missingBodyweight: Boolean,
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
    BEST_SESSION_VOLUME,
    /** Max reps with no added weight and no assistance. */
    BEST_BODYWEIGHT_REPS,
    /** Max added external mass on a completed set. */
    BEST_ADDED_WEIGHT,
    /** Lowest assistance mass among assisted working sets (lower is better). */
    LEAST_ASSISTANCE
}

data class PersonalRecord(
    val exerciseId: String,
    val kind: PersonalRecordKind,
    /** Kilograms for mass PRs; rep count for [PersonalRecordKind.BEST_BODYWEIGHT_REPS]. */
    val valueKg: Double,
    val sessionId: String,
    val setId: String? = null,
    val valueReps: Int? = null
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
    /** Body-mass fraction used for BW load models (0 when external-only). */
    fun loadCoefficient(meta: VolumeExerciseMeta): Double =
        if (meta.usesBodyweightLoad) meta.loadSemantics.bodyweightFraction.proportion else 0.0

    fun resolvedAssistanceKg(set: VolumeSetInput, options: VolumeLoadOptions): Double {
        val fromBands =
            set.selectedBandIds.sumOf { id ->
                options.bandAssistanceKgById[id]
                    ?: error("Unknown band id: $id")
            }
        return (set.assistanceKg ?: 0.0) + fromBands
    }

    /**
     * Effective load for one set before the unilateral side multiplier.
     * `max(0, bodyweight × fraction + added − assistance)`.
     */
    fun effectiveLoadKg(set: VolumeSetInput, meta: VolumeExerciseMeta, options: VolumeLoadOptions): EffectiveLoad {
        val added = set.addedWeightKg ?: 0.0
        val assistance = resolvedAssistanceKg(set, options)
        val semantics = meta.loadSemantics

        when (semantics.loadModel) {
            LoadModel.EXTERNAL_ONLY -> {
                val load = max(0.0, added - assistance)
                return EffectiveLoad(
                    loadKg = load,
                    estimated = false,
                    addedKg = added,
                    assistanceKg = assistance
                )
            }
            LoadModel.BODYWEIGHT_PLUS_EXTERNAL,
            LoadModel.BODYWEIGHT_MINUS_ASSISTANCE -> {
                val bw = options.bodyweightKg
                if (bw == null) {
                    return EffectiveLoad(
                        loadKg = 0.0,
                        estimated = true,
                        missingBodyweight = true,
                        addedKg = added,
                        assistanceKg = assistance
                    )
                }
                val fraction = semantics.bodyweightFraction.proportion
                val share = bw * fraction
                val load = max(0.0, share + added - assistance)
                val estimated =
                    semantics.bodyweightFraction.isEstimated || semantics.usesBodyweight
                return EffectiveLoad(
                    loadKg = load,
                    estimated = estimated,
                    missingBodyweight = false,
                    bodyweightShareKg = share,
                    addedKg = added,
                    assistanceKg = assistance
                )
            }
        }
    }

    fun sideMultiplier(set: VolumeSetInput, meta: VolumeExerciseMeta): Int =
        if (meta.unilateral && set.loggedPerHand) 2 else 1

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
                missingBodyweight = false,
                sideMultiplier = sides
            )
        }
        val load = effectiveLoadKg(set, meta, options)
        return SetVolumeContribution(
            setId = set.id,
            exerciseId = set.exerciseId,
            included = true,
            isHardSet = isHardSet(set),
            effectiveLoadKg = load.loadKg,
            volumeKg = load.loadKg * set.reps * sides,
            estimated = load.estimated,
            missingBodyweight = load.missingBodyweight,
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
                        estimated = rows.any { it.included && it.estimated },
                        missingBodyweight = rows.any { it.missingBodyweight }
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
            missingBodyweight = contributions.any { it.missingBodyweight },
            perExercise = perExercise,
            perMuscleVolumeKg = perMuscle,
            setContributions = contributions
        )
    }
}
