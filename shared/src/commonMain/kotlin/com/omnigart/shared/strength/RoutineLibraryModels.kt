package com.omnigart.shared.strength

/**
 * Browseable routine / programme library for Workouts › Explore.
 * Logging PR consumes these shapes via [RoutineLibraryService]; do not own in-workout UI here.
 */

enum class RoutineCategory {
    AT_HOME,
    TRAVEL,
    DUMBBELLS_ONLY,
    BAND,
    CARDIO_HIIT,
    GYM,
    BODYWEIGHT,
    SUSPENSION
}

enum class ProgrammeSplitKind {
    FULL_BODY,
    PUSH_PULL_LEGS,
    UPPER_LOWER
}

enum class ProgrammeLevel {
    BEGINNER,
    INTERMEDIATE,
    ADVANCED
}

enum class ProgrammeVenue {
    GYM,
    HOME
}

data class RoutineExerciseRef(
    val exerciseId: String,
    val illustrationKey: String,
    val nameKey: String,
    val sets: Int,
    val repsLow: Int,
    val repsHigh: Int,
    val restSeconds: Int = 60,
    val notesKey: String? = null,
    val primaryMuscles: List<String> = emptyList(),
    val secondaryMuscles: List<String> = emptyList()
) {
    val repsLabel: String get() = if (repsLow == repsHigh) "$repsLow" else "$repsLow–$repsHigh"
}

data class ReadyRoutine(
    val id: String,
    val nameKey: String,
    val descriptionKey: String,
    val category: RoutineCategory,
    val level: ProgrammeLevel,
    val estimatedMinutes: Int,
    val equipmentRequired: List<String>,
    val equipmentSummaryKey: String,
    val exercises: List<RoutineExerciseRef>,
    val tags: List<String> = emptyList()
) {
    val exerciseCount: Int get() = exercises.size
    val workedMuscles: List<String>
        get() = exercises.flatMap { it.primaryMuscles }.distinct().sorted()
}

data class ProgrammePlan(
    val id: String,
    val nameKey: String,
    val descriptionKey: String,
    val split: ProgrammeSplitKind,
    val level: ProgrammeLevel,
    val venue: ProgrammeVenue,
    val daysPerWeek: Int,
    val sessionRoutineIds: List<String>
)

data class NextWorkoutCard(
    val routineId: String,
    val nameKey: String,
    val muscleSummary: List<String>,
    val frontMuscles: List<String>,
    val backMuscles: List<String>,
    val exerciseCount: Int,
    val estimatedMinutes: Int,
    val notesKey: String?,
    val weeklyGoalDone: Int,
    val weeklyGoalTarget: Int,
    val exercises: List<RoutineExerciseRef>,
    val progressionHints: List<ProgressionHint> = emptyList()
)

data class ProgressionHint(val exerciseId: String, val kind: ProgressionHintKind, val messageKey: String)

enum class ProgressionHintKind {
    INCREASE_WEIGHT,
    ADD_REPS,
    HOLD
}

data class ExerciseDetail(
    val exerciseId: String,
    val contentKey: String,
    val illustrationKey: String,
    val nameKey: String,
    val aliases: List<String>,
    val primaryMuscles: List<String>,
    val secondaryMuscles: List<String>,
    val howToSteps: List<String>,
    val heaviestWeightKg: Double?,
    val estimated1RmKg: Double?,
    val bestSetVolumeKg: Double?,
    val personalRecords: List<ExercisePersonalRecord>,
    val historySets: List<ExerciseHistorySet>
)

data class ExercisePersonalRecord(val labelKey: String, val value: String, val recordedAtEpochMs: Long?)

data class ExerciseHistorySet(
    val sessionId: String,
    val recordedAtEpochMs: Long,
    val weightKg: Double?,
    val reps: Int,
    val rir: Int?,
    val isWarmup: Boolean
)

/** Editable next-workout list (replace / remove / reorder) without starting logging UI. */
data class EditableRoutineSession(
    val routineId: String,
    val nameKey: String,
    val exercises: List<RoutineExerciseRef>,
    val estimatedMinutes: Int,
    val notesKey: String?
)
