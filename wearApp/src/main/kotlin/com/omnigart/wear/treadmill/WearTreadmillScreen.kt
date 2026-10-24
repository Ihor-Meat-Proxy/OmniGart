package com.omnigart.wear.treadmill

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.omnigart.design.theme.OmniGartColors
import com.omnigart.design.theme.OmniGartSpacing
import com.omnigart.treadmill.android.TreadmillBleClient
import com.omnigart.wear.R

/** Wear OS treadmill live metrics + BLE link during indoor running. */
@Composable
fun WearTreadmillScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val client = remember { TreadmillBleClient(context, scope) }
    val state by client.state.collectAsState()
    val metrics by client.metrics.collectAsState()
    val discovered by client.discovered.collectAsState()

    DisposableEffect(Unit) {
        onDispose { client.disconnect() }
    }

    Column(
        modifier =
        modifier
            .padding(OmniGartSpacing.space2)
            .testTag("wear_treadmill"),
        verticalArrangement = Arrangement.spacedBy(OmniGartSpacing.space2),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.treadmill_run_title),
            style = MaterialTheme.typography.titleMedium,
            color = OmniGartColors.colorBrandAccent,
            textAlign = TextAlign.Center
        )
        Text(
            text = "${state.name} / ${metrics.protocol.name}",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center
        )
        Text(
            text =
            listOfNotNull(
                metrics.speedKmh?.let { "%.1f km/h".format(it) },
                metrics.inclinePercent?.let { "%.1f%%".format(it) },
                metrics.distanceMetres?.let { "%.0fm".format(it) },
                metrics.displayHeartRateBpm?.let { "$it bpm" }
            ).joinToString(" · ").ifEmpty { stringResource(R.string.treadmill_no_connection) },
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium
        )
        Button(
            onClick = { client.startScan() },
            modifier =
            Modifier
                .fillMaxWidth()
                .testTag("wear_treadmill_scan")
        ) {
            Text(text = stringResource(R.string.treadmill_scan))
        }
        discovered.take(2).forEach { device ->
            Button(
                onClick = { client.connect(device.address) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = device.name)
            }
        }
        Button(onClick = { client.connectRemembered() }, modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(R.string.treadmill_reconnect))
        }
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(R.string.treadmill_diagnostics_close))
        }
    }
}
