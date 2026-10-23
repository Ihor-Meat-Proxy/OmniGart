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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.omnigart.android.R
import com.omnigart.design.theme.OmniGartColors
import java.util.Locale

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
        mutableStateOf(initial.targetWeightKg?.let { String.format(Locale.US, "%.1f", it) } ?: "")
    }
    var showFirst by remember { mutableStateOf(!initial.firstGoalAcknowledged) }
    var saved by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth().testTag("nutrition_goal_settings"),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(stringResource(R.string.nutrition_goal_settings_title), style = MaterialTheme.typography.titleMedium)
        if (showFirst) {
            Text(
                text = stringResource(R.string.nutrition_goal_first_set),
                style = MaterialTheme.typography.bodySmall,
                color = OmniGartColors.colorTextLightSecondary
            )
        }
        GoalKindAndMode(kind = kind, mode = mode, onKind = { kind = it }, onMode = { mode = it })
        if (kind != "MAINTAIN") {
            RateAndTargetWeight(
                ratePercent = ratePercent,
                targetWeight = targetWeight,
                onRate = { ratePercent = it },
                onTargetWeight = { targetWeight = it }
            )
        }
        DayStartAndExercise(
            dayStartHour = dayStartHour,
            addExercise = addExercise,
            onHour = { dayStartHour = it },
            onExercise = { addExercise = it }
        )
        Button(
            onClick = {
                persistGoal(kind, mode, ratePercent, dayStartHour, addExercise, targetWeight)
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
private fun GoalKindAndMode(kind: String, mode: String, onKind: (String) -> Unit, onMode: (String) -> Unit) {
    RowChoices(
        options =
        listOf(
            "LOSE" to R.string.nutrition_goal_lose,
            "MAINTAIN" to R.string.nutrition_goal_maintain,
            "GAIN" to R.string.nutrition_goal_gain
        ),
        selected = kind,
        onSelect = onKind,
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
        onSelect = onMode,
        tag = "nutrition_goal_mode"
    )
}

@Composable
private fun RateAndTargetWeight(ratePercent: Double, targetWeight: String, onRate: (Double) -> Unit, onTargetWeight: (String) -> Unit) {
    Text(String.format(Locale.US, "%.2f%% / wk", ratePercent))
    TextButton(onClick = { onRate((ratePercent - 0.25).coerceAtLeast(0.25)) }) { Text("-") }
    TextButton(onClick = { onRate((ratePercent + 0.25).coerceAtMost(1.0)) }) { Text("+") }
    OutlinedTextField(
        value = targetWeight,
        onValueChange = onTargetWeight,
        label = { Text(stringResource(R.string.nutrition_target_weight)) },
        modifier = Modifier.fillMaxWidth().testTag("nutrition_target_weight"),
        singleLine = true
    )
}

@Composable
private fun DayStartAndExercise(dayStartHour: Int, addExercise: Boolean, onHour: (Int) -> Unit, onExercise: (Boolean) -> Unit) {
    Text(
        text = "${stringResource(R.string.nutrition_day_start)}: %02d:00".format(Locale.US, dayStartHour),
        modifier = Modifier.testTag("nutrition_day_start")
    )
    TextButton(onClick = { onHour((dayStartHour - 1).coerceAtLeast(0)) }) { Text("-") }
    TextButton(onClick = { onHour((dayStartHour + 1).coerceAtMost(12)) }) { Text("+") }
    Text(stringResource(R.string.nutrition_add_exercise))
    Switch(
        checked = addExercise,
        onCheckedChange = onExercise,
        modifier = Modifier.testTag("nutrition_add_exercise")
    )
}

private fun persistGoal(kind: String, mode: String, ratePercent: Double, dayStartHour: Int, addExercise: Boolean, targetWeight: String) {
    val goal = NutritionPrefsStore.loadGoal()
    goal.kind = kind
    goal.mode = mode
    goal.rateFractionPerWeek = if (kind == "MAINTAIN") 0.0 else ratePercent / 100.0
    goal.dayStartMinutes = dayStartHour * 60
    goal.addExerciseCalories = addExercise
    goal.targetWeightKg = targetWeight.toDoubleOrNull()
    goal.firstGoalAcknowledged = true
    NutritionPrefsStore.saveGoal(goal)
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
