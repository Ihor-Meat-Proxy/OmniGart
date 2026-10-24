package com.omnigart.shared.health.store

import com.omnigart.shared.health.WorkoutActivityType
import com.omnigart.shared.health.store.export.HealthExportService
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class LocalHealthBridgeRecentIdsTest {
    private val store = InMemoryLocalHealthStore()
    private val bridge = LocalHealthBridge(store, HealthSyncEngine(store), HealthExportService(store))

    @Test
    fun listRecentWorkoutIds_filtersByActivityAndLimit() {
        store.upsertWorkout(sample("run-1", WorkoutActivityType.RUNNING, "2026-10-21T08:00:00Z"))
        store.upsertWorkout(sample("swim-1", WorkoutActivityType.SWIMMING, "2026-10-22T08:00:00Z"))
        store.upsertWorkout(sample("run-2", WorkoutActivityType.RUNNING, "2026-10-23T08:00:00Z"))
        store.upsertWorkout(sample("yoga-1", WorkoutActivityType.YOGA, "2026-10-24T08:00:00Z"))

        assertEquals(
            listOf("run-2", "run-1"),
            bridge.listRecentWorkoutIds(listOf("RUNNING"), limit = 10)
        )
        assertEquals(
            listOf("run-2"),
            bridge.listRecentWorkoutIds(listOf("RUNNING"), limit = 1)
        )
        assertEquals(
            listOf("yoga-1"),
            bridge.listRecentWorkoutIds(listOf("YOGA", "PILATES"), limit = 5)
        )
        assertEquals(emptyList(), bridge.listRecentWorkoutIds(emptyList()))
        assertEquals(emptyList(), bridge.listRecentWorkoutIds(listOf("NOT_A_TYPE")))
    }

    private fun sample(id: String, type: WorkoutActivityType, startIso: String): LocalHealthWorkout {
        val start = Instant.parse(startIso)
        val end = Instant.parse(startIso.replace("08:00:00", "08:30:00"))
        return LocalHealthWorkout(
            id = id,
            identity = HealthSyncIdentity(HealthDataMapper.syncIdentifierFor(id), 1),
            activityType = type,
            startAt = start,
            endAt = end,
            origin = HealthRecordOrigin.OMNIGART,
            sourceBundle = HealthDataMapper.SOURCE_BUNDLE_OMNIGART,
            sourceName = "OmniGart",
            updatedAt = end
        )
    }
}
