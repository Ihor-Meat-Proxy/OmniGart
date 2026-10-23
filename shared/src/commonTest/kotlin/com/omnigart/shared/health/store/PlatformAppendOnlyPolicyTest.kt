package com.omnigart.shared.health.store

import com.omnigart.shared.health.WorkoutActivityType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Instant

/**
 * Unit + contract coverage for Ihor's hard rule: platform writes are append-only.
 * Companion CI gate: `scripts/check_no_health_platform_mutate.sh`.
 */
class PlatformAppendOnlyPolicyTest {
    private val store = InMemoryLocalHealthStore()
    private val engine = HealthSyncEngine(store)

    private fun sample(id: String = "append-w1"): LocalHealthWorkout {
        val start = Instant.parse("2026-10-22T10:00:00Z")
        val end = Instant.parse("2026-10-22T10:30:00Z")
        return LocalHealthWorkout(
            id = id,
            identity = HealthSyncIdentity(HealthDataMapper.syncIdentifierFor(id), 1),
            activityType = WorkoutActivityType.STRENGTH,
            startAt = start,
            endAt = end,
            origin = HealthRecordOrigin.OMNIGART,
            sourceBundle = HealthDataMapper.SOURCE_BUNDLE_OMNIGART,
            updatedAt = end
        )
    }

    @Test
    fun shouldAttemptPlatformWrite_onlyForUnlinkedOmniGartRows() {
        val fresh = sample()
        assertTrue(PlatformAppendOnlyPolicy.shouldAttemptPlatformWrite(fresh))
        assertFalse(
            PlatformAppendOnlyPolicy.shouldAttemptPlatformWrite(
                fresh.copy(platformRecordId = "hk-uuid")
            )
        )
        assertFalse(
            PlatformAppendOnlyPolicy.shouldAttemptPlatformWrite(
                fresh.copy(removedFromPlatform = true)
            )
        )
        assertFalse(
            PlatformAppendOnlyPolicy.shouldAttemptPlatformWrite(
                fresh.copy(deleted = true)
            )
        )
        assertFalse(
            PlatformAppendOnlyPolicy.shouldAttemptPlatformWrite(
                fresh.copy(origin = HealthRecordOrigin.APPLE_HEALTH, sourceBundle = "com.apple.Health")
            )
        )
    }

    @Test
    fun platformDeletion_marksRemovedButKeepsLocalData() {
        val recorded = engine.recordLocally(sample())
        engine.markPlatformLinked(recorded.id, "platform-uuid-1")
        val linked = store.getWorkout(recorded.id)
        assertNotNull(linked)
        assertEquals("platform-uuid-1", linked.platformRecordId)

        engine.applyPlatformDeletion(
            platform = HealthSyncPlatform.APPLE_HEALTH,
            platformRecordId = "platform-uuid-1"
        )
        val kept = store.getWorkout(recorded.id)
        assertNotNull(kept)
        assertTrue(kept.removedFromPlatform)
        assertFalse(kept.deleted)
        assertEquals(recorded.activeEnergyKcal, kept.activeEnergyKcal)
        assertFalse(PlatformAppendOnlyPolicy.shouldAttemptPlatformWrite(kept))
        assertTrue(engine.workoutsEligibleForPlatformWrite().none { it.id == recorded.id })
    }

    @Test
    fun localSoftDelete_doesNotRequirePlatformMutationApis() {
        val recorded = engine.recordLocally(sample())
        store.softDeleteWorkout(recorded.id)
        assertEquals(null, store.getWorkout(recorded.id))
        // Soft-delete is local-only; eligibility list ignores it.
        assertTrue(engine.workoutsEligibleForPlatformWrite().none { it.id == recorded.id })
    }

    @Test
    fun localEdits_stayEligibleOnlyUntilFirstPlatformLink() {
        val recorded = engine.recordLocally(sample())
        assertEquals(1, engine.workoutsEligibleForPlatformWrite().size)
        // Simulate a local edit (energy change) before any platform write.
        store.upsertWorkout(recorded.copy(activeEnergyKcal = 99.0, updatedAt = Instant.parse("2026-10-22T11:00:00Z")))
        assertEquals(1, engine.workoutsEligibleForPlatformWrite().size)
        engine.markPlatformLinked(recorded.id, "once-only")
        // Further local edits must not re-queue a platform write / SyncVersion bump.
        val afterLink = checkNotNull(store.getWorkout(recorded.id))
        store.upsertWorkout(afterLink.copy(activeEnergyKcal = 120.0))
        assertTrue(engine.workoutsEligibleForPlatformWrite().isEmpty())
        assertEquals(1L, checkNotNull(store.getWorkout(recorded.id)).identity.syncVersion)
    }

    @Test
    fun contract_forbiddenApiTokenListsArePinnedForCiGate() {
        // Keep in sync with scripts/check_no_health_platform_mutate.sh
        // Concatenate in the assertion so this test file does not trip the CI grep gate.
        val deleteObjects = "delete" + "Objects"
        val deleteRecords = "delete" + "Records("
        val updateRecords = "update" + "Records("
        assertTrue(PlatformAppendOnlyPolicy.FORBIDDEN_HEALTHKIT_TOKENS.any { it.contains(deleteObjects) })
        assertTrue(PlatformAppendOnlyPolicy.FORBIDDEN_HEALTH_CONNECT_TOKENS.contains(deleteRecords))
        assertTrue(PlatformAppendOnlyPolicy.FORBIDDEN_HEALTH_CONNECT_TOKENS.contains(updateRecords))
    }
}
