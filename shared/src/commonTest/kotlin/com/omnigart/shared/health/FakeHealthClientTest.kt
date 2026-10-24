package com.omnigart.shared.health

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest

class FakeHealthClientTest {
    @Test
    fun roundTripHeartRateAndWater() = runTest {
        val client = FakeHealthClient()
        val t0 = Instant.parse("2026-10-01T08:00:00Z")
        val t1 = Instant.parse("2026-10-01T09:00:00Z")
        client.writeHeartRate(listOf(HeartRateSample(72.0, t0)))
        client.writeWater(listOf(WaterIntake("w1", 250.0, t1)))
        val range =
            TimeRange(
                Instant.parse("2026-10-01T00:00:00Z"),
                Instant.parse("2026-10-02T00:00:00Z")
            )
        assertEquals(1, client.readHeartRate(range).size)
        assertEquals(250.0, client.readWater(range).single().volumeMl)
    }

    @Test
    fun healthKitFacadeUsesPlatformApi() = runTest {
        val api = FakeHealthKitPlatformApi()
        val kit = HealthKitClientImpl(api)
        val status =
            kit.requestAuthorization(
                setOf(HealthPermission.READ_HEART_RATE, HealthPermission.WRITE_HEART_RATE)
            )
        assertEquals(AuthStatus.AUTHORIZED, status)
        val at = Instant.parse("2026-10-01T10:00:00Z")
        kit.writeHeartRate(listOf(HeartRateSample(80.0, at)))
        val read =
            kit.readHeartRate(
                TimeRange(
                    Instant.parse("2026-10-01T00:00:00Z"),
                    Instant.parse("2026-10-02T00:00:00Z")
                )
            )
        assertEquals(80.0, read.single().beatsPerMinute)
        kit.enableBackgroundDelivery(setOf(HealthDataType.HEART_RATE))
        assertTrue(api.client.backgroundTypes.contains(HealthDataType.HEART_RATE))
    }

    @Test
    fun observersFire() = runTest {
        val client = FakeHealthClient()
        var hits = 0
        val id = client.startObserving(setOf(HealthDataType.HEART_RATE)) { hits += 1 }
        client.emitObservation()
        assertEquals(1, hits)
        client.stopObserving(id)
        client.emitObservation()
        assertEquals(1, hits)
    }

    @Test
    fun writeWorkouts_isAppendOnly_skipsExistingIds() = runTest {
        val client = FakeHealthClient()
        val start = Instant.parse("2026-10-01T08:00:00Z")
        val end = Instant.parse("2026-10-01T09:00:00Z")
        val workout =
            HealthWorkout(
                id = "omnigart.workout.session-1",
                activityType = WorkoutActivityType.STRENGTH,
                start = start,
                end = end,
                energyKcal = 200.0
            )
        client.writeWorkouts(listOf(workout))
        client.writeWorkouts(
            listOf(
                workout.copy(energyKcal = 999.0),
                workout.copy(
                    id = "omnigart.workout.session-2",
                    energyKcal = 150.0
                )
            )
        )
        val range =
            TimeRange(
                Instant.parse("2026-10-01T00:00:00Z"),
                Instant.parse("2026-10-02T00:00:00Z")
            )
        val stored = client.readWorkouts(range)
        assertEquals(2, stored.size)
        assertEquals(200.0, stored.single { it.id == "omnigart.workout.session-1" }.energyKcal)
        assertEquals(150.0, stored.single { it.id == "omnigart.workout.session-2" }.energyKcal)
    }
}
