package com.omnigart.nutrition.guardrails

import com.omnigart.nutrition.FloorWarning
import com.omnigart.nutrition.IntakeDay
import com.omnigart.nutrition.NutritionGoalKind
import com.omnigart.nutrition.NutritionSex
import com.omnigart.nutrition.DayLogStatus

/**
 * Safety and wellness guardrails for suggested energy targets.
 *
 * Floor = max(BMR, 1200 women / 1500 men); deficit ≤ 500 kcal/day.
 * No deficits under 18, during pregnancy/breastfeeding, or below BMI 18.5.
 * Low intake for 7+ days → supportive state (helplines UI; option to hide calories).
 */
object Guardrails {

    const val FLOOR_FEMALE_KCAL = 1200.0
    const val FLOOR_MALE_KCAL = 1500.0
    const val FLOOR_UNSPECIFIED_KCAL = 1350.0
    const val MAX_DEFICIT_KCAL = 500.0
    const val BMI_NO_DEFICIT = 18.5
    const val MIN_AGE_FOR_DEFICIT = 18
    const val LOW_INTAKE_DAYS = 7

    fun absoluteFloorKcal(sex: NutritionSex): Double =
        when (sex) {
            NutritionSex.FEMALE -> FLOOR_FEMALE_KCAL
            NutritionSex.MALE -> FLOOR_MALE_KCAL
            NutritionSex.UNSPECIFIED -> FLOOR_UNSPECIFIED_KCAL
        }

    fun effectiveFloorKcal(bmrKcal: Double?, sex: NutritionSex): Double {
        val abs = absoluteFloorKcal(sex)
        return if (bmrKcal == null) abs else maxOf(bmrKcal, abs)
    }

    fun bmi(weightKg: Double, heightCm: Double): Double {
        val m = heightCm / 100.0
        return weightKg / (m * m)
    }

    fun deficitAllowed(
        ageYears: Int?,
        pregnantOrBreastfeeding: Boolean,
        weightKg: Double?,
        heightCm: Double?,
    ): Boolean {
        if (pregnantOrBreastfeeding) return false
        if (ageYears == null || ageYears < MIN_AGE_FOR_DEFICIT) return false
        if (weightKg != null && heightCm != null && heightCm > 0.0) {
            if (bmi(weightKg, heightCm) < BMI_NO_DEFICIT) return false
        }
        return true
    }

    fun apply(
        proposedKcal: Double,
        tdeeKcal: Double,
        bmrKcal: Double?,
        sex: NutritionSex,
        goalKind: NutritionGoalKind,
        ageYears: Int?,
        pregnantOrBreastfeeding: Boolean,
        weightKg: Double?,
        heightCm: Double?,
    ): Pair<Double, FloorWarning?> {
        var kcal = proposedKcal
        val floor = effectiveFloorKcal(bmrKcal, sex)
        val allowDeficit =
            deficitAllowed(ageYears, pregnantOrBreastfeeding, weightKg, heightCm) &&
                goalKind == NutritionGoalKind.LOSE

        if (!allowDeficit && goalKind == NutritionGoalKind.LOSE) {
            kcal = maxOf(kcal, tdeeKcal)
        }

        if (allowDeficit) {
            val minByDeficit = tdeeKcal - MAX_DEFICIT_KCAL
            kcal = maxOf(kcal, minByDeficit)
        }

        val warning = checkFloor(kcal, floor)
        if (kcal < floor) {
            kcal = floor
        }
        return kcal to warning
    }

    /** Explicit check-and-warn helper for the minimum-calorie floor. */
    fun checkFloor(proposedKcal: Double, floorKcal: Double): FloorWarning =
        FloorWarning(
            belowFloor = proposedKcal < floorKcal,
            floorKcal = floorKcal,
            proposedKcal = proposedKcal,
        )

    fun lowIntakeSupportive(intakeDays: List<IntakeDay>, floorKcal: Double): Boolean {
        val recent =
            intakeDays
                .filter {
                    it.status == DayLogStatus.COMPLETE || it.status == DayLogStatus.INCOMPLETE
                }
                .takeLast(LOW_INTAKE_DAYS)
        if (recent.size < LOW_INTAKE_DAYS) return false
        return recent.all { day ->
            val e = day.energyKcal
            e != null && e < floorKcal
        }
    }
}
