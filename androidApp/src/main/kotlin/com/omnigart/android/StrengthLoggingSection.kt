package com.omnigart.android

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.omnigart.design.theme.OmniGartColors
import com.omnigart.design.theme.OmniGartSpacing
import com.omnigart.shared.OmniGartCore
import com.omnigart.shared.calc.MassUnit
import com.omnigart.shared.logging.DemoStrengthExercise
import com.omnigart.shared.logging.StrengthSessionSummary
import com.omnigart.shared.util.formatOneDecimal

private data class SetFormState(
    val weightText: String = "60",
    val repsText: String = "8",
    val rirText: String = "2",
    val isWarmup: Boolean = false
)

private data class StrengthUiState(
    val demos: List<DemoStrengthExercise>,
    val sessionId: String?,
    val setCount: Int,
    val selectedExercise: DemoStrengthExercise,
    val form: SetFormState,
    val includeWarmups: Boolean,
    val summary: StrengthSessionSummary?
)

private data class StrengthUiActions(
    val onStart: () -> Unit,
    val onSelectExercise: (DemoStrengthExercise) -> Unit,
    val onFormChange: (SetFormState) -> Unit,
    val onAddSet: () -> Unit,
    val onEnd: () -> Unit,
    val onWarmupsToggle: () -> Unit
)

@Composable
fun StrengthLoggingSection(core: OmniGartCore, profileId: String) {
    val demos = remember { core.strengthLogging.ensureDemoExercises() }
    var sessionId by remember { mutableStateOf<String?>(null) }
    var lastSessionId by remember { mutableStateOf<String?>(null) }
    var selectedExercise by remember { mutableStateOf(demos.first()) }
    var form by remember { mutableStateOf(SetFormState()) }
    var includeWarmups by remember { mutableStateOf(false) }
    var summary by remember { mutableStateOf<StrengthSessionSummary?>(null) }
    var setCount by remember { mutableStateOf(0) }
    val refresh: (String) -> Unit = { id ->
        val profile = core.profiles.get(profileId)
        summary =
            core.strengthLogging.sessionSummary(
                sessionId = id,
                includeWarmups = includeWarmups,
                bodyweightKg = profile?.weightKg,
                unit = MassUnit.KG,
                profileId = profileId
            )
        setCount = core.strengthLogging.getSession(id)?.sets?.size ?: 0
    }
    StrengthLoggingBody(
        state =
        StrengthUiState(
            demos = demos,
            sessionId = sessionId,
            setCount = setCount,
            selectedExercise = selectedExercise,
            form = form,
            includeWarmups = includeWarmups,
            summary = summary
        ),
        actions =
        StrengthUiActions(
            onStart = {
                val started = core.strengthLogging.startSession()
                sessionId = started.id
                lastSessionId = started.id
                summary = null
                setCount = 0
            },
            onSelectExercise = { selectedExercise = it },
            onFormChange = { form = it },
            onAddSet = {
                val id = sessionId
                if (id != null) {
                    core.strengthLogging.addSet(
                        sessionId = id,
                        exerciseId = selectedExercise.exercise.id,
                        reps = form.repsText.toIntOrNull() ?: 0,
                        weightKg = form.weightText.toDoubleOrNull(),
                        rir = form.rirText.toDoubleOrNull(),
                        isWarmup = form.isWarmup,
                        loggedPerHand = selectedExercise.unilateral
                    )
                    refresh(id)
                }
            },
            onEnd = {
                val id = sessionId
                if (id != null) {
                    core.strengthLogging.endSession(id)
                    refresh(id)
                    lastSessionId = id
                    sessionId = null
                }
            },
            onWarmupsToggle = {
                includeWarmups = !includeWarmups
                (sessionId ?: lastSessionId)?.let(refresh)
            }
        )
    )
}

@Composable
private fun StrengthLoggingBody(state: StrengthUiState, actions: StrengthUiActions) {
    Column(
        modifier =
        Modifier
            .fillMaxWidth()
            .testTag("strength_logging")
            .padding(top = OmniGartSpacing.space4),
        verticalArrangement = Arrangement.spacedBy(OmniGartSpacing.space2)
    ) {
        StrengthHeader(sessionId = state.sessionId, setCount = state.setCount)
        if (state.sessionId == null) {
            Button(onClick = actions.onStart, modifier = Modifier.testTag("workout_start")) {
                Text(stringResource(R.string.workout_start))
            }
        } else {
            ActiveSessionForm(state = state, actions = actions)
        }
        WarmupIncludeToggle(includeWarmups = state.includeWarmups, onToggle = actions.onWarmupsToggle)
        state.summary?.let { SessionSummaryBlock(it) }
    }
}

@Composable
private fun StrengthHeader(sessionId: String?, setCount: Int) {
    Text(
        text = stringResource(R.string.tab_workouts),
        style = MaterialTheme.typography.titleMedium,
        color = OmniGartColors.colorTextLightPrimary
    )
    Text(
        text = stringResource(R.string.workout_intro),
        style = MaterialTheme.typography.bodySmall,
        color = OmniGartColors.colorTextLightSecondary
    )
    Text(
        text =
        if (sessionId == null) {
            stringResource(R.string.workout_idle)
        } else {
            stringResource(R.string.workout_active) + " · $setCount"
        },
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.testTag("workout_status")
    )
}

@Composable
private fun ActiveSessionForm(state: StrengthUiState, actions: StrengthUiActions) {
    val form = state.form
    ExercisePicker(
        demos = state.demos,
        selected = state.selectedExercise,
        onSelect = actions.onSelectExercise
    )
    OutlinedTextField(
        value = form.weightText,
        onValueChange = { actions.onFormChange(form.copy(weightText = it)) },
        label = { Text(stringResource(R.string.workout_weight_kg)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth().testTag("workout_weight")
    )
    OutlinedTextField(
        value = form.repsText,
        onValueChange = { actions.onFormChange(form.copy(repsText = it)) },
        label = { Text(stringResource(R.string.workout_reps)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth().testTag("workout_reps")
    )
    OutlinedTextField(
        value = form.rirText,
        onValueChange = { actions.onFormChange(form.copy(rirText = it)) },
        label = { Text(stringResource(R.string.workout_rir)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth().testTag("workout_rir")
    )
    Text(
        text =
        if (form.isWarmup) {
            "● ${stringResource(R.string.workout_warmup)}"
        } else {
            "○ ${stringResource(R.string.workout_warmup)}"
        },
        modifier =
        Modifier
            .clickable { actions.onFormChange(form.copy(isWarmup = !form.isWarmup)) }
            .testTag("workout_warmup_toggle")
    )
    Button(onClick = actions.onAddSet, modifier = Modifier.testTag("workout_add_set")) {
        Text(stringResource(R.string.workout_add_set))
    }
    Button(onClick = actions.onEnd, modifier = Modifier.testTag("workout_end")) {
        Text(stringResource(R.string.workout_end))
    }
}

@Composable
private fun WarmupIncludeToggle(includeWarmups: Boolean, onToggle: () -> Unit) {
    Text(
        text =
        if (includeWarmups) {
            "● ${stringResource(R.string.workout_include_warmups)}"
        } else {
            "○ ${stringResource(R.string.workout_include_warmups)}"
        },
        modifier =
        Modifier
            .clickable(onClick = onToggle)
            .testTag("workout_include_warmups")
    )
}

@Composable
private fun SessionSummaryBlock(summary: StrengthSessionSummary) {
    Text(
        text = stringResource(R.string.workout_summary_title),
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.testTag("workout_summary")
    )
    Text(
        text =
        "${stringResource(R.string.workout_volume_load)}: " +
            "${formatOneDecimal(summary.volume.totalVolumeDisplay)} kg"
    )
    Text(
        text =
        "${stringResource(R.string.workout_total_weight_lifted)}: " +
            "${formatOneDecimal(summary.totalWeightLiftedDisplay)} kg"
    )
    Text(text = "${stringResource(R.string.workout_hard_sets)}: ${summary.volume.hardSets}")
    Text(
        text = stringResource(R.string.workout_per_exercise),
        style = MaterialTheme.typography.bodyMedium
    )
    summary.volume.perExercise.forEach { row ->
        Text(text = "${row.exerciseId}: ${formatOneDecimal(row.volumeKg)} kg · ${row.hardSets}")
    }
}

@Composable
private fun ExercisePicker(demos: List<DemoStrengthExercise>, selected: DemoStrengthExercise, onSelect: (DemoStrengthExercise) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(OmniGartSpacing.space1)) {
        Text(
            text = stringResource(R.string.workout_exercise),
            style = MaterialTheme.typography.bodyMedium
        )
        demos.forEach { demo ->
            val mark = if (demo.exercise.id == selected.exercise.id) "●" else "○"
            Text(
                text = "$mark ${demo.displayName}",
                modifier =
                Modifier
                    .clickable { onSelect(demo) }
                    .testTag("workout_exercise_${demo.exercise.id}")
            )
        }
    }
}
