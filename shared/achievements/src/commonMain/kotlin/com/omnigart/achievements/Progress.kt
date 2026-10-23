package com.omnigart.achievements

import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Serializable
data class AchievementProgress(
    val achievementId: String,
    val currentValue: Double = 0.0,
    val targetValue: Double,
    val unlocked: Boolean = false,
    val unlockedAt: Instant? = null,
    val unlockCount: Int = 0,
    /** For streaks: last calendar day (ISO) that contributed. */
    val lastStreakDay: String? = null,
    val currentStreak: Int = 0,
    /** ISO day keys that contributed (for windowed counters / distinct sets). */
    val contributingDays: Set<String> = emptySet(),
    val contributingWeeks: Set<String> = emptySet(),
    val contributingMonths: Map<String, Double> = emptyMap(),
    val contributingSites: Set<String> = emptySet(),
    val contributingDisciplinesByWeek: Map<String, Set<String>> = emptyMap(),
    val freezeDaysUsed: Set<String> = emptySet(),
)

@Serializable
data class ProgressSnapshot(
    val achievementId: String,
    val currentValue: Double,
    val targetValue: Double,
    val ratio: Double,
    val unlocked: Boolean,
    val unlockedAt: Instant?,
    val nextInFamily: NextTierProgress?,
)

@Serializable
data class NextTierProgress(
    val achievementId: String,
    val currentValue: Double,
    val targetValue: Double,
    val ratio: Double,
)

data class UnlockNotification(
    val achievementId: String,
    val unlockedAt: Instant,
    val hidden: Boolean,
)

data class EvaluationResult(
    val ignoredDuplicate: Boolean = false,
    val unlocks: List<UnlockNotification> = emptyList(),
    /** When non-null, UI should show one summary instead of per-unlock toasts. */
    val batchSummaryCount: Int? = null,
)

data class BackfillResult(
    val processedEvents: Int,
    val skippedDuplicates: Int,
    val unlocks: List<UnlockNotification>,
    val batchSummaryCount: Int,
)
