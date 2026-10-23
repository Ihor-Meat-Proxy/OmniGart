package com.omnigart.achievements

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class AchievementCategory {
    @SerialName("strength") STRENGTH,
    @SerialName("running") RUNNING,
    @SerialName("swimming") SWIMMING,
    @SerialName("yoga") YOGA,
    @SerialName("pilates") PILATES,
    @SerialName("nutrition") NUTRITION,
    @SerialName("water") WATER,
    @SerialName("recovery") RECOVERY,
    @SerialName("body_measurements") BODY_MEASUREMENTS,
    @SerialName("consistency") CONSISTENCY,
}

@Serializable
enum class MetalTier {
    @SerialName("bronze") BRONZE,
    @SerialName("silver") SILVER,
    @SerialName("gold") GOLD,
    @SerialName("platinum") PLATINUM,
}

@Serializable
sealed class AchievementTier {
    @Serializable
    @SerialName("metal")
    data class Metal(val metal: MetalTier) : AchievementTier()

    @Serializable
    @SerialName("level")
    data class Level(val level: Int) : AchievementTier()
}

@Serializable
enum class RuleKind {
    @SerialName("counter") COUNTER,
    @SerialName("streak") STREAK,
    @SerialName("threshold") THRESHOLD,
    @SerialName("personal_record") PERSONAL_RECORD,
    @SerialName("challenge") CHALLENGE,
}

@Serializable
enum class ComparatorOp {
    @SerialName("gte") GTE,
    @SerialName("gt") GT,
    @SerialName("lte") LTE,
    @SerialName("lt") LT,
    @SerialName("eq") EQ,
}

@Serializable
enum class WindowUnit {
    @SerialName("day") DAY,
    @SerialName("week") WEEK,
    @SerialName("month") MONTH,
    @SerialName("rolling_days") ROLLING_DAYS,
    @SerialName("challenge") CHALLENGE,
}

@Serializable
data class RuleWindow(
    val unit: WindowUnit,
    val count: Int,
)

@Serializable
data class StreakPolicy(
    val allowPlannedRest: Boolean = true,
    val allowStreakFreeze: Boolean = true,
    val maxFreezeDaysPer30Days: Int = 2,
)

@Serializable
data class AchievementRule(
    val kind: RuleKind,
    val metric: String,
    val comparator: ComparatorOp = ComparatorOp.GTE,
    val value: Double,
    val filters: Map<String, String> = emptyMap(),
    val window: RuleWindow? = null,
    val streakPolicy: StreakPolicy? = null,
)

@Serializable
data class AchievementDefinition(
    val id: String,
    val category: AchievementCategory,
    val tier: AchievementTier,
    val familyId: String,
    val hidden: Boolean = false,
    val repeatable: Boolean = false,
    val iconId: String,
    val titleKey: String,
    val descriptionKey: String,
    val rule: AchievementRule,
    val sortOrder: Int = 0,
)

@Serializable
data class HealthyByDesignFlags(
    val rewardConsistency: Boolean = true,
    val forbidCalorieRestrictionRewards: Boolean = true,
    val forbidDehydrationOrExcessWaterRewards: Boolean = true,
    val forbidTrainingThroughPoorRecoveryRewards: Boolean = true,
    val forbidRapidWeightLossRewards: Boolean = true,
    val weightAchievementsTrackLoggingOnly: Boolean = true,
    val streaksAllowPlannedRestAndFreeze: Boolean = true,
)

@Serializable
data class AchievementCatalog(
    val version: Int,
    val localePolicy: List<String> = listOf("en-GB", "uk"),
    val healthyByDesign: HealthyByDesignFlags = HealthyByDesignFlags(),
    val achievements: List<AchievementDefinition>,
) {
    fun byId(id: String): AchievementDefinition? = achievements.firstOrNull { it.id == id }

    fun familyMembers(familyId: String): List<AchievementDefinition> =
        achievements.filter { it.familyId == familyId }.sortedWith(familyComparator)

    companion object {
        private val metalOrder = listOf(
            MetalTier.BRONZE,
            MetalTier.SILVER,
            MetalTier.GOLD,
            MetalTier.PLATINUM,
        )

        val familyComparator: Comparator<AchievementDefinition> =
            Comparator { a, b ->
                val ta = a.tier
                val tb = b.tier
                when {
                    ta is AchievementTier.Metal && tb is AchievementTier.Metal ->
                        metalOrder.indexOf(ta.metal).compareTo(metalOrder.indexOf(tb.metal))
                    ta is AchievementTier.Level && tb is AchievementTier.Level ->
                        ta.level.compareTo(tb.level)
                    else -> a.sortOrder.compareTo(b.sortOrder)
                }.let { if (it != 0) it else a.sortOrder.compareTo(b.sortOrder) }
            }
    }
}
