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
    fun restore_bumpsSyncVersion_thenClearsRemovedOnComplete() {
        val recorded = engine.recordLocally(sample())
        engine.markPlatformLinked(recorded.id, "uuid-old")
        engine.applyPlatformDeletion(
            platform = HealthSyncPlatform.APPLE_HEALTH,
            platformRecordId = "uuid-old"
        )
        val removed = checkNotNull(store.getWorkout(recorded.id))
        assertTrue(PlatformAppendOnlyPolicy.shouldAttemptPlatformRestore(removed))
        assertEquals(1, engine.workoutsEligibleForRestore().size)

        val prepared = engine.prepareRestore(recorded.id)
        assertNotNull(prepared)
        assertEquals(2L, prepared.identity.syncVersion)
        assertEquals(recorded.identity.syncIdentifier, prepared.identity.syncIdentifier)
        assertEquals(null, prepared.platformRecordId)
        assertTrue(prepared.removedFromPlatform)

        engine.completeRestore(recorded.id, "uuid-restored")
        val done = checkNotNull(store.getWorkout(recorded.id))
        assertFalse(done.removedFromPlatform)
        assertEquals("uuid-restored", done.platformRecordId)
        assertEquals(2L, done.identity.syncVersion)
        assertTrue(engine.workoutsEligibleForRestore().isEmpty())
        // Still not a first-write candidate (already linked).
        assertFalse(PlatformAppendOnlyPolicy.shouldAttemptPlatformWrite(done))
    }

    @Test
    fun restore_reconcileWhenAlreadyOnPlatform_clearsFlagWithoutNeedingMutationApis() {
        val recorded = engine.recordLocally(sample())
        engine.applyPlatformDeletion(
            platform = HealthSyncPlatform.HEALTH_CONNECT,
            syncIdentifier = recorded.identity.syncIdentifier
        )
        engine.prepareRestore(recorded.id)
        engine.reconcileAlreadyOnPlatform(recorded.id, "already-there")
        val done = checkNotNull(store.getWorkout(recorded.id))
        assertFalse(done.removedFromPlatform)
        assertEquals("already-there", done.platformRecordId)
    }

    @Test
    fun restore_rejectsForeignOrigin() {
        val foreign =
            sample().copy(
                origin = HealthRecordOrigin.APPLE_HEALTH,
                sourceBundle = "com.apple.Health",
                removedFromPlatform = true
            )
        store.upsertWorkout(foreign)
        assertFalse(PlatformAppendOnlyPolicy.shouldAttemptPlatformRestore(foreign))
        assertEquals(null, engine.prepareRestore(foreign.id))
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

    @Test
    fun healthConnectClientRecordVersion_isFixedAtOne_neverBumpForRestore() {
        // Health Connect treats a higher clientRecordVersion as a rewrite of the same
        // clientRecordId. Restores must insert-if-absent with version 1 (weigh-in pattern).
        assertEquals(1L, PlatformAppendOnlyPolicy.HEALTH_CONNECT_CLIENT_RECORD_VERSION)
        val recorded = engine.recordLocally(sample())
        engine.markPlatformLinked(recorded.id, "uuid-old")
        engine.applyPlatformDeletion(
            platform = HealthSyncPlatform.HEALTH_CONNECT,
            platformRecordId = "uuid-old"
        )
        val prepared = checkNotNull(engine.prepareRestore(recorded.id))
        // Local syncVersion may bump for Apple Health metadata; HC writers ignore it and
        // always stamp HEALTH_CONNECT_CLIENT_RECORD_VERSION.
        assertTrue(prepared.identity.syncVersion >= 1L)
        assertEquals(
            PlatformAppendOnlyPolicy.HEALTH_CONNECT_CLIENT_RECORD_VERSION,
            1L
        )
    }
}
