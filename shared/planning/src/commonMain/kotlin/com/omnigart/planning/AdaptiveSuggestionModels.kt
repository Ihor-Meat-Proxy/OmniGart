package com.omnigart.planning

/**
 * Adaptive planner suggestions are rule-based, on-device wellness guides.
 * They never rewrite Health / Health Connect records — only app-local plans.
 */
enum class AdaptiveSuggestionAction {
    MOVE,
    SWAP,
    SKIP
}

data class PlannedSessionSnapshot(
    val id: String,
    val dateIso: String,
    val kind: PlannerSessionKind,
    val intensity: PlannerIntensity,
    val muscleGroups: List<String>,
    val titleKey: String
)

data class CompletedSessionSnapshot(
    val dateIso: String,
    val kind: PlannerSessionKind,
    val intensity: PlannerIntensity,
    val muscleGroups: List<String>,
    /** Wellness load units (TRIMP-style proxy); relative scale only. */
    val load: Double
)

enum class PlannerReadinessBand {
    HIGH,
    NORMAL,
    LOW,
    REST,
    UNKNOWN
}

data class AdaptiveSuggestionContext(
    /** Today as ISO yyyy-MM-dd. */
    val todayIso: String,
    val planned: List<PlannedSessionSnapshot>,
    val completedRecent: List<CompletedSessionSnapshot>,
    /** Acute:chronic training-load ratio; null when unknown. */
    val acuteChronicRatio: Double?,
    val readiness: PlannerReadinessBand,
    /** Optional morning HRV ln(rMSSD) vs baseline z-score (negative = lower than baseline). */
    val hrvZScore: Double? = null,
    /** Resting HR delta vs 30-day average (positive = elevated). */
    val restingHrDeltaBpm: Double? = null,
    /** Last night sleep hours vs need ratio (1.0 = met need). */
    val sleepRatio: Double? = null,
    /** Suggestion ids the user already dismissed (do not re-emit). */
    val dismissedSuggestionIds: Set<String> = emptySet()
)

data class AdaptiveSuggestion(
    val id: String,
    val action: AdaptiveSuggestionAction,
    val targetSessionId: String,
    /** ISO date to move to when [action] is MOVE; otherwise null. */
    val proposedDateIso: String?,
    /** Replacement kind when [action] is SWAP. */
    val proposedKind: PlannerSessionKind?,
    val proposedIntensity: PlannerIntensity?,
    /**
     * Stable i18n key for a short wellness reason
     * (e.g. `planner_suggest_reason_load_elevated`).
     */
    val reasonKey: String,
    /** Optional secondary detail key (muscle name, etc.). */
    val reasonDetail: String? = null
)

data class AdaptiveSuggestionResult(val suggestions: List<AdaptiveSuggestion>)
