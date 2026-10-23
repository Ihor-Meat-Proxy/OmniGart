package com.omnigart.shared.domain

import kotlinx.serialization.Serializable

/**
 * Strength training experience band (4 levels).
 * Thresholds are deliberate Phase 0 placeholders; see docs/OPEN_QUESTIONS.md.
 */
@Serializable
enum class StrengthLevel {
    BEGINNER,
    NOVICE,
    INTERMEDIATE,
    ADVANCED
}

/**
 * Running level: Newbie plus five progressive bands.
 * Classification uses weekly volume (km) and either easy pace (min/km) or 5 km time (minutes),
 * taking the **lower** level when the two signals disagree.
 */
@Serializable
enum class RunningLevel {
    NEWBIE,
    LEVEL_1,
    LEVEL_2,
    LEVEL_3,
    LEVEL_4,
    LEVEL_5
}

/**
 * Swimming level: Newbie plus four progressive bands.
 */
@Serializable
enum class SwimmingLevel {
    NEWBIE,
    LEVEL_1,
    LEVEL_2,
    LEVEL_3,
    LEVEL_4
}

object DisciplineClassifier {
    /**
     * Maps months of consistent training to a strength level.
     *
     * - Beginner: &lt; 6 months
     * - Novice: 6–18 months
     * - Intermediate: 18–48 months
     * - Advanced: 48+ months
     */
    fun classifyStrength(monthsOfTraining: Int): StrengthLevel {
        require(monthsOfTraining >= 0) { "monthsOfTraining must be non-negative" }
        return when {
            monthsOfTraining < 6 -> StrengthLevel.BEGINNER
            monthsOfTraining < 18 -> StrengthLevel.NOVICE
            monthsOfTraining < 48 -> StrengthLevel.INTERMEDIATE
            else -> StrengthLevel.ADVANCED
        }
    }

    /**
     * Classifies running level from weekly kilometres and a pace/time signal.
     *
     * @param weeklyKm average weekly running volume
     * @param easyPaceMinPerKm easy-pace minutes per kilometre (lower is faster); null if unknown
     * @param fiveKmTimeMinutes recent 5 km finish time in minutes; null if unknown
     *
     * When both pace and 5 km time are provided and disagree, the lower (more conservative) level wins.
     * When only volume is known, volume alone is used.
     */
    fun classifyRunning(weeklyKm: Double, easyPaceMinPerKm: Double? = null, fiveKmTimeMinutes: Double? = null): RunningLevel {
        require(weeklyKm >= 0) { "weeklyKm must be non-negative" }
        val fromVolume = runningLevelFromWeeklyKm(weeklyKm)
        val fromPace = easyPaceMinPerKm?.let { runningLevelFromEasyPace(it) }
        val fromFiveKm = fiveKmTimeMinutes?.let { runningLevelFromFiveKm(it) }

        val paceSignals = listOfNotNull(fromPace, fromFiveKm)
        return if (paceSignals.isEmpty()) {
            fromVolume
        } else {
            val paceLevel = paceSignals.minBy { it.ordinal }
            minOf(fromVolume, paceLevel)
        }
    }

    fun classifySwimming(weeklyMetres: Double, canSwimContinuously: Boolean): SwimmingLevel {
        require(weeklyMetres >= 0) { "weeklyMetres must be non-negative" }
        if (!canSwimContinuously || weeklyMetres < 500) return SwimmingLevel.NEWBIE
        return when {
            weeklyMetres < 2_000 -> SwimmingLevel.LEVEL_1
            weeklyMetres < 5_000 -> SwimmingLevel.LEVEL_2
            weeklyMetres < 10_000 -> SwimmingLevel.LEVEL_3
            else -> SwimmingLevel.LEVEL_4
        }
    }

    private fun runningLevelFromWeeklyKm(weeklyKm: Double): RunningLevel = when {
        weeklyKm < 10 -> RunningLevel.NEWBIE
        weeklyKm < 20 -> RunningLevel.LEVEL_1
        weeklyKm < 35 -> RunningLevel.LEVEL_2
        weeklyKm < 50 -> RunningLevel.LEVEL_3
        weeklyKm < 70 -> RunningLevel.LEVEL_4
        else -> RunningLevel.LEVEL_5
    }

    /** Easy pace thresholds (min/km): slower pace → lower level. */
    private fun runningLevelFromEasyPace(minPerKm: Double): RunningLevel = when {
        minPerKm > 7.5 -> RunningLevel.NEWBIE
        minPerKm > 6.5 -> RunningLevel.LEVEL_1
        minPerKm > 5.8 -> RunningLevel.LEVEL_2
        minPerKm > 5.2 -> RunningLevel.LEVEL_3
        minPerKm > 4.7 -> RunningLevel.LEVEL_4
        else -> RunningLevel.LEVEL_5
    }

    /** 5 km time thresholds (minutes). */
    private fun runningLevelFromFiveKm(minutes: Double): RunningLevel = when {
        minutes > 40 -> RunningLevel.NEWBIE
        minutes > 32 -> RunningLevel.LEVEL_1
        minutes > 27 -> RunningLevel.LEVEL_2
        minutes > 24 -> RunningLevel.LEVEL_3
        minutes > 21 -> RunningLevel.LEVEL_4
        else -> RunningLevel.LEVEL_5
    }
}
