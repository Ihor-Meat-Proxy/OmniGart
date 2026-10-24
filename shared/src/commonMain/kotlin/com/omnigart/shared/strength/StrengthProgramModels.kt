package com.omnigart.shared.strength

import com.omnigart.shared.domain.ProfileGoal
import com.omnigart.shared.domain.StrengthLevel

/** Strength programme structure offered in the programme picker. */
enum class ProgramSplit {
    FULL_BODY,
    UPPER_LOWER,
    PUSH_PULL_LEGS,
    BRO_SPLIT
}

data class StrengthProgramPreferences(
    val profileId: String,
    val split: ProgramSplit = ProgramSplit.FULL_BODY,
    val daysPerWeek: Int = 3,
    val weekIndex: Int = 1
)

data class StrengthProgramRequest(
    val equipment: UserEquipmentProfile,
    val split: ProgramSplit,
    val daysPerWeek: Int,
    val goal: ProfileGoal,
    val level: StrengthLevel,
    val bodyMassKg: Double,
    val weekIndex: Int = 1,
    /** Optional one-session equipment overlay (Adjust equipment before start). */
    val sessionEquipmentIds: Set<String>? = null
)

data class GeneratedStrengthProgram(
    val split: ProgramSplit,
    val daysPerWeek: Int,
    val weekIndex: Int,
    val sessions: List<GeneratedStrengthSession>,
    val notes: List<String> = emptyList()
)

data class GeneratedStrengthSession(
    val dayIndex: Int,
    val titleKey: String,
    val focus: String,
    /** Distinct primary muscles worked (for body-map preview). */
    val workedMuscles: List<String>,
    val slots: List<GeneratedExerciseSlot>
)

data class GeneratedExerciseSlot(
    val exerciseId: String,
    val sets: Int,
    val repsLow: Int,
    val repsHigh: Int,
    val restSeconds: Int,
    val rirLow: Int,
    val rirHigh: Int,
    /** Effective load hint in kilograms (body mass counted for pull-up / dip variants). */
    val suggestedLoadKg: Double?,
    val loadIncludesBodyMass: Boolean,
    val substitutedFromId: String?,
    val primaryMuscles: List<String>,
    val movementPattern: String
) {
    val repsLabel: String get() = if (repsLow == repsHigh) "$repsLow" else "$repsLow-$repsHigh"
    val rirLabel: String get() = if (rirLow == rirHigh) "$rirLow" else "$rirLow-$rirHigh"
}
