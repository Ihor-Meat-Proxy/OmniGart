package com.omnigart.shared.health.store

import com.omnigart.shared.health.WorkoutActivityType
import kotlin.time.Instant
import kotlinx.serialization.Serializable

/** Where a local health record originally came from. */
@Serializable
enum class HealthRecordOrigin {
    /** Recorded inside OmniGart (phone, watch, or Wear). */
    OMNIGART,

    /** Imported from Apple Health / Fitness (e.g. system Workout app). */
    APPLE_HEALTH,

    /** Imported from Android Health Connect. */
    HEALTH_CONNECT,

    /** Restored from an OmniGart JSON export. */
    OMNIGART_JSON_IMPORT
}

@Serializable
enum class HealthSampleType {
    HEART_RATE,
    ACTIVE_ENERGY,
    BASAL_ENERGY,
    DISTANCE,
    STEPS,
    CADENCE
}

@Serializable
enum class HealthEventType {
    PAUSE,
    RESUME,
    LAP,
    SEGMENT,
    MARKER
}

@Serializable
enum class HealthSyncPlatform {
    APPLE_HEALTH,
    HEALTH_CONNECT
}

/**
 * Stable sync identity for platform dedupe / first-write / restore.
 *
 * Apple Health: [HKMetadataKeySyncIdentifier] / [HKMetadataKeySyncVersion].
 * Health Connect: [Metadata.clientRecordId] / [Metadata.clientRecordVersion].
 *
 * Platform writes are append-only: never call platform delete/update APIs.
 * - First insert stamps version 1.
 * - Local edits do **not** bump version (stay in OmniGart only).
 * - User-initiated **restore** after platform deletion bumps version and inserts again
 *   with the same sync id (HealthKit/HC allow reusing a deleted sync id with a higher version).
 * - If the sync id already exists on the platform, skip the write.
 */
@Serializable
data class HealthSyncIdentity(val syncIdentifier: String, val syncVersion: Long = 1) {
    init {
        require(syncIdentifier.isNotBlank()) { "syncIdentifier must not be blank" }
        require(syncVersion >= 1) { "syncVersion must be >= 1" }
    }

    /** Bump for a user-initiated restore insert after platform deletion. */
    fun bumped(): HealthSyncIdentity = copy(syncVersion = syncVersion + 1)
}

@Serializable
data class LocalHeartRatePoint(val recordedAt: Instant, val beatsPerMinute: Double) {
    init {
        require(beatsPerMinute > 0)
    }
}

@Serializable
data class LocalQuantitySample(
    val id: String,
    val type: HealthSampleType,
    val recordedAt: Instant,
    val endAt: Instant? = null,
    val value: Double,
    val unit: String
)

@Serializable
data class LocalRoutePoint(
    val id: String,
    val recordedAt: Instant,
    val latitude: Double,
    val longitude: Double,
    val altitudeMetres: Double? = null,
    val pointIndex: Int
)

@Serializable
data class LocalWorkoutEvent(
    val id: String,
    val type: HealthEventType,
    val startAt: Instant,
    val endAt: Instant? = null,
    val label: String? = null,
    val eventIndex: Int
)

@Serializable
data class LocalWorkoutSet(
    val id: String,
    val exerciseKey: String,
    val setIndex: Int,
    val reps: Int? = null,
    val loadKg: Double? = null,
    val durationSeconds: Int? = null,
    val distanceMetres: Double? = null,
    val rir: Double? = null,
    val rpe: Double? = null,
    val isWarmup: Boolean = false,
    val isDropSet: Boolean = false,
    val isFailure: Boolean = false,
    val note: String? = null
)

/**
 * Canonical local workout. Always stored in OmniGart's DB regardless of platform permissions.
 */
@Serializable
data class LocalHealthWorkout(
    val id: String,
    val identity: HealthSyncIdentity,
    val activityType: WorkoutActivityType,
    val startAt: Instant,
    val endAt: Instant,
    val activeEnergyKcal: Double? = null,
    val basalEnergyKcal: Double? = null,
    val distanceMetres: Double? = null,
    val steps: Long? = null,
    val averageHeartRateBpm: Double? = null,
    val maxHeartRateBpm: Double? = null,
    val averageCadenceSpm: Double? = null,
    val planId: String? = null,
    val sessionId: String? = null,
    val origin: HealthRecordOrigin,
    val sourceBundle: String? = null,
    val sourceName: String? = null,
    val platformRecordId: String? = null,
    val deleted: Boolean = false,
    /**
     * Platform deleted or revoked this sample. OmniGart keeps the local copy;
     * never re-inserts to the platform for this row.
     */
    val removedFromPlatform: Boolean = false,
    val updatedAt: Instant,
    val notes: String? = null,
    val heartRateSeries: List<LocalHeartRatePoint> = emptyList(),
    val samples: List<LocalQuantitySample> = emptyList(),
    val route: List<LocalRoutePoint> = emptyList(),
    val events: List<LocalWorkoutEvent> = emptyList(),
    val sets: List<LocalWorkoutSet> = emptyList()
) {
    init {
        require(endAt >= startAt) { "endAt must be >= startAt" }
    }

    fun durationSeconds(): Long = (endAt.toEpochMilliseconds() - startAt.toEpochMilliseconds()) / 1000L
}

@Serializable
data class HealthSyncStatus(
    val platform: HealthSyncPlatform,
    val authStatus: String,
    val lastSyncAt: Instant? = null,
    val changeToken: String? = null
)

@Serializable
data class HealthConflictRecord(
    val id: String,
    val workoutId: String,
    val platform: HealthSyncPlatform,
    val fieldName: String,
    val localValue: String?,
    val platformValue: String?,
    val preferredSource: String,
    val resolved: Boolean = false,
    val createdAt: Instant
)

@Serializable
data class HealthExportRange(val start: Instant? = null, val endExclusive: Instant? = null)
