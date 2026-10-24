package com.omnigart.shared.health.store

import com.omnigart.shared.health.store.device.DeviceTransferIngestResult
import com.omnigart.shared.health.store.device.DeviceWorkoutTransferCodec
import com.omnigart.shared.health.store.device.DeviceWorkoutTransferDocument
import kotlin.time.Clock

/**
 * Orchestrates local-first persistence and optional platform sync.
 *
 * Rules:
 * - Always write to [LocalHealthStore] first.
 * - Never write back data that originated from another app.
 * - Platform writes are **append-only**: never delete or rewrite Apple Health /
 *   Health Connect objects (including OmniGart-owned ones).
 * - Before a platform insert, writers exist-check [HealthSyncIdentity.syncIdentifier]
 *   / clientRecordId and skip when already present.
 * - Local edits stay in OmniGart only.
 * - Platform-side deletion marks [LocalHealthWorkout.removedFromPlatform] and keeps data.
 * - User-initiated **restore** bumps sync version and inserts again (still no update/delete).
 * - Incremental via change tokens / anchors stored in [health_sync_state].
 * - Device transfer (Watch / Wear → phone) upserts by sync id; Apple Watch path never
 *   re-writes HealthKit; Wear path may append-only insert into Health Connect once.
 */
class HealthSyncEngine(private val store: LocalHealthStore, private val clock: Clock = Clock.System) {
    /**
     * Persist a workout OmniGart just recorded. Works with or without platform permission.
     */
    fun recordLocally(workout: LocalHealthWorkout): LocalHealthWorkout {
        val stamped =
            workout.copy(
                updatedAt = clock.now(),
                origin = HealthRecordOrigin.OMNIGART,
                sourceBundle = workout.sourceBundle ?: HealthDataMapper.SOURCE_BUNDLE_OMNIGART
            )
        store.upsertWorkout(stamped)
        return stamped
    }

    /**
     * Ingest a watch / Wear transfer on the phone. Idempotent by [HealthSyncIdentity.syncIdentifier].
     * Does not write to any platform store — callers decide (iPhone: never HK; Android: HC once).
     */
    fun ingestDeviceTransfer(document: DeviceWorkoutTransferDocument): DeviceTransferIngestResult {
        if (document.schema != DeviceWorkoutTransferDocument.SCHEMA_ID) {
            return DeviceTransferIngestResult.REJECTED
        }
        if (!DeviceWorkoutTransferDocument.isSupportedSchemaVersion(document.schemaVersion)) {
            return DeviceTransferIngestResult.REJECTED
        }
        if (!HealthDataMapper.mayWriteToPlatform(
                DeviceWorkoutTransferCodec.toLocalWorkout(document, clock)
            )
        ) {
            return DeviceTransferIngestResult.REJECTED
        }
        val existing = store.getBySyncIdentifier(document.syncIdentifier)
        if (existing != null) {
            return DeviceTransferIngestResult.ALREADY_PRESENT
        }
        recordLocally(DeviceWorkoutTransferCodec.toLocalWorkout(document, clock))
        return DeviceTransferIngestResult.INSERTED
    }

    /**
     * Import a workout read from a platform store (system Workout app, Health Connect, etc.).
     * Links to [planId] when known. Does not write back to the platform.
     */
    fun importFromPlatform(incoming: LocalHealthWorkout, platform: HealthSyncPlatform): LocalHealthWorkout {
        val existing =
            store.getBySyncIdentifier(incoming.identity.syncIdentifier)
                ?: store.getWorkout(incoming.id)
        val merged =
            if (existing == null) {
                incoming.copy(updatedAt = clock.now())
            } else {
                val conflicts =
                    HealthConflictPolicy.findScalarConflicts(existing, incoming, platform)
                conflicts.forEach { store.upsertConflict(it) }
                HealthConflictPolicy.resolvePreferringOrigin(existing, incoming)
                    .copy(
                        updatedAt = clock.now(),
                        // Re-import from platform clears a prior "removed" flag when the sample returns.
                        removedFromPlatform = false
                    )
            }
        store.upsertWorkout(merged)
        return merged
    }

    /**
     * Candidates OmniGart may attempt a first-time platform insert for.
     * Writers must still exist-check sync id / clientRecordId and skip if present.
     */
    fun workoutsEligibleForPlatformWrite(): List<LocalHealthWorkout> =
        store.listWorkouts().filter { PlatformAppendOnlyPolicy.shouldAttemptPlatformWrite(it) }

    /** OmniGart-owned local copies that the user may restore to the platform. */
    fun workoutsEligibleForRestore(): List<LocalHealthWorkout> =
        store.listWorkouts().filter { PlatformAppendOnlyPolicy.shouldAttemptPlatformRestore(it) }

    /**
     * Prepare a restore: bump [HealthSyncIdentity.syncVersion] for a new platform insert
     * with the same sync id. Returns null when not eligible.
     */
    fun prepareRestore(localId: String): LocalHealthWorkout? {
        val local = store.getWorkout(localId) ?: return null
        if (!PlatformAppendOnlyPolicy.shouldAttemptPlatformRestore(local)) return null
        val prepared =
            local.copy(
                identity = local.identity.bumped(),
                // New platform object after restore; clear stale UUID until insert succeeds.
                platformRecordId = null,
                updatedAt = clock.now()
            )
        store.upsertWorkout(prepared)
        return prepared
    }

    /**
     * Successful restore insert: clear removed flag and link the new platform record id.
     */
    fun completeRestore(localId: String, platformRecordId: String) {
        val local = store.getWorkout(localId) ?: return
        store.upsertWorkout(
            local.copy(
                removedFromPlatform = false,
                platformRecordId = platformRecordId,
                updatedAt = clock.now()
            )
        )
    }

    /**
     * Sync id / clientRecordId already present on the platform — skip insert, clear removed flag.
     */
    fun reconcileAlreadyOnPlatform(localId: String, platformRecordId: String? = null) {
        val local = store.getWorkout(localId) ?: return
        store.upsertWorkout(
            local.copy(
                removedFromPlatform = false,
                platformRecordId = platformRecordId ?: local.platformRecordId,
                updatedAt = clock.now()
            )
        )
    }

    /**
     * Platform deleted a sample. Keep the OmniGart copy; mark it removed from the platform.
     * Never cascades a delete to the other platform or to HealthKit/Health Connect.
     */
    fun applyPlatformDeletion(platform: HealthSyncPlatform, syncIdentifier: String? = null, platformRecordId: String? = null) {
        val local =
            when {
                !syncIdentifier.isNullOrBlank() -> store.getBySyncIdentifier(syncIdentifier)
                !platformRecordId.isNullOrBlank() -> store.getByPlatformRecordId(platformRecordId)
                else -> null
            } ?: return
        store.markRemovedFromPlatform(local.id)
        val status =
            store.getSyncStatus(platform)
                ?: HealthSyncStatus(platform, authStatus = "AUTHORIZED")
        store.upsertSyncStatus(status.copy(lastSyncAt = clock.now()))
    }

    /** Record that a first-time platform insert succeeded (local bookkeeping only). */
    fun markPlatformLinked(localId: String, platformRecordId: String) {
        store.linkPlatformRecord(localId, platformRecordId)
    }

    fun markSynced(platform: HealthSyncPlatform, authStatus: String, changeToken: String?) {
        store.upsertSyncStatus(
            HealthSyncStatus(
                platform = platform,
                authStatus = authStatus,
                lastSyncAt = clock.now(),
                changeToken = changeToken
            )
        )
    }
}
