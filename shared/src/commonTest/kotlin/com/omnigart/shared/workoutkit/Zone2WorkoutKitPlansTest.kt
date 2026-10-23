package com.omnigart.shared.workoutkit

import com.omnigart.activity.hr.Zone2
import com.omnigart.activity.hr.Zone2Method
import com.omnigart.shared.health.WorkoutActivityType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class Zone2WorkoutKitPlansTest {
    @Test
    fun continuousRunAlertBoundsMatchKarvonenProfile() {
        val hrMax = 180
        val hrRest = 60
        val expected = Zone2.karvonen(hrMax = hrMax, hrRest = hrRest)

        val plan =
            Zone2WorkoutKitPlans.continuousRun(
                id = "zone2-wkt-1",
                hrMax = hrMax,
                hrRest = hrRest,
                workSeconds = 1800,
                scheduledStartEpochMillis = 1_700_000_000_000L,
            )

        assertEquals(WorkoutActivityType.RUNNING, plan.activityType)
        assertEquals(3, plan.blocks.size)
        assertEquals(WorkoutBlockKind.WARM_UP, plan.blocks[0].kind)
        assertEquals(WorkoutBlockKind.WORK, plan.blocks[1].kind)
        assertEquals(WorkoutBlockKind.COOL_DOWN, plan.blocks[2].kind)

        val alert = plan.blocks[1].alerts.single()
        assertEquals(WorkoutAlertKind.HEART_RATE_ZONE, alert.kind)
        assertEquals(expected.lowerBpm.toDouble(), alert.lower)
        assertEquals(expected.upperBpm.toDouble(), alert.upper)
        assertEquals(132.0, alert.lower)
        assertEquals(144.0, alert.upper)
        assertEquals(1_700_000_000_000L, plan.scheduledStartEpochMillis)
        assertTrue(ScheduledWorkoutValidator.validate(plan).ok)
    }

    @Test
    fun continuousRunRespectsPercentHrMaxMethod() {
        val expected =
            Zone2.profile(
                method = Zone2Method.PERCENT_HR_MAX,
                hrMax = 200,
                hrRest = 50,
            )
        val plan =
            Zone2WorkoutKitPlans.continuousRun(
                id = "zone2-pct",
                hrMax = 200,
                hrRest = 50,
                workSeconds = 1200,
                method = Zone2Method.PERCENT_HR_MAX,
            )
        val alert = plan.blocks[1].alerts.single()
        assertEquals(expected.lowerBpm.toDouble(), alert.lower)
        assertEquals(expected.upperBpm.toDouble(), alert.upper)
    }

    @Test
    fun continuousRunWorkGoalIsTimeSeconds() {
        val plan =
            Zone2WorkoutKitPlans.continuousRun(
                id = "zone2-time",
                hrMax = 180,
                hrRest = 60,
                workSeconds = 2400,
            )
        val workGoal = assertNotNull(plan.blocks[1].goal)
        assertEquals(WorkoutGoalKind.TIME_SECONDS, workGoal.kind)
        assertEquals(2400.0, workGoal.value)
    }
}
