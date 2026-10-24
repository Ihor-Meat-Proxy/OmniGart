package com.omnigart.nutrition.confidence

import com.omnigart.nutrition.ConfidenceLevel
import com.omnigart.nutrition.DayLogStatus
import com.omnigart.nutrition.IntakeDay
import com.omnigart.nutrition.WeightPoint

/**
 * Confidence label for the suggested target: rough / good / personalised.
 */
object Confidence {

    fun level(
        hasWeight: Boolean,
        roughBmr: Boolean,
        adaptiveEligible: Boolean,
        energyBalanceEligible: Boolean,
        intakeDays: List<IntakeDay>,
        weighIns: List<WeightPoint>,
    ): ConfidenceLevel {
        if (!hasWeight || roughBmr) return ConfidenceLevel.ROUGH
        val completeLast28 =
            intakeDays
                .filter { it.status == DayLogStatus.COMPLETE }
                .takeLast(28)
                .size
        val weighLast28 = weighIns.size
        return when {
            (adaptiveEligible || energyBalanceEligible) && completeLast28 >= 20 && weighLast28 >= 10 ->
                ConfidenceLevel.PERSONALISED
            completeLast28 >= 7 && weighLast28 >= 3 -> ConfidenceLevel.GOOD
            else -> ConfidenceLevel.ROUGH
        }
    }
}
