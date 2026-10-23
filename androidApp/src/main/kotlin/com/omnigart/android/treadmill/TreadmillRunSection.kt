package com.omnigart.android.treadmill

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.omnigart.android.R
import com.omnigart.design.theme.OmniGartColors
import com.omnigart.design.theme.OmniGartSpacing
import com.omnigart.sensors.treadmill.TreadmillBridge
import com.omnigart.sensors.treadmill.TreadmillSampleDto
import com.omnigart.shared.OmniGartCore
import com.omnigart.shared.health.WorkoutActivityType
import java.util.UUID
import kotlin.time.Clock
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Treadmill run entry on the Workouts tab. Uses the in-process FTMS simulator so
 * emulators (no BLE) can exercise the flow; phone BLE adapters live in android/sensors.
 */
@Composable
fun TreadmillRunSection(core: OmniGartCore, @Suppress("UNUSED_PARAMETER") profileId: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf<String?>(null) }
    var sampleText by remember { mutableStateOf("—") }
    var running by remember { mutableStateOf(false) }
    var controlArmed by remember { mutableStateOf(false) }
    var startedAtMs by remember { mutableStateOf<Long?>(null) }
    var lastSample by remember { mutableStateOf<TreadmillSampleDto?>(null) }
    var hrEpoch by remember { mutableStateOf(longArrayOf()) }
    var hrBpm by remember { mutableStateOf(doubleArrayOf()) }

    DisposableEffect(Unit) {
        TreadmillBridge.simulatorReset()
        onDispose { }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(OmniGartSpacing.space2),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(R.string.treadmill_run_title),
            style = MaterialTheme.typography.titleMedium,
            color = OmniGartColors.colorBrandAccent
        )
        Text(
            text = stringResource(R.string.treadmill_run_body),
            style = MaterialTheme.typography.bodySmall,
            color = OmniGartColors.colorTextLightSecondary
        )
        Text(text = sampleText, style = MaterialTheme.typography.bodyLarge)
        status?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = OmniGartColors.colorTextLightSecondary
            )
        }
        Button(
            onClick = {
                TreadmillBridge.simulatorReset()
                controlArmed = false
                running = false
                lastSample = null
                status = context.getString(R.string.treadmill_sim_ready)
            },
            modifier = Modifier.testTag("treadmill_use_simulator")
        ) {
            Text(stringResource(R.string.treadmill_use_simulator))
        }
        Button(
            onClick = {
                val response = TreadmillBridge.simulatorControl(TreadmillBridge.controlRequestControl())
                controlArmed = response.size >= 3 && (response[2].toInt() and 0xFF) == 1
                status =
                    if (controlArmed) {
                        context.getString(R.string.treadmill_control_armed)
                    } else {
                        context.getString(R.string.treadmill_control_failed)
                    }
            },
            modifier = Modifier.testTag("treadmill_arm_control")
        ) {
            Text(stringResource(R.string.treadmill_arm_control))
        }
        Button(
            onClick = {
                if (!controlArmed) {
                    status = context.getString(R.string.treadmill_arm_first)
                    return@Button
                }
                if (running) {
                    TreadmillBridge.simulatorControl(TreadmillBridge.controlStop())
                    running = false
                    status = context.getString(R.string.treadmill_stopped)
                } else {
                    if (startedAtMs == null) {
                        startedAtMs = Clock.System.now().toEpochMilliseconds()
                        hrEpoch = longArrayOf()
                        hrBpm = doubleArrayOf()
                    }
                    TreadmillBridge.simulatorControl(TreadmillBridge.controlStart())
                    running = true
                    status = context.getString(R.string.treadmill_started)
                    scope.launch {
                        while (isActive && running) {
                            val dto = TreadmillBridge.simulatorTick()
                            lastSample = dto
                            sampleText =
                                buildString {
                                    append(dto.optionalSpeed()?.let { "%.1f km/h".format(it) } ?: "—")
                                    append(" · ")
                                    append(dto.optionalIncline()?.let { "%.1f%%".format(it) } ?: "—")
                                    append(" · ")
                                    append(dto.optionalDistance()?.let { "%.0f m".format(it) } ?: "—")
                                }
                            dto.optionalHr()?.let { bpm ->
                                hrEpoch = hrEpoch + Clock.System.now().toEpochMilliseconds()
                                hrBpm = hrBpm + bpm.toDouble()
                            }
                            delay(1_000)
                        }
                    }
                }
            },
            modifier = Modifier.testTag("treadmill_toggle_belt")
        ) {
            Text(
                stringResource(
                    if (running) R.string.treadmill_stop_belt else R.string.treadmill_start_belt
                )
            )
        }
        TreadmillBridge.presetIds().forEach { id ->
            Button(
                onClick = {
                    if (!controlArmed) {
                        status = context.getString(R.string.treadmill_arm_first)
                    } else {
                        TreadmillBridge.simulatorApplyPreset(id)
                    }
                },
                modifier = Modifier.testTag("treadmill_preset_$id")
            ) {
                Text(presetLabel(id))
            }
        }
        Button(
            onClick = {
                val end = Clock.System.now().toEpochMilliseconds()
                val start = startedAtMs ?: end
                val localId = "treadmill-${UUID.randomUUID()}"
                val dto = lastSample
                val avg = if (hrBpm.isEmpty()) null else hrBpm.average()
                val syncId =
                    core.localHealth.recordOmniGartWorkout(
                        localId = localId,
                        activityTypeName = WorkoutActivityType.RUNNING.name,
                        startEpochMs = start,
                        endEpochMs = end,
                        activeEnergyKcal = dto?.optionalEnergy(),
                        basalEnergyKcal = null,
                        distanceMetres = dto?.optionalDistance(),
                        averageHeartRateBpm = avg,
                        maxHeartRateBpm = hrBpm.maxOrNull(),
                        sessionId = localId,
                        planId = null,
                        heartRateEpochMs = hrEpoch,
                        heartRateBpm = hrBpm,
                        syncVersion = 1
                    )
                status = context.getString(R.string.treadmill_save_ok) + " ($syncId)"
                TreadmillBridge.simulatorControl(TreadmillBridge.controlStop())
                running = false
            },
            modifier = Modifier.testTag("treadmill_save")
        ) {
            Text(stringResource(R.string.treadmill_save))
        }
    }
}

@Composable
private fun presetLabel(id: String): String {
    val key = TreadmillBridge.presetLabelKey(id)
    val resId =
        when (key) {
            "treadmill_preset_easy" -> R.string.treadmill_preset_easy
            "treadmill_preset_steady" -> R.string.treadmill_preset_steady
            "treadmill_preset_tempo" -> R.string.treadmill_preset_tempo
            "treadmill_preset_hill" -> R.string.treadmill_preset_hill
            "treadmill_preset_walk" -> R.string.treadmill_preset_walk
            else -> R.string.treadmill_run_title
        }
    return stringResource(resId)
}
