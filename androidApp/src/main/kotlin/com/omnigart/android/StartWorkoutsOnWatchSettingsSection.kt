package com.omnigart.android

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.omnigart.android.health.WearLaunchPrefs
import com.omnigart.design.theme.OmniGartColors

@Composable
fun StartWorkoutsOnWatchSettingsSection() {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(WearLaunchPrefs.isStartOnWatchEnabled(context)) }
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(
            text = stringResource(R.string.workout_watch_settings_title),
            style = MaterialTheme.typography.titleMedium
        )
        Row(
            modifier = Modifier.fillMaxWidth().testTag("profile_start_workouts_on_watch_toggle"),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(text = stringResource(R.string.workout_start_on_watch_toggle))
                Text(
                    text = stringResource(R.string.workout_start_on_watch_toggle_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = OmniGartColors.colorTextLightSecondary
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = {
                    enabled = it
                    WearLaunchPrefs.setStartOnWatchEnabled(context, it)
                }
            )
        }
    }
}
