package com.omnigart.shared.health.store

import com.omnigart.shared.db.OmniGartDatabase
import com.omnigart.shared.health.WorkoutActivityType
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Local-first health data store. Every workout OmniGart records or receives is persisted here,
 * whether or not Apple Health / Health Connect permission is granted.
 */
interface LocalHealthStore {
    fun upsertWorkout(workout: LocalHealthWorkout)

    fun getWorkout(id: String): LocalHealthWorkout?

    fun getBySyncIdentifier(syncIdentifier: String): LocalHealthWorkout?

    fun getByPlatformRecordId(platformRecordId: String): LocalHealthWorkout?

    fun listWorkouts(range: HealthExportRange = HealthExportRange()): List<LocalHealthWorkout>

    /** Soft-delete locally only. Never deletes Apple Health / Health Connect objects. */
    fun softDeleteWorkout(id: String)

    /** Platform removed the sample; keep OmniGart data and stop platform re-writes. */
    fun markRemovedFromPlatform(id: String)

    /** Clear [LocalHealthWorkout.removedFromPlatform] after restore or reconcile. */
    fun clearRemovedFromPlatform(id: String)

    /** After a successful first-time platform insert. */
    fun linkPlatformRecord(id: String, platformRecordId: String)

    fun getSyncStatus(platform: HealthSyncPlatform): HealthSyncStatus?

    fun upsertSyncStatus(status: HealthSyncStatus)

    fun listOpenConflicts(): List<HealthConflictRecord>

    fun upsertConflict(conflict: HealthConflictRecord)

    fun resolveConflict(id: String)
}

class SqlDelightLocalHealthStore(private val db: OmniGartDatabase, private val clock: Clock = Clock.System) : LocalHealthStore {
    private val q get() = db.omniGartQueries

    override fun upsertWorkout(workout: LocalHealthWorkout) {
        q.transaction {
            q.upsertLocalHealthWorkout(
                id = workout.id,
                sync_identifier = workout.identity.syncIdentifier,
                sync_version = workout.identity.syncVersion,
                activity_type = workout.activityType.name,
                start_at = workout.startAt.toString(),
                end_at = workout.endAt.toString(),
                active_energy_kcal = workout.activeEnergyKcal,
                basal_energy_kcal = workout.basalEnergyKcal,
                distance_metres = workout.distanceMetres,
                steps = workout.steps,
                average_heart_rate_bpm = workout.averageHeartRateBpm,
                max_heart_rate_bpm = workout.maxHeartRateBpm,
                average_cadence_spm = workout.averageCadenceSpm,
                plan_id = workout.planId,
                session_id = workout.sessionId,
                origin = workout.origin.name,
                source_bundle = workout.sourceBundle,
                source_name = workout.sourceName,
                platform_record_id = workout.platformRecordId,
                deleted = if (workout.deleted) 1L else 0L,
                removed_from_platform = if (workout.removedFromPlatform) 1L else 0L,
                updated_at = workout.updatedAt.toString(),
                notes = workout.notes
            )
            q.deleteSamplesForWorkout(workout.id)
            workout.heartRateSeries.forEachIndexed { index, point ->
                q.insertLocalHealthSample(
                    id = "${workout.id}-hr-$index",
                    workout_id = workout.id,
                    sample_type = HealthSampleType.HEART_RATE.name,
                    recorded_at = point.recordedAt.toString(),
                    end_at = null,
                    value_ = point.beatsPerMinute,
                    unit = "bpm"
                )
            }
            workout.samples.forEach { sample ->
                q.insertLocalHealthSample(
                    id = sample.id,
                    workout_id = workout.id,
                    sample_type = sample.type.name,
                    recorded_at = sample.recordedAt.toString(),
                    end_at = sample.endAt?.toString(),
                    value_ = sample.value,
                    unit = sample.unit
                )
            }
            q.deleteRouteForWorkout(workout.id)
            workout.route.forEach { point ->
                q.insertLocalHealthRoutePoint(
                    id = point.id,
                    workout_id = workout.id,
                    recorded_at = point.recordedAt.toString(),
                    latitude = point.latitude,
                    longitude = point.longitude,
                    altitude_metres = point.altitudeMetres,
                    point_index = point.pointIndex.toLong()
                )
            }
            q.deleteEventsForWorkout(workout.id)
            workout.events.forEach { event ->
                q.insertLocalHealthEvent(
                    id = event.id,
                    workout_id = workout.id,
                    event_type = event.type.name,
                    start_at = event.startAt.toString(),
                    end_at = event.endAt?.toString(),
                    label = event.label,
                    event_index = event.eventIndex.toLong()
                )
            }
            q.deleteSetsForWorkout(workout.id)
            workout.sets.forEach { set ->
                q.insertLocalHealthSet(
                    id = set.id,
                    workout_id = workout.id,
                    exercise_key = set.exerciseKey,
                    set_index = set.setIndex.toLong(),
                    reps = set.reps?.toLong(),
                    load_kg = set.loadKg,
                    duration_seconds = set.durationSeconds?.toLong(),
                    distance_metres = set.distanceMetres,
                    rir = set.rir,
                    rpe = set.rpe,
                    is_warmup = if (set.isWarmup) 1L else 0L,
                    is_drop_set = if (set.isDropSet) 1L else 0L,
                    is_failure = if (set.isFailure) 1L else 0L,
                    note = set.note
                )
            }
        }
    }

    override fun getWorkout(id: String): LocalHealthWorkout? = q.selectLocalHealthWorkout(id).executeAsOneOrNull()?.let { hydrate(it) }

    override fun getBySyncIdentifier(syncIdentifier: String): LocalHealthWorkout? =
        q.selectLocalHealthWorkoutBySyncId(syncIdentifier).executeAsOneOrNull()?.let { hydrate(it) }

    override fun getByPlatformRecordId(platformRecordId: String): LocalHealthWorkout? =
        q.selectLocalHealthWorkoutByPlatformRecordId(platformRecordId).executeAsOneOrNull()?.let { hydrate(it) }

    override fun listWorkouts(range: HealthExportRange): List<LocalHealthWorkout> {
        val start = range.start?.toString() ?: Instant.fromEpochMilliseconds(0).toString()
        val end = range.endExclusive?.toString()
            ?: Instant.fromEpochMilliseconds(Long.MAX_VALUE / 2).toString()
        return q.selectLocalHealthWorkoutsInRange(start, end).executeAsList().map { hydrate(it) }
    }

    override fun softDeleteWorkout(id: String) {
        q.softDeleteLocalHealthWorkout(clock.now().toString(), id)
    }

    override fun markRemovedFromPlatform(id: String) {
        q.markLocalHealthWorkoutRemovedFromPlatform(clock.now().toString(), id)
    }

    override fun clearRemovedFromPlatform(id: String) {
        q.clearLocalHealthWorkoutRemovedFromPlatform(clock.now().toString(), id)
    }

    override fun linkPlatformRecord(id: String, platformRecordId: String) {
        q.linkLocalHealthWorkoutPlatformRecord(platformRecordId, clock.now().toString(), id)
    }

    override fun getSyncStatus(platform: HealthSyncPlatform): HealthSyncStatus? =
        q.selectHealthSyncState(platform.name).executeAsOneOrNull()?.let {
            HealthSyncStatus(
                platform = platform,
                authStatus = it.auth_status,
                lastSyncAt = it.last_sync_at?.let(Instant::parse),
                changeToken = it.change_token
            )
        }

    override fun upsertSyncStatus(status: HealthSyncStatus) {
        q.upsertHealthSyncState(
            platform = status.platform.name,
            change_token = status.changeToken,
            last_sync_at = status.lastSyncAt?.toString(),
            auth_status = status.authStatus
        )
    }

    override fun listOpenConflicts(): List<HealthConflictRecord> = q.selectOpenHealthConflicts().executeAsList().map { row ->
        HealthConflictRecord(
            id = row.id,
            workoutId = row.workout_id,
            platform = HealthSyncPlatform.valueOf(row.platform),
            fieldName = row.field_name,
            localValue = row.local_value,
            platformValue = row.platform_value,
            preferredSource = row.preferred_source,
            resolved = row.resolved != 0L,
            createdAt = Instant.parse(row.created_at)
        )
    }

    override fun upsertConflict(conflict: HealthConflictRecord) {
        q.upsertHealthConflict(
            id = conflict.id,
            workout_id = conflict.workoutId,
            platform = conflict.platform.name,
            field_name = conflict.fieldName,
            local_value = conflict.localValue,
            platform_value = conflict.platformValue,
            preferred_source = conflict.preferredSource,
            resolved = if (conflict.resolved) 1L else 0L,
            created_at = conflict.createdAt.toString()
        )
    }

    override fun resolveConflict(id: String) {
        q.resolveHealthConflict(id)
    }

    private fun hydrate(row: com.omnigart.shared.db.Local_health_workout): LocalHealthWorkout {
        val sampleRows = q.selectSamplesForWorkout(row.id).executeAsList()
        val heartRate =
            sampleRows
                .filter { it.sample_type == HealthSampleType.HEART_RATE.name }
                .map {
                    LocalHeartRatePoint(
                        recordedAt = Instant.parse(it.recorded_at),
                        beatsPerMinute = it.value_
                    )
                }
        val otherSamples =
            sampleRows
                .filter { it.sample_type != HealthSampleType.HEART_RATE.name }
                .map {
                    LocalQuantitySample(
                        id = it.id,
                        type = HealthSampleType.valueOf(it.sample_type),
                        recordedAt = Instant.parse(it.recorded_at),
                        endAt = it.end_at?.let(Instant::parse),
                        value = it.value_,
                        unit = it.unit
                    )
                }
        val route =
            q.selectRouteForWorkout(row.id).executeAsList().map {
                LocalRoutePoint(
                    id = it.id,
                    recordedAt = Instant.parse(it.recorded_at),
                    latitude = it.latitude,
                    longitude = it.longitude,
                    altitudeMetres = it.altitude_metres,
                    pointIndex = it.point_index.toInt()
                )
            }
        val events =
            q.selectEventsForWorkout(row.id).executeAsList().map {
                LocalWorkoutEvent(
                    id = it.id,
                    type = HealthEventType.valueOf(it.event_type),
                    startAt = Instant.parse(it.start_at),
                    endAt = it.end_at?.let(Instant::parse),
                    label = it.label,
                    eventIndex = it.event_index.toInt()
                )
            }
        val sets =
            q.selectSetsForWorkout(row.id).executeAsList().map {
                LocalWorkoutSet(
                    id = it.id,
                    exerciseKey = it.exercise_key,
                    setIndex = it.set_index.toInt(),
                    reps = it.reps?.toInt(),
                    loadKg = it.load_kg,
                    durationSeconds = it.duration_seconds?.toInt(),
                    distanceMetres = it.distance_metres,
                    rir = it.rir,
                    rpe = it.rpe,
                    isWarmup = it.is_warmup != 0L,
                    isDropSet = it.is_drop_set != 0L,
                    isFailure = it.is_failure != 0L,
                    note = it.note
                )
            }
        return LocalHealthWorkout(
            id = row.id,
            identity = HealthSyncIdentity(row.sync_identifier, row.sync_version),
            activityType = WorkoutActivityType.valueOf(row.activity_type),
            startAt = Instant.parse(row.start_at),
            endAt = Instant.parse(row.end_at),
            activeEnergyKcal = row.active_energy_kcal,
            basalEnergyKcal = row.basal_energy_kcal,
            distanceMetres = row.distance_metres,
            steps = row.steps,
            averageHeartRateBpm = row.average_heart_rate_bpm,
            maxHeartRateBpm = row.max_heart_rate_bpm,
            averageCadenceSpm = row.average_cadence_spm,
            planId = row.plan_id,
            sessionId = row.session_id,
            origin = HealthRecordOrigin.valueOf(row.origin),
            sourceBundle = row.source_bundle,
            sourceName = row.source_name,
            platformRecordId = row.platform_record_id,
            deleted = row.deleted != 0L,
            removedFromPlatform = row.removed_from_platform != 0L,
            updatedAt = Instant.parse(row.updated_at),
            notes = row.notes,
            heartRateSeries = heartRate,
            samples = otherSamples,
            route = route,
            events = events,
            sets = sets
        )
    }
}
