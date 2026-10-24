package com.omnigart.nutrition.tdee

import com.omnigart.nutrition.DayLogStatus
import com.omnigart.nutrition.IntakeDay
import com.omnigart.nutrition.WeightPoint

/**
 * Data-sufficiency rules for adaptive updates.
 *
 * Needs food logged on at least 4 of 7 days and at least 1 weigh-in a week.
 * Empty days are skipped; incomplete days are excluded (or flagged for the check-in).
 * When data is insufficient, pause updates and keep the last estimate; surface a logging-break notice.
 */
object DataSufficiency {

    const val MIN_FOOD_DAYS_PER_WEEK = 4
    const val MIN_WEIGH_INS_PER_WEEK = 1

    data class WeekAssessment(
        val foodDaysLogged: Int,
        val weighIns: Int,
        val incompleteDays: List<String>,
        val emptyDaysSkipped: Int,
        val sufficient: Boolean,
        val loggingBreak: Boolean,
    )

    fun assessWeek(
        intakeDays: List<IntakeDay>,
        weights: List<WeightPoint>,
        weekStartEpochMs: Long,
        weekEndEpochMs: Long,
    ): WeekAssessment {
        val inWeek =
            intakeDays.filter { day ->
                // Hosts supply already-windowed days; we still filter by status logic.
                true
            }
        val foodLogged =
            inWeek.count {
                it.status == DayLogStatus.COMPLETE || it.status == DayLogStatus.FASTING
            }
        val incomplete =
            inWeek.filter { it.status == DayLogStatus.INCOMPLETE }.map { it.dayKey }
        val emptySkipped = inWeek.count { it.status == DayLogStatus.EMPTY }
        val weighIns =
            weights.count { it.epochMs in weekStartEpochMs until weekEndEpochMs }
        val sufficient =
            foodLogged >= MIN_FOOD_DAYS_PER_WEEK && weighIns >= MIN_WEIGH_INS_PER_WEEK
        val loggingBreak = foodLogged == 0 && weighIns == 0
        return WeekAssessment(
            foodDaysLogged = foodLogged,
            weighIns = weighIns,
            incompleteDays = incomplete,
            emptyDaysSkipped = emptySkipped,
            sufficient = sufficient,
            loggingBreak = loggingBreak,
        )
    }

    /** Exclude incomplete and empty days from mean-intake calculations. */
    fun daysForMeanIntake(days: List<IntakeDay>): List<IntakeDay> =
        days.filter {
            when (it.status) {
                DayLogStatus.COMPLETE, DayLogStatus.FASTING -> true
                DayLogStatus.INCOMPLETE, DayLogStatus.EMPTY -> false
            }
        }

    fun markIncompleteIfPartial(
        energyKcal: Double?,
        expectedMealsLogged: Int,
        expectedMeals: Int,
        userMarkedFasting: Boolean,
    ): DayLogStatus {
        if (userMarkedFasting) return DayLogStatus.FASTING
        if (energyKcal == null || expectedMealsLogged <= 0) return DayLogStatus.EMPTY
        if (expectedMealsLogged < expectedMeals) return DayLogStatus.INCOMPLETE
        return DayLogStatus.COMPLETE
    }
}
