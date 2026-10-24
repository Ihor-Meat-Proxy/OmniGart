package com.omnigart.android

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.omnigart.design.theme.OmniGartColors
import com.omnigart.design.theme.OmniGartSpacing
import com.omnigart.shared.calc.MassUnit
import com.omnigart.shared.domain.Session
import com.omnigart.shared.logging.DemoStrengthExercise
import com.omnigart.shared.logging.InWorkoutLogging
import com.omnigart.shared.util.formatOneDecimal

@Composable
internal fun LiveHeader(model: LiveHeaderModel, onRestToggle: () -> Unit, onFinish: () -> Unit) {
    val unitLabel = if (model.unit == MassUnit.LB) "lb" else "kg"
    Row(
        modifier = Modifier.fillMaxWidth().testTag("workout_status"),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        HeaderStat(stringResource(R.string.workout_summary_duration), model.elapsedLabel)
        HeaderStat(
            stringResource(R.string.workout_volume_short),
            "${formatOneDecimal(model.volumeDisplay)} $unitLabel"
        )
        HeaderStat(stringResource(R.string.workout_sets_count), "${model.setCount}")
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(OmniGartSpacing.space2)
    ) {
        Text(
            text =
            if (model.muscles.isEmpty()) {
                stringResource(R.string.workout_body_map)
            } else {
                model.muscles.take(3).joinToString(" · ")
            },
            style = MaterialTheme.typography.labelSmall,
            color = OmniGartColors.colorTextLightSecondary,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onRestToggle, modifier = Modifier.testTag("workout_rest_timer")) {
            Text(
                if (model.restRunning) {
                    "${stringResource(R.string.workout_rest_timer)} ${model.restRemaining}s"
                } else {
                    stringResource(R.string.workout_rest_timer)
                }
            )
        }
        Button(onClick = onFinish, modifier = Modifier.testTag("workout_end")) {
            Text(stringResource(R.string.workout_finish))
        }
    }
}

@Composable
private fun HeaderStat(title: String, value: String) {
    Column {
        Text(title, style = MaterialTheme.typography.labelSmall, color = OmniGartColors.colorTextLightSecondary)
        Text(value, style = MaterialTheme.typography.titleSmall)
    }
}

internal fun activeMuscles(session: Session, demos: List<DemoStrengthExercise>): List<String> {
    val keys =
        InWorkoutLogging.exercisesInOrder(session).flatMap { id ->
            demos.firstOrNull { it.exercise.id == id }?.exercise?.primaryMuscles.orEmpty()
        }
    return keys.distinct().sorted()
}
