package com.omnigart.shared.logging

import com.omnigart.shared.calc.MassUnit
import com.omnigart.shared.calc.MassUnits
import com.omnigart.shared.calc.MuscleMassKindInfer
import com.omnigart.shared.calc.OneRepMax
import com.omnigart.shared.calc.ResistanceEnergyInput
import com.omnigart.shared.calc.ResistanceEnergySetInput
import com.omnigart.shared.calc.ResistanceWorkoutEnergy
import com.omnigart.shared.calc.WorkoutEnergyBreakdown
import com.omnigart.shared.domain.BiologicalSex
import com.omnigart.shared.domain.Session
import com.omnigart.shared.domain.UnitSystem
import com.omnigart.shared.domain.WorkoutSet
import com.omnigart.shared.health.store.LocalWorkoutSet
import kotlin.math.max

/** Default rest between working sets when the prescription has none. */
const val DEFAULT_REST_SECONDS: Int = 90

/**
 * Pure helpers for the in-workout logging screen (phone + watch + Wear).
 * Persistence stays in [StrengthLoggingService] / local health store.
 */
object InWorkoutLogging {
    /** Working-set ordinal within an exercise (warm-ups labelled "W"). */
    fun setLabel(setsForExercise: List<WorkoutSet>, setId: String): String {
        val ordered = setsForExercise.sortedBy { it.setIndex }
        val target = ordered.firstOrNull { it.id == setId } ?: return "—"
        if (target.isWarmup) return "W"
        var n = 0
        for (s in ordered) {
            if (s.isWarmup) continue
            n++
            if (s.id == setId) return n.toString()
        }
        return "—"
    }

    fun previousDisplay(previous: WorkoutSet?, unit: MassUnit): String? {
        if (previous == null) return null
        val reps = previous.reps ?: return null
        val kg = previous.weightKg ?: return "$reps reps"
        val display = MassUnits.kgToDisplay(kg, unit)
        val unitLabel = if (unit == MassUnit.LB) "lb" else "kg"
        val formatted =
            if (display == display.toLong().toDouble()) {
                display.toLong().toString()
            } else {
                ((display * 10).toLong() / 10.0).toString()
            }
        return "$formatted$unitLabel × $reps"
    }

    /**
     * Last completed non-warmup set for [exerciseId] before [beforeSetIndex] in [session],
     * or from [historySessions] (most recent first) when the live session has none.
     */
    fun findPreviousSet(
        session: Session,
        exerciseId: String,
        beforeSetIndex: Int,
        historySessions: List<Session> = emptyList()
    ): WorkoutSet? {
        val inSession =
            session.sets
                .filter {
                    it.exerciseId == exerciseId &&
                        it.setIndex < beforeSetIndex &&
                        it.completed &&
                        !it.isWarmup
                }
                .maxByOrNull { it.setIndex }
        if (inSession != null) return inSession
        for (past in historySessions) {
            val hit =
                past.sets
                    .filter { it.exerciseId == exerciseId && it.completed && !it.isWarmup }
                    .maxByOrNull { it.setIndex }
            if (hit != null) return hit
        }
        return null
    }

    fun massUnitFor(preferred: UnitSystem): MassUnit = MassUnits.fromUnitSystem(preferred)

    fun prefillWeightKg(set: WorkoutSet, previous: WorkoutSet?): Double? = set.weightKg ?: set.targetWeightKg ?: previous?.weightKg

    fun prefillReps(set: WorkoutSet, previous: WorkoutSet?): Int? = set.reps ?: set.targetReps ?: previous?.reps

    fun restSecondsFor(set: WorkoutSet, defaultSeconds: Int = DEFAULT_REST_SECONDS): Int =
        set.restSeconds?.takeIf { it > 0 } ?: defaultSeconds

    /** Total volume (kg) and completed set count for the live header. */
    fun headerStats(session: Session, includeWarmups: Boolean = false): InWorkoutHeaderStats {
        var volume = 0.0
        var completed = 0
        for (set in session.sets) {
            if (!set.completed) continue
            if (set.isWarmup && !includeWarmups) continue
            val reps = set.reps ?: continue
            val load = set.weightKg ?: 0.0
            val side = if (set.loggedPerHand) 2 else 1
            volume += load * reps * side
            completed++
        }
        return InWorkoutHeaderStats(totalVolumeKg = volume, completedSetCount = completed)
    }

    fun toLocalHealthSets(session: Session): List<LocalWorkoutSet> = session.sets.map { set ->
        LocalWorkoutSet(
            id = set.id,
            exerciseKey = set.exerciseId,
            setIndex = set.setIndex,
            reps = set.reps,
            loadKg = set.weightKg,
            durationSeconds = set.durationSeconds,
            distanceMetres = set.distanceMetres,
            rir = set.rir,
            rpe = set.rpe,
            isWarmup = set.isWarmup,
            isDropSet = set.isDropSet,
            isFailure = set.isFailure,
            note = set.note
        )
    }

    fun toResistanceEnergySets(
        session: Session,
        primaryMusclesByExercise: Map<String, List<String>> = emptyMap()
    ): List<ResistanceEnergySetInput> = session.sets.map { set ->
        val muscles = primaryMusclesByExercise[set.exerciseId].orEmpty()
        val kind = MuscleMassKindInfer.infer(muscles, compound = muscles.size >= 2)
        val load = set.weightKg ?: 0.0
        val reps = set.reps ?: 0
        val e1rm =
            if (load > 0 && reps >= 1) {
                OneRepMax.epley(load, reps)
            } else {
                null
            }
        ResistanceEnergySetInput(
            loadKg = load,
            reps = reps,
            estimated1RmKg = e1rm,
            rpe = set.rpe,
            rir = set.rir,
            isWarmup = set.isWarmup,
            isDropSet = set.isDropSet,
            isFailure = set.isFailure,
            completed = set.completed,
            muscleMassKind = kind
        )
    }

    fun estimateEnergy(
        session: Session,
        weightKg: Double,
        heightCm: Double,
        ageYears: Int,
        sex: BiologicalSex,
        averageHrBpm: Double? = null,
        restSecondsTotal: Double = 0.0,
        primaryMusclesByExercise: Map<String, List<String>> = emptyMap(),
        bmrKcalPerDay: Double? = null
    ): WorkoutEnergyBreakdown {
        val durationSec =
            max(
                0.0,
                (
                    (session.endedAt ?: session.startedAt).toEpochMilliseconds() -
                        session.startedAt.toEpochMilliseconds()
                    ) / 1000.0
            )
        return ResistanceWorkoutEnergy.estimate(
            ResistanceEnergyInput(
                weightKg = weightKg,
                heightCm = heightCm,
                ageYears = ageYears,
                sex = sex,
                sessionDurationSeconds = durationSec,
                sets = toResistanceEnergySets(session, primaryMusclesByExercise),
                restSecondsTotal = restSecondsTotal,
                averageHrBpm = averageHrBpm,
                bmrKcalPerDay = bmrKcalPerDay
            )
        )
    }

    /** Group session sets by exercise, preserving first-seen order. */
    fun exercisesInOrder(session: Session): List<String> {
        val seen = linkedSetOf<String>()
        session.sets.forEach { seen.add(it.exerciseId) }
        return seen.toList()
    }

    fun setsForExercise(session: Session, exerciseId: String): List<WorkoutSet> =
        session.sets.filter { it.exerciseId == exerciseId }.sortedBy { it.setIndex }
}

data class InWorkoutHeaderStats(val totalVolumeKg: Double, val completedSetCount: Int)

/**
 * Rest-timer state machine for phone / watch / Wear UI shells.
 * Hosts drive ticks; this type stays free of clocks.
 */
data class RestTimerState(val totalSeconds: Int = DEFAULT_REST_SECONDS, val remainingSeconds: Int = 0, val running: Boolean = false) {
    fun start(seconds: Int = totalSeconds): RestTimerState {
        val s = seconds.coerceAtLeast(0)
        return copy(totalSeconds = s, remainingSeconds = s, running = s > 0)
    }

    fun tick(): RestTimerState {
        if (!running) return this
        val next = remainingSeconds - 1
        return if (next <= 0) {
            copy(remainingSeconds = 0, running = false)
        } else {
            copy(remainingSeconds = next)
        }
    }

    fun skip(): RestTimerState = copy(remainingSeconds = 0, running = false)

    val finished: Boolean get() = !running && remainingSeconds == 0 && totalSeconds > 0
}

/**
 * Progression chip copy: target load × reps when present.
 */
object ProgressionChip {
    fun label(targetWeightKg: Double?, targetReps: Int?, unit: MassUnit): String? {
        val reps = targetReps ?: return null
        val kg = targetWeightKg ?: return "$reps reps"
        val display = MassUnits.kgToDisplay(kg, unit)
        val unitLabel = if (unit == MassUnit.LB) "lb" else "kg"
        return "${formatMass(display)}$unitLabel × $reps"
    }

    private fun formatMass(value: Double): String {
        val tenths = (value * 10).toLong() / 10.0
        return if (tenths == tenths.toLong().toDouble()) tenths.toLong().toString() else tenths.toString()
    }
}
