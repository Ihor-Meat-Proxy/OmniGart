package com.omnigart.android.nutrition

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier.modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.omnigart.android.R
import com.omnigart.design.theme.OmniGartColors

@Composable
fun NutritionGoalSettingsSection() {
    val context = LocalContext.current
    NutritionPrefsStore.init(context)
    val initial = NutritionPrefsStore.loadGoal()
    var kind by remember { mutableStateOf(initial.kind) }
    var mode by remember { mutableStateOf(initial.mode) }
    var ratePercent by remember {
        mutableDoubleStateOf(
            (kotlin.math.abs(initial.rateFractionPerWeek) * 100.0).takeIf { it > 0 } ?: 0.5
        )
    }
    var dayStartHour by remember { mutableIntStateOf((initial.dayStartMinutes / 60).coerceIn(0, 12)) }
    var addExercise by remember { mutableStateOf(initial.addExerciseCalories) }
    var targetWeight by remember {
        mutableStateOf(initial.targetWeightKg?.let { String.format("%.1f", it) } ?: "")
    }
    var showFirst by remember { mutableStateOf(!initial.firstGoalAcknowledged) }
    var saved by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth().testTag("nutrition_goal_settings"),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = stringResource(R.string.nutrition_goal_settings_title),
            style = MaterialTheme.typography.titleMedium
        )
        if (showFirst) {
            Text(
                text = stringResource(R.string.nutrition_goal_first_set),
                style = MaterialTheme.typography.bodySmall,
                color = OmniGartColors.colorTextLightSecondary
            )
        }
        RowChoices(
            options =
            listOf(
                "LOSE" to R.string.nutrition_goal_lose,
                "MAINTAIN" to R.string.nutrition_goal_maintain,
                "GAIN" to R.string.nutrition_goal_gain
            ),
            selected = kind,
            onSelect = { kind = it },
            tag = "nutrition_goal_kind"
        )
        RowChoices(
            options =
            listOf(
                "GUIDED" to R.string.nutrition_mode_guided,
                "FLEXIBLE_WEEKLY" to R.string.nutrition_mode_flexible,
                "MANUAL" to R.string.nutrition_mode_manual
            ),
            selected = mode,
            onSelect = { mode = it },
            tag = "nutrition_goal_mode"
        )
        if (kind != "MAINTAIN") {
            Text(String.format("%.2f%% / wk", ratePercent))
            TextButton(onClick = { ratePercent = (ratePercent - 0.25).coerceAtLeast(0.25) }) {
                Text("-")
            }
            TextButton(onClick = { ratePercent = (ratePercent + 0.25).coerceAtMost(1.0) }) {
                Text("+")
            }
            OutlinedTextField(
                value = targetWeight,
                onValueChange = { targetWeight = it },
                label = { Text(stringResource(R.string.nutrition_target_weight)) },
                modifier = Modifier.fillMaxWidth().testTag("nutrition_target_weight"),
                singleLine = true
            )
        }
        Text(
            text = "${stringResource(R.string.nutrition_day_start)}: %02d:00".format(dayStartHour),
            modifier = Modifier.testTag("nutrition_day_start")
        )
        TextButton(onClick = { dayStartHour = (dayStartHour - 1).coerceAtLeast(0) }) { Text("-") }
        TextButton(onClick = { dayStartHour = (dayStartHour + 1).coerceAtMost(12) }) { Text("+") }
        Text(stringResource(R.string.nutrition_add_exercise))
        Switch(
            checked = addExercise,
            onCheckedChange = { addExercise = it },
            modifier = Modifier.testTag("nutrition_add_exercise")
        )
        Button(
            onClick = {
                val goal = NutritionPrefsStore.loadGoal()
                goal.kind = kind
                goal.mode = mode
                goal.rateFractionPerWeek = if (kind == "MAINTAIN") 0.0 else ratePercent / 100.0
                goal.dayStartMinutes = dayStartHour * 60
                goal.addExerciseCalories = addExercise
                goal.targetWeightKg = targetWeight.toDoubleOrNull()
                goal.firstGoalAcknowledged = true
                NutritionPrefsStore.saveGoal(goal)
                showFirst = false
                saved = true
                NutritionReminderScheduler.reschedule(context)
            },
            modifier = Modifier.testTag("nutrition_goal_save")
        ) {
            Text(stringResource(R.string.profile_save))
        }
        if (saved) {
            Text(
                text = stringResource(R.string.profile_saved),
                color = OmniGartColors.colorBrandAccent,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun RowChoices(options: List<Pair<String, Int>>, selected: String, onSelect: (String) -> Unit, tag: String) {
    Column(modifier = Modifier.testTag(tag)) {
        options.forEach { (code, res) ->
            TextButton(onClick = { onSelect(code) }) {
                val mark = if (code == selected) "✓ " else ""
                Text(mark + stringResource(res))
            }
        }
    }
}
