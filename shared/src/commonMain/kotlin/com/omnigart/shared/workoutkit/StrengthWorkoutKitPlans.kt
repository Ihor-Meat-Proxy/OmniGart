package com.omnigart.shared.workoutkit

import com.omnigart.shared.health.WorkoutActivityType
import com.omnigart.shared.logging.DEFAULT_REST_SECONDS

/**
 * Export a strength routine as a WorkoutKit **custom workout** of work + recovery steps.
 *
 * Apple's built-in Workout app **cannot** accept weight or rep entry from third-party apps.
 * Each set becomes an open (or time-based) **work** step and each rest a **recovery** step so
 * the athlete can still follow the routine set-by-set in the system Workout app, while
 * OmniGart's own watch app remains the place to log load × reps.
 *
 * See `docs/WORKOUTKIT.md` § Strength custom workouts.
 */
object StrengthWorkoutKitPlans {
    const val DEFAULT_DISPLAY_NAME_KEY: String = "workout.strength.custom"

    data class RoutineSetStep(
        val exerciseDisplayKey: String,
        /** When non-null, work step uses a time goal; otherwise OPEN. */
        val workSeconds: Long? = null,
        val restSeconds: Int = DEFAULT_REST_SECONDS,
        val isWarmup: Boolean = false
    )

    /**
     * Build a [ScheduledWorkoutPlan] for traditional strength: alternating WORK / RECOVERY.
     * Never emits [WorkoutBlockKind.STRENGTH_UNSUPPORTED].
     */
    fun fromRoutineSets(
        id: String,
        steps: List<RoutineSetStep>,
        scheduledStartEpochMillis: Long? = null,
        displayNameKey: String = DEFAULT_DISPLAY_NAME_KEY
    ): ScheduledWorkoutPlan {
        require(steps.isNotEmpty()) { "Strength WorkoutKit plan needs at least one set step" }
        val blocks = mutableListOf<WorkoutBlockSpec>()
        steps.forEachIndexed { index, step ->
            val workGoal =
                if (step.workSeconds != null && step.workSeconds > 0) {
                    WorkoutGoalSpec(WorkoutGoalKind.TIME_SECONDS, value = step.workSeconds.toDouble())
                } else {
                    WorkoutGoalSpec(WorkoutGoalKind.OPEN)
                }
            val prefix = if (step.isWarmup) "W" else "${index + 1}"
            blocks +=
                WorkoutBlockSpec(
                    kind = WorkoutBlockKind.WORK,
                    displayNameKey = "$prefix · ${step.exerciseDisplayKey}",
                    goal = workGoal
                )
            val rest = step.restSeconds.coerceAtLeast(0)
            if (rest > 0 && index < steps.lastIndex) {
                blocks +=
                    WorkoutBlockSpec(
                        kind = WorkoutBlockKind.RECOVERY,
                        displayNameKey = "workout.rest",
                        goal = WorkoutGoalSpec(WorkoutGoalKind.TIME_SECONDS, value = rest.toDouble())
                    )
            }
        }
        return ScheduledWorkoutPlan(
            id = id,
            activityType = WorkoutActivityType.STRENGTH,
            displayNameKey = displayNameKey,
            blocks = blocks,
            scheduledStartEpochMillis = scheduledStartEpochMillis
        )
    }
}
