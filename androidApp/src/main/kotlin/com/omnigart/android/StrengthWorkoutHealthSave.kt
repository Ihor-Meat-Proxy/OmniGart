package com.omnigart.android

import android.content.Context
import com.omnigart.shared.OmniGartCore
import com.omnigart.shared.calc.MassUnit
import com.omnigart.shared.calc.MuscleMassKindInfer
import com.omnigart.shared.calc.OneRepMax
import com.omnigart.shared.calc.ResistanceEnergyInput
import com.omnigart.shared.calc.ResistanceEnergySetInput
import com.omnigart.shared.calc.ResistanceWorkoutEnergy
import com.omnigart.shared.calc.WorkoutEnergyBreakdown
import com.omnigart.shared.domain.Session
import com.omnigart.shared.domain.UserProfile
import com.omnigart.shared.domain.WorkoutSet
import com.omnigart.shared.health.HealthWorkout
import com.omnigart.shared.health.SdkHealthConnectGateway
import com.omnigart.shared.health.WorkoutActivityType
import com.omnigart.shared.logging.DemoStrengthExercise
import com.omnigart.shared.util.formatOneDecimal
import kotlin.time.Instant

private data class WorkoutWindow(val startMs: Long, val endMs: Long) {
    val durationSec: Double get() = ((endMs - startMs) / 1000.0).coerceAtLeast(0.0)
}

internal suspend fun finishAndSave(request: StrengthSaveRequest): List<String> {
    val core = request.core
    val sessionId = request.sessionId
    core.strengthLogging.endSession(sessionId)
    val profile = core.profiles.get(request.profileId) ?: return emptyList()
    val session = core.strengthLogging.getSession(sessionId) ?: return emptyList()
    val window =
        WorkoutWindow(
            startMs = core.strengthLogging.sessionStartEpochMillis(sessionId),
            endMs = core.strengthLogging.sessionEndEpochMillis(sessionId)
        )
    val breakdown = estimateResistanceEnergy(core, profile, session, request.demos, window)
    val syncId = persistLocalWorkout(core, session, window, breakdown)
    writePlatformWorkout(request.context, core, sessionId, syncId, window, breakdown)
    return buildEnergyLines(request, profile, breakdown)
}

private fun estimateResistanceEnergy(
    core: OmniGartCore,
    profile: UserProfile,
    session: Session,
    demos: List<DemoStrengthExercise>,
    window: WorkoutWindow
): WorkoutEnergyBreakdown = ResistanceWorkoutEnergy.estimate(
    ResistanceEnergyInput(
        weightKg = profile.weightKg,
        heightCm = profile.heightCm,
        ageYears = core.ageYears(profile),
        sex = profile.biologicalSex,
        sessionDurationSeconds = window.durationSec,
        sets = session.sets.map { toEnergySetInput(it, demos) },
        restSecondsTotal = 0.0
    )
)

private fun toEnergySetInput(set: WorkoutSet, demos: List<DemoStrengthExercise>): ResistanceEnergySetInput {
    val muscles = demos.firstOrNull { it.exercise.id == set.exerciseId }?.exercise?.primaryMuscles.orEmpty()
    val kind = MuscleMassKindInfer.infer(muscles, compound = muscles.size >= 2)
    val load = set.weightKg ?: 0.0
    val reps = set.reps ?: 0
    return ResistanceEnergySetInput(
        loadKg = load,
        reps = reps,
        estimated1RmKg = if (load > 0 && reps >= 1) OneRepMax.epley(load, reps) else null,
        rpe = set.rpe,
        rir = set.rir,
        isWarmup = set.isWarmup,
        isDropSet = set.isDropSet,
        isFailure = set.isFailure,
        completed = set.completed,
        muscleMassKind = kind
    )
}

private fun persistLocalWorkout(core: OmniGartCore, session: Session, window: WorkoutWindow, breakdown: WorkoutEnergyBreakdown): String {
    val sets = session.sets
    return core.localHealth.recordOmniGartWorkoutWithSets(
        localId = session.id,
        activityTypeName = "STRENGTH",
        startEpochMs = window.startMs,
        endEpochMs = window.endMs,
        activeEnergyKcal = breakdown.activeKcal,
        basalEnergyKcal = breakdown.basalKcal,
        distanceMetres = null,
        averageHeartRateBpm = null,
        maxHeartRateBpm = null,
        sessionId = session.id,
        planId = null,
        heartRateEpochMs = LongArray(0),
        heartRateBpm = DoubleArray(0),
        setIds = sets.map { it.id }.toTypedArray(),
        setExerciseKeys = sets.map { it.exerciseId }.toTypedArray(),
        setIndexes = sets.map { it.setIndex }.toIntArray(),
        setReps = sets.map { it.reps ?: -1 }.toIntArray(),
        setLoadKg = sets.map { it.weightKg ?: -1.0 }.toDoubleArray(),
        setIsWarmup = sets.map { it.isWarmup }.toBooleanArray(),
        setRir = sets.map { it.rir ?: -1.0 }.toDoubleArray(),
        setRpe = sets.map { it.rpe ?: -1.0 }.toDoubleArray(),
        setIsDrop = sets.map { it.isDropSet }.toBooleanArray(),
        setIsFailure = sets.map { it.isFailure }.toBooleanArray(),
        syncVersion = 1
    )
}

private suspend fun writePlatformWorkout(
    context: Context,
    core: OmniGartCore,
    sessionId: String,
    syncId: String,
    window: WorkoutWindow,
    breakdown: WorkoutEnergyBreakdown
) {
    runCatching {
        if (SdkHealthConnectGateway.isAvailable(context)) {
            val gateway = SdkHealthConnectGateway(context)
            val start = Instant.fromEpochMilliseconds(window.startMs)
            val end = Instant.fromEpochMilliseconds(window.endMs)
            if (!gateway.clientRecordIdExists(syncId, start, end)) {
                gateway.writeWorkouts(
                    listOf(
                        HealthWorkout(
                            id = sessionId,
                            activityType = WorkoutActivityType.STRENGTH,
                            start = start,
                            end = end,
                            energyKcal = breakdown.activeKcal,
                            basalEnergyKcal = breakdown.basalKcal,
                            clientRecordVersion = 1
                        )
                    )
                )
                core.localHealth.markPlatformLinked(sessionId, syncId)
            }
        }
    }
}

private fun buildEnergyLines(request: StrengthSaveRequest, profile: UserProfile, breakdown: WorkoutEnergyBreakdown): List<String> {
    val context = request.context
    val unitLabel = if (request.massUnit == MassUnit.LB) "lb" else "kg"
    val summary =
        request.core.strengthLogging.sessionSummary(
            sessionId = request.sessionId,
            includeWarmups = request.includeWarmups,
            bodyweightKg = profile.weightKg,
            unit = request.massUnit,
            profileId = request.profileId
        )
    return listOf(
        "${context.getString(R.string.workout_summary_active_kcal)}: ${breakdown.activeKcal.toLong()}",
        "${context.getString(R.string.workout_summary_total_kcal)}: ${breakdown.totalKcal.toLong()}",
        context.getString(R.string.workout_summary_energy_estimate),
        "${context.getString(R.string.workout_volume_load)}: ${formatOneDecimal(summary.volume.totalVolumeDisplay)} $unitLabel"
    )
}
