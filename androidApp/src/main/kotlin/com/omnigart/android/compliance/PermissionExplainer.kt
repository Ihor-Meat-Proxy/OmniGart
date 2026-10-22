package com.omnigart.android.compliance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.omnigart.android.R
import com.omnigart.design.theme.OmniGartSpacing

/**
 * Just-in-time pre-permission explainer. Callers request the OS permission only
 * after [onContinue]; [onNotNow] leaves the app fully usable without the grant.
 */
@Composable
fun PermissionExplainer(
    title: String,
    body: String,
    onContinue: () -> Unit,
    onNotNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier =
        modifier
            .fillMaxWidth()
            .padding(OmniGartSpacing.space4)
            .testTag("permission_explainer"),
        verticalArrangement = Arrangement.spacedBy(OmniGartSpacing.space3)
    ) {
        Text(text = title, style = MaterialTheme.typography.titleLarge)
        Text(text = body, style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onNotNow) {
                Text(stringResource(R.string.perm_not_now))
            }
            Button(onClick = onContinue) {
                Text(stringResource(R.string.perm_continue))
            }
        }
    }
}
