package com.omnigart.shared.domain

import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Serializable
enum class ExerciseModality {
    STRENGTH,
    CARDIO,
    MOBILITY,
    SPORT,
    OTHER
}

@Serializable
data class Exercise(
    val id: String,
    /** Stable content-pack key, e.g. `ex.barbell_back_squat`. */
    val contentKey: String,
    val modality: ExerciseModality,
    val primaryMuscles: List<String> = emptyList()
)

@Serializable
data class WorkoutSet(
    val id: String,
    val exerciseId: String,
    val setIndex: Int,
    /** Repetitions performed; null for timed / distance cardio. */
    val reps: Int? = null,
    /** Load in kilograms (canonical): implement / added external mass. */
    val weightKg: Double? = null,
    /** Duration in seconds. */
    val durationSeconds: Int? = null,
    /** Distance in metres. */
    val distanceMetres: Double? = null,
    /** Reps in reserve (RIR), if logged. */
    val rir: Double? = null,
    /** Rating of perceived exertion (CR-10 / 0–10), if logged. */
    val rpe: Double? = null,
    val completed: Boolean = true,
    /** Warm-up sets are excluded from volume by default. */
    val isWarmup: Boolean = false,
    /** Drop-set flag (same exercise, reduced load continuation). */
    val isDropSet: Boolean = false,
    /** Taken to momentary muscular failure. */
    val isFailure: Boolean = false,
    /** Assistance mass (kg) subtracted from effective load. */
    val assistanceKg: Double? = null,
    /**
     * When true and the exercise is unilateral, volume counts both sides (×2).
     */
    val loggedPerHand: Boolean = false,
    /** Optional per-set coach / athlete note. */
    val note: String? = null,
    /** Planned rest after this set (seconds); drives the in-workout rest timer. */
    val restSeconds: Int? = null,
    /** Target load (kg) used to prefill the input. */
    val targetWeightKg: Double? = null,
    /** Target reps used to prefill the input. */
    val targetReps: Int? = null
) {
    init {
        require(setIndex >= 0) { "setIndex must be non-negative" }
        reps?.let { require(it >= 0) { "reps must be non-negative" } }
        weightKg?.let { require(it >= 0) { "weightKg must be non-negative" } }
        durationSeconds?.let { require(it >= 0) { "durationSeconds must be non-negative" } }
        distanceMetres?.let { require(it >= 0) { "distanceMetres must be non-negative" } }
        rir?.let { require(it in 0.0..10.0) { "rir must be between 0 and 10" } }
        rpe?.let { require(it in 0.0..10.0) { "rpe must be between 0 and 10" } }
        assistanceKg?.let { require(it >= 0) { "assistanceKg must be non-negative" } }
        restSeconds?.let { require(it >= 0) { "restSeconds must be non-negative" } }
        targetWeightKg?.let { require(it >= 0) { "targetWeightKg must be non-negative" } }
        targetReps?.let { require(it >= 0) { "targetReps must be non-negative" } }
    }
}

/**
 * A planned or template workout (ordered exercises / sets).
 */
@Serializable
data class Workout(val id: String, val name: String, val notes: String? = null, val exerciseIds: List<String> = emptyList())

/**
 * A performed training session (logged execution of a workout or ad-hoc sets).
 */
@Serializable
data class Session(
    val id: String,
    val workoutId: String? = null,
    val startedAt: Instant,
    val endedAt: Instant? = null,
    val sets: List<WorkoutSet> = emptyList(),
    val perceivedExertion: Int? = null,
    val notes: String? = null
) {
    init {
        perceivedExertion?.let { require(it in 1..10) { "perceivedExertion must be 1..10" } }
        endedAt?.let { require(it >= startedAt) { "endedAt must be >= startedAt" } }
    }

    /** Duration in hours for water / load calculations; 0 if still in progress. */
    fun durationHours(): Double {
        val end = endedAt ?: return 0.0
        val millis = end.toEpochMilliseconds() - startedAt.toEpochMilliseconds()
        return millis / 3_600_000.0
    }
}
