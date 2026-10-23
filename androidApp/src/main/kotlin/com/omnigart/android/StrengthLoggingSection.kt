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

@Composable
fun StrengthLoggingSection(core: OmniGartCore, profileId: String) {
    val demos = remember { core.strengthLogging.ensureDemoExercises() }
    var sessionId by remember { mutableStateOf<String?>(null) }
    var lastSessionId by remember { mutableStateOf<String?>(null) }
    var selectedExercise by remember { mutableStateOf(demos.first()) }
    var weightText by remember { mutableStateOf("60") }
    var repsText by remember { mutableStateOf("8") }
    var rirText by remember { mutableStateOf("2") }
    var isWarmup by remember { mutableStateOf(false) }
    var includeWarmups by remember { mutableStateOf(false) }
    var summary by remember { mutableStateOf<StrengthSessionSummary?>(null) }
    var setCount by remember { mutableStateOf(0) }

    fun refreshSummary(id: String) {
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

    Column(
        modifier =
        Modifier
            .fillMaxWidth()
            .testTag("strength_logging")
            .padding(top = OmniGartSpacing.space4),
        verticalArrangement = Arrangement.spacedBy(OmniGartSpacing.space2)
    ) {
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
        if (sessionId == null) {
            Button(
                onClick = {
                    val started = core.strengthLogging.startSession()
                    sessionId = started.id
                    lastSessionId = started.id
                    summary = null
                    setCount = 0
                },
                modifier = Modifier.testTag("workout_start")
            ) {
                Text(stringResource(R.string.workout_start))
            }
        } else {
            ExercisePicker(demos = demos, selected = selectedExercise, onSelect = { selectedExercise = it })
            OutlinedTextField(
                value = weightText,
                onValueChange = { weightText = it },
                label = { Text(stringResource(R.string.workout_weight_kg)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth().testTag("workout_weight")
            )
            OutlinedTextField(
                value = repsText,
                onValueChange = { repsText = it },
                label = { Text(stringResource(R.string.workout_reps)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth().testTag("workout_reps")
            )
            OutlinedTextField(
                value = rirText,
                onValueChange = { rirText = it },
                label = { Text(stringResource(R.string.workout_rir)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth().testTag("workout_rir")
            )
            Text(
                text =
                if (isWarmup) {
                    "● ${stringResource(R.string.workout_warmup)}"
                } else {
                    "○ ${stringResource(R.string.workout_warmup)}"
                },
                modifier =
                Modifier
                    .clickable { isWarmup = !isWarmup }
                    .testTag("workout_warmup_toggle")
            )
            Button(
                onClick = {
                    val id = sessionId ?: return@Button
                    core.strengthLogging.addSet(
                        sessionId = id,
                        exerciseId = selectedExercise.exercise.id,
                        reps = repsText.toIntOrNull() ?: 0,
                        weightKg = weightText.toDoubleOrNull(),
                        rir = rirText.toDoubleOrNull(),
                        isWarmup = isWarmup,
                        loggedPerHand = selectedExercise.unilateral
                    )
                    refreshSummary(id)
                },
                modifier = Modifier.testTag("workout_add_set")
            ) {
                Text(stringResource(R.string.workout_add_set))
            }
            Button(
                onClick = {
                    val id = sessionId ?: return@Button
                    core.strengthLogging.endSession(id)
                    refreshSummary(id)
                    lastSessionId = id
                    sessionId = null
                },
                modifier = Modifier.testTag("workout_end")
            ) {
                Text(stringResource(R.string.workout_end))
            }
        }
        Text(
            text =
            if (includeWarmups) {
                "● ${stringResource(R.string.workout_include_warmups)}"
            } else {
                "○ ${stringResource(R.string.workout_include_warmups)}"
            },
            modifier =
            Modifier
                .clickable {
                    includeWarmups = !includeWarmups
                    val id = sessionId ?: lastSessionId
                    if (id != null) refreshSummary(id)
                }
                .testTag("workout_include_warmups")
        )
        summary?.let { s ->
            Text(
                text = stringResource(R.string.workout_summary_title),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.testTag("workout_summary")
            )
            Text(
                text =
                "${stringResource(R.string.workout_volume_load)}: " +
                    "${formatOneDecimal(s.volume.totalVolumeDisplay)} kg"
            )
            Text(
                text =
                "${stringResource(R.string.workout_total_weight_lifted)}: " +
                    "${formatOneDecimal(s.totalWeightLiftedDisplay)} kg"
            )
            Text(
                text = "${stringResource(R.string.workout_hard_sets)}: ${s.volume.hardSets}"
            )
            Text(
                text = stringResource(R.string.workout_per_exercise),
                style = MaterialTheme.typography.bodyMedium
            )
            s.volume.perExercise.forEach { row ->
                Text(
                    text = "${row.exerciseId}: ${formatOneDecimal(row.volumeKg)} kg · ${row.hardSets}"
                )
            }
        }
    }
}

@Composable
private fun ExercisePicker(
    demos: List<DemoStrengthExercise>,
    selected: DemoStrengthExercise,
    onSelect: (DemoStrengthExercise) -> Unit
) {
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
