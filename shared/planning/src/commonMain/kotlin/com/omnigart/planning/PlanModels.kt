package com.omnigart.planning

import com.omnigart.content.Discipline
import com.omnigart.content.Level
import com.omnigart.content.Muscle
import com.omnigart.content.TrainingGoal
import com.omnigart.filter.EquipmentProfile

/** Prefixed to avoid Shared.framework clash with domain BiologicalSex. */
enum class PlanBiologicalSex { FEMALE, MALE, UNSPECIFIED }

/** Prefixed to avoid Shared.framework clash with calc ReadinessBand. */
enum class PlanReadinessBand { VERY_LOW, LOW, NORMAL, HIGH }

data class DisciplineLevel(val discipline: Discipline, val level: Level)

data class UserTrainingProfile(
    val sex: PlanBiologicalSex = PlanBiologicalSex.UNSPECIFIED,
    val ageYears: Int,
    val heightCm: Double? = null,
    val bodyMassKg: Double? = null,
    val levels: List<DisciplineLevel>,
    val goals: Set<TrainingGoal>,
    val availableDaysPerWeek: Int,
    val sessionMinutes: Int,
    val equipment: EquipmentProfile,
    val injuries: Set<String> = emptySet(),
    val hrZonesConfigured: Boolean = false,
    val recentWeeklyHardSets: Map<Muscle, Int> = emptyMap(),
    val readiness: PlanReadinessBand = PlanReadinessBand.NORMAL,
    val hardWeeksSinceDeload: Int = 0
)

data class PlanGenerationInput(
    val profile: UserTrainingProfile,
    val preferredTemplateId: String? = null,
    val allowedExerciseIds: Set<String>? = null
)

data class EvidencePointer(val refId: String, val label: String)

data class PlanExplanation(val code: String, val message: String, val evidence: EvidencePointer)

data class PlannedSetPrescription(val exerciseId: String, val sets: Int, val reps: String, val rir: String, val restSeconds: Int)

data class PlannedSession(
    val dayIndex: Int,
    val title: String,
    val focus: String,
    val prescriptions: List<PlannedSetPrescription>,
    val explanations: List<PlanExplanation>
)

data class WeeklyVolumeTarget(val muscle: Muscle, val hardSets: Int)

data class PlanRecommendation(
    val generatorId: String,
    val weekNumber: Int,
    val isDeload: Boolean,
    val volumeTargets: List<WeeklyVolumeTarget>,
    val sessions: List<PlannedSession>,
    val explanations: List<PlanExplanation>,
    val intensityNotes: List<PlanExplanation>
)

/** Structured proposal from an AI planner before safety validation. */
data class AiPlanProposal(val sessions: List<PlannedSession>, val claimedVolume: List<WeeklyVolumeTarget> = emptyList())
