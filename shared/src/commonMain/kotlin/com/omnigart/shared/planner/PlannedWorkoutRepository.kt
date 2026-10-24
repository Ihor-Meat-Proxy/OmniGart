package com.omnigart.shared.planner

import com.omnigart.planning.PlannerIntensity
import com.omnigart.planning.PlannerSessionKind
import com.omnigart.shared.db.OmniGartDatabase
import kotlin.time.Instant

class PlannedWorkoutRepository(private val db: OmniGartDatabase) {
    fun upsert(workout: PlannedWorkout) {
        db.omniGartQueries.upsertPlannedWorkout(
            id = workout.id,
            profile_id = workout.profileId,
            scheduled_date = workout.scheduledDateIso,
            title_key = workout.titleKey,
            session_kind = workout.kind.name,
            intensity = workout.intensity.name,
            muscle_groups = workout.muscleGroups.joinToString(","),
            notes = workout.notes,
            source = workout.source.name,
            updated_at = workout.updatedAt.toString()
        )
    }

    fun get(id: String): PlannedWorkout? = db.omniGartQueries.selectPlannedWorkout(id).executeAsOneOrNull()?.toDomain()

    fun forProfile(profileId: String): List<PlannedWorkout> =
        db.omniGartQueries.selectPlannedWorkoutsForProfile(profileId).executeAsList().map { it.toDomain() }

    fun inRange(profileId: String, startIso: String, endIso: String): List<PlannedWorkout> = db.omniGartQueries
        .selectPlannedWorkoutsInRange(profileId, startIso, endIso)
        .executeAsList()
        .map { it.toDomain() }

    fun delete(id: String) {
        db.omniGartQueries.deletePlannedWorkout(id)
    }

    fun deleteAllForProfile(profileId: String) {
        db.omniGartQueries.deletePlannedWorkoutsForProfile(profileId)
    }

    fun dismissedSuggestionIds(): Set<String> = db.omniGartQueries.selectDismissedSuggestionIds().executeAsList().toSet()

    fun recordDecision(id: String, suggestionId: String, decision: PlannerSuggestionDecisionKind, at: Instant) {
        db.omniGartQueries.upsertPlannerSuggestionDecision(
            id = id,
            suggestion_id = suggestionId,
            decision = decision.name,
            decided_at = at.toString()
        )
    }
}

private fun com.omnigart.shared.db.Planned_workout.toDomain(): PlannedWorkout = PlannedWorkout(
    id = id,
    profileId = profile_id,
    scheduledDateIso = scheduled_date,
    titleKey = title_key,
    kind = runCatching { PlannerSessionKind.valueOf(session_kind) }.getOrDefault(PlannerSessionKind.FREE_STRENGTH),
    intensity = runCatching { PlannerIntensity.valueOf(intensity) }.getOrDefault(PlannerIntensity.MODERATE),
    muscleGroups = if (muscle_groups.isBlank()) emptyList() else muscle_groups.split(',').map { it.trim() }.filter { it.isNotEmpty() },
    notes = notes,
    source = runCatching { PlannedWorkoutSource.valueOf(source) }.getOrDefault(PlannedWorkoutSource.USER),
    updatedAt = Instant.parse(updated_at)
)
