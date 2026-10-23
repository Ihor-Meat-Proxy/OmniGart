package com.omnigart.nutrition

/**
 * Primitive-friendly entry points for Swift / simple Android hosts.
 */
object NutritionFacade {

    fun computeForProfile(
        sexCode: String,
        ageYears: Int,
        heightCm: Double,
        weightKg: Double,
        bodyFatPercent: Double,
        hasBodyFat: Boolean,
        goalKindCode: String,
        rateFractionPerWeek: Double,
        targetWeightKg: Double,
        hasTargetWeight: Boolean,
        modeCode: String,
        manualDailyKcal: Double,
        hasManualDaily: Boolean,
        previousTargetKcal: Double,
        hasPreviousTarget: Boolean,
        recentWeightKg: DoubleArray,
        recentWeightEpochMs: LongArray,
        meanSteps: Int,
        dayStartMinutes: Int,
    ): NutritionTargetResult {
        val sex =
            when (sexCode.uppercase()) {
                "MALE" -> NutritionSex.MALE
                "FEMALE" -> NutritionSex.FEMALE
                else -> NutritionSex.UNSPECIFIED
            }
        val kind =
            when (goalKindCode.uppercase()) {
                "LOSE" -> NutritionGoalKind.LOSE
                "GAIN" -> NutritionGoalKind.GAIN
                else -> NutritionGoalKind.MAINTAIN
            }
        val mode =
            when (modeCode.uppercase()) {
                "FLEXIBLE_WEEKLY" -> TargetMode.FLEXIBLE_WEEKLY
                "MANUAL" -> TargetMode.MANUAL
                else -> TargetMode.GUIDED
            }
        val weights =
            if (recentWeightKg.isEmpty()) {
                listOf(WeightPoint(1L, weightKg))
            } else {
                recentWeightKg.indices.map { i ->
                    WeightPoint(
                        epochMs = recentWeightEpochMs.getOrElse(i) { i.toLong() },
                        massKg = recentWeightKg[i],
                    )
                }
            }
        val intake =
            (0 until 7).map { i ->
                IntakeDay("d$i", 2000.0, DayLogStatus.COMPLETE)
            }
        return NutritionTargetEngine.compute(
            NutritionTargetEngine.Input(
                profile =
                    NutritionProfileInput(
                        sex = sex,
                        ageYears = ageYears.takeIf { it > 0 },
                        heightCm = heightCm.takeIf { it > 0.0 },
                        weightKg = weightKg.takeIf { it > 0.0 },
                        bodyFatPercent = if (hasBodyFat) bodyFatPercent else null,
                        isAthleticBodyFat = false,
                        pregnantOrBreastfeeding = false,
                    ),
                goal =
                    NutritionGoalSettings(
                        kind = kind,
                        rateFractionPerWeek = rateFractionPerWeek,
                        targetWeightKg = if (hasTargetWeight) targetWeightKg else null,
                        mode = mode,
                        manualDailyKcal = if (hasManualDaily) manualDailyKcal else null,
                        dayStartMinutes = dayStartMinutes,
                    ),
                intakeDays = intake,
                weights = weights,
                activityDays =
                    listOf(
                        ActivityDay("d0", activeEnergyKcal = null, steps = meanSteps),
                    ),
                previousPublishedTargetKcal = if (hasPreviousTarget) previousTargetKcal else null,
                trustedBodyFat = hasBodyFat,
                weekStartEpochMs = 0L,
                weekEndEpochMs = Long.MAX_VALUE,
            ),
        )
    }

    fun confidenceLabelKey(level: ConfidenceLevel): String =
        when (level) {
            ConfidenceLevel.ROUGH -> "nutrition_confidence_rough"
            ConfidenceLevel.GOOD -> "nutrition_confidence_good"
            ConfidenceLevel.PERSONALISED -> "nutrition_confidence_personalised"
        }

    fun suggestedKcalOrNull(result: NutritionTargetResult): Double =
        result.suggestedDailyKcal ?: -1.0

    fun changeReasonKeys(result: NutritionTargetResult): Array<String> =
        result.changeReasons.map { it.key }.toTypedArray()
}
