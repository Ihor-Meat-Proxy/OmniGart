package com.omnigart.android

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.omnigart.design.theme.OmniGartColors
import com.omnigart.design.theme.OmniGartSpacing
import com.omnigart.shared.calc.MassUnit
import com.omnigart.shared.logging.InWorkoutLogging
import com.omnigart.shared.logging.ProgressionChip

@Composable
internal fun ExerciseCard(model: ExerciseCardModel, drafts: SetDrafts, actions: ExerciseCardActions) {
    val sets = InWorkoutLogging.setsForExercise(model.session, model.exerciseId)
    val chip =
        ProgressionChip.label(
            targetWeightKg = sets.firstOrNull()?.targetWeightKg,
            targetReps = sets.firstOrNull()?.targetReps,
            unit = model.massUnit
        )
    Column(verticalArrangement = Arrangement.spacedBy(OmniGartSpacing.space1)) {
        ExerciseCardTitleRow(model, chip, actions)
        ExerciseColumnHeaders(model.massUnit)
        sets.forEach { set ->
            SetRow(
                model = SetRowModel(model.session, model.exerciseId, set, model.massUnit),
                drafts = drafts,
                onComplete = actions.onComplete,
                onUpdateSet = actions.onUpdateSet
            )
        }
        Button(onClick = actions.onAddSet, modifier = Modifier.testTag("workout_add_set")) {
            Text(stringResource(R.string.workout_add_set))
        }
    }
}

@Composable
private fun ExerciseCardTitleRow(model: ExerciseCardModel, chip: String?, actions: ExerciseCardActions) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = model.displayName,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.testTag("workout_exercise_${model.exerciseId}")
        )
        if (chip != null) {
            Text(text = chip, style = MaterialTheme.typography.labelSmall, color = OmniGartColors.colorBrandAccent)
        }
        TextButton(onClick = { menuOpen = true }) {
            Text(stringResource(R.string.workout_set_menu))
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            model.replaceOptions.filter { it.exercise.id != model.exerciseId }.forEach { demo ->
                DropdownMenuItem(
                    text = { Text("${stringResource(R.string.workout_replace_exercise)}: ${demo.displayName}") },
                    onClick = {
                        menuOpen = false
                        actions.onReplace(demo.exercise.id)
                    }
                )
            }
            DropdownMenuItem(
                text = { Text(stringResource(R.string.workout_remove_exercise)) },
                onClick = {
                    menuOpen = false
                    actions.onRemove()
                }
            )
        }
    }
}

@Composable
private fun ExerciseColumnHeaders(massUnit: MassUnit) {
    Row(modifier = Modifier.fillMaxWidth()) {
        ColumnHeader(stringResource(R.string.workout_col_set), width = 36.dp)
        ColumnHeader(stringResource(R.string.workout_col_previous), width = null)
        ColumnHeader(
            if (massUnit == MassUnit.LB) stringResource(R.string.workout_col_lb) else stringResource(R.string.workout_col_kg),
            width = 56.dp
        )
        ColumnHeader(stringResource(R.string.workout_col_reps), width = 48.dp)
        ColumnHeader(stringResource(R.string.workout_col_done), width = 40.dp)
    }
}

@Composable
private fun RowScope.ColumnHeader(text: String, width: Dp?) {
    Text(
        text = text,
        modifier = if (width != null) Modifier.width(width) else Modifier.weight(1f),
        style = MaterialTheme.typography.labelSmall
    )
}
