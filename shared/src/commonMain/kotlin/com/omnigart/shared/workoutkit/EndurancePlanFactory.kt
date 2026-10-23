package com.omnigart.shared.workoutkit

import com.omnigart.shared.health.WorkoutActivityType

/**
 * Builds common endurance [ScheduledWorkoutPlan] shapes for WorkoutKit scheduling.
 */
object EndurancePlanFactory {
    /**
     * Continuous work block with optional HR band (e.g. Zone 2), plus warm-up / cool-down.
     */
    fun continuousRun(
        id: String,
        displayNameKey: String,
        workSeconds: Long,
        warmUpSeconds: Long = 300,
        coolDownSeconds: Long = 300,
        hrAlertLowerBpm: Double? = null,
        hrAlertUpperBpm: Double? = null,
        scheduledStartEpochMillis: Long? = null,
    ): ScheduledWorkoutPlan {
        val alerts =
            if (hrAlertLowerBpm != null && hrAlertUpperBpm != null) {
                listOf(
                    WorkoutAlertSpec(
                        kind = WorkoutAlertKind.HEART_RATE_ZONE,
                        lower = hrAlertLowerBpm,
                        upper = hrAlertUpperBpm,
                    ),
                )
            } else {
                emptyList()
            }
        return ScheduledWorkoutPlan(
            id = id,
            activityType = WorkoutActivityType.RUNNING,
            displayNameKey = displayNameKey,
            scheduledStartEpochMillis = scheduledStartEpochMillis,
            blocks =
                listOf(
                    WorkoutBlockSpec(
                        kind = WorkoutBlockKind.WARM_UP,
                        displayNameKey = "$displayNameKey.warmup",
                        goal = WorkoutGoalSpec(WorkoutGoalKind.TIME_SECONDS, warmUpSeconds.toDouble()),
                    ),
                    WorkoutBlockSpec(
                        kind = WorkoutBlockKind.WORK,
                        displayNameKey = "$displayNameKey.work",
                        goal = WorkoutGoalSpec(WorkoutGoalKind.TIME_SECONDS, workSeconds.toDouble()),
                        alerts = alerts,
                    ),
                    WorkoutBlockSpec(
                        kind = WorkoutBlockKind.COOL_DOWN,
                        displayNameKey = "$displayNameKey.cooldown",
                        goal = WorkoutGoalSpec(WorkoutGoalKind.TIME_SECONDS, coolDownSeconds.toDouble()),
                    ),
                ),
        )
    }
}
