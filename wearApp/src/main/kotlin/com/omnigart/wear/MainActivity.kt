package com.omnigart.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val core =
            OmniGartCore(
                DatabaseDriverFactory(applicationContext),
                contentStore = AssetContentStore(applicationContext.assets)
            )
        val profile = core.ensureDemoProfile()
        val headline = core.summaryLines(profile).take(2)
        setContent {
            MaterialTheme {
                WearStartScreen(lines = headline)
            }
        }
    }
}

@Composable
fun WearStartScreen(lines: List<String>) {
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
        Button(
            onClick = {},
            modifier =
            Modifier
                .padding(top = OmniGartSpacing.space3)
                .testTag("wear_start_button")
        ) {
            Text(text = stringResource(R.string.watch_start))
        }
    }
}
