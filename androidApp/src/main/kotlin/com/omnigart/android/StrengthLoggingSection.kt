package com.omnigart.android

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.omnigart.design.theme.OmniGartColors
import com.omnigart.design.theme.OmniGartSpacing
import com.omnigart.shared.OmniGartCore
import com.omnigart.shared.calc.MassUnit
import com.omnigart.shared.domain.Session
import com.omnigart.shared.logging.InWorkoutLogging
import kotlinx.coroutines.launch

@Composable
fun StrengthLoggingSection(core: OmniGartCore, profileId: String) {
    val context = LocalContext.current
    val state = remember { StrengthLoggingState(core, profileId) }
    state.profileId = profileId
    LaunchedEffect(state.sessionId, state.restRunning) { state.runTimer() }

    Column(
        modifier =
        Modifier
            .fillMaxWidth()
            .testTag("strength_logging")
            .padding(top = OmniGartSpacing.space4),
        verticalArrangement = Arrangement.spacedBy(OmniGartSpacing.space2)
    ) {
        Text(
            text = stringResource(R.string.workout_intro),
            style = MaterialTheme.typography.bodySmall,
            color = OmniGartColors.colorTextLightSecondary
        )
        if (state.sessionId == null) {
            IdleWorkoutControls(state)
        } else {
            ActiveWorkout(state)
        }
        WarmupToggle(state)
        state.summary?.let {
            SessionSummaryBlock(
                summary = it,
                unitLabel = if (state.massUnit == MassUnit.LB) "lb" else "kg",
                energyLines = state.energyLines,
                onExportPdf = { exportStrengthPdf(context, core, state.lastSessionId, state.summary) }
            )
        }
    }
}

@Composable
private fun IdleWorkoutControls(state: StrengthLoggingState) {
    Text(
        text = stringResource(R.string.workout_idle),
        modifier = Modifier.testTag("workout_status")
    )
    Button(onClick = state::startSession, modifier = Modifier.testTag("workout_start")) {
        Text(stringResource(R.string.workout_start))
    }
}

@Composable
private fun ActiveWorkout(state: StrengthLoggingState) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val id = state.sessionId ?: return
    val session = state.core.strengthLogging.getSession(id)
    LiveHeader(
        model =
        LiveHeaderModel(
            elapsedLabel = state.elapsedLabel,
            volumeDisplay = state.volumeDisplay,
            unit = state.massUnit,
            setCount = state.setCount,
            restRemaining = state.restRemaining,
            restRunning = state.restRunning,
            muscles = session?.let { activeMuscles(it, state.demos) }.orEmpty()
        ),
        onRestToggle = state::toggleRest,
        onFinish = { scope.launch { state.finishWorkout(context, id) } }
    )
    session?.let { s ->
        InWorkoutLogging.exercisesInOrder(s).forEach { exerciseId ->
            SessionExerciseCard(state, id, s, exerciseId)
        }
    }
}

@Composable
private fun SessionExerciseCard(state: StrengthLoggingState, id: String, session: Session, exerciseId: String) {
    val demo = state.demos.firstOrNull { it.exercise.id == exerciseId }
    ExerciseCard(
        model =
        ExerciseCardModel(
            session = session,
            exerciseId = exerciseId,
            displayName = demo?.displayName ?: exerciseId,
            massUnit = state.massUnit,
            replaceOptions = state.demos
        ),
        drafts =
        SetDrafts(
            weight = state.draftWeight,
            reps = state.draftReps,
            onWeight = state::updateDraftWeight,
            onReps = state::updateDraftReps
        ),
        actions =
        ExerciseCardActions(
            onComplete = { state.completeSet(id, it) },
            onUpdateSet = { state.updateSet(id, it) },
            onAddSet = { state.addSet(id, session, exerciseId) },
            onReplace = { state.replaceExercise(id, exerciseId, it) },
            onRemove = { state.removeExercise(id, exerciseId) }
        )
    )
}

@Composable
private fun WarmupToggle(state: StrengthLoggingState) {
    val label = stringResource(R.string.workout_include_warmups)
    Text(
        text = if (state.includeWarmups) "● $label" else "○ $label",
        modifier =
        Modifier
            .clickable(onClick = state::toggleWarmups)
            .testTag("workout_include_warmups")
    )
}
