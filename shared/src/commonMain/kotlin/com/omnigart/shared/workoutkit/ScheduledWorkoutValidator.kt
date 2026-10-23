package com.omnigart.shared.workoutkit

import com.omnigart.shared.health.WorkoutActivityType

object ScheduledWorkoutValidator {
    /** Documented soft cap mirrored in Swift; OS may enforce a similar limit. */
    const val MAX_SCHEDULED_WORKOUTS: Int = 50

    fun validate(plan: ScheduledWorkoutPlan): ScheduleResult {
        if (plan.activityType == WorkoutActivityType.STRENGTH ||
            plan.blocks.any { it.kind == WorkoutBlockKind.STRENGTH_UNSUPPORTED }
        ) {
            return ScheduleResult(
                ok = false,
                failure = ScheduleFailureReason.STRENGTH_NOT_SUPPORTED,
                detail = "WorkoutKit scheduling does not support strength set/rep blocks"
            )
        }
        if (plan.blocks.any { it.repeatCount < 1 }) {
            return ScheduleResult(
                ok = false,
                failure = ScheduleFailureReason.INVALID_PLAN,
                detail = "repeatCount must be >= 1"
            )
        }
        for (block in plan.blocks) {
            for (alert in block.alerts) {
                if (alert.lower > alert.upper) {
                    return ScheduleResult(
                        ok = false,
                        failure = ScheduleFailureReason.INVALID_PLAN,
                        detail = "alert lower > upper in block ${block.displayNameKey}"
                    )
                }
            }
        }
        return ScheduleResult(ok = true)
    }

    fun checkScheduleCap(currentScheduledCount: Int): ScheduleResult {
        if (currentScheduledCount >= MAX_SCHEDULED_WORKOUTS) {
            return ScheduleResult(
                ok = false,
                scheduledCount = currentScheduledCount,
                failure = ScheduleFailureReason.SCHEDULE_CAP_REACHED,
                detail = "At WorkoutScheduler cap ($MAX_SCHEDULED_WORKOUTS)"
            )
        }
        return ScheduleResult(ok = true, scheduledCount = currentScheduledCount)
    }
}
