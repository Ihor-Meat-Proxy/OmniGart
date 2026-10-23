package com.omnigart.shared.workoutkit

import com.omnigart.shared.health.WorkoutActivityType
import kotlinx.serialization.Serializable

/**
 * Platform-agnostic workout plan for Apple WorkoutKit scheduling.
 *
 * Strength set/rep prescriptions are intentionally unsupported — see [WorkoutBlockKind.STRENGTH_UNSUPPORTED].
 */
@Serializable
enum class WorkoutBlockKind {
    WARM_UP,
    WORK,
    RECOVERY,
    COOL_DOWN,
    /** Rejected by [ScheduledWorkoutValidator] — no WorkoutKit strength-set equivalent. */
    STRENGTH_UNSUPPORTED,
}

@Serializable
enum class WorkoutGoalKind {
    OPEN,
    TIME_SECONDS,
    DISTANCE_METRES,
    HEART_RATE_ZONE,
}

@Serializable
enum class WorkoutAlertKind {
    HEART_RATE_ZONE,
    PACE_MIN_PER_KM,
    CADENCE_SPM,
}

@Serializable
data class WorkoutGoalSpec(
    val kind: WorkoutGoalKind,
    /** Seconds, metres, or zone index depending on [kind]. */
    val value: Double? = null,
    /** Inclusive lower bound for zone / pace / cadence alerts paired with the goal. */
    val lower: Double? = null,
    val upper: Double? = null,
)

@Serializable
data class WorkoutAlertSpec(
    val kind: WorkoutAlertKind,
    val lower: Double,
    val upper: Double,
)

@Serializable
data class WorkoutBlockSpec(
    val kind: WorkoutBlockKind,
    val displayNameKey: String,
    val goal: WorkoutGoalSpec = WorkoutGoalSpec(WorkoutGoalKind.OPEN),
    val alerts: List<WorkoutAlertSpec> = emptyList(),
    /** For interval groups: repeat count (1 = single block). */
    val repeatCount: Int = 1,
)

@Serializable
data class ScheduledWorkoutPlan(
    val id: String,
    val activityType: WorkoutActivityType,
    val displayNameKey: String,
    val blocks: List<WorkoutBlockSpec>,
    /** Epoch millis for the intended start; null = next available schedule slot. */
    val scheduledStartEpochMillis: Long? = null,
) {
    init {
        require(id.isNotBlank())
        require(displayNameKey.isNotBlank())
        require(blocks.isNotEmpty()) { "WorkoutKit plan needs at least one block" }
    }
}

@Serializable
enum class ScheduleFailureReason {
    FEATURE_DISABLED,
    AUTHORIZATION_DENIED,
    SCHEDULE_CAP_REACHED,
    STRENGTH_NOT_SUPPORTED,
    INVALID_PLAN,
    UNSUPPORTED_ON_PLATFORM,
    UNKNOWN,
}

@Serializable
data class ScheduleResult(
    val ok: Boolean,
    val scheduledCount: Int = 0,
    val failure: ScheduleFailureReason? = null,
    val detail: String? = null,
)
