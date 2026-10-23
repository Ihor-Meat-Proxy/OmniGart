package com.omnigart.nutrition

import com.omnigart.nutrition.activity.ActivityEnergy
import com.omnigart.nutrition.bmr.BmrSelector
import com.omnigart.nutrition.confidence.Confidence
import com.omnigart.nutrition.goal.ExerciseCalorieDiscount
import com.omnigart.nutrition.goal.GoalTargets
import com.omnigart.nutrition.guardrails.Guardrails
import com.omnigart.nutrition.macros.MacroTargets
import com.omnigart.nutrition.tdee.AdaptiveTdee
import com.omnigart.nutrition.tdee.DataSufficiency
import com.omnigart.nutrition.targets.WeekdayTargets
import com.omnigart.nutrition.water.WaterGoal

/**
 * Orchestrates BMR → activity → adaptive/energy-balance TDEE → goal → guardrails → macros.
 */
object NutritionTargetEngine {

    data class Input(
        val profile: NutritionProfileInput,
        val goal: NutritionGoalSettings,
        val intakeDays: List<IntakeDay>,
        val weights: List<WeightPoint>,
        val activityDays: List<ActivityDay>,
        val weekdayPattern: WeekdayPattern = WeekdayPattern(),
        val flexibleDaySplits: Map<Int, Double> = emptyMap(),
        val previousPublishedTargetKcal: Double? = null,
        val exerciseKcalToday: Double = 0.0,
        val trainingHoursToday: Double = 0.0,
        val trustedBodyFat: Boolean = false,
        val weekStartEpochMs: Long = 0L,
        val weekEndEpochMs: Long = Long.MAX_VALUE,
        val weeklyLossFractions: List<Double> = emptyList(),
        val activeScale: Double = ActivityEnergy.DEFAULT_ACTIVE_SCALE,
    )

    fun compute(input: Input): NutritionTargetResult {
        val reasons = mutableListOf<ChangeReason>()
        val weightKg = input.profile.weightKg
        if (weightKg == null || weightKg <= 0.0) {
            return NutritionTargetResult(
                estimatedTdeeKcal = null,
                suggestedDailyKcal = null,
                changeReasons = listOf(ChangeReason("nutrition_reason_no_weight")),
                dataInsufficient = true,
            )
        }

        val sufficiency =
            DataSufficiency.assessWeek(
                intakeDays = input.intakeDays,
                weights = input.weights,
                weekStartEpochMs = input.weekStartEpochMs,
                weekEndEpochMs = input.weekEndEpochMs,
            )

        if (!sufficiency.sufficient && input.previousPublishedTargetKcal != null) {
            reasons += ChangeReason("nutrition_reason_paused_insufficient")
            if (sufficiency.loggingBreak) {
                reasons += ChangeReason("nutrition_reason_logging_break")
            }
            return NutritionTargetResult(
                estimatedTdeeKcal = input.previousPublishedTargetKcal,
                suggestedDailyKcal = input.previousPublishedTargetKcal,
                confidence = ConfidenceLevel.GOOD,
                changeReasons = reasons,
                dataInsufficient = true,
                loggingBreak = sufficiency.loggingBreak,
                pausedKeepingLastEstimate = true,
            )
        }

        val bmr =
            BmrSelector.select(
                weightKg = weightKg,
                heightCm = input.profile.heightCm,
                ageYears = input.profile.ageYears,
                sex = input.profile.sex,
                bodyFatPercent = input.profile.bodyFatPercent,
                trustedBodyFat = input.trustedBodyFat,
                athletic = input.profile.isAthleticBodyFat,
            )
        if (bmr == null) {
            return NutritionTargetResult(
                estimatedTdeeKcal = null,
                suggestedDailyKcal = null,
                changeReasons = listOf(ChangeReason("nutrition_reason_no_weight")),
            )
        }
        if (bmr.roughEstimate) {
            reasons += ChangeReason("nutrition_reason_rough_bmr")
        }

        val usableIntake = DataSufficiency.daysForMeanIntake(input.intakeDays)
        val meanIntake =
            usableIntake.mapNotNull { it.energyKcal }.takeIf { it.isNotEmpty() }?.average()

        val activity =
            ActivityEnergy.resolve(
                last7Days = input.activityDays.takeLast(7),
                bmrKcal = bmr.bmrKcal,
                meanIntakeKcal = meanIntake,
                activeScale = input.activeScale,
            )
        val baselineTdee = ActivityEnergy.maintenanceTdee(bmr.bmrKcal, activity)

        val ewma =
            AdaptiveTdee.ewmaAdaptive(
                intakeDays = usableIntake,
                weights = input.weights,
            )
        val energyBalance =
            AdaptiveTdee.energyBalance21(
                intakeDays = usableIntake,
                weights = input.weights,
            )

        var tdee = baselineTdee
        when {
            energyBalance != null && energyBalance.eligible -> {
                val blended =
                    AdaptiveTdee.blendWithBaseline(
                        baselineTdee,
                        energyBalance.expenditureKcal,
                        blendWeight = 0.7,
                    )
                tdee = blended
                reasons += ChangeReason("nutrition_reason_energy_balance")
            }
            ewma != null && ewma.eligible -> {
                tdee =
                    AdaptiveTdee.blendWithBaseline(
                        baselineTdee,
                        ewma.adaptiveTdeeKcal,
                        ewma.blendWeight,
                    )
                reasons += ChangeReason("nutrition_reason_adaptive_ewma")
            }
            else -> reasons += ChangeReason("nutrition_reason_baseline")
        }

        var suggested =
            when (input.goal.mode) {
                TargetMode.MANUAL ->
                    input.goal.manualDailyKcal ?: GoalTargets.applyGoalToTdee(tdee, weightKg, input.goal)
                TargetMode.FLEXIBLE_WEEKLY, TargetMode.GUIDED ->
                    GoalTargets.applyGoalToTdee(tdee, weightKg, input.goal)
            }

        if (GoalTargets.shouldAutoRaiseTarget(input.weeklyLossFractions)) {
            suggested += 100.0
            reasons += ChangeReason("nutrition_reason_auto_raise")
        }

        if (input.goal.addExerciseCalories && input.exerciseKcalToday > 0.0) {
            suggested +=
                ExerciseCalorieDiscount.creditedKcal(
                    exerciseKcal = input.exerciseKcalToday,
                    bodyFatPercent = input.profile.bodyFatPercent,
                    base = input.goal.exerciseDiscountBase,
                )
            reasons += ChangeReason("nutrition_reason_exercise_credit")
        }

        val (guarded, floorWarning) =
            Guardrails.apply(
                proposedKcal = suggested,
                tdeeKcal = tdee,
                bmrKcal = bmr.bmrKcal,
                sex = input.profile.sex,
                goalKind = input.goal.kind,
                ageYears = input.profile.ageYears,
                pregnantOrBreastfeeding = input.profile.pregnantOrBreastfeeding,
                weightKg = weightKg,
                heightCm = input.profile.heightCm,
            )
        suggested = guarded
        if (floorWarning?.belowFloor == true) {
            reasons += ChangeReason("nutrition_reason_floor")
        }

        suggested =
            AdaptiveTdee.applyWeeklyCap(
                previousTargetKcal = input.previousPublishedTargetKcal,
                proposedKcal = suggested,
            )

        val weekday =
            WeekdayTargets.forMode(
                mode = input.goal.mode,
                meanDailyKcal = suggested,
                pattern = input.weekdayPattern,
                flexibleWeeklyBudgetKcal = input.goal.flexibleWeeklyBudgetKcal,
                flexibleSplits = input.flexibleDaySplits,
                manualDailyKcal = input.goal.manualDailyKcal,
            )

        // Re-apply floor per weekday for Flexible mode (guardrails stay ON).
        val floor = Guardrails.effectiveFloorKcal(bmr.bmrKcal, input.profile.sex)
        val weekdayGuarded =
            weekday.mapValues { (_, v) ->
                val capped = maxOf(v, floor)
                if (input.goal.mode != TargetMode.MANUAL) {
                    val minByDeficit = tdee - Guardrails.MAX_DEFICIT_KCAL
                    maxOf(capped, minByDeficit)
                } else {
                    capped
                }
            }

        val macros =
            MacroTargets.compute(
                energyKcal = suggested,
                weightKg = weightKg,
                heightCm = input.profile.heightCm,
                goalKind = input.goal.kind,
                targetWeightKg = input.goal.targetWeightKg,
            )

        val water =
            WaterGoal.litres(
                sex = input.profile.sex,
                weightKg = weightKg,
                trainingHoursToday = input.trainingHoursToday,
            )

        val confidence =
            Confidence.level(
                hasWeight = true,
                roughBmr = bmr.roughEstimate,
                adaptiveEligible = ewma?.eligible == true,
                energyBalanceEligible = energyBalance?.eligible == true,
                intakeDays = input.intakeDays,
                weighIns = input.weights,
            )

        val lowIntake = Guardrails.lowIntakeSupportive(input.intakeDays, floor)

        if (sufficiency.loggingBreak) {
            reasons += ChangeReason("nutrition_reason_logging_break")
        }

        return NutritionTargetResult(
            estimatedTdeeKcal = tdee,
            suggestedDailyKcal = suggested,
            weekdayTargetsKcal = weekdayGuarded,
            proteinG = macros.proteinG,
            fatG = macros.fatG,
            carbsG = macros.carbsG,
            fibreG = macros.fibreG,
            waterLitres = water,
            bmrKcal = bmr.bmrKcal,
            bmrMethod = bmr.method,
            activitySource = activity.source,
            confidence = confidence,
            changeReasons = reasons,
            floorWarning = floorWarning,
            lowIntakeSupportive = lowIntake,
            dataInsufficient = !sufficiency.sufficient,
            loggingBreak = sufficiency.loggingBreak,
            pausedKeepingLastEstimate = false,
            labelRoughEstimate = bmr.roughEstimate,
        )
    }
}
