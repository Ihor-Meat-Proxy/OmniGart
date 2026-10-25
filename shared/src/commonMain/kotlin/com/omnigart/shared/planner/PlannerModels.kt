package com.omnigart.shared.planner

import com.omnigart.planning.AdaptiveSuggestion
import com.omnigart.planning.PlannerCalendar
import com.omnigart.planning.PlannerIntensity
import com.omnigart.planning.PlannerSchemeSplit
import com.omnigart.planning.PlannerSessionKind
import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Serializable
enum class PlannedWorkoutSource {
    USER,
    SUGGESTED_SCHEME,
    ADAPTIVE
}

@Serializable
data class PlannedWorkout(
    val id: String,
    val profileId: String,
    /** ISO local date yyyy-MM-dd. */
    val scheduledDateIso: String,
    /** i18n key (e.g. watch_strength_start = Guided strength). */
    val titleKey: String,
    val kind: PlannerSessionKind,
    val intensity: PlannerIntensity,
    val muscleGroups: List<String> = emptyList(),
    val notes: String? = null,
    val source: PlannedWorkoutSource = PlannedWorkoutSource.USER,
    val updatedAt: Instant
)

@Serializable
enum class PlannerSuggestionDecisionKind {
    ACCEPTED,
    DISMISSED
}

data class PlannerCalendarSnapshot(
    val week: List<PlannerCalendar.DayCell>,
    val month: List<PlannerCalendar.DayCell>,
    val year: Int,
    val monthNumber: Int,
    val weekStartIso: String,
    val selectedDateIso: String,
    val plannedForSelected: List<PlannedWorkout>,
    val completedCountForSelected: Int
)

data class PlannerSuggestionsSnapshot(val suggestions: List<AdaptiveSuggestion>)

data class GenerateSchemeOptions(
    val weekStartIso: String,
    val availableDaysPerWeek: Int = 4,
    val primaryGoal: String = "GENERAL_FITNESS",
    val split: PlannerSchemeSplit = PlannerSchemeSplit.MIXED,
    val includeRuns: Boolean = true,
    val includeTreadmill: Boolean = false,
    val includeZone2: Boolean = true,
    val hasStrengthEquipment: Boolean = true,
    /** When true, replace existing planned sessions in that week. */
    val replaceWeek: Boolean = true
)
