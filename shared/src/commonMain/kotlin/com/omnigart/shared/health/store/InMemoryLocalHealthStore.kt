package com.omnigart.shared.health.store

/**
 * In-memory [LocalHealthStore] for tests and hosts that do not yet open SQLDelight.
 */
class InMemoryLocalHealthStore : LocalHealthStore {
    private val workouts = linkedMapOf<String, LocalHealthWorkout>()
    private val syncById = linkedMapOf<String, String>()
    private val syncState = mutableMapOf<HealthSyncPlatform, HealthSyncStatus>()
    private val conflicts = linkedMapOf<String, HealthConflictRecord>()

    override fun upsertWorkout(workout: LocalHealthWorkout) {
        workouts[workout.id] = workout
        syncById[workout.identity.syncIdentifier] = workout.id
    }

    override fun getWorkout(id: String): LocalHealthWorkout? = workouts[id]?.takeUnless { it.deleted }

    override fun getBySyncIdentifier(syncIdentifier: String): LocalHealthWorkout? {
        val id = syncById[syncIdentifier] ?: return null
        return getWorkout(id)
    }

    override fun listWorkouts(range: HealthExportRange): List<LocalHealthWorkout> = workouts.values
        .filter { !it.deleted }
        .filter { range.start == null || it.startAt >= range.start }
        .filter { range.endExclusive == null || it.startAt < range.endExclusive }
        .sortedByDescending { it.startAt }

    override fun softDeleteWorkout(id: String) {
        val existing = workouts[id] ?: return
        workouts[id] =
            existing.copy(
                deleted = true,
                identity = existing.identity.bumped()
            )
    }

    override fun getSyncStatus(platform: HealthSyncPlatform): HealthSyncStatus? = syncState[platform]

    override fun upsertSyncStatus(status: HealthSyncStatus) {
        syncState[status.platform] = status
    }

    override fun listOpenConflicts(): List<HealthConflictRecord> =
        conflicts.values.filter { !it.resolved }.sortedByDescending { it.createdAt }

    override fun upsertConflict(conflict: HealthConflictRecord) {
        conflicts[conflict.id] = conflict
    }

    override fun resolveConflict(id: String) {
        conflicts[id]?.let { conflicts[id] = it.copy(resolved = true) }
    }
}
