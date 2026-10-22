package com.omnigart.shared.calc

/**
 * Session comparison, personal records, and weekly/monthly aggregates built on [VolumeLoad].
 */
object VolumeLoadStats {
    /**
     * Prefer matching [VolumeSessionInput.workoutId]; otherwise match by exercise-id multiset.
     */
    fun findPreviousSameWorkout(current: VolumeSessionInput, history: List<VolumeSessionInput>): VolumeSessionInput? {
        val prior = history.filter { it.id != current.id && it.startedAt < current.startedAt }
        if (prior.isEmpty()) return null
        current.workoutId?.let { wid ->
            prior.filter { it.workoutId == wid }.maxByOrNull { it.startedAt }?.let { return it }
        }
        val currentKey = exerciseMultiset(current.sets)
        return prior
            .filter { exerciseMultiset(it.sets) == currentKey }
            .maxByOrNull { it.startedAt }
    }

    fun compareWithPrevious(current: WorkoutVolumeSummary, previous: WorkoutVolumeSummary?): SessionVolumeComparison? {
        previous ?: return null
        return SessionVolumeComparison(
            currentVolumeKg = current.totalVolumeKg,
            previousVolumeKg = previous.totalVolumeKg,
            volumeDeltaKg = current.totalVolumeKg - previous.totalVolumeKg,
            currentHardSets = current.hardSets,
            previousHardSets = previous.hardSets,
            hardSetsDelta = current.hardSets - previous.hardSets
        )
    }

    /**
     * Detects PRs for exercises in [current] relative to [history] (excluding current).
     * e1RM uses Epley on effective load (estimated BW loads included).
     */
    fun detectPersonalRecords(
        current: VolumeSessionInput,
        currentSummary: WorkoutVolumeSummary,
        history: List<VolumeSessionInput>,
        exercises: Map<String, VolumeExerciseMeta>,
        options: VolumeLoadOptions
    ): List<PersonalRecord> {
        val priorSessions = history.filter { it.id != current.id }
        val records = mutableListOf<PersonalRecord>()
        current.sets.map { it.exerciseId }.toSet().forEach { exerciseId ->
            records +=
                recordsForExercise(
                    exerciseId = exerciseId,
                    current = current,
                    currentSummary = currentSummary,
                    priorSessions = priorSessions,
                    exercises = exercises,
                    options = options
                )
        }
        return records.sortedWith(compareBy({ it.exerciseId }, { it.kind }))
    }

    fun aggregatePeriod(
        sessions: List<VolumeSessionInput>,
        exercises: Map<String, VolumeExerciseMeta>,
        options: VolumeLoadOptions
    ): PeriodVolumeStats {
        if (sessions.isEmpty()) {
            return PeriodVolumeStats(
                totalVolumeKg = 0.0,
                hardSets = 0,
                sessionCount = 0,
                perExerciseVolumeKg = emptyMap(),
                perMuscleVolumeKg = emptyMap(),
                estimatedVolumeKg = 0.0
            )
        }
        var total = 0.0
        var hard = 0
        var estimated = 0.0
        val perExercise = linkedMapOf<String, Double>()
        val perMuscle = linkedMapOf<String, Double>()
        sessions.forEach { session ->
            val summary = VolumeLoad.summarize(session.sets, exercises, options)
            total += summary.totalVolumeKg
            hard += summary.hardSets
            estimated += summary.estimatedVolumeKg
            summary.perExercise.forEach { row ->
                perExercise[row.exerciseId] = (perExercise[row.exerciseId] ?: 0.0) + row.volumeKg
            }
            summary.perMuscleVolumeKg.forEach { (muscle, vol) ->
                perMuscle[muscle] = (perMuscle[muscle] ?: 0.0) + vol
            }
        }
        return PeriodVolumeStats(
            totalVolumeKg = total,
            hardSets = hard,
            sessionCount = sessions.size,
            perExerciseVolumeKg = perExercise,
            perMuscleVolumeKg = perMuscle,
            estimatedVolumeKg = estimated
        )
    }

    private fun recordsForExercise(
        exerciseId: String,
        current: VolumeSessionInput,
        currentSummary: WorkoutVolumeSummary,
        priorSessions: List<VolumeSessionInput>,
        exercises: Map<String, VolumeExerciseMeta>,
        options: VolumeLoadOptions
    ): List<PersonalRecord> {
        val meta = exercises.getValue(exerciseId)
        val out = mutableListOf<PersonalRecord>()

        bestE1rm(current.sets, exerciseId, meta, options)?.let { (setId, e1rm) ->
            if (e1rm > bestHistoricalE1rm(priorSessions, exerciseId, meta, options)) {
                out +=
                    PersonalRecord(
                        exerciseId = exerciseId,
                        kind = PersonalRecordKind.BEST_E1RM,
                        valueKg = e1rm,
                        sessionId = current.id,
                        setId = setId
                    )
            }
        }

        currentSummary.setContributions
            .filter { it.exerciseId == exerciseId && it.included }
            .maxByOrNull { it.volumeKg }
            ?.let { bestSet ->
                if (bestSet.volumeKg > bestHistoricalSetVolume(priorSessions, exerciseId, exercises, options)) {
                    out +=
                        PersonalRecord(
                            exerciseId = exerciseId,
                            kind = PersonalRecordKind.BEST_SET_VOLUME,
                            valueKg = bestSet.volumeKg,
                            sessionId = current.id,
                            setId = bestSet.setId
                        )
                }
            }

        val currentSessionVol =
            currentSummary.perExercise.firstOrNull { it.exerciseId == exerciseId }?.volumeKg ?: 0.0
        if (currentSessionVol > bestHistoricalSessionVolume(priorSessions, exerciseId, exercises, options)) {
            out +=
                PersonalRecord(
                    exerciseId = exerciseId,
                    kind = PersonalRecordKind.BEST_SESSION_VOLUME,
                    valueKg = currentSessionVol,
                    sessionId = current.id
                )
        }
        return out
    }

    private fun bestE1rm(
        sets: List<VolumeSetInput>,
        exerciseId: String,
        meta: VolumeExerciseMeta,
        options: VolumeLoadOptions
    ): Pair<String, Double>? = sets
        .filter { it.exerciseId == exerciseId && VolumeLoad.shouldInclude(it, options.includeWarmups) }
        .mapNotNull { set ->
            val load = VolumeLoad.effectiveLoadKg(set, meta, options.bodyweightKg).loadKg
            if (load <= 0.0) null else set.id to OneRepMax.epley(load, set.reps)
        }
        .maxByOrNull { it.second }

    private fun bestHistoricalE1rm(
        priorSessions: List<VolumeSessionInput>,
        exerciseId: String,
        meta: VolumeExerciseMeta,
        options: VolumeLoadOptions
    ): Double = priorSessions.maxOfOrNull { session ->
        bestE1rm(session.sets, exerciseId, meta, options)?.second ?: 0.0
    } ?: 0.0

    private fun bestHistoricalSetVolume(
        priorSessions: List<VolumeSessionInput>,
        exerciseId: String,
        exercises: Map<String, VolumeExerciseMeta>,
        options: VolumeLoadOptions
    ): Double = priorSessions.maxOfOrNull { session ->
        VolumeLoad.summarize(session.sets, exercises, options)
            .setContributions
            .filter { it.exerciseId == exerciseId && it.included }
            .maxOfOrNull { it.volumeKg } ?: 0.0
    } ?: 0.0

    private fun bestHistoricalSessionVolume(
        priorSessions: List<VolumeSessionInput>,
        exerciseId: String,
        exercises: Map<String, VolumeExerciseMeta>,
        options: VolumeLoadOptions
    ): Double = priorSessions.maxOfOrNull { session ->
        VolumeLoad.summarize(session.sets, exercises, options)
            .perExercise
            .firstOrNull { it.exerciseId == exerciseId }
            ?.volumeKg ?: 0.0
    } ?: 0.0

    private fun exerciseMultiset(sets: List<VolumeSetInput>): Map<String, Int> = sets.groupingBy { it.exerciseId }.eachCount()
}
