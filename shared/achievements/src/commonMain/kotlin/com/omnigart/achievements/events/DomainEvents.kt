package com.omnigart.achievements.events

import kotlin.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Domain events that drive incremental achievement evaluation.
 * [eventId] must be stable for idempotency (replaying the same id is a no-op).
 */
@Serializable
sealed class DomainEvent {
    abstract val eventId: String
    abstract val occurredAt: Instant
    abstract val timeZoneId: String
    abstract val deleted: Boolean
}

@Serializable
@SerialName("workout_completed")
data class WorkoutCompleted(
    override val eventId: String,
    override val occurredAt: Instant,
    override val timeZoneId: String,
    override val deleted: Boolean = false,
    val discipline: String,
    val durationMin: Double = 0.0,
    val distanceM: Double? = null,
    val volumeKg: Double? = null,
    val setsCount: Int = 0,
    val swimStrokes: List<String> = emptyList(),
    val isPersonalRecord: Boolean = false,
    val plannedRest: Boolean = false,
) : DomainEvent()

@Serializable
@SerialName("set_logged")
data class SetLogged(
    override val eventId: String,
    override val occurredAt: Instant,
    override val timeZoneId: String,
    override val deleted: Boolean = false,
    val discipline: String = "strength",
    val volumeKg: Double = 0.0,
) : DomainEvent()

@Serializable
@SerialName("food_logged")
data class FoodLogged(
    override val eventId: String,
    override val occurredAt: Instant,
    override val timeZoneId: String,
    override val deleted: Boolean = false,
    /** Calendar day key in the user's zone, yyyy-MM-dd — one count per day. */
    val localDay: String,
) : DomainEvent()

@Serializable
@SerialName("water_logged")
data class WaterLogged(
    override val eventId: String,
    override val occurredAt: Instant,
    override val timeZoneId: String,
    override val deleted: Boolean = false,
    val localDay: String,
    val amountMl: Int,
    val goalMl: Int,
    val goalMet: Boolean,
) : DomainEvent()

@Serializable
@SerialName("sleep_imported")
data class SleepImported(
    override val eventId: String,
    override val occurredAt: Instant,
    override val timeZoneId: String,
    override val deleted: Boolean = false,
    val localDay: String,
    val midpointVarianceMin: Double,
    val consistent: Boolean,
) : DomainEvent()

@Serializable
@SerialName("measurement_added")
data class MeasurementAdded(
    override val eventId: String,
    override val occurredAt: Instant,
    override val timeZoneId: String,
    override val deleted: Boolean = false,
    val localDay: String,
    val kind: String,
) : DomainEvent()

@Serializable
@SerialName("planned_rest_honoured")
data class PlannedRestHonoured(
    override val eventId: String,
    override val occurredAt: Instant,
    override val timeZoneId: String,
    override val deleted: Boolean = false,
    val localDay: String,
    val reason: String = "readiness",
) : DomainEvent()

@Serializable
@SerialName("freeze_token_used")
data class FreezeTokenUsed(
    override val eventId: String,
    override val occurredAt: Instant,
    override val timeZoneId: String,
    override val deleted: Boolean = false,
    val localDay: String,
) : DomainEvent()
