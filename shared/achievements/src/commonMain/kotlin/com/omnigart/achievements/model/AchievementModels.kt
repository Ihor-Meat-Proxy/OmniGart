package com.omnigart.achievements.model

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
    @SerialName("body") BODY,
    @SerialName("general") GENERAL,
}

@Serializable
enum class AchievementType {
    @SerialName("streak") STREAK,
    @SerialName("milestone") MILESTONE,
    @SerialName("single_session") SINGLE_SESSION,
    @SerialName("personal_record") PERSONAL_RECORD,
    @SerialName("challenge") CHALLENGE,
    @SerialName("discovery") DISCOVERY,
}

@Serializable
enum class TierId {
    @SerialName("bronze") BRONZE,
    @SerialName("silver") SILVER,
    @SerialName("gold") GOLD,
    @SerialName("platinum") PLATINUM,
    @SerialName("level_1") LEVEL_1,
    @SerialName("level_2") LEVEL_2,
    @SerialName("level_3") LEVEL_3,
    @SerialName("level_4") LEVEL_4,
    @SerialName("level_5") LEVEL_5,
    @SerialName("unlocked") UNLOCKED,
}

@Serializable
data class AchievementTier(
    val id: TierId,
    val threshold: Double,
    val titleKeySuffix: String? = null,
)

@Serializable
enum class MetricId {
    @SerialName("workouts_completed") WORKOUTS_COMPLETED,
    @SerialName("sets_logged") SETS_LOGGED,
    @SerialName("volume_kg") VOLUME_KG,
    @SerialName("session_duration_min") SESSION_DURATION_MIN,
    @SerialName("distance_m") DISTANCE_M,
    @SerialName("pace_sec_per_km") PACE_SEC_PER_KM,
    @SerialName("swim_strokes_distinct") SWIM_STROKES_DISTINCT,
    @SerialName("yoga_minutes") YOGA_MINUTES,
    @SerialName("pilates_minutes") PILATES_MINUTES,
    @SerialName("food_days_logged") FOOD_DAYS_LOGGED,
    @SerialName("water_goal_days") WATER_GOAL_DAYS,
    @SerialName("sleep_consistent_days") SLEEP_CONSISTENT_DAYS,
    @SerialName("measurements_logged") MEASUREMENTS_LOGGED,
    @SerialName("measurement_days") MEASUREMENT_DAYS,
    @SerialName("personal_record_events") PERSONAL_RECORD_EVENTS,
    @SerialName("disciplines_tried") DISCIPLINES_TRIED,
    @SerialName("planned_rest_honoured") PLANNED_REST_HONOURED,
    @SerialName("freeze_tokens_used") FREEZE_TOKENS_USED,
    @SerialName("challenge_points") CHALLENGE_POINTS,
}

@Serializable
enum class Aggregation {
    @SerialName("count") COUNT,
    @SerialName("sum") SUM,
    @SerialName("max") MAX,
    @SerialName("min") MIN,
    @SerialName("distinct") DISTINCT,
    @SerialName("streak") STREAK,
    @SerialName("exists") EXISTS,
}

@Serializable
enum class WindowKind {
    @SerialName("lifetime") LIFETIME,
    @SerialName("rolling_days") ROLLING_DAYS,
    @SerialName("calendar") CALENDAR,
    @SerialName("single_event") SINGLE_EVENT,
    @SerialName("challenge_period") CHALLENGE_PERIOD,
    @SerialName("streak_days") STREAK_DAYS,
}

@Serializable
enum class StreakQualify {
    @SerialName("activity") ACTIVITY,
    @SerialName("log") LOG,
    @SerialName("goal_met") GOAL_MET,
    @SerialName("activity_or_planned_rest") ACTIVITY_OR_PLANNED_REST,
}

@Serializable
enum class CalendarUnit {
    @SerialName("day") DAY,
    @SerialName("week") WEEK,
    @SerialName("month") MONTH,
}

@Serializable
enum class ChallengePeriod {
    @SerialName("current_month") CURRENT_MONTH,
    @SerialName("current_week") CURRENT_WEEK,
    @SerialName("explicit") EXPLICIT,
}

@Serializable
enum class CompareOp {
    @SerialName("gte") GTE,
    @SerialName("lte") LTE,
    @SerialName("eq") EQ,
}

@Serializable
data class RuleWindow(
    val kind: WindowKind,
    val days: Int? = null,
    val unit: CalendarUnit? = null,
    val count: Int? = null,
    val period: ChallengePeriod? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val qualify: StreakQualify? = null,
    val restDayTolerancePerWeek: Int? = null,
    val freezeTokensAllowed: Boolean? = null,
    val maxFreezePerStreak: Int? = null,
)

@Serializable
data class RuleFilters(
    val discipline: List<String>? = null,
    val minDurationMin: Double? = null,
    val minDistanceM: Double? = null,
    val maxDistanceM: Double? = null,
    val stroke: List<String>? = null,
    val measurementKind: List<String>? = null,
    val waterGoalMl: Int? = null,
    val sleepMidpointVarianceMin: Double? = null,
    val excludeCaloriesBelow: Int? = null,
    val excludeWeightLossRate: Boolean? = null,
)

@Serializable
data class AchievementRule(
    val metric: MetricId,
    val aggregation: Aggregation,
    val window: RuleWindow,
    val threshold: Double? = null,
    val filters: RuleFilters? = null,
    val compare: CompareOp = CompareOp.GTE,
)

@Serializable
data class AchievementDefinition(
    val id: String,
    val category: AchievementCategory,
    val type: AchievementType,
    val tiers: List<AchievementTier>,
    val rule: AchievementRule,
    val iconId: String,
    val hidden: Boolean = false,
    val repeatable: Boolean = false,
    val titleKey: String,
    val descriptionKey: String,
    val healthyNotes: String? = null,
)
