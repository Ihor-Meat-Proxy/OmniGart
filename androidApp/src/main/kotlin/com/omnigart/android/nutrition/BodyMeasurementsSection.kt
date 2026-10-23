package com.omnigart.android.nutrition

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier.modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.omnigart.android.R
import com.omnigart.design.theme.OmniGartColors
import kotlinx.coroutines.launch

@Composable
fun BodyMeasurementsSection() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var weight by remember { mutableStateOf("70") }
    var waist by remember { mutableStateOf("") }
    var hips by remember { mutableStateOf("") }
    var chest by remember { mutableStateOf("") }
    var arm by remember { mutableStateOf("") }
    var thigh by remember { mutableStateOf("") }
    var bodyFat by remember { mutableStateOf("") }
    var whtr by remember { mutableStateOf("—") }
    var saved by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth().testTag("nutrition_measurements"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.nutrition_measurements_title),
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = stringResource(R.string.nutrition_measurements_intro),
            style = MaterialTheme.typography.bodySmall,
            color = OmniGartColors.colorTextLightSecondary
        )
        field(R.string.nutrition_weight, weight) { weight = it }
        field(R.string.nutrition_site_waist, waist) { waist = it }
        field(R.string.nutrition_site_hips, hips) { hips = it }
        field(R.string.nutrition_site_chest, chest) { chest = it }
        field(R.string.nutrition_site_arm, arm) { arm = it }
        field(R.string.nutrition_site_thigh, thigh) { thigh = it }
        field(R.string.nutrition_body_fat, bodyFat) { bodyFat = it }
        Text("${stringResource(R.string.nutrition_whtr_label)}: $whtr")
        Button(
            onClick = {
                NutritionPrefsStore.init(context)
                weight.toDoubleOrNull()?.takeIf { it > 0 }?.let { mass ->
                    val sample = NutritionPrefsStore.appendWeight(mass)
                    scope.launch {
                        NutritionHealthConnectBridge.appendWeighInIfAbsent(
                            context = context,
                            localId = sample.id,
                            massKg = mass
                        )
                    }
                }
                mapOf(
                    "WAIST" to waist,
                    "HIPS" to hips,
                    "CHEST" to chest,
                    "ARM" to arm,
                    "THIGH" to thigh
                ).forEach { (site, raw) ->
                    raw.toDoubleOrNull()?.takeIf { it > 0 }?.let {
                        NutritionPrefsStore.appendGirth(site, it)
                    }
                }
                bodyFat.toDoubleOrNull()?.takeIf { it > 0 }?.let {
                    NutritionPrefsStore.appendBodyFat(it)
                }
                waist.toDoubleOrNull()?.takeIf { it > 0 }?.let {
                    whtr = String.format("%.2f", it / 170.0)
                }
                saved = true
                NutritionReminderScheduler.reschedule(context)
            },
            modifier = Modifier.testTag("nutrition_save_measurement")
        ) {
            Text(stringResource(R.string.nutrition_save_measurement))
        }
        if (saved) {
            Text(
                text = stringResource(R.string.profile_saved),
                color = OmniGartColors.colorBrandAccent,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun field(labelRes: Int, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(stringResource(labelRes)) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
}
