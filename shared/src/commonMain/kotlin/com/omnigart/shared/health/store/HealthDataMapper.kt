package com.omnigart.shared.health.store

import com.omnigart.shared.health.HealthWorkout
import com.omnigart.shared.health.WorkoutActivityType
import kotlin.math.abs
import kotlin.math.round

/**
 * Lossless mapping helpers between OmniGart local records and platform field shapes.
 *
 * Units of record (canonical local):
 * - Energy: kilocalories, split into active vs basal
 * - Distance: metres
 * - Heart rate: beats per minute, full series preferred over averages
 * - Cadence: steps per minute (or strokes/rev depending on activity — stored as SPM)
 * - Timestamps: Instant (UTC epoch); platforms convert to local zone at the boundary
 *
 * See `docs/HEALTH_DATA_MAPPING.md` for the full matrix.
 */
object HealthDataMapper {
    const val SOURCE_BUNDLE_OMNIGART = "com.omnigart"
    const val METADATA_SYNC_IDENTIFIER = "omnigart.syncIdentifier"
    const val METADATA_SYNC_VERSION = "omnigart.syncVersion"
    const val METADATA_ORIGIN = "omnigart.origin"
    const val METADATA_SCHEMA_VERSION = "omnigart.healthSchemaVersion"
    const val HEALTH_SCHEMA_VERSION = 1

    /** HKMetadataKeySyncIdentifier / Health Connect clientRecordId prefix. */
    fun syncIdentifierFor(localId: String): String = "omnigart.workout.$localId"

    fun activityToHealthKitRaw(type: WorkoutActivityType): String = when (type) {
        WorkoutActivityType.STRENGTH -> "traditionalStrengthTraining"
        WorkoutActivityType.RUNNING -> "running"
        WorkoutActivityType.CYCLING -> "cycling"
        WorkoutActivityType.SWIMMING -> "swimming"
        WorkoutActivityType.WALKING -> "walking"
        WorkoutActivityType.HIIT -> "highIntensityIntervalTraining"
        WorkoutActivityType.YOGA -> "yoga"
        WorkoutActivityType.PILATES -> "pilates"
        WorkoutActivityType.OTHER -> "other"
    }

    fun activityFromHealthKitRaw(raw: String): WorkoutActivityType = when (raw.lowercase()) {
        "traditionalstrengthtraining", "functionalstrengthtraining" -> WorkoutActivityType.STRENGTH
        "running", "runningtreadmill" -> WorkoutActivityType.RUNNING
        "cycling", "handcycling" -> WorkoutActivityType.CYCLING
        "swimming", "swimBikeRun" -> WorkoutActivityType.SWIMMING
        "walking", "hiking" -> WorkoutActivityType.WALKING
        "highintensityintervaltraining" -> WorkoutActivityType.HIIT
        "yoga" -> WorkoutActivityType.YOGA
        "pilates" -> WorkoutActivityType.PILATES
        else -> WorkoutActivityType.OTHER
    }

    fun activityToHealthConnectType(type: WorkoutActivityType): String = when (type) {
        WorkoutActivityType.STRENGTH -> "EXERCISE_TYPE_STRENGTH_TRAINING"
        WorkoutActivityType.RUNNING -> "EXERCISE_TYPE_RUNNING"
        WorkoutActivityType.CYCLING -> "EXERCISE_TYPE_BIKING"
        WorkoutActivityType.SWIMMING -> "EXERCISE_TYPE_SWIMMING_POOL"
        WorkoutActivityType.WALKING -> "EXERCISE_TYPE_WALKING"
        WorkoutActivityType.HIIT -> "EXERCISE_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING"
        WorkoutActivityType.YOGA -> "EXERCISE_TYPE_YOGA"
        WorkoutActivityType.PILATES -> "EXERCISE_TYPE_PILATES"
        WorkoutActivityType.OTHER -> "EXERCISE_TYPE_OTHER_WORKOUT"
    }

    /**
     * Health Connect exercise type for an OmniGart session.
     * Indoor treadmill runs use [EXERCISE_TYPE_RUNNING_TREADMILL]; outdoor Zone 2 stays RUNNING.
     */
    fun activityToHealthConnectType(type: WorkoutActivityType, indoorTreadmill: Boolean): String =
        if (indoorTreadmill && type == WorkoutActivityType.RUNNING) {
            "EXERCISE_TYPE_RUNNING_TREADMILL"
        } else {
            activityToHealthConnectType(type)
        }

    /** HealthKit activity raw string; treadmill stays `running` with indoor location metadata. */
    fun activityToHealthKitRaw(type: WorkoutActivityType, indoorTreadmill: Boolean): String =
        if (indoorTreadmill && type == WorkoutActivityType.RUNNING) {
            "running"
        } else {
            activityToHealthKitRaw(type)
        }

    fun activityFromHealthConnectType(raw: String): WorkoutActivityType = when {
        raw.contains("STRENGTH", ignoreCase = true) -> WorkoutActivityType.STRENGTH
        raw.contains("RUNNING", ignoreCase = true) -> WorkoutActivityType.RUNNING
        raw.contains("BIKING", ignoreCase = true) ||
            raw.contains("CYCLING", ignoreCase = true) -> WorkoutActivityType.CYCLING
        raw.contains("SWIMMING", ignoreCase = true) -> WorkoutActivityType.SWIMMING
        raw.contains("WALKING", ignoreCase = true) -> WorkoutActivityType.WALKING
        raw.contains("HIGH_INTENSITY", ignoreCase = true) ||
            raw.contains("HIIT", ignoreCase = true) -> WorkoutActivityType.HIIT
        raw.contains("YOGA", ignoreCase = true) -> WorkoutActivityType.YOGA
        raw.contains("PILATES", ignoreCase = true) -> WorkoutActivityType.PILATES
        else -> WorkoutActivityType.OTHER
    }

    /** Joules ↔ kilocalories (nutrition / physiological kcal = 4184 J). */
    fun kcalToJoules(kcal: Double): Double = kcal * 4184.0

    fun joulesToKcal(joules: Double): Double = joules / 4184.0

    fun metresToKilometres(metres: Double): Double = metres / 1000.0

    fun kilometresToMetres(km: Double): Double = km * 1000.0

    /** Round trip helpers for floating point stability in tests. */
    fun roundEnergyKcal(value: Double): Double = round(value * 1000.0) / 1000.0

    fun roundDistanceMetres(value: Double): Double = round(value * 1000.0) / 1000.0

    fun roundBpm(value: Double): Double = round(value * 10.0) / 10.0

    fun nearlyEqual(a: Double?, b: Double?, epsilon: Double = 1e-6): Boolean {
        if (a == null && b == null) return true
        if (a == null || b == null) return false
        return abs(a - b) <= epsilon
    }

    /**
     * Build a thin [HealthWorkout] view for the legacy [HealthClient] surface.
     * Prefer [LocalHealthWorkout] for full-fidelity storage.
     */
    fun toLegacyHealthWorkout(local: LocalHealthWorkout): HealthWorkout = HealthWorkout(
        id = local.id,
        activityType = local.activityType,
        start = local.startAt,
        end = local.endAt,
        energyKcal = local.activeEnergyKcal,
        distanceMetres = local.distanceMetres,
        averageHeartRateBpm = local.averageHeartRateBpm,
        sourceName = local.sourceName
    )

    /**
     * Merge platform import into local: keep the richest series locally.
     * Never drop existing high-resolution samples when the platform only has averages.
     */
    fun mergeRichest(local: LocalHealthWorkout, incoming: LocalHealthWorkout): LocalHealthWorkout {
        val heartRate =
            when {
                incoming.heartRateSeries.size > local.heartRateSeries.size -> incoming.heartRateSeries
                else -> local.heartRateSeries.ifEmpty { incoming.heartRateSeries }
            }
        val route =
            when {
                incoming.route.size > local.route.size -> incoming.route
                else -> local.route.ifEmpty { incoming.route }
            }
        val samples =
            when {
                incoming.samples.size > local.samples.size -> incoming.samples
                else -> local.samples.ifEmpty { incoming.samples }
            }
        val sets =
            when {
                incoming.sets.size > local.sets.size -> incoming.sets
                else -> local.sets.ifEmpty { incoming.sets }
            }
        val events =
            when {
                incoming.events.size > local.events.size -> incoming.events
                else -> local.events.ifEmpty { incoming.events }
            }
        return local.copy(
            identity = HealthSyncIdentity(
                syncIdentifier = local.identity.syncIdentifier,
                syncVersion = maxOf(local.identity.syncVersion, incoming.identity.syncVersion)
            ),
            activeEnergyKcal = local.activeEnergyKcal ?: incoming.activeEnergyKcal,
            basalEnergyKcal = local.basalEnergyKcal ?: incoming.basalEnergyKcal,
            distanceMetres = local.distanceMetres ?: incoming.distanceMetres,
            steps = local.steps ?: incoming.steps,
            averageHeartRateBpm = local.averageHeartRateBpm ?: incoming.averageHeartRateBpm,
            maxHeartRateBpm = local.maxHeartRateBpm ?: incoming.maxHeartRateBpm,
            averageCadenceSpm = local.averageCadenceSpm ?: incoming.averageCadenceSpm,
            planId = local.planId ?: incoming.planId,
            sessionId = local.sessionId ?: incoming.sessionId,
            platformRecordId = incoming.platformRecordId ?: local.platformRecordId,
            sourceBundle = local.sourceBundle ?: incoming.sourceBundle,
            sourceName = local.sourceName ?: incoming.sourceName,
            notes = local.notes ?: incoming.notes,
            heartRateSeries = heartRate,
            samples = samples,
            route = route,
            events = events,
            sets = sets,
            updatedAt = maxOf(local.updatedAt, incoming.updatedAt)
        )
    }

    /** True when this record was created by OmniGart and may be written back to a platform. */
    fun mayWriteToPlatform(workout: LocalHealthWorkout): Boolean = workout.origin == HealthRecordOrigin.OMNIGART &&
        (workout.sourceBundle == null || workout.sourceBundle == SOURCE_BUNDLE_OMNIGART)
}
