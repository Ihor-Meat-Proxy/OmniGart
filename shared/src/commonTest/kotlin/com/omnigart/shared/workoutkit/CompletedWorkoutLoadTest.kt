package com.omnigart.shared.workoutkit

import com.omnigart.shared.health.HealthWorkout
import com.omnigart.shared.health.WorkoutActivityType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

class CompletedWorkoutLoadTest {
    @Test
    fun durationOnlyWhenNoHr() {
        val w =
            HealthWorkout(
                id = "w1",
                activityType = WorkoutActivityType.RUNNING,
                start = Instant.parse("2026-10-23T10:00:00Z"),
                end = Instant.parse("2026-10-23T11:00:00Z")
            )
        val c = CompletedWorkoutLoad.fromHealthWorkout(w)
        assertEquals(3600, c.durationSeconds)
        assertEquals(60.0, c.loadScore, absoluteTolerance = 0.01)
    }

    @Test
    fun karvonenStyleIntensity() {
        // rest 60, max 180, avg 120 → reserve 0.5 → intensity 1.0 → 30 min * 1.0
        val factor = CompletedWorkoutLoad.intensityFactor(120.0, 60.0, 180.0)
        assertEquals(1.0, factor, absoluteTolerance = 0.001)
        val w =
            HealthWorkout(
                id = "w2",
                activityType = WorkoutActivityType.RUNNING,
                start = Instant.parse("2026-10-23T10:00:00Z"),
                end = Instant.parse("2026-10-23T10:30:00Z"),
                averageHeartRateBpm = 120.0
            )
        val c = CompletedWorkoutLoad.fromHealthWorkout(w, restingHrBpm = 60.0, maxHrBpm = 180.0)
        assertEquals(30.0, c.loadScore, absoluteTolerance = 0.01)
    }

    @Test
    fun totalLoadSums() {
        val a =
            CompletedWorkoutLoad.fromHealthWorkout(
                HealthWorkout(
                    "a",
                    WorkoutActivityType.RUNNING,
                    Instant.parse("2026-10-23T10:00:00Z"),
                    Instant.parse("2026-10-23T10:20:00Z")
                )
            )
        val b =
            CompletedWorkoutLoad.fromHealthWorkout(
                HealthWorkout(
                    "b",
                    WorkoutActivityType.RUNNING,
                    Instant.parse("2026-10-23T12:00:00Z"),
                    Instant.parse("2026-10-23T12:10:00Z")
                )
            )
        assertTrue(CompletedWorkoutLoad.totalLoad(listOf(a, b)) > 20.0)
    }
}
