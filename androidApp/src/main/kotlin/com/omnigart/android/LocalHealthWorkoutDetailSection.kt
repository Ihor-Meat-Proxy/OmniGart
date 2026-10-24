package com.omnigart.android

import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.health.connect.client.PermissionController
import com.omnigart.design.theme.OmniGartColors
import com.omnigart.design.theme.OmniGartSpacing
import com.omnigart.shared.OmniGartCore
import kotlinx.coroutines.launch

private const val ACTION_HEALTH_CONNECT_SETTINGS = "androidx.health.ACTION_HEALTH_CONNECT_SETTINGS"

/** Workout detail with one-tap restore to Health Connect when removed from the platform. */
@Composable
fun LocalHealthWorkoutDetailSection(core: OmniGartCore, workoutId: String, onChanged: () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var message by remember { mutableStateOf<String?>(null) }
    var restoring by remember { mutableStateOf(false) }
    var removed by remember {
        mutableStateOf(core.localHealth.isRemovedFromPlatform(workoutId))
    }
    val permissionLauncher =
        rememberLauncherForActivityResult(
            PermissionController.createRequestPermissionResultContract()
        ) {
            scope.launch {
                runRestore(context, core, workoutId, onChanged) { o, r ->
                    message = o
                    removed = r
                    restoring = false
                }
            }
        }

    Column(verticalArrangement = Arrangement.spacedBy(OmniGartSpacing.space2)) {
        Text(
            text = core.localHealth.workoutDetailTitle(workoutId),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.testTag("health_workout_detail_title")
        )
        Text(
            text = core.localHealth.workoutDetailSubtitle(workoutId),
            style = MaterialTheme.typography.bodySmall,
            color = OmniGartColors.colorTextLightSecondary
        )
        if (removed) {
            RestoreButton(
                restoring = restoring,
                onClick = {
                    scope.launch {
                        restoring = true
                        val outcome =
                            HealthConnectWorkoutRestorer.restore(context, core, workoutId)
                        if (outcome == HealthConnectWorkoutRestorer.Outcome.PERMISSION_NEEDED) {
                            restoring = false
                            runCatching {
                                permissionLauncher.launch(
                                    HealthConnectWorkoutRestorer.writePermissions()
                                )
                            }.onFailure {
                                runCatching {
                                    context.startActivity(Intent(ACTION_HEALTH_CONNECT_SETTINGS))
                                }
                                message =
                                    context.getString(R.string.health_restore_permission_needed)
                            }
                        } else {
                            message = outcomeMessage(context, outcome)
                            removed = core.localHealth.isRemovedFromPlatform(workoutId)
                            restoring = false
                            onChanged()
                        }
                    }
                }
            )
        }
        message?.let {
            Text(text = it, modifier = Modifier.testTag("health_restore_message"))
        }
        Button(
            onClick = { OmnigartPdfShare.shareWorkout(context, core, workoutId) },
            modifier = Modifier.testTag("health_workout_detail_pdf")
        ) {
            Text(text = stringResource(R.string.pdf_export_button))
        }
    }
}

@Composable
private fun RestoreButton(restoring: Boolean, onClick: () -> Unit) {
    Text(
        text = stringResource(R.string.health_restore_removed_banner),
        style = MaterialTheme.typography.labelSmall,
        color = OmniGartColors.colorTextLightSecondary,
        modifier = Modifier.testTag("health_restore_removed_banner")
    )
    Button(
        onClick = onClick,
        enabled = !restoring,
        modifier = Modifier.testTag("health_restore_to_health_connect")
    ) {
        if (restoring) {
            CircularProgressIndicator()
        } else {
            Text(text = stringResource(R.string.health_restore_to_health_connect))
        }
    }
}

private suspend fun runRestore(
    context: Context,
    core: OmniGartCore,
    workoutId: String,
    onChanged: () -> Unit,
    done: (String, Boolean) -> Unit
) {
    val outcome = HealthConnectWorkoutRestorer.restore(context, core, workoutId)
    done(outcomeMessage(context, outcome), core.localHealth.isRemovedFromPlatform(workoutId))
    onChanged()
}

private fun outcomeMessage(context: Context, outcome: HealthConnectWorkoutRestorer.Outcome): String = when (outcome) {
    HealthConnectWorkoutRestorer.Outcome.RESTORED ->
        context.getString(R.string.health_restore_success)
    HealthConnectWorkoutRestorer.Outcome.ALREADY_PRESENT ->
        context.getString(R.string.health_restore_already_present)
    HealthConnectWorkoutRestorer.Outcome.SKIPPED ->
        context.getString(R.string.health_restore_skipped)
    HealthConnectWorkoutRestorer.Outcome.PERMISSION_NEEDED ->
        context.getString(R.string.health_restore_permission_needed)
    HealthConnectWorkoutRestorer.Outcome.FAILED ->
        context.getString(R.string.health_restore_failed)
}
