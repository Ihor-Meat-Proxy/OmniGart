package com.omnigart.nutrition.activity

import com.omnigart.nutrition.ActivityDay
import com.omnigart.nutrition.ActivitySource

/**
 * Activity energy for TDEE. Uses **either** measured active energy **or** a PAL from step bands —
 * never both, and never stacks workout kcal on top of daily active energy.
 */
object ActivityEnergy {

    const val DEFAULT_ACTIVE_SCALE = 0.80
    const val MIN_ACTIVE_SCALE = 0.75
    const val MAX_ACTIVE_SCALE = 0.85
    const val TEF_FRACTION_OF_INTAKE = 0.10

    data class Result(
        val activityKcal: Double,
        val source: ActivitySource,
        val pal: Double? = null,
        val meanActiveKcal: Double? = null,
    )

    /**
     * @param activeScale design choice in [0.75, 0.85]; applied to 7-day mean measured active energy.
     * @param meanIntakeKcal used only for TEF when measured path is chosen.
     */
    fun resolve(
        last7Days: List<ActivityDay>,
        bmrKcal: Double,
        meanIntakeKcal: Double?,
        activeScale: Double = DEFAULT_ACTIVE_SCALE,
    ): Result {
        val scale = activeScale.coerceIn(MIN_ACTIVE_SCALE, MAX_ACTIVE_SCALE)
        val measured =
            last7Days.mapNotNull { it.activeEnergyKcal }.filter { it >= 0.0 }
        if (measured.isNotEmpty()) {
            val meanActive = measured.average() * scale
            val tef = (meanIntakeKcal ?: 0.0).coerceAtLeast(0.0) * TEF_FRACTION_OF_INTAKE
            return Result(
                activityKcal = meanActive + tef,
                source = ActivitySource.MEASURED_ACTIVE_ENERGY,
                meanActiveKcal = meanActive,
            )
        }

        val steps = last7Days.mapNotNull { it.steps }.filter { it >= 0 }
        if (steps.isNotEmpty()) {
            val meanSteps = steps.average()
            val pal = palFromSteps(meanSteps)
            return Result(
                activityKcal = bmrKcal * (pal - 1.0),
                source = ActivitySource.PAL_STEPS,
                pal = pal,
            )
        }

        return Result(activityKcal = 0.0, source = ActivitySource.NONE)
    }

    fun palFromSteps(meanDailySteps: Double): Double =
        when {
            meanDailySteps < 5_000 -> 1.2
            meanDailySteps < 7_500 -> 1.375
            meanDailySteps < 10_000 -> 1.55
            else -> 1.725
        }

    /** Maintenance-style TDEE from BMR + activity component (PAL or measured). */
    fun maintenanceTdee(bmrKcal: Double, activity: Result): Double =
        when (activity.source) {
            ActivitySource.PAL_STEPS -> bmrKcal * (activity.pal ?: 1.2)
            ActivitySource.MEASURED_ACTIVE_ENERGY -> bmrKcal + activity.activityKcal
            ActivitySource.NONE -> bmrKcal * 1.2
        }
}
