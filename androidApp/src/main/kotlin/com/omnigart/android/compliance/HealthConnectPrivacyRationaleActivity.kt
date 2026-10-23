package com.omnigart.android.compliance

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.omnigart.android.OmniGartTheme
import com.omnigart.android.R

/**
 * Required Health Connect privacy-policy rationale surface for Play health apps policy.
 *
 * Declared with `androidx.health.ACTION_SHOW_PERMISSIONS_RATIONALE` so the system
 * permissions UI can deep-link here. The app remains usable if the user denies
 * Health Connect access afterward.
 */
class HealthConnectPrivacyRationaleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OmniGartTheme {
                Column(
                    modifier =
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp)
                        .testTag("hc_privacy_rationale"),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.hc_rationale_title),
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = stringResource(R.string.hc_rationale_body),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    TextButton(
                        onClick = {
                            // Public policy URL — replace when the live policy is published.
                            val uri = Uri.parse("https://omnigart.com/privacy")
                            startActivity(Intent(Intent.ACTION_VIEW, uri))
                        }
                    ) {
                        Text(stringResource(R.string.hc_rationale_privacy_policy))
                    }
                    Button(onClick = { finish() }) {
                        Text(stringResource(R.string.perm_continue))
                    }
                }
            }
        }
    }
}
