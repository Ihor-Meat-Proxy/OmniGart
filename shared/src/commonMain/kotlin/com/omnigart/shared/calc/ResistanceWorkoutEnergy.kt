package com.omnigart.shared.calc

import com.omnigart.shared.domain.BiologicalSex
import kotlin.math.max
import kotlin.math.min

/**
 * Resistance-aware wellness energy estimate.
 *
 * Prefer a Keytel-style heart-rate model over active (non-rest) intervals when HR samples
 * are available. Otherwise estimate from mechanical work proxies: load relative to body mass
 * and estimated 1RM, reps, time under tension, set density / rest, RPE, and an exercise
 * muscle-mass factor. A modest EPOC term is added. Basal energy is Mifflin–St Jeor BMR
 * prorated over the whole session window.
 *
 * **Wellness estimates only — not medical claims.** See `docs/CALORIE_MODEL.md`.
 */
object ResistanceWorkoutEnergy {
    /** Default seconds under tension per rep when tempo is unknown. */
    const val DEFAULT_SECONDS_PER_REP: Double = 3.0

    /** Fraction of active work energy added as EPOC at moderate intensity. */
    const val EPOC_BASE_FRACTION: Double = 0.07

    /** Extra EPOC fraction per RPE point above 5 (capped). */
    const val EPOC_PER_RPE_ABOVE_5: Double = 0.012

    /**
     * kcal scale for the work-based proxy (tuned for sanity ranges in tests).
     * Applied to Σ (load/BW × reps × relative intensity × muscle × RPE factors).
     */
    const val WORK_KCAL_SCALE: Double = 7.0

    /**
     * Keytel et al. (2005) energy expenditure from heart rate during submaximal exercise.
     * Returns kilocalories for the sample window. Formula outputs kJ/min; ÷ 4.184 → kcal/min.
     *
     * Applied only over [activeSeconds] (work intervals), not rest.
     */
    fun keytelActiveKcal(averageHrBpm: Double, weightKg: Double, ageYears: Int, sex: BiologicalSex, activeSeconds: Double): Double {
        require(averageHrBpm > 0.0) { "averageHrBpm must be positive" }
        require(weightKg > 0.0) { "weightKg must be positive" }
        require(ageYears > 0) { "ageYears must be positive" }
        require(activeSeconds >= 0.0) { "activeSeconds must be non-negative" }
        if (activeSeconds == 0.0) return 0.0
        val kjPerMin =
            when (sex) {
                BiologicalSex.MALE ->
                    -55.0969 + 0.6309 * averageHrBpm + 0.1988 * weightKg + 0.2017 * ageYears
                BiologicalSex.FEMALE ->
                    -20.4022 + 0.4472 * averageHrBpm - 0.1263 * weightKg + 0.074 * ageYears
            }
        val kcalPerMin = max(0.0, kjPerMin / 4.184)
        return kcalPerMin * (activeSeconds / 60.0)
    }

    /** Muscle-mass involvement factor for a movement family (compound / isolation × region). */
    fun muscleMassFactor(kind: MuscleMassKind): Double = kind.factor

    /**
     * Work-based active kilocalories from completed sets (no HR).
     * Warm-up sets contribute at a reduced rate; drop/failure slightly increase the intensity term.
     */
    fun workBasedActiveKcal(sets: List<ResistanceEnergySetInput>, bodyWeightKg: Double, restSecondsTotal: Double = 0.0): Double {
        require(bodyWeightKg > 0.0) { "bodyWeightKg must be positive" }
        require(restSecondsTotal >= 0.0)
        if (sets.isEmpty()) return 0.0

        var workKcal = 0.0
        var tutSeconds = 0.0
        var meanRpe = 0.0
        var rpeCount = 0

        for (set in sets) {
            if (!set.completed || set.reps < 1) continue
            val load = max(0.0, set.loadKg)
            val e1rm = set.estimated1RmKg?.takeIf { it > 0 } ?: OneRepMax.epley(max(load, 0.5), set.reps)
            val relative = if (e1rm > 0) min(1.2, load / e1rm) else 0.5
            val loadToBw = load / bodyWeightKg
            val tut = set.reps * (set.secondsPerRep ?: DEFAULT_SECONDS_PER_REP)
            tutSeconds += tut
            val rpe = set.rpe ?: rpeFromRir(set.rir)
            rpe?.let {
                meanRpe += it
                rpeCount++
            }
            val rpeFactor = 1.0 + ((rpe ?: 6.0) - 5.0) * 0.06
            val warmupFactor = if (set.isWarmup) 0.35 else 1.0
            val failureBump = if (set.isFailure) 1.08 else 1.0
            val dropBump = if (set.isDropSet) 1.05 else 1.0
            val muscle = muscleMassFactor(set.muscleMassKind)
            val mechanical =
                loadToBw * set.reps.toDouble() * relative * muscle * rpeFactor *
                    warmupFactor * failureBump * dropBump
            // Mild TUT boost: longer tempos cost a little more than the default 3 s/rep.
            val tutBoost = 0.85 + 0.15 * (tut / (set.reps * DEFAULT_SECONDS_PER_REP)).coerceIn(0.5, 2.0)
            workKcal += mechanical * WORK_KCAL_SCALE * tutBoost
        }

        val density =
            if (tutSeconds + restSecondsTotal > 0) {
                tutSeconds / (tutSeconds + restSecondsTotal)
            } else {
                0.5
            }
        // Sparse sessions (long rests) burn a little less continuous metabolic cost.
        val densityFactor = 0.85 + 0.3 * density.coerceIn(0.0, 1.0)
        val avgRpe = if (rpeCount > 0) meanRpe / rpeCount else 6.0
        val epocFraction =
            (EPOC_BASE_FRACTION + EPOC_PER_RPE_ABOVE_5 * max(0.0, avgRpe - 5.0)).coerceIn(0.05, 0.18)

        val activeWithoutEpoc = workKcal * densityFactor
        return activeWithoutEpoc * (1.0 + epocFraction)
    }

    /**
     * Full breakdown: HR path when [averageHrBpm] is usable, else work-based; basal from BMR.
     */
    fun estimate(input: ResistanceEnergyInput): WorkoutEnergyBreakdown {
        require(input.weightKg > 0.0)
        require(input.heightCm > 0.0)
        require(input.ageYears > 0)
        require(input.sessionDurationSeconds >= 0.0)

        val durationHours = input.sessionDurationSeconds / 3600.0
        val activeSeconds =
            input.activeWorkSeconds
                ?: input.sets.sumOf { set ->
                    if (!set.completed || set.reps < 1) {
                        0.0
                    } else {
                        set.reps * (set.secondsPerRep ?: DEFAULT_SECONDS_PER_REP)
                    }
                }.coerceAtMost(input.sessionDurationSeconds)

        val hr = input.averageHrBpm
        val active =
            if (hr != null && hr >= 90.0 && activeSeconds > 0.0) {
                keytelActiveKcal(
                    averageHrBpm = hr,
                    weightKg = input.weightKg,
                    ageYears = input.ageYears,
                    sex = input.sex,
                    activeSeconds = activeSeconds
                )
            } else {
                workBasedActiveKcal(
                    sets = input.sets,
                    bodyWeightKg = input.weightKg,
                    restSecondsTotal = input.restSecondsTotal
                ).let { work ->
                    // Floor: never below a light MET floor for very empty logs with duration.
                    if (work > 0.0 || input.sets.isNotEmpty()) {
                        work
                    } else {
                        WorkoutEnergyEstimate.activeKcal(
                            met = WorkoutEnergyEstimate.STRENGTH_MET * 0.6,
                            weightKg = input.weightKg,
                            durationHours = durationHours
                        )
                    }
                }
            }

        val bmr =
            input.bmrKcalPerDay
                ?: Bmr.mifflinStJeor(input.weightKg, input.heightCm, input.ageYears, input.sex)
        val basal = WorkoutEnergyEstimate.basalKcal(bmr, durationHours)
        return WorkoutEnergyBreakdown(
            activeKcal = active,
            basalKcal = basal,
            totalKcal = WorkoutEnergyEstimate.totalKcal(active, basal)
        )
    }

    /** Map RIR to an approximate RPE (0–10 Borg CR-10 style). */
    fun rpeFromRir(rir: Double?): Double? {
        if (rir == null) return null
        return (10.0 - rir).coerceIn(1.0, 10.0)
    }
}

/** Movement family for muscle-mass energy scaling. */
enum class MuscleMassKind(val factor: Double) {
    LOWER_COMPOUND(1.0),
    UPPER_COMPOUND(0.85),
    LOWER_ISOLATION(0.55),
    UPPER_ISOLATION(0.45),
    CORE(0.35),
    UNKNOWN(0.65)
}

data class ResistanceEnergySetInput(
    val loadKg: Double,
    val reps: Int,
    val estimated1RmKg: Double? = null,
    val rpe: Double? = null,
    val rir: Double? = null,
    val isWarmup: Boolean = false,
    val isDropSet: Boolean = false,
    val isFailure: Boolean = false,
    val completed: Boolean = true,
    val muscleMassKind: MuscleMassKind = MuscleMassKind.UNKNOWN,
    val secondsPerRep: Double? = null
)

data class ResistanceEnergyInput(
    val weightKg: Double,
    val heightCm: Double,
    val ageYears: Int,
    val sex: BiologicalSex,
    val sessionDurationSeconds: Double,
    val sets: List<ResistanceEnergySetInput> = emptyList(),
    val restSecondsTotal: Double = 0.0,
    /** Average HR over active intervals; when ≥ 90 bpm, Keytel path is used. */
    val averageHrBpm: Double? = null,
    /** Explicit active work duration; defaults to Σ TUT from sets. */
    val activeWorkSeconds: Double? = null,
    /** Onboarding BMR when already known; else Mifflin–St Jeor. */
    val bmrKcalPerDay: Double? = null
)

/**
 * Infer [MuscleMassKind] from primary muscle keys and a coarse compound hint.
 * Adapters for content packs should prefer explicit metadata when the library PR lands.
 */
object MuscleMassKindInfer {
    private val lower =
        setOf(
            "quadriceps",
            "gluteus_maximus",
            "hamstrings",
            "gastrocnemius",
            "soleus",
            "adductors",
            "hip_flexors"
        )
    private val upper =
        setOf(
            "pectoralis_major",
            "latissimus_dorsi",
            "triceps",
            "biceps",
            "front_deltoid",
            "side_deltoid",
            "rear_deltoid",
            "trapezius",
            "rhomboids",
            "forearms"
        )
    private val core = setOf("rectus_abdominis", "obliques", "erector_spinae", "transverse_abdominis")

    fun infer(primaryMuscles: List<String>, compound: Boolean = primaryMuscles.size >= 2): MuscleMassKind {
        val keys = primaryMuscles.map { it.lowercase() }.toSet()
        val isCore = keys.any { it in core } && keys.none { it in lower || it in upper }
        if (isCore) return MuscleMassKind.CORE
        val isLower = keys.any { it in lower }
        val isUpper = keys.any { it in upper }
        return when {
            isLower && compound -> MuscleMassKind.LOWER_COMPOUND
            isUpper && compound -> MuscleMassKind.UPPER_COMPOUND
            isLower -> MuscleMassKind.LOWER_ISOLATION
            isUpper -> MuscleMassKind.UPPER_ISOLATION
            else -> MuscleMassKind.UNKNOWN
        }
    }
}
