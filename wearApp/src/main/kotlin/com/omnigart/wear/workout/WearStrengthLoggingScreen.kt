package com.omnigart.wear.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.omnigart.design.theme.OmniGartColors
import com.omnigart.design.theme.OmniGartSpacing
import com.omnigart.wear.R
import java.util.Locale
import kotlinx.coroutines.delay

/**
 * Per-set strength logger: exercise, set #, weight/reps steppers, Done → rest,
 * plus live HR / kcal from [WearExerciseRecorder].
 */
@Composable
fun WearStrengthLoggingScreen(recorder: WearExerciseRecorder, onFinished: () -> Unit = {}) {
    var epoch by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(500)
            recorder.tickRest()
            epoch = recorder.uiEpoch()
        }
    }
    @Suppress("UNUSED_VARIABLE")
    val observed = epoch

    Column(
        modifier =
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(OmniGartSpacing.space2)
            .testTag("wear_strength"),
        verticalArrangement = Arrangement.spacedBy(OmniGartSpacing.space1, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        StrengthHeader(recorder)
        StrengthSteppers(recorder)
        StrengthMetrics(recorder)
        StrengthActions(recorder, onFinished)
        recorder.lastError()?.let { err ->
            Text(
                text = err,
                style = MaterialTheme.typography.bodySmall,
                color = OmniGartColors.colorSemanticDanger,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun StrengthHeader(recorder: WearExerciseRecorder) {
    Text(
        text = recorder.currentExerciseName(),
        style = MaterialTheme.typography.titleSmall,
        textAlign = TextAlign.Center,
        color = OmniGartColors.colorBrandAccent,
        modifier = Modifier.testTag("wear_strength_exercise")
    )
    Text(
        text = stringResource(R.string.wear_strength_set, recorder.currentSetNumber()),
        style = MaterialTheme.typography.bodySmall,
        color = OmniGartColors.colorTextLightSecondary
    )
}

@Composable
private fun StrengthSteppers(recorder: WearExerciseRecorder) {
    val resting = recorder.isRestRunning()
    val restSec = recorder.restRemainingSeconds()
    StepperRow(
        label = stringResource(R.string.wear_strength_weight),
        value = String.format(Locale.US, "%.1f", recorder.weightKg()),
        onMinus = { recorder.adjustWeight(-2.5) },
        onPlus = { recorder.adjustWeight(2.5) },
        testTag = "wear_strength_weight"
    )
    StepperRow(
        label = stringResource(R.string.wear_strength_reps),
        value = recorder.reps().toString(),
        onMinus = { recorder.adjustReps(-1) },
        onPlus = { recorder.adjustReps(1) },
        testTag = "wear_strength_reps"
    )
    if (resting || restSec > 0) {
        Text(
            text = stringResource(R.string.wear_strength_rest, restSec),
            style = MaterialTheme.typography.bodyMedium,
            color = OmniGartColors.colorBrandAccent,
            modifier = Modifier.testTag("wear_strength_rest")
        )
    }
}

@Composable
private fun StrengthMetrics(recorder: WearExerciseRecorder) {
    Text(
        text = stringResource(R.string.wear_strength_hr, recorder.heartRateBpm()),
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.testTag("wear_strength_hr")
    )
    Text(
        text =
        stringResource(
            R.string.wear_strength_kcal,
            String.format(Locale.US, "%.0f", recorder.activeEnergyKcal())
        ),
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.testTag("wear_strength_kcal")
    )
}

@Composable
private fun StrengthActions(recorder: WearExerciseRecorder, onFinished: () -> Unit) {
    val resting = recorder.isRestRunning()
    Button(
        onClick = { recorder.logSet() },
        enabled = !resting,
        modifier =
        Modifier
            .fillMaxWidth()
            .testTag("wear_strength_done")
    ) {
        Text(text = stringResource(R.string.wear_strength_done))
    }
    Button(
        onClick = {
            when {
                recorder.isRunning() -> recorder.stop { onFinished() }
                recorder.loggedSets().isNotEmpty() -> {
                    recorder.recordManualStrengthAndTransfer()
                    onFinished()
                }
                else -> onFinished()
            }
        },
        modifier =
        Modifier
            .fillMaxWidth()
            .testTag("wear_strength_stop")
    ) {
        Text(text = stringResource(R.string.wear_strength_stop))
    }
}

@Composable
private fun StepperRow(label: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit, testTag: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(onClick = onMinus, modifier = Modifier.testTag("${testTag}_minus")) {
            Text(text = "−")
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = OmniGartColors.colorTextLightSecondary
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.testTag(testTag)
            )
        }
        Button(onClick = onPlus, modifier = Modifier.testTag("${testTag}_plus")) {
            Text(text = "+")
        }
    }
}
