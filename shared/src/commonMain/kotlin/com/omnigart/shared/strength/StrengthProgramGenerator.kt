package com.omnigart.shared.strength

import com.omnigart.shared.domain.StrengthLevel

/**
 * Deterministic, offline strength programme generator.
 *
 * Fills split templates with library exercises allowed by [UserEquipmentProfile],
 * walks regressions when equipment is missing, balances muscle coverage, and
 * applies [ProgressiveOverload] from onboarding goal / experience.
 */
class StrengthProgramGenerator(private val library: List<StrengthLibraryExercise> = BundledStrengthLibrary.exercises) {
    fun generate(request: StrengthProgramRequest): GeneratedStrengthProgram {
        val equipment =
            request.sessionEquipmentIds?.let { request.equipment.withSessionOverride(it) }
                ?: request.equipment
        val days = request.daysPerWeek.coerceIn(allowedDays(request.split).first, allowedDays(request.split).last)
        val blueprints = StrengthSessionBlueprints.forSplit(request.split, days)
        val maxLevel = maxLevelOrdinal(request.level)
        val used = mutableSetOf<String>()
        val sessions =
            blueprints.mapIndexed { index, blueprint ->
                buildSession(index, blueprint, equipment, request, maxLevel, used)
            }
        return GeneratedStrengthProgram(
            split = request.split,
            daysPerWeek = days,
            weekIndex = request.weekIndex,
            sessions = sessions,
            notes =
            listOf(
                "volume.landmarks",
                "equipment.filtered",
                if (request.sessionEquipmentIds != null) "equipment.session_override" else "equipment.profile"
            )
        )
    }

    /** Rebuild a single session with a temporary equipment set (pre-start adjust). */
    fun regenerateSession(program: GeneratedStrengthProgram, dayIndex: Int, request: StrengthProgramRequest): GeneratedStrengthSession {
        val refreshed = generate(request)
        return refreshed.sessions.getOrElse(dayIndex) {
            program.sessions.getOrElse(dayIndex) {
                GeneratedStrengthSession(dayIndex, "program_session_empty", "rest", emptyList(), emptyList())
            }
        }
    }

    fun allowedDays(split: ProgramSplit): IntRange = when (split) {
        ProgramSplit.FULL_BODY -> 2..3
        ProgramSplit.UPPER_LOWER -> 3..4
        ProgramSplit.PUSH_PULL_LEGS -> 3..6
        ProgramSplit.BRO_SPLIT -> 5..5
    }

    private fun buildSession(
        index: Int,
        blueprint: SessionBlueprint,
        equipment: UserEquipmentProfile,
        request: StrengthProgramRequest,
        maxLevel: Int,
        used: MutableSet<String>
    ): GeneratedStrengthSession {
        val slots = mutableListOf<GeneratedExerciseSlot>()
        for (slot in blueprint.slots) {
            val selection =
                ExerciseSubstitution.select(
                    preferredIds = slot.preferredIds,
                    pattern = slot.pattern,
                    targetMuscles = slot.muscles,
                    equipment = equipment,
                    library = library,
                    excludeIds = used,
                    maxLevelOrdinal = maxLevel
                ) ?: continue
            used += selection.exercise.id
            val compound = selection.exercise.mechanics == "compound"
            val rx =
                ProgressiveOverload.prescription(
                    goal = request.goal,
                    level = request.level,
                    weekIndex = request.weekIndex,
                    compound = compound
                )
            val baseExternal = defaultExternalKg(selection.exercise, equipment, request.bodyMassKg)
            val load =
                ProgressiveOverload.suggestedLoadKg(
                    exercise = selection.exercise,
                    bodyMassKg = request.bodyMassKg,
                    externalOrAssistanceKg = baseExternal,
                    weekNudgeKg = rx.externalLoadNudgeKg
                )
            slots +=
                GeneratedExerciseSlot(
                    exerciseId = selection.exercise.id,
                    sets = rx.sets,
                    repsLow = rx.repsLow,
                    repsHigh = rx.repsHigh,
                    restSeconds = rx.restSeconds,
                    rirLow = rx.rirLow,
                    rirHigh = rx.rirHigh,
                    suggestedLoadKg = load,
                    loadIncludesBodyMass = selection.exercise.usesBodyweightLoad(),
                    substitutedFromId = selection.substitutedFromId,
                    primaryMuscles = selection.exercise.primaryMuscles,
                    movementPattern = selection.exercise.movementPattern
                )
        }
        return GeneratedStrengthSession(
            dayIndex = index,
            titleKey = blueprint.titleKey,
            focus = blueprint.focus,
            workedMuscles = slots.flatMap { it.primaryMuscles }.distinct().sorted(),
            slots = slots
        )
    }

    private fun maxLevelOrdinal(level: StrengthLevel): Int = when (level) {
        StrengthLevel.BEGINNER, StrengthLevel.NOVICE -> 0
        StrengthLevel.INTERMEDIATE -> 1
        StrengthLevel.ADVANCED -> 2
    }

    private fun defaultExternalKg(exercise: StrengthLibraryExercise, equipment: UserEquipmentProfile, bodyMassKg: Double): Double {
        if (exercise.loadModel == "bodyweight_minus_assistance") {
            return (bodyMassKg * 0.2).coerceIn(5.0, 40.0)
        }
        if (EquipmentItem.DUMBBELL.id in exercise.equipment) {
            val min = equipment.dumbbellMinKg ?: 4.0
            val max = equipment.dumbbellMaxKg ?: 20.0
            return ((min + max) / 4.0).coerceIn(min, max)
        }
        val machineTags =
            setOf(
                EquipmentItem.MACHINE.id,
                EquipmentItem.CABLE.id,
                EquipmentItem.LEG_PRESS.id,
                EquipmentItem.LAT_PULLDOWN.id,
                EquipmentItem.SMITH_MACHINE.id
            )
        return when {
            EquipmentItem.KETTLEBELL.id in exercise.equipment -> 12.0
            EquipmentItem.BARBELL.id in exercise.equipment -> 20.0
            exercise.equipment.any { it in machineTags || it.contains("machine") } -> 25.0
            else -> 0.0
        }
    }
}
