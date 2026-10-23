package com.omnigart.shared.workoutkit

import com.omnigart.shared.health.WorkoutActivityType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ScheduledWorkoutValidatorTest {
    private fun runningPlan(vararg blocks: WorkoutBlockSpec) =
        ScheduledWorkoutPlan(
            id = "plan-1",
            activityType = WorkoutActivityType.RUNNING,
            displayNameKey = "running.session.easy",
            blocks = blocks.toList(),
        )

    @Test
    fun acceptsWarmWorkCool() {
        val plan =
            runningPlan(
                WorkoutBlockSpec(WorkoutBlockKind.WARM_UP, "w", WorkoutGoalSpec(WorkoutGoalKind.TIME_SECONDS, 300.0)),
                WorkoutBlockSpec(
                    WorkoutBlockKind.WORK,
                    "z2",
                    WorkoutGoalSpec(WorkoutGoalKind.TIME_SECONDS, 1800.0),
                    alerts = listOf(WorkoutAlertSpec(WorkoutAlertKind.HEART_RATE_ZONE, 120.0, 140.0)),
                ),
                WorkoutBlockSpec(WorkoutBlockKind.COOL_DOWN, "c", WorkoutGoalSpec(WorkoutGoalKind.TIME_SECONDS, 300.0)),
            )
        assertTrue(ScheduledWorkoutValidator.validate(plan).ok)
    }

    @Test
    fun rejectsStrengthActivity() {
        val plan =
            ScheduledWorkoutPlan(
                id = "s",
                activityType = WorkoutActivityType.STRENGTH,
                displayNameKey = "strength.session",
                blocks =
                    listOf(
                        WorkoutBlockSpec(WorkoutBlockKind.WORK, "sets"),
                    ),
            )
        val result = ScheduledWorkoutValidator.validate(plan)
        assertFalse(result.ok)
        assertEquals(ScheduleFailureReason.STRENGTH_NOT_SUPPORTED, result.failure)
    }

    @Test
    fun rejectsStrengthBlockKind() {
        val plan =
            runningPlan(
                WorkoutBlockSpec(WorkoutBlockKind.STRENGTH_UNSUPPORTED, "bad"),
            )
        assertEquals(
            ScheduleFailureReason.STRENGTH_NOT_SUPPORTED,
            ScheduledWorkoutValidator.validate(plan).failure,
        )
    }

    @Test
    fun scheduleCap() {
        assertTrue(ScheduledWorkoutValidator.checkScheduleCap(49).ok)
        val full = ScheduledWorkoutValidator.checkScheduleCap(50)
        assertFalse(full.ok)
        assertEquals(ScheduleFailureReason.SCHEDULE_CAP_REACHED, full.failure)
    }
}
