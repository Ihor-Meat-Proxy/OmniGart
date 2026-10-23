package com.omnigart.shared.health.store

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
 * - Incremental via change tokens / anchors stored in [health_sync_state].
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
