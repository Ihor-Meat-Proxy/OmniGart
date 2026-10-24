package com.omnigart.android

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.omnigart.shared.OmniGartCore
import com.omnigart.shared.calc.MassUnits
import com.omnigart.shared.domain.Session
import com.omnigart.shared.domain.UnitSystem
import com.omnigart.shared.domain.UserProfile
import com.omnigart.shared.domain.WorkoutSet
import com.omnigart.shared.logging.DEFAULT_REST_SECONDS
import com.omnigart.shared.logging.DemoStrengthExercise
import com.omnigart.shared.logging.InWorkoutLogging
import com.omnigart.shared.logging.StrengthSessionSummary
import kotlinx.coroutines.delay

private const val DEFAULT_SET_REPS = 8
private const val DEFAULT_SET_WEIGHT_KG = 60.0
private const val DEMO_EXERCISE_COUNT = 2

@Stable
internal class StrengthLoggingState(val core: OmniGartCore, var profileId: String) {
    val demos: List<DemoStrengthExercise> = core.strengthLogging.ensureDemoExercises()
    var sessionId by mutableStateOf<String?>(null)
    var lastSessionId by mutableStateOf<String?>(null)
    var includeWarmups by mutableStateOf(false)
    var summary by mutableStateOf<StrengthSessionSummary?>(null)
    var setCount by mutableIntStateOf(0)
    var volumeDisplay by mutableStateOf(0.0)
    var elapsedLabel by mutableStateOf("0:00")
    var restRemaining by mutableIntStateOf(0)
    var restRunning by mutableStateOf(false)
    var energyLines by mutableStateOf<List<String>>(emptyList())
    var draftWeight by mutableStateOf<Map<String, String>>(emptyMap())
    var draftReps by mutableStateOf<Map<String, String>>(emptyMap())

    private val profile: UserProfile? get() = core.profiles.get(profileId)

    val massUnit get() = MassUnits.fromUnitSystem(profile?.preferredUnits ?: UnitSystem.METRIC)

    fun refresh(id: String) {
        summary =
            core.strengthLogging.sessionSummary(
                sessionId = id,
                includeWarmups = includeWarmups,
                bodyweightKg = profile?.weightKg,
                unit = massUnit,
                profileId = profileId
            )
        val session = core.strengthLogging.getSession(id)
        setCount = session?.sets?.count { it.completed } ?: 0
        volumeDisplay = summary?.volume?.totalVolumeDisplay ?: 0.0
    }

    suspend fun runTimer() {
        while (true) {
            val id = sessionId ?: break
            val startMs = core.strengthLogging.sessionStartEpochMillis(id)
            val elapsedSec = ((System.currentTimeMillis() - startMs) / 1000L).coerceAtLeast(0)
            elapsedLabel = formatElapsed(elapsedSec)
            tickRest()
            delay(1_000)
        }
    }

    private fun tickRest() {
        if (!restRunning) return
        if (restRemaining <= 0) {
            restRunning = false
        } else {
            restRemaining -= 1
        }
    }

    fun toggleRest() {
        if (restRunning) {
            restRunning = false
            restRemaining = 0
        } else {
            restRemaining = DEFAULT_REST_SECONDS
            restRunning = true
        }
    }

    fun toggleWarmups() {
        includeWarmups = !includeWarmups
        (sessionId ?: lastSessionId)?.let(::refresh)
    }

    fun startSession() {
        val started = core.strengthLogging.startSession()
        sessionId = started.id
        lastSessionId = started.id
        summary = null
        energyLines = emptyList()
        demos.take(DEMO_EXERCISE_COUNT).forEach { demo ->
            core.strengthLogging.addSet(
                sessionId = started.id,
                exerciseId = demo.exercise.id,
                reps = DEFAULT_SET_REPS,
                weightKg = DEFAULT_SET_WEIGHT_KG,
                rir = 2.0,
                isWarmup = false,
                loggedPerHand = demo.unilateral,
                completed = false,
                restSeconds = DEFAULT_REST_SECONDS,
                targetWeightKg = DEFAULT_SET_WEIGHT_KG,
                targetReps = DEFAULT_SET_REPS
            )
        }
        refresh(started.id)
    }

    fun updateDraftWeight(key: String, value: String) {
        draftWeight = draftWeight + (key to value)
    }

    fun updateDraftReps(key: String, value: String) {
        draftReps = draftReps + (key to value)
    }

    fun completeSet(id: String, set: WorkoutSet) {
        val display =
            draftWeight["${set.id}-w"]?.toDoubleOrNull()
                ?: MassUnits.kgToDisplay(set.weightKg ?: set.targetWeightKg ?: 0.0, massUnit)
        val kg = MassUnits.displayToKg(display, massUnit)
        val reps = draftReps["${set.id}-r"]?.toIntOrNull() ?: set.reps ?: 0
        core.strengthLogging.updateSet(id, set.copy(weightKg = kg, reps = reps, completed = true))
        restRemaining = core.strengthLogging.completeSet(id, set.id)
        restRunning = restRemaining > 0
        refresh(id)
    }

    fun updateSet(id: String, set: WorkoutSet) {
        core.strengthLogging.updateSet(id, set)
        refresh(id)
    }

    fun addSet(id: String, session: Session, exerciseId: String) {
        val prev =
            InWorkoutLogging.findPreviousSet(
                session = session,
                exerciseId = exerciseId,
                beforeSetIndex = session.sets.size
            )
        core.strengthLogging.addSet(
            sessionId = id,
            exerciseId = exerciseId,
            reps = prev?.reps ?: DEFAULT_SET_REPS,
            weightKg = prev?.weightKg ?: DEFAULT_SET_WEIGHT_KG,
            loggedPerHand = demos.firstOrNull { it.exercise.id == exerciseId }?.unilateral == true,
            completed = false,
            restSeconds = DEFAULT_REST_SECONDS,
            targetWeightKg = prev?.weightKg ?: DEFAULT_SET_WEIGHT_KG,
            targetReps = prev?.reps ?: DEFAULT_SET_REPS
        )
        refresh(id)
    }

    fun replaceExercise(id: String, fromId: String, toId: String) {
        core.strengthLogging.replaceExercise(id, fromId, toId)
        refresh(id)
    }

    fun removeExercise(id: String, exerciseId: String) {
        core.strengthLogging.removeExercise(id, exerciseId)
        refresh(id)
    }

    suspend fun finishWorkout(context: android.content.Context, id: String) {
        val lines =
            finishAndSave(
                StrengthSaveRequest(
                    context = context,
                    core = core,
                    profileId = profileId,
                    sessionId = id,
                    demos = demos,
                    includeWarmups = includeWarmups,
                    massUnit = massUnit
                )
            )
        energyLines = lines
        refresh(id)
        lastSessionId = id
        sessionId = null
        restRunning = false
    }
}

internal fun formatElapsed(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) {
        "%d:%02d:%02d".format(h, m, s)
    } else {
        "%d:%02d".format(m, s)
    }
}
