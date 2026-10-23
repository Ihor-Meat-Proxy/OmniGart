package com.omnigart.shared.health

import kotlin.time.Instant
import kotlinx.serialization.Serializable

/** Logical types OmniGart reads/writes across HealthKit, Health Connect, and Google Health API. */
enum class HealthDataType {
    WORKOUT,
    HEART_RATE,
    HRV_RMSSD,
    RESTING_HEART_RATE,
    SLEEP,
    BODY_MASS,
    HEIGHT,
    BODY_FAT_PERCENT,
    WAIST_CIRCUMFERENCE,
    NUTRITION,
    WATER,
    LOCATION_WORKOUT // Google Health API activity location scope; not stored as PII in core
}

@Serializable
data class TimeRange(val start: Instant, val end: Instant) {
    init {
        require(end >= start) { "end must be >= start" }
    }
}

@Serializable
data class HeartRateSample(val beatsPerMinute: Double, val recordedAt: Instant) {
    init {
        require(beatsPerMinute > 0) { "beatsPerMinute must be positive" }
    }
}

@Serializable
data class HrvSample(
    /** rMSSD in milliseconds. */
    val rmssdMs: Double,
    val recordedAt: Instant
) {
    init {
        require(rmssdMs > 0) { "rmssdMs must be positive" }
    }
}

@Serializable
data class SleepSession(
    val id: String,
    val start: Instant,
    val end: Instant,
    /** 0–1 time-asleep / time-in-bed when known. */
    val efficiency: Double? = null,
    val stages: List<SleepStage> = emptyList()
) {
    init {
        require(end >= start)
        efficiency?.let { require(it in 0.0..1.0) }
    }

    fun durationHours(): Double {
        val millis = end.toEpochMilliseconds() - start.toEpochMilliseconds()
        return millis / 3_600_000.0
    }
}

@Serializable
enum class SleepStageType {
    AWAKE,
    LIGHT,
    DEEP,
    REM,
    UNKNOWN
}

@Serializable
data class SleepStage(val type: SleepStageType, val start: Instant, val end: Instant)

@Serializable
data class BodyMeasurement(val type: HealthDataType, val value: Double, val unit: MeasurementUnit, val recordedAt: Instant)

@Serializable
enum class MeasurementUnit {
    KILOGRAM,
    CENTIMETRE,
    PERCENT,
    MILLILITRE,
    KILOCALORIE,
    GRAM,
    COUNT
}

@Serializable
data class NutritionEntry(
    val id: String,
    val recordedAt: Instant,
    val energyKcal: Double? = null,
    val proteinG: Double? = null,
    val carbohydrateG: Double? = null,
    val fatG: Double? = null,
    val waterMl: Double? = null,
    val name: String? = null
)

@Serializable
data class WaterIntake(val id: String, val volumeMl: Double, val recordedAt: Instant) {
    init {
        require(volumeMl > 0)
    }
}

@Serializable
enum class WorkoutActivityType {
    STRENGTH,
    RUNNING,
    CYCLING,
    SWIMMING,
    WALKING,
    HIIT,
    YOGA,
    OTHER
}

@Serializable
data class HealthWorkout(
    val id: String,
    val activityType: WorkoutActivityType,
    val start: Instant,
    val end: Instant,
    val energyKcal: Double? = null,
    val distanceMetres: Double? = null,
    val averageHeartRateBpm: Double? = null,
    val sourceName: String? = null
) {
    init {
        require(end >= start)
    }
}

/** Opaque platform change token (Health Connect ChangesToken / HK anchor). */
@Serializable
data class HealthChangeToken(val platform: String, val value: String)

@Serializable
data class HealthChanges(
    val upserts: List<HealthRecordRef> = emptyList(),
    val deletions: List<String> = emptyList(),
    val nextToken: HealthChangeToken?
)

@Serializable
data class HealthRecordRef(val id: String, val type: HealthDataType, val recordedAt: Instant)

enum class HealthPermission {
    READ_WORKOUT,
    WRITE_WORKOUT,
    READ_HEART_RATE,
    WRITE_HEART_RATE,
    READ_HRV,
    WRITE_HRV,
    READ_RESTING_HEART_RATE,
    WRITE_RESTING_HEART_RATE,
    READ_SLEEP,
    WRITE_SLEEP,
    READ_BODY_MEASUREMENTS,
    WRITE_BODY_MEASUREMENTS,
    READ_NUTRITION,
    WRITE_NUTRITION,
    READ_WATER,
    WRITE_WATER,
    READ_HISTORY,
    READ_BACKGROUND,
    READ_LOCATION
}

enum class AuthStatus {
    NOT_DETERMINED,
    DENIED,
    AUTHORIZED,
    PARTIAL
}
