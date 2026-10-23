package com.omnigart.shared.workoutkit

import com.omnigart.shared.health.AuthStatus
import com.omnigart.shared.health.FakeHealthClient
import com.omnigart.shared.health.HealthWorkout
import com.omnigart.shared.health.TimeRange
import com.omnigart.shared.health.WorkoutActivityType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest

class WorkoutKitSchedulingServiceTest {
    private val samplePlan =
        ScheduledWorkoutPlan(
            id = "p",
            activityType = WorkoutActivityType.RUNNING,
            displayNameKey = "running.session.zone2",
            blocks =
            listOf(
                WorkoutBlockSpec(
                    WorkoutBlockKind.WORK,
                    "work",
                    WorkoutGoalSpec(WorkoutGoalKind.TIME_SECONDS, 1800.0),
                    alerts = listOf(WorkoutAlertSpec(WorkoutAlertKind.HEART_RATE_ZONE, 120.0, 145.0))
                )
            )
        )

    @Test
    fun disabledByDefaultFeatureFlag() = runTest {
        val svc = WorkoutKitSchedulingService(RecordingGateway())
        val result = svc.schedule(samplePlan)
        assertFalse(result.ok)
        assertEquals(ScheduleFailureReason.FEATURE_DISABLED, result.failure)
    }

    @Test
    fun importEmptyWhenFlagOff() = runTest {
        val health = FakeHealthClient()
        health.writeWorkouts(
            listOf(
                HealthWorkout(
                    "w",
                    WorkoutActivityType.RUNNING,
                    Instant.parse("2026-10-23T10:00:00Z"),
                    Instant.parse("2026-10-23T11:00:00Z")
                )
            )
        )
        val svc = WorkoutKitSchedulingService(UnsupportedWorkoutKitSchedulerGateway(), health)
        val imported =
            svc.importLoadContributions(
                TimeRange(
                    Instant.parse("2026-10-23T00:00:00Z"),
                    Instant.parse("2026-10-24T00:00:00Z")
                )
            )
        assertEquals(0, imported.size)
    }

    private class RecordingGateway : WorkoutKitSchedulerGateway {
        override fun isSupported(): Boolean = true

        override suspend fun requestAuthorization(): AuthStatus = AuthStatus.AUTHORIZED

        override suspend fun scheduledCount(): Int = 0

        override suspend fun schedule(plan: ScheduledWorkoutPlan): ScheduleResult = ScheduleResult(ok = true, scheduledCount = 1)

        override suspend fun removeAllScheduled(): ScheduleResult = ScheduleResult(ok = true)
    }
}
