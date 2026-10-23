package com.omnigart.achievements

import kotlin.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Domain events that drive incremental achievement evaluation.
 * [eventId] must be stable for idempotent ingest (retries / Health re-imports).
 */
@Serializable
sealed class DomainEvent {
    abstract val eventId: String
    abstract val occurredAt: Instant
    /** IANA time zone id used for calendar day boundaries at event time. */
    abstract val timeZoneId: String

    @Serializable
    @SerialName("workout_completed")
    data class WorkoutCompleted(
        override val eventId: String,
        override val occurredAt: Instant,
        override val timeZoneId: String,
        val discipline: String,
        val durationMin: Double = 0.0,
        val distanceM: Double = 0.0,
        val planned: Boolean = false,
    ) : DomainEvent()

    @Serializable
    @SerialName("set_logged")
    data class SetLogged(
        override val eventId: String,
        override val occurredAt: Instant,
        override val timeZoneId: String,
        val discipline: String = "strength",
        val loadKg: Double = 0.0,
        val reps: Int = 0,
    ) : DomainEvent()

    @Serializable
    @SerialName("personal_record")
    data class PersonalRecord(
        override val eventId: String,
        override val occurredAt: Instant,
        override val timeZoneId: String,
        val discipline: String,
        val metric: String,
        val value: Double,
        val exerciseId: String? = null,
    ) : DomainEvent()

    @Serializable
    @SerialName("water_logged")
    data class WaterLogged(
        override val eventId: String,
        override val occurredAt: Instant,
        override val timeZoneId: String,
        val amountMl: Double,
        val dailyTotalMl: Double,
        val goalMl: Double,
    ) : DomainEvent()

    @Serializable
    @SerialName("meal_logged")
    data class MealLogged(
        override val eventId: String,
        override val occurredAt: Instant,
        override val timeZoneId: String,
        val proteinG: Double? = null,
        val proteinTargetG: Double? = null,
    ) : DomainEvent()

    @Serializable
    @SerialName("sleep_imported")
    data class SleepImported(
        override val eventId: String,
        override val occurredAt: Instant,
        override val timeZoneId: String,
        val durationMin: Double = 0.0,
    ) : DomainEvent()

    @Serializable
    @SerialName("hrv_logged")
    data class HrvLogged(
        override val eventId: String,
        override val occurredAt: Instant,
        override val timeZoneId: String,
        val hrvMs: Double,
    ) : DomainEvent()

    @Serializable
    @SerialName("stress_checkin")
    data class StressCheckin(
        override val eventId: String,
        override val occurredAt: Instant,
        override val timeZoneId: String,
        val level: Int,
    ) : DomainEvent()

    @Serializable
    @SerialName("measurement_logged")
    data class MeasurementLogged(
        override val eventId: String,
        override val occurredAt: Instant,
        override val timeZoneId: String,
        val site: String,
        /** Never used for weight-loss magnitude rewards — logging only. */
        val value: Double? = null,
        val unit: String? = null,
    ) : DomainEvent()

    @Serializable
    @SerialName("planned_rest_day")
    data class PlannedRestDay(
        override val eventId: String,
        override val occurredAt: Instant,
        override val timeZoneId: String,
        val discipline: String? = null,
        val lowReadiness: Boolean = false,
    ) : DomainEvent()

    @Serializable
    @SerialName("streak_freeze")
    data class StreakFreeze(
        override val eventId: String,
        override val occurredAt: Instant,
        override val timeZoneId: String,
        val metric: String? = null,
    ) : DomainEvent()

    @Serializable
    @SerialName("challenge_completed")
    data class ChallengeCompleted(
        override val eventId: String,
        override val occurredAt: Instant,
        override val timeZoneId: String,
        val challengeType: String,
        val challengeInstanceId: String,
    ) : DomainEvent()

    @Serializable
    @SerialName("weekly_goal_met")
    data class WeeklyGoalMet(
        override val eventId: String,
        override val occurredAt: Instant,
        override val timeZoneId: String,
        val weekStartIso: String,
    ) : DomainEvent()
}
