package com.omnigart.shared.logging

import com.omnigart.shared.calc.BodyweightLoadCoefficients
import com.omnigart.shared.calc.BodyweightResolution
import com.omnigart.shared.calc.BodyweightResolver
import com.omnigart.shared.calc.BodyweightSample
import com.omnigart.shared.calc.MassUnit
import com.omnigart.shared.calc.MassUnits
import com.omnigart.shared.calc.VolumeExerciseMeta
import com.omnigart.shared.calc.VolumeLoad
import com.omnigart.shared.calc.VolumeLoadOptions
import com.omnigart.shared.calc.VolumeSessionInput
import com.omnigart.shared.calc.VolumeSetInput
import com.omnigart.shared.calc.WorkoutVolumeSummary
import com.omnigart.shared.calc.strategy.FormulaSettingsService
import com.omnigart.shared.data.ExerciseRepository
import com.omnigart.shared.data.SessionRepository
import com.omnigart.shared.data.UserProfileRepository
import com.omnigart.shared.domain.BodyweightFraction
import com.omnigart.shared.domain.Exercise
import com.omnigart.shared.domain.ExerciseLoadSemantics
import com.omnigart.shared.domain.ExerciseModality
import com.omnigart.shared.domain.LoadModel
import com.omnigart.shared.domain.Session
import com.omnigart.shared.domain.WorkoutSet
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Thin strength-session logger over [SessionRepository] + volume-load maths.
 * UI shells call these methods; all persistence stays in shared SQLDelight.
 */
data class StrengthSessionSummary(
    val sessionId: String,
    val volume: WorkoutVolumeSummary,
    /**
     * Σ (effective_load_kg × reps × side_multiplier) for included sets —
     * same basis as volume load; surfaced for “total weight lifted” UI copy.
     */
    val totalWeightLiftedKg: Double,
    val totalWeightLiftedDisplay: Double,
    val displayUnit: MassUnit,
    val includeWarmups: Boolean,
    val bodyweightResolution: BodyweightResolution?,
    /** Per-set estimated 1RM (kg) when weight × reps allow a formula. */
    val setE1rmKg: Map<String, Double> = emptyMap()
)

data class DemoStrengthExercise(
    val exercise: Exercise,
    val unilateral: Boolean,
    val loadSemantics: ExerciseLoadSemantics,
    val displayName: String
)

class StrengthLoggingService(
    private val sessions: SessionRepository,
    private val exercises: ExerciseRepository,
    private val profiles: UserProfileRepository,
    private val formulaSettings: FormulaSettingsService,
    private val clock: Clock = Clock.System
) {
    /** Swift-friendly overload (Kotlin default args are not exported to Apple). */
    fun startSession(): Session = startSession(workoutId = null, startedAt = clock.now())

    fun startSession(workoutId: String?): Session = startSession(workoutId = workoutId, startedAt = clock.now())

    fun startSession(startedAt: Instant): Session = startSession(workoutId = null, startedAt = startedAt)

    fun startSession(workoutId: String?, startedAt: Instant): Session {
        val session =
            Session(
                id = "session-${startedAt.toEpochMilliseconds()}",
                workoutId = workoutId,
                startedAt = startedAt,
                sets = emptyList()
            )
        sessions.upsert(session)
        return session
    }

    /** Swift-friendly overload (Kotlin default args are not exported to Apple). */
    fun endSession(sessionId: String): Session = endSession(sessionId = sessionId, endedAt = clock.now())

    fun endSession(sessionId: String, endedAt: Instant): Session {
        val current = requireSession(sessionId)
        val ended = current.copy(endedAt = maxOf(endedAt, current.startedAt))
        sessions.upsert(ended)
        return ended
    }

    fun addSet(
        sessionId: String,
        exerciseId: String,
        reps: Int,
        weightKg: Double? = null,
        rir: Double? = null,
        isWarmup: Boolean = false,
        assistanceKg: Double? = null,
        loggedPerHand: Boolean = false,
        completed: Boolean = true
    ): Session {
        val current = requireSession(sessionId)
        val set =
            WorkoutSet(
                id = "set-${clock.now().toEpochMilliseconds()}-${current.sets.size}",
                exerciseId = exerciseId,
                setIndex = current.sets.size,
                reps = reps,
                weightKg = weightKg,
                rir = rir,
                completed = completed,
                isWarmup = isWarmup,
                assistanceKg = assistanceKg,
                loggedPerHand = loggedPerHand
            )
        val next = current.copy(sets = current.sets + set)
        sessions.upsert(next)
        return next
    }

    fun updateSet(sessionId: String, set: WorkoutSet): Session {
        val current = requireSession(sessionId)
        require(current.sets.any { it.id == set.id }) { "Set ${set.id} not in session $sessionId" }
        val next = current.copy(sets = current.sets.map { if (it.id == set.id) set else it })
        sessions.upsert(next)
        return next
    }

    fun deleteSet(sessionId: String, setId: String): Session {
        val current = requireSession(sessionId)
        val remaining =
            current.sets
                .filterNot { it.id == setId }
                .mapIndexed { index, set -> set.copy(setIndex = index) }
        val next = current.copy(sets = remaining)
        sessions.upsert(next)
        return next
    }

    fun getSession(sessionId: String): Session? = sessions.get(sessionId)

    fun ensureDemoExercises(): List<DemoStrengthExercise> {
        val catalog = demoCatalog()
        catalog.forEach { demo -> exercises.upsert(demo.exercise) }
        return catalog
    }

    fun demoExercises(): List<DemoStrengthExercise> = demoCatalog()

    fun volumeMetaFor(exercise: Exercise, unilateral: Boolean = false, loadSemantics: ExerciseLoadSemantics? = null): VolumeExerciseMeta =
        VolumeExerciseMeta(
            exerciseId = exercise.id,
            unilateral = unilateral,
            primaryMuscles = exercise.primaryMuscles,
            loadSemantics = loadSemantics ?: inferLoadSemantics(exercise)
        )

    fun buildVolumeSessionInput(session: Session): VolumeSessionInput = VolumeSessionInput(
        id = session.id,
        workoutId = session.workoutId,
        startedAt = session.startedAt,
        sets = session.sets.map { it.toVolumeSetInput() }
    )

    fun buildExerciseMetaMap(session: Session, overrides: Map<String, VolumeExerciseMeta> = emptyMap()): Map<String, VolumeExerciseMeta> {
        val ids = session.sets.map { it.exerciseId }.toSet()
        val fromDemo = demoCatalog().associateBy { it.exercise.id }
        return ids.associateWith { id ->
            overrides[id]
                ?: fromDemo[id]?.let {
                    volumeMetaFor(it.exercise, it.unilateral, it.loadSemantics)
                }
                ?: exercises.get(id)?.let { volumeMetaFor(it) }
                ?: VolumeExerciseMeta(
                    exerciseId = id,
                    unilateral = false,
                    primaryMuscles = emptyList(),
                    loadSemantics = ExerciseLoadSemantics.EXTERNAL_ONLY
                )
        }
    }

    /**
     * Volume-load summary for a session.
     * [bodyweightKg] overrides profile / health resolution when non-null.
     */
    fun sessionSummary(
        sessionId: String,
        includeWarmups: Boolean = false,
        bodyweightKg: Double? = null,
        unit: MassUnit = MassUnit.KG,
        profileId: String? = null,
        healthSamples: List<BodyweightSample> = emptyList(),
        bandAssistanceKgById: Map<String, Double> = emptyMap()
    ): StrengthSessionSummary {
        val session = requireSession(sessionId)
        val profile = profileId?.let { profiles.get(it) }
        val resolution =
            BodyweightResolver.resolve(
                workoutAt = session.startedAt,
                healthOrBodySamples = healthSamples,
                profileWeightKg = bodyweightKg ?: profile?.weightKg,
                profileUpdatedAt = null
            )
        val resolvedBw = bodyweightKg ?: resolution.weightKg
        val options =
            VolumeLoadOptions(
                bodyweightKg = resolvedBw,
                includeWarmups = includeWarmups,
                displayUnit = unit,
                bandAssistanceKgById = bandAssistanceKgById
            )
        val meta = buildExerciseMetaMap(session)
        val volume = VolumeLoad.summarize(session.sets.map { it.toVolumeSetInput() }, meta, options)
        val totalLifted = volume.totalVolumeKg
        val e1rm =
            if (profileId != null) {
                estimateSetE1rm(session, meta, options, profileId)
            } else {
                emptyMap()
            }
        return StrengthSessionSummary(
            sessionId = sessionId,
            volume = volume,
            totalWeightLiftedKg = totalLifted,
            totalWeightLiftedDisplay = MassUnits.kgToDisplay(totalLifted, unit),
            displayUnit = unit,
            includeWarmups = includeWarmups,
            bodyweightResolution = resolution,
            setE1rmKg = e1rm
        )
    }

    /** Swift-friendly overload without optional collections. */
    fun sessionSummary(sessionId: String, includeWarmups: Boolean, bodyweightKg: Double?, unit: MassUnit): StrengthSessionSummary =
        sessionSummary(
            sessionId = sessionId,
            includeWarmups = includeWarmups,
            bodyweightKg = bodyweightKg,
            unit = unit,
            profileId = null,
            healthSamples = emptyList(),
            bandAssistanceKgById = emptyMap()
        )

    private fun estimateSetE1rm(
        session: Session,
        meta: Map<String, VolumeExerciseMeta>,
        options: VolumeLoadOptions,
        profileId: String
    ): Map<String, Double> {
        val formula = formulaSettings.getSelection(profileId).toPreferences().oneRepMax
        val out = linkedMapOf<String, Double>()
        session.sets.forEach { set ->
            val reps = set.reps ?: return@forEach
            if (reps < 1 || set.isWarmup) return@forEach
            val exerciseMeta = meta[set.exerciseId] ?: return@forEach
            val load = VolumeLoad.effectiveLoadKg(set.toVolumeSetInput(), exerciseMeta, options).loadKg
            if (load <= 0.0) return@forEach
            out[set.id] = formula.e1rm(load, reps)
        }
        return out
    }

    private fun requireSession(sessionId: String): Session = sessions.get(sessionId) ?: error("Unknown session: $sessionId")

    companion object {
        fun inferLoadSemantics(exercise: Exercise): ExerciseLoadSemantics {
            if (BodyweightLoadCoefficients.isKnownBodyweightMovement(exercise.id)) {
                val proportion = BodyweightLoadCoefficients.forExerciseId(exercise.id)
                val assisted = exercise.id.contains("assisted")
                return ExerciseLoadSemantics(
                    loadModel =
                    if (assisted) {
                        LoadModel.BODYWEIGHT_MINUS_ASSISTANCE
                    } else {
                        LoadModel.BODYWEIGHT_PLUS_EXTERNAL
                    },
                    bodyweightFraction =
                    BodyweightFraction(
                        proportion = proportion,
                        basis = "estimate",
                        rationale = "Inferred from BodyweightLoadCoefficients for ${exercise.id}"
                    ),
                    allowsAddedWeight = !assisted,
                    allowsAssistance = assisted || proportion > 0.0
                )
            }
            return ExerciseLoadSemantics.EXTERNAL_ONLY
        }

        fun demoCatalog(): List<DemoStrengthExercise> = listOf(
            DemoStrengthExercise(
                exercise =
                Exercise(
                    id = "barbell-bench-press",
                    contentKey = "ex.barbell_bench_press",
                    modality = ExerciseModality.STRENGTH,
                    primaryMuscles = listOf("pectoralis_major", "triceps", "front_deltoid")
                ),
                unilateral = false,
                loadSemantics = ExerciseLoadSemantics.EXTERNAL_ONLY,
                displayName = "Barbell bench press"
            ),
            DemoStrengthExercise(
                exercise =
                Exercise(
                    id = "push-up",
                    contentKey = "ex.push_up",
                    modality = ExerciseModality.STRENGTH,
                    primaryMuscles = listOf("pectoralis_major", "triceps", "front_deltoid")
                ),
                unilateral = false,
                loadSemantics =
                ExerciseLoadSemantics(
                    loadModel = LoadModel.BODYWEIGHT_PLUS_EXTERNAL,
                    bodyweightFraction =
                    BodyweightFraction(
                        proportion = BodyweightLoadCoefficients.PUSH_UP,
                        basis = "estimate",
                        rationale = "Phase 1 push-up coefficient"
                    ),
                    allowsAddedWeight = true,
                    allowsAssistance = true
                ),
                displayName = "Push-up"
            ),
            DemoStrengthExercise(
                exercise =
                Exercise(
                    id = "assisted-pull-up-band",
                    contentKey = "ex.assisted_pull_up_band",
                    modality = ExerciseModality.STRENGTH,
                    primaryMuscles = listOf("latissimus_dorsi", "biceps")
                ),
                unilateral = false,
                loadSemantics =
                ExerciseLoadSemantics(
                    loadModel = LoadModel.BODYWEIGHT_MINUS_ASSISTANCE,
                    bodyweightFraction =
                    BodyweightFraction(
                        proportion = BodyweightLoadCoefficients.PULL_UP,
                        basis = "estimate",
                        rationale = "Full body mass for vertical pull"
                    ),
                    allowsAddedWeight = false,
                    allowsAssistance = true
                ),
                displayName = "Assisted pull-up (band)"
            ),
            DemoStrengthExercise(
                exercise =
                Exercise(
                    id = "reverse-lunge-dumbbell",
                    contentKey = "ex.reverse_lunge_dumbbell",
                    modality = ExerciseModality.STRENGTH,
                    primaryMuscles = listOf("quadriceps", "gluteus_maximus")
                ),
                unilateral = true,
                loadSemantics = ExerciseLoadSemantics.EXTERNAL_ONLY,
                displayName = "Dumbbell reverse lunge"
            )
        )
    }
}

private fun WorkoutSet.toVolumeSetInput(): VolumeSetInput = VolumeSetInput(
    id = id,
    exerciseId = exerciseId,
    reps = reps ?: 0,
    addedWeightKg = weightKg,
    isWarmup = isWarmup,
    completed = completed,
    assistanceKg = assistanceKg,
    loggedPerHand = loggedPerHand,
    rir = rir
)
