package com.omnigart.android.treadmill

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.omnigart.android.R
import com.omnigart.design.theme.OmniGartColors
import com.omnigart.design.theme.OmniGartSpacing
import com.omnigart.shared.OmniGartCore
import com.omnigart.shared.health.HealthWorkout
import com.omnigart.shared.health.SdkHealthConnectGateway
import com.omnigart.shared.health.WorkoutActivityType
import com.omnigart.shared.health.store.HealthRecordOrigin
import com.omnigart.shared.health.store.HealthSyncIdentity
import com.omnigart.shared.health.store.LocalHealthWorkout
import com.omnigart.treadmill.android.TreadmillBleClient
import com.omnigart.treadmill.api.TreadmillSessionNotes
import com.omnigart.treadmill.model.TreadmillLiveMetrics
import com.omnigart.treadmill.model.TreadmillWorkoutSummary
import java.io.File
import java.util.UUID
import kotlin.time.Clock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TreadmillRunSection(core: OmniGartCore, profileId: String, modifier: Modifier = Modifier, initialWalking: Boolean = false) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val client = remember { TreadmillBleClient(context, scope) }
    val state by client.state.collectAsState()
    val discovered by client.discovered.collectAsState()
    val metrics by client.metrics.collectAsState()
    var allowControl by remember { mutableStateOf(false) }
    var walking by remember { mutableStateOf(initialWalking) }
    var running by remember { mutableStateOf(false) }
    var startedAtMs by remember { mutableStateOf<Long?>(null) }
    var status by remember { mutableStateOf<String?>(null) }
    var showDiag by remember { mutableStateOf(false) }
    var calibration by remember { mutableStateOf("") }

    DisposableEffect(Unit) {
        onDispose {
            if (!running) client.disconnect()
        }
    }

    fun finishSession() {
        scope.launch {
            status =
                withContext(Dispatchers.IO) {
                    saveWorkout(core, client, startedAtMs, calibration, context, walking)
                }
            running = false
            client.disconnect()
        }
    }

    Column(
        modifier = modifier.padding(vertical = OmniGartSpacing.space2),
        verticalArrangement = Arrangement.spacedBy(OmniGartSpacing.space2)
    ) {
        TreadmillHeader(onShowDiagnostics = { showDiag = true })
        RowSwitch(stringResource(R.string.treadmill_walk_title), walking) { walking = it }
        Text(text = "BLE: ${state.name} / ${metrics.protocol.name}")
        TreadmillConnectionButtons(
            client = client,
            discovered = discovered,
            showSimulator = isDebuggable(context) || state == TreadmillBleClient.State.Failed
        )
        RowSwitch(stringResource(R.string.treadmill_allow_control), allowControl) {
            allowControl = it
            client.allowRemoteControl = it
        }
        TreadmillMetricsBlock(metrics = metrics)
        if (allowControl) {
            TreadmillControlButtons(client = client, metrics = metrics)
        }
        TreadmillSessionButtons(
            running = running,
            onStart = {
                running = true
                startedAtMs = System.currentTimeMillis()
                if (client.usingSimulator) {
                    client.allowRemoteControl = true
                    allowControl = true
                    client.startSimulatorBelt()
                } else {
                    client.connectRemembered()
                }
            },
            onFinish = {
                if (client.usingSimulator) client.stopSimulatorBelt()
                finishSession()
            }
        )
        status?.let { Text(text = it) }
        if (!metrics.connected) {
            TreadmillCalibrationFields(calibration = calibration, onChange = { calibration = it })
        }
        if (showDiag) {
            TreadmillDiagnosticsBlock(
                client = client,
                onClose = { showDiag = false },
                onExported = { status = it }
            )
        }
        Text(text = profileId.take(0), modifier = Modifier.padding(0.dp))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TreadmillHeader(onShowDiagnostics: () -> Unit) {
    Text(
        text = stringResource(R.string.treadmill_run_title),
        style = MaterialTheme.typography.titleMedium,
        color = OmniGartColors.colorBrandAccent,
        modifier =
        Modifier
            .testTag("treadmill_run_title")
            .combinedClickable(onClick = {}, onLongClick = onShowDiagnostics)
    )
    Text(
        text = stringResource(R.string.treadmill_run_body),
        color = OmniGartColors.colorTextLightSecondary
    )
}

@Composable
private fun TreadmillConnectionButtons(
    client: TreadmillBleClient,
    discovered: List<TreadmillBleClient.Discovered>,
    showSimulator: Boolean
) {
    Button(onClick = { client.startScan() }, modifier = Modifier.testTag("treadmill_scan")) {
        Text(stringResource(R.string.treadmill_scan))
    }
    Button(onClick = { client.connectRemembered() }) {
        Text(stringResource(R.string.treadmill_reconnect))
    }
    if (showSimulator) {
        Button(
            onClick = {
                client.useSimulator()
                client.allowRemoteControl = true
            },
            modifier = Modifier.testTag("treadmill_use_simulator")
        ) {
            Text(stringResource(R.string.treadmill_use_simulator))
        }
    }
    discovered.forEach { device ->
        Button(onClick = { client.connect(device.address) }, modifier = Modifier.fillMaxWidth()) {
            Text("${device.name} (${device.address})")
        }
    }
}

private fun isDebuggable(context: Context): Boolean =
    (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0

@Composable
private fun TreadmillMetricsBlock(metrics: TreadmillLiveMetrics) {
    metric(stringResource(R.string.treadmill_speed), metrics.speedKmh?.let { "%.1f km/h".format(it) })
    metric(stringResource(R.string.treadmill_pace), metrics.paceMinPerKm?.let { "%.2f".format(it) })
    metric(stringResource(R.string.treadmill_incline), metrics.inclinePercent?.let { "%.1f %%".format(it) })
    metric(stringResource(R.string.treadmill_distance), metrics.distanceMetres?.let { "%.0f m".format(it) })
    metric(stringResource(R.string.treadmill_hr), metrics.displayHeartRateBpm?.toString())
    metric(stringResource(R.string.treadmill_zone), metrics.heartRateZone?.let { "Z$it" })
    metric(stringResource(R.string.treadmill_calories), metrics.energyKcal?.let { "%.0f".format(it) })
}

@Composable
private fun TreadmillControlButtons(client: TreadmillBleClient, metrics: TreadmillLiveMetrics) {
    Button(onClick = { client.setTargetSpeedKmh((metrics.speedKmh ?: 5.0) + 0.5) }) {
        Text(stringResource(R.string.treadmill_speed_up))
    }
    Button(onClick = { client.setTargetInclinePercent((metrics.inclinePercent ?: 0.0) + 0.5) }) {
        Text(stringResource(R.string.treadmill_incline_up))
    }
}

@Composable
private fun TreadmillSessionButtons(running: Boolean, onStart: () -> Unit, onFinish: () -> Unit) {
    if (!running) {
        Button(onClick = onStart, modifier = Modifier.testTag("treadmill_start")) {
            Text(stringResource(R.string.treadmill_start))
        }
    } else {
        Button(onClick = onFinish, modifier = Modifier.testTag("treadmill_finish")) {
            Text(stringResource(R.string.treadmill_finish))
        }
    }
}

@Composable
private fun TreadmillCalibrationFields(calibration: String, onChange: (String) -> Unit) {
    Text(stringResource(R.string.treadmill_calibration_body))
    OutlinedTextField(
        value = calibration,
        onValueChange = onChange,
        label = { Text(stringResource(R.string.treadmill_calibration_metres)) },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun TreadmillDiagnosticsBlock(client: TreadmillBleClient, onClose: () -> Unit, onExported: (String) -> Unit) {
    val context = LocalContext.current
    Text(
        text = stringResource(R.string.treadmill_diagnostics_title),
        style = MaterialTheme.typography.titleSmall
    )
    Text(text = client.diagnostics.exportText(), style = MaterialTheme.typography.bodySmall)
    Button(
        onClick = {
            val file = File(context.cacheDir, "omnigart-treadmill-diagnostics.json")
            file.writeText(client.diagnostics.exportJson(pretty = true))
            onExported(file.absolutePath)
        }
    ) {
        Text(stringResource(R.string.treadmill_diagnostics_export))
    }
    Button(onClick = onClose) {
        Text(stringResource(R.string.treadmill_diagnostics_close))
    }
}

@Composable
private fun RowSwitch(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Column {
        Text(text = label)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun metric(label: String, value: String?) {
    Text(text = "$label: ${value ?: "—"}")
}

private suspend fun saveWorkout(
    core: OmniGartCore,
    client: TreadmillBleClient,
    startedAtMs: Long?,
    calibration: String,
    context: Context,
    walking: Boolean = false
): String {
    val end = Clock.System.now()
    val start = startedAtMs?.let { kotlin.time.Instant.fromEpochMilliseconds(it) } ?: end
    val metrics = client.metrics.value
    var distance = metrics.distanceMetres
    var usedEstimate = false
    if (distance == null || distance <= 0.0) {
        usedEstimate = true
        distance = calibration.toDoubleOrNull()
    }
    val activity = if (walking) WorkoutActivityType.WALKING else WorkoutActivityType.RUNNING
    val summary =
        TreadmillWorkoutSummary(
            protocol = metrics.protocol,
            deviceName = null,
            distanceMetres = distance,
            averageSpeedKmh = metrics.speedKmh,
            averageInclinePercent = metrics.inclinePercent,
            machineAverageHeartRateBpm = metrics.displayHeartRateBpm?.toDouble(),
            energyKcal = metrics.energyKcal,
            steps = metrics.steps,
            usedWatchEstimatedDistance = usedEstimate,
            calibrationFactor = null
        )
    val localId = UUID.randomUUID().toString()
    val workout =
        LocalHealthWorkout(
            id = localId,
            identity = HealthSyncIdentity(syncIdentifier = "omnigart.workout.$localId", syncVersion = 1),
            activityType = activity,
            startAt = start,
            endAt = end,
            activeEnergyKcal = metrics.energyKcal,
            distanceMetres = distance,
            steps = metrics.steps,
            averageHeartRateBpm = metrics.displayHeartRateBpm?.toDouble(),
            origin = HealthRecordOrigin.OMNIGART,
            sourceBundle = context.packageName,
            sourceName = "OmniGart Treadmill",
            updatedAt = end,
            notes = TreadmillSessionNotes.encode(summary)
        )
    core.localHealthStore.upsertWorkout(workout)
    if (SdkHealthConnectGateway.isAvailable(context)) {
        runCatching {
            SdkHealthConnectGateway(context).writeWorkouts(
                listOf(
                    HealthWorkout(
                        id = localId,
                        activityType = activity,
                        start = start,
                        end = end,
                        energyKcal = metrics.energyKcal,
                        distanceMetres = distance,
                        averageHeartRateBpm = metrics.displayHeartRateBpm?.toDouble(),
                        sourceName = "OmniGart Treadmill",
                        clientRecordVersion = 1L
                    )
                )
            )
        }
    }
    return context.getString(R.string.treadmill_saved)
}
