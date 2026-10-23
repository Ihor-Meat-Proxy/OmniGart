package com.omnigart.shared.health.store

import kotlin.time.Clock

/**
 * Orchestrates local-first persistence and optional two-way platform sync.
 *
 * Rules:
 * - Always write to [LocalHealthStore] first.
 * - Never write back data that originated from another app.
 * - Never delete platform data OmniGart did not create.
 * - Idempotent via [HealthSyncIdentity] (sync identifier + version).
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
                    .copy(updatedAt = clock.now())
            }
        store.upsertWorkout(merged)
        return merged
    }

    /**
     * Candidates OmniGart may push to a platform: OmniGart-origin only, not deleted.
     */
    fun workoutsEligibleForPlatformWrite(): List<LocalHealthWorkout> =
        store.listWorkouts().filter { HealthDataMapper.mayWriteToPlatform(it) && !it.deleted }

    /**
     * Apply a platform-side deletion only when the local record is OmniGart-owned and linked.
     * Foreign platform deletions soft-delete the local mirror but never cascade to the other platform.
     */
    fun applyPlatformDeletion(syncIdentifier: String, platform: HealthSyncPlatform) {
        val local = store.getBySyncIdentifier(syncIdentifier) ?: return
        store.softDeleteWorkout(local.id)
        val status =
            store.getSyncStatus(platform)
                ?: HealthSyncStatus(platform, authStatus = "AUTHORIZED")
        store.upsertSyncStatus(status.copy(lastSyncAt = clock.now()))
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

    fun bumpVersionForResave(workout: LocalHealthWorkout): LocalHealthWorkout {
        val bumped = workout.copy(identity = workout.identity.bumped(), updatedAt = clock.now())
        store.upsertWorkout(bumped)
        return bumped
    }
}
