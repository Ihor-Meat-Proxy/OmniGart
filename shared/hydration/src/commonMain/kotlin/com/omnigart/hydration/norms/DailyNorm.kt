package com.omnigart.hydration.norms

/**
 * Biological sex used only for fixed Adequate Intake tables (EFSA / IOM).
 * Prefer user-selectable; do not infer from Health demographics without consent.
 */
enum class NormSex {
    FEMALE,
    MALE,
    UNSPECIFIED,
}

enum class NormMethod {
    /** EFSA NDA 2010 Adequate Intake for total water. Default. */
    EFSA_2010,

    /** IOM 2004 Adequate Intake for total water (US). */
    IOM_2004,

    /**
     * 35 ml × body-mass (kg) as total water.
     * Honest label: weak evidence — a clinical heuristic, not an EFSA/IOM AI.
     */
    WEIGHT_HEURISTIC_35ML_KG,
}

enum class EvidenceStrength {
    ADEQUATE_INTAKE,
    WEAK_HEURISTIC,
}

enum class WorkoutIntensity {
    LIGHT,
    MODERATE,
    HARD,
}

data class NormProfile(
    val sex: NormSex = NormSex.UNSPECIFIED,
    val ageYears: Int? = null,
    val bodyMassKg: Double? = null,
    val pregnant: Boolean = false,
    val lactating: Boolean = false,
    /** Extra millilitres the user attributes to heat (no location APIs). */
    val heatExtraMl: Double = 0.0,
)

data class TrainingFluidAdjustment(
    /** Estimated or measured sweat loss to replace today, millilitres. */
    val sweatLossMl: Double,
    val source: String,
)

data class DailyNormResult(
    val method: NormMethod,
    val evidenceStrength: EvidenceStrength,
    /** Total water AI / heuristic before lifestyle adjustments (ml). */
    val baselineTotalWaterMl: Double,
    /** Assumed food moisture share (0–1). Default 0.20 per EFSA framing. */
    val foodWaterFraction: Double,
    /** Beverage target before pregnancy/lactation/heat/sweat (ml). */
    val baselineBeverageMl: Double,
    val pregnancyExtraMl: Double,
    val lactationExtraMl: Double,
    val heatExtraMl: Double,
    val trainingSweatMl: Double,
    /** Final recommended beverage intake for the day (ml). */
    val beverageTargetMl: Double,
    /** Soft upper bound before hyponatraemia-style warnings (ml). */
    val cautionCeilingMl: Double,
    val methodLabelEnGb: String,
    val notesEnGb: List<String>,
)

data class HyponatraemiaAssessment(
    val warn: Boolean,
    val reasons: List<String>,
    val citation: String,
)

/**
 * Pure daily fluid-norm calculator.
 *
 * Citations:
 * - EFSA NDA (2010) Scientific Opinion on Dietary Reference Values for water.
 *   DOI 10.2903/j.efsa.2010.1459 — AI 2.0 L ♀ / 2.5 L ♂ total water; ~20% from food
 *   is a practical split used here for a beverage target.
 * - IOM (2004) DRI: Water, Potassium, Sodium, Chloride, Sulfate. DOI 10.17226/10925 —
 *   AI 2.7 L ♀ / 3.7 L ♂ (19–30 y) total water.
 * - ACSM (2007) Exercise and Fluid Replacement. DOI 10.1249/mss.0b013e31802ca597.
 * - Hew-Butler et al. (2015) 3rd International EAH Consensus.
 *   DOI 10.1136/bjsports-2015-095004.
 */
object DailyNormCalculator {

    const val DEFAULT_FOOD_WATER_FRACTION = 0.20
    const val EFSA_FEMALE_TOTAL_ML = 2000.0
    const val EFSA_MALE_TOTAL_ML = 2500.0
    const val IOM_FEMALE_TOTAL_ML = 2700.0
    const val IOM_MALE_TOTAL_ML = 3700.0
    const val WEIGHT_ML_PER_KG = 35.0
    const val PREGNANCY_EXTRA_ML = 300.0
    const val LACTATION_EXTRA_ML = 700.0

    /** Absolute daily beverage volume that always triggers a caution (ml). */
    const val ABSOLUTE_CAUTION_ML = 6000.0

    /** Sustained hourly intake that warrants a rate caution (ml/h). */
    const val HOURLY_RATE_CAUTION_ML = 1000.0

    fun defaultMethod(): NormMethod = NormMethod.EFSA_2010

    fun calculate(
        method: NormMethod = defaultMethod(),
        profile: NormProfile,
        training: TrainingFluidAdjustment? = null,
        foodWaterFraction: Double = DEFAULT_FOOD_WATER_FRACTION,
    ): DailyNormResult {
        val fraction = foodWaterFraction.coerceIn(0.0, 0.5)
        val baselineTotal = baselineTotalWaterMl(method, profile)
        val baselineBeverage = baselineTotal * (1.0 - fraction)
        val pregnancyExtra = if (profile.pregnant) PREGNANCY_EXTRA_ML else 0.0
        val lactationExtra = if (profile.lactating) LACTATION_EXTRA_ML else 0.0
        val heat = profile.heatExtraMl.coerceAtLeast(0.0)
        val sweat = training?.sweatLossMl?.coerceAtLeast(0.0) ?: 0.0

        val beverageTarget =
            (baselineBeverage + pregnancyExtra + lactationExtra + heat + sweat)
                .coerceAtLeast(0.0)

        val cautionCeiling =
            maxOf(beverageTarget * 1.5, beverageTarget + 2000.0, ABSOLUTE_CAUTION_ML)
                .coerceAtLeast(beverageTarget)

        val strength =
            when (method) {
                NormMethod.WEIGHT_HEURISTIC_35ML_KG -> EvidenceStrength.WEAK_HEURISTIC
                else -> EvidenceStrength.ADEQUATE_INTAKE
            }

        val notes = buildList {
            add(methodNote(method))
            add(
                "Beverage target assumes ${(fraction * 100).toInt()}% of total water comes from food " +
                    "(practical split aligned with EFSA’s drinks+food framing; DOI 10.2903/j.efsa.2010.1459).",
            )
            if (profile.pregnant) {
                add("Pregnancy: +${PREGNANCY_EXTRA_ML.toInt()} ml/day (EFSA 2010).")
            }
            if (profile.lactating) {
                add("Lactation: +${LACTATION_EXTRA_ML.toInt()} ml/day (EFSA 2010).")
            }
            if (heat > 0) {
                add("Heat adjustment: +${heat.toInt()} ml (user-entered; no location used).")
            }
            if (sweat > 0) {
                add(
                    "Training fluid replacement: +${sweat.toInt()} ml (${training?.source ?: "training"}). " +
                        "ACSM 2007: customise to sweat rate; avoid drinking well above losses " +
                        "(DOI 10.1249/mss.0b013e31802ca597).",
                )
            }
            profile.ageYears?.let { age ->
                when {
                    age < 14 -> add(
                        "Age $age: paediatric AIs differ; this calculator uses adult tables — confirm with a clinician.",
                    )
                    age >= 65 -> add(
                        "Age $age: EFSA adult AI still applies, but thirst can be blunted — prefer scheduled sips in heat/training.",
                    )
                    else -> Unit
                }
            }
            if (method == NormMethod.WEIGHT_HEURISTIC_35ML_KG) {
                add(
                    "Evidence strength: WEAK. 35 ml/kg is a convenience heuristic, not an Adequate Intake from EFSA or IOM.",
                )
            }
        }

        return DailyNormResult(
            method = method,
            evidenceStrength = strength,
            baselineTotalWaterMl = baselineTotal,
            foodWaterFraction = fraction,
            baselineBeverageMl = baselineBeverage,
            pregnancyExtraMl = pregnancyExtra,
            lactationExtraMl = lactationExtra,
            heatExtraMl = heat,
            trainingSweatMl = sweat,
            beverageTargetMl = beverageTarget,
            cautionCeilingMl = cautionCeiling,
            methodLabelEnGb = methodLabel(method),
            notesEnGb = notes,
        )
    }

    fun baselineTotalWaterMl(method: NormMethod, profile: NormProfile): Double =
        when (method) {
            NormMethod.EFSA_2010 ->
                when (profile.sex) {
                    NormSex.FEMALE -> EFSA_FEMALE_TOTAL_ML
                    NormSex.MALE -> EFSA_MALE_TOTAL_ML
                    NormSex.UNSPECIFIED -> (EFSA_FEMALE_TOTAL_ML + EFSA_MALE_TOTAL_ML) / 2.0
                }
            NormMethod.IOM_2004 ->
                when (profile.sex) {
                    NormSex.FEMALE -> IOM_FEMALE_TOTAL_ML
                    NormSex.MALE -> IOM_MALE_TOTAL_ML
                    NormSex.UNSPECIFIED -> (IOM_FEMALE_TOTAL_ML + IOM_MALE_TOTAL_ML) / 2.0
                }
            NormMethod.WEIGHT_HEURISTIC_35ML_KG -> {
                val kg = profile.bodyMassKg
                require(kg != null && kg > 0.0) {
                    "Weight heuristic requires a positive bodyMassKg"
                }
                kg * WEIGHT_ML_PER_KG
            }
        }

    /**
     * ACSM-style sweat rate from guided pre/post weigh-in (litres ≈ kg change).
     * sweat_L = (preKg - postKg) + fluidConsumedL - urineL; rate = sweat_L / durationHours.
     */
    fun measuredSweatLossMl(
        preExerciseMassKg: Double,
        postExerciseMassKg: Double,
        fluidConsumedDuringMl: Double,
        urineDuringMl: Double = 0.0,
    ): Double {
        val deltaKg = preExerciseMassKg - postExerciseMassKg
        val sweatL = deltaKg + (fluidConsumedDuringMl / 1000.0) - (urineDuringMl / 1000.0)
        return (sweatL * 1000.0).coerceAtLeast(0.0)
    }

    /**
     * Weak intensity×duration heuristic when no weigh-in is available.
     * Anchored loosely in ACSM’s observed 0.5–2.0 L/h range for many athletes.
     */
    fun estimatedSweatLossMl(
        durationMinutes: Int,
        intensity: WorkoutIntensity,
    ): TrainingFluidAdjustment {
        val rateLPerHour =
            when (intensity) {
                WorkoutIntensity.LIGHT -> 0.4
                WorkoutIntensity.MODERATE -> 0.8
                WorkoutIntensity.HARD -> 1.2
            }
        val ml = rateLPerHour * 1000.0 * (durationMinutes.coerceAtLeast(0) / 60.0)
        return TrainingFluidAdjustment(
            sweatLossMl = ml,
            source = "intensity_heuristic_${intensity.name.lowercase()}",
        )
    }

    fun assessHyponatraemiaRisk(
        beverageIntakeMl: Double,
        norm: DailyNormResult,
        sustainedHourlyIntakeMl: Double? = null,
        duringExerciseIntakeMl: Double? = null,
        duringExerciseSweatLossMl: Double? = null,
    ): HyponatraemiaAssessment {
        val reasons = mutableListOf<String>()
        if (beverageIntakeMl > norm.cautionCeilingMl) {
            reasons +=
                "Daily beverage intake (${beverageIntakeMl.toInt()} ml) exceeds the caution ceiling " +
                    "(${norm.cautionCeilingMl.toInt()} ml)."
        }
        if (beverageIntakeMl >= ABSOLUTE_CAUTION_ML) {
            reasons +=
                "Intake at or above ${ABSOLUTE_CAUTION_ML.toInt()} ml/day raises exercise-associated " +
                    "hyponatraemia (EAH) risk when fluid exceeds losses."
        }
        sustainedHourlyIntakeMl?.let { rate ->
            if (rate > HOURLY_RATE_CAUTION_ML) {
                reasons +=
                    "Sustained intake above ${HOURLY_RATE_CAUTION_ML.toInt()} ml/h can outpace free-water excretion."
            }
        }
        if (duringExerciseIntakeMl != null && duringExerciseSweatLossMl != null) {
            if (duringExerciseIntakeMl > duringExerciseSweatLossMl * 1.05) {
                reasons +=
                    "Drinking more than measured/estimated sweat loss during exercise is the primary EAH risk pattern; " +
                        "prefer drinking to thirst (Hew-Butler et al. 2015)."
            }
        }
        return HyponatraemiaAssessment(
            warn = reasons.isNotEmpty(),
            reasons = reasons,
            citation =
                "Hew-Butler et al. (2015) 3rd International Exercise-Associated Hyponatremia Consensus. " +
                    "DOI 10.1136/bjsports-2015-095004",
        )
    }

    private fun methodLabel(method: NormMethod): String =
        when (method) {
            NormMethod.EFSA_2010 -> "EFSA 2010 Adequate Intake (default)"
            NormMethod.IOM_2004 -> "IOM 2004 Adequate Intake"
            NormMethod.WEIGHT_HEURISTIC_35ML_KG -> "Weight heuristic 35 ml/kg (weak evidence)"
        }

    private fun methodNote(method: NormMethod): String =
        when (method) {
            NormMethod.EFSA_2010 ->
                "EFSA AI total water: 2.0 L/day (women) / 2.5 L/day (men), moderate climate and activity " +
                    "(DOI 10.2903/j.efsa.2010.1459)."
            NormMethod.IOM_2004 ->
                "IOM AI total water: 2.7 L/day (women) / 3.7 L/day (men), ages 19–30 " +
                    "(DOI 10.17226/10925)."
            NormMethod.WEIGHT_HEURISTIC_35ML_KG ->
                "Uses 35 ml × body mass (kg) as total water. Not an EFSA or IOM Adequate Intake."
        }
}
