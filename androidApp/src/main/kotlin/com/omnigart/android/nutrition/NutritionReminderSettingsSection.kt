package com.omnigart.android.nutrition

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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

@Composable
fun NutritionReminderSettingsSection() {
    val context = LocalContext.current
    NutritionPrefsStore.init(context)
    val goal0 = NutritionPrefsStore.loadGoal()
    var paused by remember { mutableStateOf(goal0.remindersPaused) }
    var weighIn by remember { mutableStateOf(goal0.weighInNudgeEnabled) }
    var weeks by remember { mutableIntStateOf(goal0.measurementReminderWeeks.coerceIn(2, 4)) }

    Column(
        modifier = Modifier.fillMaxWidth().testTag("nutrition_reminders"),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = stringResource(R.string.nutrition_reminders_title),
            style = MaterialTheme.typography.titleMedium
        )
        labeledSwitch(
            labelRes = R.string.nutrition_reminders_pause,
            checked = paused,
            tag = "nutrition_reminders_pause"
        ) {
            paused = it
            persist(context, paused, weighIn, weeks)
        }
        labeledSwitch(
            labelRes = R.string.nutrition_reminders_weigh_in,
            checked = weighIn,
            enabled = !paused,
            tag = "nutrition_reminders_weigh_in"
        ) {
            weighIn = it
            persist(context, paused, weighIn, weeks)
        }
        Text(stringResource(R.string.nutrition_reminders_measurements) + ": $weeks wk")
        TextButton(
            onClick = {
                weeks = (weeks - 1).coerceAtLeast(2)
                persist(context, paused, weighIn, weeks)
            },
            enabled = !paused
        ) { Text("-") }
        TextButton(
            onClick = {
                weeks = (weeks + 1).coerceAtMost(4)
                persist(context, paused, weighIn, weeks)
            },
            enabled = !paused,
            modifier = Modifier.testTag("nutrition_reminders_measurements")
        ) { Text("+") }
        Text(
            text = stringResource(R.string.nutrition_checkin_title),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun labeledSwitch(labelRes: Int, checked: Boolean, tag: String, enabled: Boolean = true, onChecked: (Boolean) -> Unit) {
    Column(modifier = Modifier.testTag(tag)) {
        Text(stringResource(labelRes))
        Switch(checked = checked, onCheckedChange = onChecked, enabled = enabled)
    }
}

private fun persist(context: android.content.Context, paused: Boolean, weighIn: Boolean, weeks: Int) {
    val goal = NutritionPrefsStore.loadGoal()
    goal.remindersPaused = paused
    goal.weighInNudgeEnabled = weighIn
    goal.measurementReminderWeeks = weeks
    NutritionPrefsStore.saveGoal(goal)
    NutritionReminderScheduler.reschedule(context)
}
