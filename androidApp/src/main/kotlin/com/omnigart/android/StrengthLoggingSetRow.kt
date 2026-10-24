package com.omnigart.android

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.omnigart.design.theme.OmniGartColors
import com.omnigart.shared.calc.MassUnits
import com.omnigart.shared.domain.WorkoutSet
import com.omnigart.shared.logging.InWorkoutLogging
import com.omnigart.shared.util.formatOneDecimal

@Composable
internal fun SetRow(model: SetRowModel, drafts: SetDrafts, onComplete: (WorkoutSet) -> Unit, onUpdateSet: (WorkoutSet) -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    val set = model.set
    val previous = InWorkoutLogging.findPreviousSet(model.session, model.exerciseId, set.setIndex)
    val prevText = InWorkoutLogging.previousDisplay(previous, model.massUnit) ?: "—"
    val label =
        InWorkoutLogging.setLabel(InWorkoutLogging.setsForExercise(model.session, model.exerciseId), set.id)
    val wKey = "${set.id}-w"
    val rKey = "${set.id}-r"
    val weightText =
        drafts.weight[wKey]
            ?: formatOneDecimal(
                MassUnits.kgToDisplay(
                    set.weightKg ?: set.targetWeightKg ?: previous?.weightKg ?: 0.0,
                    model.massUnit
                )
            )
    val repsText = drafts.reps[rKey] ?: (set.reps ?: set.targetReps ?: previous?.reps ?: 8).toString()
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier.width(36.dp),
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = prevText,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            color = OmniGartColors.colorTextLightSecondary
        )
        OutlinedTextField(
            value = weightText,
            onValueChange = { drafts.onWeight(wKey, it) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.width(64.dp).testTag("workout_weight"),
            singleLine = true
        )
        OutlinedTextField(
            value = repsText,
            onValueChange = { drafts.onReps(rKey, it) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.width(56.dp).testTag("workout_reps"),
            singleLine = true
        )
        Checkbox(
            checked = set.completed,
            onCheckedChange = { onComplete(set) },
            modifier = Modifier.testTag("workout_set_done_${set.id}")
        )
        TextButton(onClick = { menuOpen = true }) { Text("···") }
        SetOptionsMenu(
            expanded = menuOpen,
            set = set,
            onDismiss = { menuOpen = false },
            onUpdateSet = onUpdateSet
        )
    }
}

@Composable
private fun SetOptionsMenu(expanded: Boolean, set: WorkoutSet, onDismiss: () -> Unit, onUpdateSet: (WorkoutSet) -> Unit) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        SetMenuItem(stringResource(R.string.workout_mark_warmup), onDismiss) {
            onUpdateSet(set.copy(isWarmup = !set.isWarmup))
        }
        SetMenuItem(stringResource(R.string.workout_mark_drop), onDismiss) {
            onUpdateSet(set.copy(isDropSet = !set.isDropSet))
        }
        SetMenuItem(stringResource(R.string.workout_mark_failure), onDismiss) {
            onUpdateSet(set.copy(isFailure = !set.isFailure))
        }
        SetMenuItem(stringResource(R.string.workout_record_rpe), onDismiss) {
            onUpdateSet(set.copy(rpe = 8.0))
        }
    }
}

@Composable
private fun SetMenuItem(text: String, onDismiss: () -> Unit, onSelect: () -> Unit) {
    DropdownMenuItem(
        text = { Text(text) },
        onClick = {
            onDismiss()
            onSelect()
        }
    )
}
