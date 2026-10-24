package com.omnigart.shared.health.store

import com.omnigart.shared.data.createInMemoryDatabase
import com.omnigart.shared.health.WorkoutActivityType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class SqlDelightLocalHealthStoreTest {
    @Test
    fun upsertAndReload_preservesSeriesAndSets() {
        val db = createInMemoryDatabase()
        val store = SqlDelightLocalHealthStore(db)
        val start = Instant.parse("2026-10-21T08:00:00Z")
        val end = Instant.parse("2026-10-21T08:40:00Z")
        val workout =
            LocalHealthWorkout(
                id = "sql-w1",
                identity = HealthSyncIdentity("omnigart.workout.sql-w1", 2),
                activityType = WorkoutActivityType.STRENGTH,
                startAt = start,
                endAt = end,
                activeEnergyKcal = 220.0,
                basalEnergyKcal = 40.0,
                origin = HealthRecordOrigin.OMNIGART,
                sourceBundle = HealthDataMapper.SOURCE_BUNDLE_OMNIGART,
                updatedAt = end,
                heartRateSeries =
                listOf(
                    LocalHeartRatePoint(Instant.parse("2026-10-21T08:05:00Z"), 110.0),
                    LocalHeartRatePoint(Instant.parse("2026-10-21T08:20:00Z"), 130.0)
                ),
                sets =
                listOf(
                    LocalWorkoutSet(
                        id = "sql-w1-s0",
                        exerciseKey = "barbell-bench-press",
                        setIndex = 0,
                        reps = 8,
                        loadKg = 60.0
                    )
                )
            )
        store.upsertWorkout(workout)
        val loaded = store.getWorkout("sql-w1")
        assertNotNull(loaded)
        assertEquals(2, loaded.identity.syncVersion)
        assertEquals(2, loaded.heartRateSeries.size)
        assertEquals(1, loaded.sets.size)
        assertEquals(60.0, loaded.sets.first().loadKg)

        store.upsertSyncStatus(
            HealthSyncStatus(
                platform = HealthSyncPlatform.HEALTH_CONNECT,
                authStatus = "AUTHORIZED",
                lastSyncAt = end,
                changeToken = "token-1"
            )
        )
        assertEquals("AUTHORIZED", store.getSyncStatus(HealthSyncPlatform.HEALTH_CONNECT)?.authStatus)
        assertEquals("token-1", store.getSyncStatus(HealthSyncPlatform.HEALTH_CONNECT)?.changeToken)

        store.upsertConflict(
            HealthConflictRecord(
                id = "c1",
                workoutId = "sql-w1",
                platform = HealthSyncPlatform.HEALTH_CONNECT,
                fieldName = "activeEnergyKcal",
                localValue = "220.0",
                platformValue = "240.0",
                preferredSource = "omnigart",
                createdAt = end
            )
        )
        assertTrue(store.listOpenConflicts().isNotEmpty())
        store.resolveConflict("c1")
        assertTrue(store.listOpenConflicts().isEmpty())
    }
}
