package com.omnigart.nutrition.targets

import com.omnigart.nutrition.TargetMode
import com.omnigart.nutrition.WeekdayPattern

/**
 * Per-weekday energy targets (training vs rest) and flexible weekly budget splitting.
 */
object WeekdayTargets {

    /** Extra kcal on training days relative to rest when distributing a weekly mean. */
    const val DEFAULT_TRAINING_BONUS_KCAL = 200.0

    /**
     * Build 7 daily targets from a guided mean. Training days get [trainingBonusKcal] more;
     * rest days get less so the weekly mean is preserved.
     */
    fun fromGuidedMean(
        meanDailyKcal: Double,
        pattern: WeekdayPattern,
        trainingBonusKcal: Double = DEFAULT_TRAINING_BONUS_KCAL,
    ): Map<Int, Double> {
        val trainingCount = pattern.trainingDays.count { it }
        if (trainingCount == 0 || trainingCount == 7) {
            return (0..6).associateWith { meanDailyKcal }
        }
        val restCount = 7 - trainingCount
        // mean = (trainingCount*(rest+bonus) + restCount*rest) / 7
        // → rest = mean - trainingCount*bonus/7
        val rest = meanDailyKcal - trainingCount * trainingBonusKcal / 7.0
        val training = rest + trainingBonusKcal
        return (0..6).associateWith { day ->
            if (pattern.trainingDays[day]) training else rest
        }
    }

    /**
     * Flexible weekly budget: user supplies per-day splits (must sum ≈ weekly budget).
     * Missing days are filled evenly from the remainder.
     */
    fun fromFlexibleBudget(
        weeklyBudgetKcal: Double,
        userSplits: Map<Int, Double>,
    ): Map<Int, Double> {
        val filled = userSplits.filterKeys { it in 0..6 }.mapValues { it.value.coerceAtLeast(0.0) }
        val assigned = filled.values.sum()
        val missing = (0..6).filter { it !in filled }
        if (missing.isEmpty()) {
            // Rescale to weekly budget if user over/under-shoots slightly.
            val scale = if (assigned > 0) weeklyBudgetKcal / assigned else 1.0
            return filled.mapValues { it.value * scale }
        }
        val remainder = (weeklyBudgetKcal - assigned).coerceAtLeast(0.0)
        val each = remainder / missing.size
        return (0..6).associateWith { day ->
            filled[day] ?: each
        }
    }

    fun forMode(
        mode: TargetMode,
        meanDailyKcal: Double,
        pattern: WeekdayPattern,
        flexibleWeeklyBudgetKcal: Double?,
        flexibleSplits: Map<Int, Double>,
        manualDailyKcal: Double?,
    ): Map<Int, Double> =
        when (mode) {
            TargetMode.GUIDED -> fromGuidedMean(meanDailyKcal, pattern)
            TargetMode.FLEXIBLE_WEEKLY -> {
                val budget = flexibleWeeklyBudgetKcal ?: (meanDailyKcal * 7.0)
                fromFlexibleBudget(budget, flexibleSplits)
            }
            TargetMode.MANUAL -> {
                val daily = manualDailyKcal ?: meanDailyKcal
                (0..6).associateWith { daily }
            }
        }
}
