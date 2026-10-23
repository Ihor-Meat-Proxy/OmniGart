package com.omnigart.achievements.engine

import com.omnigart.achievements.model.TierId
import kotlinx.serialization.Serializable

@Serializable
data class TierProgress(
    val currentTier: TierId? = null,
    val nextTier: TierId? = null,
    val currentValue: Double = 0.0,
    val nextThreshold: Double? = null,
    /** 0.0–1.0 toward next tier; 1.0 if max tier reached. */
    val progressRatio: Double = 0.0,
)

@Serializable
data class AchievementProgress(
    val achievementId: String,
    val value: Double = 0.0,
    val unlockedTier: List<TierId> = emptyList(),
    val progress: TierProgress = TierProgress(),
    /** Distinct keys contributing to value (days, disciplines, strokes, …). */
    val distinctKeys: Set<String> = emptySet(),
    /** Qualifying local days for streak computation (yyyy-MM-dd). */
    val qualifyingDays: Set<String> = emptySet(),
    /** Freeze-token days consumed for this achievement streak. */
    val freezeDays: Set<String> = emptySet(),
    val plannedRestDays: Set<String> = emptySet(),
)

@Serializable
data class UnlockNotification(
    val achievementId: String,
    val tier: TierId,
    val value: Double,
)

@Serializable
data class EvaluationSummary(
    val processedEvents: Int,
    val skippedDuplicates: Int,
    val unlocks: List<UnlockNotification>,
    /** When [backfill] was true, individual unlocks are suppressed in favour of this summary. */
    val backfill: Boolean,
    val summaryMessageKey: String? = null,
    val summaryUnlockCount: Int = 0,
)

@Serializable
data class EvaluationState(
    val processedEventIds: Set<String> = emptySet(),
    val progressByAchievement: Map<String, AchievementProgress> = emptyMap(),
    /** Tombstones for deleted source entities (workout ids etc.). */
    val revokedEventIds: Set<String> = emptySet(),
) {
    fun progressOf(id: String): AchievementProgress =
        progressByAchievement[id] ?: AchievementProgress(achievementId = id)
}
