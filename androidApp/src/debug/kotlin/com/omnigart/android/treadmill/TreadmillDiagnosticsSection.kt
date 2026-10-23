package com.omnigart.android.treadmill

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.core.content.FileProvider
import com.omnigart.android.R
import com.omnigart.design.theme.OmniGartSpacing
import com.omnigart.sensors.treadmill.TreadmillBridge
import com.omnigart.sensors.treadmill.TreadmillDiagnosticsLog
import java.io.File

/**
 * Developer-only treadmill BLE capture UI (debug source set).
 * Real scanning needs a physical device; this section still exports simulator frames as JSON.
 */
@Composable
fun TreadmillDiagnosticsSection(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val log = remember { TreadmillDiagnosticsLog() }
    var status by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier.testTag("treadmill_diagnostics"),
        verticalArrangement = Arrangement.spacedBy(OmniGartSpacing.space2)
    ) {
        Text(text = stringResource(R.string.treadmill_diag_title))
        Text(text = stringResource(R.string.treadmill_diag_body))
        Button(
            onClick = {
                TreadmillBridge.simulatorReset()
                TreadmillBridge.simulatorControl(TreadmillBridge.controlRequestControl())
                TreadmillBridge.simulatorControl(TreadmillBridge.controlStart())
                val dto = TreadmillBridge.simulatorTick()
                // Capture encoded FTMS bytes via a second tick round-trip note.
                log.append(
                    deviceId = "sim-treadmill-ftms",
                    deviceName = "OmniGart Simulated Treadmill",
                    serviceUuid = "00001826-0000-1000-8000-00805f9b34fb",
                    characteristicUuid = "00002acd-0000-1000-8000-00805f9b34fb",
                    payload = byteArrayOf(0x4C, 0x05),
                    note = "sim-sample protocol=${dto.protocolName} speed=${dto.optionalSpeed()}"
                )
                log.protocolGuess = com.omnigart.sensors.treadmill.TreadmillProtocolKind.Simulated
                status = context.getString(R.string.treadmill_diag_captured)
            },
            modifier = Modifier.testTag("treadmill_diag_capture_sim")
        ) {
            Text(stringResource(R.string.treadmill_diag_capture_sim))
        }
        Button(
            onClick = {
                val json = log.toJson(pretty = true)
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
