package com.omnigart.shared.strength

import com.omnigart.shared.data.ExerciseRepository
import com.omnigart.shared.data.SessionRepository
import com.omnigart.shared.data.UserProfileRepository
import com.omnigart.shared.domain.StrengthLevel
import com.omnigart.shared.util.formatOneDecimal

/**
 * Shared API for Explore routines, next-workout cards, and exercise detail.
 * Live logging UI stays in the logging PR — this only exposes data + substitutions.
 */
class RoutineLibraryService(
    private val equipmentService: EquipmentProfileService,
    private val profiles: UserProfileRepository,
    private val exercises: ExerciseRepository,
    private val sessions: SessionRepository,
    private val library: List<StrengthLibraryExercise> = BundledStrengthLibrary.exercises
) {
    fun equipmentCatalogue(): List<EquipmentDefinition> = EquipmentDefinitions.selectable()

    fun equipmentGrouped(): Map<EquipmentGroup, List<EquipmentDefinition>> = EquipmentDefinitions.grouped()

    fun equipmentSections(): List<EquipmentGroupSection> = EquipmentGroup.entries
        .filter { it != EquipmentGroup.ALWAYS }
        .mapNotNull { group ->
            val items = EquipmentDefinitions.grouped()[group].orEmpty()
            if (items.isEmpty()) {
                null
            } else {
                EquipmentGroupSection(
                    group = group,
                    nameKey = "equipment_group_${group.name.lowercase()}",
                    items = items
                )
            }
        }

    fun searchEquipment(query: String): List<EquipmentDefinition> = EquipmentDefinitions.search(query)

    fun equipmentPresets(): List<EquipmentBrowsePreset> = EquipmentBrowsePreset.entries.toList()

    fun applyBrowsePreset(profileId: String, preset: EquipmentBrowsePreset): UserEquipmentProfile {
        val owned = preset.ownedIds()
        val current = equipmentService.getEquipment(profileId)
        return equipmentService.saveEquipment(
            current.copy(
                ownedIds = owned,
                activePreset =
                when (preset) {
                    EquipmentBrowsePreset.AT_HOME -> EquipmentPresetKind.HOME
                    EquipmentBrowsePreset.GYM -> EquipmentPresetKind.GYM
                    EquipmentBrowsePreset.TRAVEL -> EquipmentPresetKind.TRAVEL
                    else -> EquipmentPresetKind.CUSTOM
                },
                dumbbellMinKg = if ("dumbbell" in owned || "adjustable_dumbbell" in owned) current.dumbbellMinKg ?: 2.0 else null,
                dumbbellMaxKg = if ("dumbbell" in owned || "adjustable_dumbbell" in owned) current.dumbbellMaxKg ?: 24.0 else null
            )
        )
    }

    fun programmes(split: ProgrammeSplitKind?, level: ProgrammeLevel?, venue: ProgrammeVenue?): List<ProgrammePlan> =
        RoutineLibraryCatalog.programmes(split, level, venue)

    /** Swift-friendly: all programmes. */
    fun programmes(): List<ProgrammePlan> = programmes(null, null, null)

    fun nextWorkoutCard(profileId: String): NextWorkoutCard? =
        nextWorkoutCard(profileId, routineId = null, weeklyDone = 0, weeklyTarget = 3)

    fun nextWorkoutCard(profileId: String, routineId: String): NextWorkoutCard? =
        nextWorkoutCard(profileId, routineId = routineId, weeklyDone = 0, weeklyTarget = 3)

    fun searchEquipment(): List<EquipmentDefinition> = searchEquipment("")

    fun routineCategories(): List<RoutineCategory> = RoutineCategory.entries.toList()

    fun routines(category: RoutineCategory, profileId: String): List<ReadyRoutine> {
        val equipment = equipmentService.getEquipment(profileId)
        return RoutineLibraryCatalog.routinesIn(category).filter { equipment.canPerform(it.equipmentRequired) }
    }

    fun allRoutinesForBrowse(profileId: String): List<ReadyRoutine> {
        val equipment = equipmentService.getEquipment(profileId)
        return RoutineLibraryCatalog.routines.filter { equipment.canPerform(it.equipmentRequired) }
    }

    fun adaptRoutine(profileId: String, routineId: String): ReadyRoutine? {
        val base = RoutineLibraryCatalog.routineById(routineId) ?: return null
        val equipment = equipmentService.getEquipment(profileId)
        val profile = profiles.get(profileId)
        val maxLevel =
            when (profile?.strengthLevel ?: StrengthLevel.BEGINNER) {
                StrengthLevel.BEGINNER, StrengthLevel.NOVICE -> 0
                StrengthLevel.INTERMEDIATE -> 1
                StrengthLevel.ADVANCED -> 2
            }
        val used = mutableSetOf<String>()
        val adapted = mutableListOf<RoutineExerciseRef>()
        for (slot in base.exercises) {
            val seed = library.firstOrNull { it.id == slot.exerciseId }
            val selection =
                ExerciseSubstitution.select(
                    preferredIds = listOf(slot.exerciseId),
                    pattern = seed?.movementPattern ?: "other",
                    targetMuscles = seed?.primaryMuscles?.toSet().orEmpty(),
                    equipment = equipment,
                    library = library,
                    excludeIds = used,
                    maxLevelOrdinal = maxLevel
                ) ?: continue
            used += selection.exercise.id
            val snake = selection.exercise.id.replace('-', '_')
            adapted +=
                slot.copy(
                    exerciseId = selection.exercise.id,
                    illustrationKey = "ex_$snake",
                    nameKey = "exercise.$snake.name",
                    primaryMuscles = selection.exercise.primaryMuscles,
                    secondaryMuscles = selection.exercise.secondaryMuscles
                )
        }
        return base.copy(exercises = adapted)
    }

    fun nextWorkoutCard(profileId: String, routineId: String? = null, weeklyDone: Int = 0, weeklyTarget: Int = 3): NextWorkoutCard? {
        val candidateId =
            routineId
                ?: allRoutinesForBrowse(profileId).firstOrNull()?.id
                ?: return null
        val routine = adaptRoutine(profileId, candidateId) ?: return null
        val (front, back) = splitBodyMapMuscles(routine.workedMuscles)
        val hints = progressionHints(profileId, routine)
        return NextWorkoutCard(
            routineId = routine.id,
            nameKey = routine.nameKey,
            muscleSummary = routine.workedMuscles,
            frontMuscles = front,
            backMuscles = back,
            exerciseCount = routine.exerciseCount,
            estimatedMinutes = routine.estimatedMinutes,
            notesKey = routine.descriptionKey,
            weeklyGoalDone = weeklyDone.coerceAtLeast(0),
            weeklyGoalTarget = weeklyTarget.coerceAtLeast(1),
            exercises = routine.exercises,
            progressionHints = hints
        )
    }

    fun editableSession(profileId: String, routineId: String): EditableRoutineSession? {
        val routine = adaptRoutine(profileId, routineId) ?: return null
        return EditableRoutineSession(
            routineId = routine.id,
            nameKey = routine.nameKey,
            exercises = routine.exercises,
            estimatedMinutes = routine.estimatedMinutes,
            notesKey = routine.descriptionKey
        )
    }

    fun replaceExercise(
        session: EditableRoutineSession,
        index: Int,
        preferredIds: List<String>,
        profileId: String
    ): EditableRoutineSession {
        if (index !in session.exercises.indices) return session
        val equipment = equipmentService.getEquipment(profileId)
        val current = session.exercises[index]
        val seed = library.firstOrNull { it.id == (preferredIds.firstOrNull() ?: current.exerciseId) }
        val used = session.exercises.map { it.exerciseId }.toMutableSet()
        used.remove(current.exerciseId)
        val selection =
            ExerciseSubstitution.select(
                preferredIds = preferredIds.ifEmpty { listOf(current.exerciseId) },
                pattern = seed?.movementPattern ?: current.movementPatternFallback(),
                targetMuscles = current.primaryMuscles.toSet(),
                equipment = equipment,
                library = library,
                excludeIds = used,
                maxLevelOrdinal = 2
            ) ?: return session
        val snake = selection.exercise.id.replace('-', '_')
        val next =
            current.copy(
                exerciseId = selection.exercise.id,
                illustrationKey = "ex_$snake",
                nameKey = "exercise.$snake.name",
                primaryMuscles = selection.exercise.primaryMuscles,
                secondaryMuscles = selection.exercise.secondaryMuscles
            )
        val list = session.exercises.toMutableList()
        list[index] = next
        return session.copy(exercises = list)
    }

    fun removeExercise(session: EditableRoutineSession, index: Int): EditableRoutineSession {
        if (index !in session.exercises.indices) return session
        return session.copy(exercises = session.exercises.filterIndexed { i, _ -> i != index })
    }

    fun reorderExercise(session: EditableRoutineSession, from: Int, to: Int): EditableRoutineSession {
        if (from !in session.exercises.indices || to !in session.exercises.indices) return session
        val list = session.exercises.toMutableList()
        val item = list.removeAt(from)
        list.add(to, item)
        return session.copy(exercises = list)
    }

    fun exerciseDetail(profileId: String, exerciseId: String): ExerciseDetail? {
        val lib = library.firstOrNull { it.id == exerciseId }
        val domain = exercises.getAll().firstOrNull { it.id == exerciseId }
        val snake = exerciseId.replace('-', '_')
        val history = historyFor(profileId, exerciseId)
        val heaviest = history.mapNotNull { it.weightKg }.maxOrNull()
        val bestVolume = history.maxOfOrNull { (it.weightKg ?: 0.0) * it.reps }
        val e1rm =
            history
                .mapNotNull { set ->
                    val w = set.weightKg ?: return@mapNotNull null
                    if (set.reps <= 0) null else w * (1.0 + set.reps / 30.0)
                }.maxOrNull()
        return ExerciseDetail(
            exerciseId = exerciseId,
            contentKey = domain?.contentKey ?: "ex.$snake",
            illustrationKey = "ex_$snake",
            nameKey = "exercise.$snake.name",
            aliases = ExerciseContentHints.aliases(exerciseId),
            primaryMuscles = lib?.primaryMuscles ?: domain?.primaryMuscles.orEmpty(),
            secondaryMuscles = lib?.secondaryMuscles.orEmpty(),
            howToSteps = howToStepsFor(exerciseId),
            heaviestWeightKg = heaviest,
            estimated1RmKg = e1rm,
            bestSetVolumeKg = bestVolume,
            personalRecords =
            listOfNotNull(
                heaviest?.let {
                    ExercisePersonalRecord("exercise_pr_heaviest", "${formatOneDecimal(it)} kg", null)
                },
                e1rm?.let {
                    ExercisePersonalRecord("exercise_pr_e1rm", "${formatOneDecimal(it)} kg", null)
                },
                bestVolume?.let {
                    ExercisePersonalRecord("exercise_pr_volume", "${formatOneDecimal(it)} kg·reps", null)
                }
            ),
            historySets = history
        )
    }

    private fun historyFor(profileId: String, exerciseId: String): List<ExerciseHistorySet> {
        // Sessions are local; filter completed working sets for this exercise across all rows.
        return sessions
            .getAll()
            .asSequence()
            .sortedByDescending { it.startedAt }
            .flatMap { session ->
                session.sets
                    .asSequence()
                    .filter { it.exerciseId == exerciseId && it.completed && !it.isWarmup }
                    .map { set ->
                        ExerciseHistorySet(
                            sessionId = session.id,
                            recordedAtEpochMs = session.startedAt.toEpochMilliseconds(),
                            weightKg = set.weightKg,
                            reps = set.reps ?: 0,
                            rir = set.rir?.toInt(),
                            isWarmup = set.isWarmup
                        )
                    }
            }
            .take(40)
            .toList()
    }

    private fun howToStepsFor(exerciseId: String): List<String> {
        val snake = exerciseId.replace('-', '_')
        val count = ExerciseContentHints.techniqueStepCount(exerciseId).coerceIn(1, 8)
        return (0 until count).map { "exercise.$snake.technique.$it" }
    }

    private fun progressionHints(profileId: String, routine: ReadyRoutine): List<ProgressionHint> {
        return routine.exercises.mapNotNull { slot ->
            val last =
                historyFor(profileId, slot.exerciseId)
                    .firstOrNull { it.reps > 0 }
                    ?: return@mapNotNull null
            when {
                last.reps >= slot.repsHigh ->
                    ProgressionHint(
                        exerciseId = slot.exerciseId,
                        kind = ProgressionHintKind.INCREASE_WEIGHT,
                        messageKey = "progression_hint_increase_weight"
                    )
                last.reps < slot.repsLow ->
                    ProgressionHint(
                        exerciseId = slot.exerciseId,
                        kind = ProgressionHintKind.ADD_REPS,
                        messageKey = "progression_hint_add_reps"
                    )
                else ->
                    ProgressionHint(
                        exerciseId = slot.exerciseId,
                        kind = ProgressionHintKind.HOLD,
                        messageKey = "progression_hint_hold"
                    )
            }
        }
    }

    private fun splitBodyMapMuscles(muscles: List<String>): Pair<List<String>, List<String>> {
        val front =
            setOf(
                "pectoralis_major",
                "front_deltoid",
                "lateral_deltoid",
                "biceps",
                "rectus_abdominis",
                "obliques",
                "quadriceps",
                "hip_flexors",
                "tibialis_anterior",
                "serratus_anterior",
                "transverse_abdominis"
            )
        val back =
            setOf(
                "latissimus_dorsi",
                "rhomboids",
                "trapezius",
                "rear_deltoid",
                "triceps",
                "erector_spinae",
                "gluteus_maximus",
                "gluteus_medius",
                "hamstrings",
                "calves",
                "forearms"
            )
        return muscles.filter { it in front } to muscles.filter { it in back }
    }

    private fun RoutineExerciseRef.movementPatternFallback(): String = when {
        primaryMuscles.any { it in setOf("quadriceps", "gluteus_maximus") } -> "squat"
        primaryMuscles.any { it in setOf("latissimus_dorsi", "rhomboids") } -> "pull"
        primaryMuscles.any { it in setOf("pectoralis_major", "triceps", "front_deltoid") } -> "push"
        primaryMuscles.any { it in setOf("hamstrings", "erector_spinae") } -> "hinge"
        else -> "other"
    }
}

enum class EquipmentBrowsePreset {
    AT_HOME,
    GYM,
    TRAVEL,
    DUMBBELLS_ONLY,
    BANDS,
    BODYWEIGHT,
    SUSPENSION;

    fun ownedIds(): Set<String> = when (this) {
        AT_HOME ->
            setOf(
                "chair",
                "sturdy_table",
                "doorframe_pull_up_bar",
                "towel",
                "backpack",
                "water_bottles",
                "dumbbell",
                "bands",
                "yoga_mat",
                "bodyweight"
            )
        GYM -> EquipmentDefinitions.selectable().map { it.id }.toSet() + "bodyweight"
        TRAVEL -> setOf("bands", "mini_bands", "suspension", "yoga_mat", "jump_rope", "bodyweight")
        DUMBBELLS_ONLY -> setOf("dumbbell", "adjustable_dumbbell", "bench_flat", "bodyweight")
        BANDS -> setOf("bands", "loop_bands", "tube_bands", "mini_bands", "door_anchor", "bodyweight")
        BODYWEIGHT -> setOf("bodyweight", "yoga_mat", "chair", "wall")
        SUSPENSION -> setOf("suspension", "suspension_straps", "rings", "bodyweight", "yoga_mat")
    }

    val nameKey: String get() = "equipment_browse_preset_${name.lowercase()}"
}

data class EquipmentGroupSection(val group: EquipmentGroup, val nameKey: String, val items: List<EquipmentDefinition>)
