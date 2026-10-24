package com.omnigart.android.treadmill

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
import androidx.core.content.FileProvider
import com.omnigart.android.R
import com.omnigart.design.theme.OmniGartSpacing
import com.omnigart.treadmill.android.TreadmillBleClient
import com.omnigart.treadmill.ble.TreadmillBleUuids
import com.omnigart.treadmill.model.TreadmillProtocol
import java.io.File

/**
 * Developer-only treadmill BLE capture UI (debug source set).
 * Mirrors iOS treadmill diagnostics: scan, GATT services, notify hex log, JSON export/share.
 */
@Composable
fun TreadmillDiagnosticsSection(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val client = remember { TreadmillBleClient(context, scope) }
    val state by client.state.collectAsState()
    val discovered by client.discovered.collectAsState()
    var status by remember { mutableStateOf<String?>(null) }
    var logText by remember { mutableStateOf(client.diagnostics.exportText()) }

    fun refreshLog() {
        logText = client.diagnostics.exportText()
    }

    DisposableEffect(Unit) {
        onDispose { client.disconnect() }
    }

    Column(
        modifier =
            modifier
                .testTag("treadmill_diagnostics")
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(OmniGartSpacing.space2)
    ) {
        Text(
            text = stringResource(R.string.treadmill_diag_title),
            style = MaterialTheme.typography.titleMedium
        )
        Text(text = stringResource(R.string.treadmill_diag_body))
        Text(text = "BLE: ${state.name}")
        Button(
            onClick = {
                client.startScan()
                refreshLog()
            },
            modifier = Modifier.testTag("treadmill_diag_scan")
        ) {
            Text(stringResource(R.string.treadmill_scan))
        }
        discovered.forEach { device ->
            Button(
                onClick = {
                    client.connect(device.address)
                    refreshLog()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("${device.name} (${device.address})")
            }
        }
        Button(
            onClick = {
                client.useSimulator()
                client.startSimulatorBelt()
                client.diagnostics.protocolGuess = TreadmillProtocol.SIMULATED.name
                client.diagnostics.appendNotify(
                    deviceId = TreadmillBleClient.SIMULATOR_DEVICE_ID,
                    deviceName = "OmniGart Simulated Treadmill",
                    serviceUuid = TreadmillBleUuids.FITNESS_MACHINE_SERVICE,
                    characteristicUuid = TreadmillBleUuids.TREADMILL_DATA,
                    payload = byteArrayOf(0x4C, 0x05),
                    note = "sim-sample"
                )
                refreshLog()
                status = context.getString(R.string.treadmill_diag_captured)
            },
            modifier = Modifier.testTag("treadmill_diag_capture_sim")
        ) {
            Text(stringResource(R.string.treadmill_diag_capture_sim))
        }
        Text(
            text = stringResource(R.string.treadmill_diag_services),
            style = MaterialTheme.typography.titleSmall
        )
        Text(
            text = stringResource(R.string.treadmill_diag_log),
            style = MaterialTheme.typography.titleSmall
        )
        Text(
            text = logText.ifBlank { "—" },
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.testTag("treadmill_diag_log_body")
        )
        Button(
            onClick = {
                client.diagnostics.clear()
                refreshLog()
                status = null
            },
            modifier = Modifier.testTag("treadmill_diag_clear")
        ) {
            Text(stringResource(R.string.treadmill_diag_clear))
        }
        Button(
            onClick = {
                refreshLog()
                val json = client.diagnostics.exportJson(pretty = true)
                val file = File(context.cacheDir, "treadmill-diagnostics.json")
                file.writeText(json)
                val uri =
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                val share =
                    Intent(Intent.ACTION_SEND).apply {
                        type = "application/json"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                context.startActivity(
                    Intent.createChooser(share, context.getString(R.string.treadmill_diag_share))
                )
            },
            modifier = Modifier.testTag("treadmill_diag_export")
        ) {
            Text(stringResource(R.string.treadmill_diag_export))
        }
        status?.let { Text(text = it) }
    }
}
