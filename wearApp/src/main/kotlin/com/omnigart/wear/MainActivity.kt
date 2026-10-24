package com.omnigart.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.omnigart.design.theme.OmniGartColors
import com.omnigart.design.theme.OmniGartSpacing
import com.omnigart.shared.OmniGartCore
import com.omnigart.shared.content.AssetContentStore
import com.omnigart.shared.data.DatabaseDriverFactory
import com.omnigart.wear.treadmill.WearTreadmillScreen
import com.omnigart.wear.workout.WearExerciseRecorder
import com.omnigart.wear.workout.WearPhoneWorkoutTransfer
import com.omnigart.wear.workout.WearStrengthLoggingScreen
import com.omnigart.wear.workout.WearWorkoutMode

class MainActivity : ComponentActivity() {
    private lateinit var core: OmniGartCore
    private lateinit var transfer: WearPhoneWorkoutTransfer
    private lateinit var recorder: WearExerciseRecorder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        core =
            OmniGartCore(
                DatabaseDriverFactory(applicationContext),
                contentStore = AssetContentStore(applicationContext.assets)
            )
        transfer = WearPhoneWorkoutTransfer(applicationContext, core).also { it.start() }
        recorder = WearExerciseRecorder(applicationContext, core, transfer)
        val profile = core.ensureDemoProfile()
        val headline = core.summaryLines(profile).take(2)
        setContent {
            MaterialTheme {
                var showTreadmill by remember { mutableStateOf(false) }
                var strengthActive by remember { mutableStateOf(false) }
                when {
                    showTreadmill -> {
                        WearTreadmillScreen(onBack = { showTreadmill = false })
                    }
                    strengthActive -> {
                        WearStrengthLoggingScreen(
                            recorder = recorder,
                            onFinished = { strengthActive = false }
                        )
                    }
                    else -> {
                        WearStartScreen(
                            lines = headline,
                            onStart = {
                                // Prefer Health Services ExerciseClient; fall back to manual publish for emulators.
                                if (recorder.isRunning()) {
                                    recorder.stop { }
                                } else {
                                    runCatching { recorder.start(WearWorkoutMode.RUNNING) }
                                        .onFailure {
                                            recorder.recordManualAndTransfer()
                                        }
                                    if (!recorder.isRunning()) {
                                        recorder.recordManualAndTransfer()
                                    }
                                }
                            },
                            onStartStrength = {
                                recorder.setCurrentExercise("Bench press", "barbell-bench-press")
                                runCatching { recorder.start(WearWorkoutMode.STRENGTH) }
                                strengthActive = true
                            },
                            onTreadmill = { showTreadmill = true }
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        transfer.stop()
        super.onDestroy()
    }
}

@Composable
fun WearStartScreen(
    lines: List<String>,
    onStart: () -> Unit = {},
    onStartStrength: () -> Unit = {},
    onTreadmill: () -> Unit = {}
) {
    var status by remember { mutableStateOf("") }
    Column(
        modifier =
        Modifier
            .fillMaxSize()
            .padding(OmniGartSpacing.space3)
            .testTag("wear_start"),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            color = OmniGartColors.colorBrandAccent,
            modifier = Modifier.testTag("wear_brand")
        )
        Text(
            text = stringResource(R.string.watch_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = OmniGartColors.colorTextLightSecondary,
            modifier = Modifier.padding(vertical = OmniGartSpacing.space2)
        )
        lines.forEach { line ->
            Text(text = line, textAlign = TextAlign.Center)
        }
        if (status.isNotEmpty()) {
            Text(text = status, textAlign = TextAlign.Center)
        }
        Button(
            onClick = {
                onStart()
                status = "sync"
            },
            modifier =
            Modifier
                .padding(top = OmniGartSpacing.space3)
                .testTag("wear_start_button")
        ) {
            Text(text = stringResource(R.string.watch_start))
        }
        Button(
            onClick = {
                onStartStrength()
                status = "strength"
            },
            modifier =
            Modifier
                .padding(top = OmniGartSpacing.space2)
                .testTag("wear_strength_start_button")
        ) {
            Text(text = stringResource(R.string.wear_strength_start))
        }
        Button(
            onClick = onTreadmill,
            modifier =
            Modifier
                .padding(top = OmniGartSpacing.space2)
                .testTag("wear_treadmill_button")
        ) {
            Text(text = stringResource(R.string.treadmill_run_title))
        }
    }
}
