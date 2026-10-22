package com.omnigart.content

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class TrainingGoal {
    @SerialName("general_fitness")
    GENERAL_FITNESS,

    @SerialName("hypertrophy")
    HYPERTROPHY,

    @SerialName("strength")
    STRENGTH,

    @SerialName("endurance")
    ENDURANCE,

    @SerialName("mobility")
    MOBILITY,

    @SerialName("fat_loss_support")
    FAT_LOSS_SUPPORT,

    @SerialName("technique")
    TECHNIQUE,

    @SerialName("race_prep")
    RACE_PREP
}

@Serializable
enum class EquipmentPresetId {
    @SerialName("home_gym")
    HOME_GYM,

    @SerialName("commercial_gym")
    COMMERCIAL_GYM,

    @SerialName("travel")
    TRAVEL,

    @SerialName("bodyweight")
    BODYWEIGHT,

    @SerialName("pool")
    POOL,

    @SerialName("custom")
    CUSTOM
}

@Serializable
data class TemplateSetItem(
    val exerciseId: String,
    val sets: Int? = null,
    val reps: String? = null,
    val rir: String? = null,
    val restSeconds: Int? = null,
    val durationSeconds: Int? = null,
    val distanceMeters: Int? = null,
    val notes: String? = null
)

@Serializable
data class TemplateBlock(val kind: String, val items: List<TemplateSetItem>)

@Serializable
data class TemplateSession(val id: String, val nameKeys: LocalizedNameKeys, val blocks: List<TemplateBlock>)

@Serializable
data class WorkoutTemplate(
    val id: String,
    val nameKeys: LocalizedNameKeys,
    val disciplines: List<Discipline>,
    val level: Level,
    val goal: TrainingGoal,
    val daysPerWeek: Int,
    val sessionMinutes: Int,
    val equipmentPreset: EquipmentPresetId,
    val equipmentRequired: List<String>,
    val rationale: String,
    val evidenceRefs: List<EvidenceRef>,
    val sessions: List<TemplateSession>
)
