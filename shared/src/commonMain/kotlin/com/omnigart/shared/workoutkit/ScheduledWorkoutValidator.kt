package com.omnigart.shared.workoutkit

import com.omnigart.shared.health.WorkoutActivityType

object ScheduledWorkoutValidator {
    /** Documented soft cap mirrored in Swift; OS may enforce a similar limit. */
    const val MAX_SCHEDULED_WORKOUTS: Int = 50

    fun validate(plan: ScheduledWorkoutPlan): ScheduleResult {
        // STRENGTH_UNSUPPORTED blocks remain rejected. STRENGTH activity with WORK/RECOVERY
        // steps is allowed for system Workout app follow-along (no weight/rep entry — see docs).
        if (plan.blocks.any { it.kind == WorkoutBlockKind.STRENGTH_UNSUPPORTED }) {
            return ScheduleResult(
                ok = false,
                failure = ScheduleFailureReason.STRENGTH_NOT_SUPPORTED,
                detail = "STRENGTH_UNSUPPORTED blocks cannot be scheduled via WorkoutKit"
            )
        }
        if (plan.activityType == WorkoutActivityType.STRENGTH) {
            val okKinds =
                setOf(
                    WorkoutBlockKind.WARM_UP,
                    WorkoutBlockKind.WORK,
                    WorkoutBlockKind.RECOVERY,
                    WorkoutBlockKind.COOL_DOWN
                )
            if (plan.blocks.any { it.kind !in okKinds }) {
                return ScheduleResult(
                    ok = false,
                    failure = ScheduleFailureReason.STRENGTH_NOT_SUPPORTED,
                    detail = "Strength WorkoutKit plans may only use warm-up / work / recovery / cool-down"
                )
            }
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
