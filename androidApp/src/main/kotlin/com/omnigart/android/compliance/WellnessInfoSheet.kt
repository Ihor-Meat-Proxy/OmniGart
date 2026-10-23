package com.omnigart.android.compliance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.omnigart.android.R
import com.omnigart.design.theme.OmniGartSpacing

/** ⓘ sheet content: estimate framing + required non-medical disclaimer footer. */
@Composable
fun WellnessInfoSheet(modifier: Modifier = Modifier) {
    Column(
        modifier =
        modifier
            .padding(OmniGartSpacing.space4)
            .testTag("wellness_info_sheet"),
        verticalArrangement = Arrangement.spacedBy(OmniGartSpacing.space2)
    ) {
        Text(
            text = stringResource(R.string.info_sheet_title),
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = stringResource(R.string.info_sheet_body),
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = stringResource(R.string.info_sheet_not_medical_advice),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.testTag("info_sheet_disclaimer")
        )
    }
}
