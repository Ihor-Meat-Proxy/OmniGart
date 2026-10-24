package com.omnigart.android.nutrition

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.omnigart.android.R
import com.omnigart.shared.OmniGartCore

@Composable
fun FoodTabNutritionSection(core: OmniGartCore, profileId: String, showMeasurements: Boolean, onShowMeasurements: (Boolean) -> Unit) {
    if (showMeasurements) {
        BodyMeasurementsSection()
        TextButton(onClick = { onShowMeasurements(false) }) {
            Text(stringResource(R.string.tab_food))
        }
    } else {
        NutritionTargetSection(
            core = core,
            profileId = profileId,
            onOpenMeasurements = { onShowMeasurements(true) }
        )
    }
}
