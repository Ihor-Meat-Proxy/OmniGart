package com.omnigart.planning

/**
 * Session kinds used when building an editable weekly scheme.
 * Titles are stable i18n keys resolved by the UI shells.
 */
enum class PlannerSessionKind {
    /** Programme / equipment-guided session — UI label: Guided strength. */
    GUIDED_STRENGTH,

    /** Ad-hoc traditional strength — UI label: Free strength. */
    FREE_STRENGTH,

    /** Functional strength session — UI label: Functional. */
    FUNCTIONAL,
    STRENGTH_FULL_BODY,
    STRENGTH_SPLIT,
    RUN,
    TREADMILL,
    ZONE2,
    REST
}

enum class PlannerIntensity {
    EASY,
    MODERATE,
    HARD
}

enum class PlannerSchemeSplit {
    FULL_BODY,
    UPPER_LOWER,
    ENDURANCE_BIAS,
    MIXED
}

data class SuggestedSchemeRequest(
    /** ISO Monday (or any week-start day) for the scheme. */
    val weekStartIso: String,
    val availableDaysPerWeek: Int,
    val primaryGoal: String,
    val split: PlannerSchemeSplit,
    val includeRuns: Boolean = true,
    val includeTreadmill: Boolean = false,
    val includeZone2: Boolean = true,
    /** True when the user reports some gym / free-weight equipment. */
    val hasStrengthEquipment: Boolean = true
)

data class SuggestedSchemeSession(
    val dayOffset: Int,
    val dateIso: String,
    val kind: PlannerSessionKind,
    val intensity: PlannerIntensity,
    /** i18n key for the session title. */
    val titleKey: String,
    val muscleGroups: List<String>,
    val explanationCode: String
)

data class SuggestedWeeklyScheme(
    val weekStartIso: String,
    val split: PlannerSchemeSplit,
    val sessions: List<SuggestedSchemeSession>,
    val explanationCodes: List<String>
)
