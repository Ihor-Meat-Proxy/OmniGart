package com.omnigart.android.food

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.omnigart.android.R
import com.omnigart.design.theme.OmniGartColors
import com.omnigart.shared.OmniGartCore
import com.omnigart.shared.food.models.MealGroup
import com.omnigart.shared.food.models.QuickMacroAdd
import com.omnigart.shared.food.models.QuickServing
import com.omnigart.shared.food.models.CustomFood
import com.omnigart.shared.food.nutrients.BiologicalSexBand
import com.omnigart.shared.food.nutrients.NutrientId
import com.omnigart.shared.food.nutrients.NutrientRankBasis
import com.omnigart.shared.food.settings.FoodSettings
import kotlin.time.Clock

@Composable
fun FoodRecentsBlock(core: OmniGartCore, profileId: String, onLogged: () -> Unit) {
    Text(text = stringResource(R.string.food_recents_title), style = MaterialTheme.typography.titleSmall)
    val recents = remember(profileId) { core.foodLogging.recents(profileId, 8) }
    if (recents.isEmpty()) {
        Text(
            text = stringResource(R.string.food_recents_empty),
            color = OmniGartColors.colorTextLightSecondary,
            style = MaterialTheme.typography.bodySmall
        )
    } else {
        recents.forEach { entry ->
            TextButton(
                onClick = {
                    core.foodLogging.repeatRecent(profileId, entry.id, entry.mealGroup)
                    core.logFood(profileId, entry.name, entry.energyKcal, entry.proteinG, entry.carbsG, entry.fatG)
                    onLogged()
                },
                modifier = Modifier.testTag("food_recent_${entry.id}")
            ) {
                Text(entry.name)
            }
        }
    }
}

@Composable
fun FoodFavouritesBlock(core: OmniGartCore, profileId: String) {
    Text(text = stringResource(R.string.food_favourites_title), style = MaterialTheme.typography.titleSmall)
    val favs = remember(profileId) { core.foodLogging.favourites(profileId) }
    if (favs.isEmpty()) {
        Text(
            text = stringResource(R.string.food_favourites_empty),
            color = OmniGartColors.colorTextLightSecondary,
            style = MaterialTheme.typography.bodySmall
        )
    } else {
        favs.forEach { entry ->
            Text(text = entry.name, modifier = Modifier.testTag("food_favourite_${entry.id}"))
        }
    }
}

@Composable
fun FoodBestSourcesBlock(core: OmniGartCore, profileId: String) {
    Text(text = stringResource(R.string.food_gaps_title), style = MaterialTheme.typography.titleSmall)
    val end = Clock.System.now().toEpochMilliseconds()
    val gapSummary =
        remember(profileId) {
            core.foodLogging.fillGapsSummary(profileId, end - 86_400_000L, end, 30, BiologicalSexBand.UNSPECIFIED)
        }
    Text(
        text =
        if (gapSummary.isEmpty()) {
            stringResource(R.string.food_gaps_none)
        } else {
            stringResource(R.string.food_gaps_hint) + "\n" + gapSummary
        },
        style = MaterialTheme.typography.bodySmall,
        color = OmniGartColors.colorTextLightSecondary,
        modifier = Modifier.testTag("food_gaps")
    )
    val ranks =
        remember(profileId) {
            core.foodLogging.bestSources(profileId, NutrientId.VITAMIN_C, NutrientRankBasis.PER_100_KCAL, null, 3)
        }
    Text(
        text =
        stringResource(R.string.food_best_sources_title) +
            if (ranks.isEmpty()) {
                ""
            } else {
                ": " + ranks.joinToString { it.food.name }
            },
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier.testTag("food_best_sources")
    )
}

@Composable
fun FoodToolsBlock(core: OmniGartCore, profileId: String, onLogged: () -> Unit) {
    var barcode by remember { mutableStateOf("") }
    var labelText by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    Text(text = stringResource(R.string.food_tools_title), style = MaterialTheme.typography.titleSmall)
    Button(
        onClick = {
            val end = Clock.System.now().toEpochMilliseconds()
            val start = end - 86_400_000L
            core.foodLogging.copyDayEpoch(profileId, start - 86_400_000L, start, start)
            onLogged()
            message = "Copied"
        },
        modifier = Modifier.testTag("food_copy_day")
    ) {
        Text(stringResource(R.string.food_copy_yesterday))
    }
    OutlinedTextField(
        value = barcode,
        onValueChange = { barcode = it },
        label = { Text(stringResource(R.string.food_barcode_manual)) },
        modifier = Modifier.fillMaxWidth().testTag("food_barcode_manual")
    )
    Button(
        onClick = {
            val code = FoodLabelBarcodeHelpers.validateBarcode(barcode) ?: return@Button
            val food = core.foodLogging.findBarcode(profileId, code) ?: return@Button
            core.foodLogging.logQuickServing(profileId, food.id, QuickServing.PER_100_G, MealGroup.SNACKS, "en-GB")
            onLogged()
        },
        modifier = Modifier.testTag("food_barcode_lookup")
    ) {
        Text(stringResource(R.string.food_barcode_lookup))
    }
    OutlinedTextField(
        value = labelText,
        onValueChange = { labelText = it },
        label = { Text(stringResource(R.string.food_label_paste_hint)) },
        modifier = Modifier.fillMaxWidth().testTag("food_label_ocr_text")
    )
    Button(
        onClick = {
            val nutrients = FoodLabelBarcodeHelpers.parseLabelOcr(labelText) ?: return@Button
            core.foodLogging.quickAdd(
                profileId,
                QuickMacroAdd(
                    name = "Label scan",
                    energyKcal = nutrients.energyKcal,
                    proteinG = nutrients.proteinG,
                    carbsG = nutrients.carbsG,
                    fatG = nutrients.fatG
                ),
                MealGroup.SNACKS
            )
            onLogged()
        },
        modifier = Modifier.testTag("food_label_parse")
    ) {
        Text(stringResource(R.string.food_label_parse))
    }
    var importPaste by remember { mutableStateOf("") }
    var customName by remember { mutableStateOf("") }
    var overrideAmount by remember { mutableStateOf("") }
    OutlinedTextField(
        value = importPaste,
        onValueChange = { importPaste = it },
        label = { Text(stringResource(R.string.food_import_paste)) },
        modifier = Modifier.fillMaxWidth().testTag("food_import_paste")
    )
    Button(
        onClick = {
            val n = core.foodLogging.importJson(profileId, importPaste)
            message = "Imported $n"
            onLogged()
        },
        modifier = Modifier.testTag("food_import_json")
    ) {
        Text(stringResource(R.string.food_import_json))
    }
    Button(
        onClick = {
            val n = core.foodLogging.importCsv(profileId, importPaste)
            message = "Imported $n"
            onLogged()
        },
        modifier = Modifier.testTag("food_import_csv")
    ) {
        Text(stringResource(R.string.food_import_csv))
    }
    Button(
        onClick = {
            importPaste = core.foodLogging.exportJson(profileId)
            message = "Exported"
        },
        modifier = Modifier.testTag("food_export_json")
    ) {
        Text(stringResource(R.string.food_export_json))
    }
    OutlinedTextField(
        value = customName,
        onValueChange = { customName = it },
        label = { Text(stringResource(R.string.food_custom_name)) },
        modifier = Modifier.fillMaxWidth().testTag("food_custom_name")
    )
    Button(
        onClick = {
            val name = customName.trim()
            if (name.isEmpty()) return@Button
            core.foodLogging.upsertCustomFood(
                CustomFood(
                    id = "custom-${Clock.System.now().toEpochMilliseconds()}",
                    profileId = profileId,
                    name = name,
                    energyKcalPer100g = 100.0
                )
            )
            customName = ""
            onLogged()
        },
        modifier = Modifier.testTag("food_custom_save")
    ) {
        Text(stringResource(R.string.food_custom_save))
    }
    OutlinedTextField(
        value = overrideAmount,
        onValueChange = { overrideAmount = it },
        label = { Text(stringResource(R.string.food_override_amount)) },
        modifier = Modifier.fillMaxWidth().testTag("food_override_amount")
    )
    Button(
        onClick = {
            overrideAmount.toDoubleOrNull()?.let {
                core.foodLogging.setReferenceOverrideAmount(profileId, NutrientId.VITAMIN_C.raw, it)
            } ?: core.foodLogging.clearReferenceOverride(profileId, NutrientId.VITAMIN_C.raw)
            onLogged()
        },
        modifier = Modifier.testTag("food_override_vitamin_c")
    ) {
        Text(stringResource(R.string.food_override_vitamin_c))
    }
    message?.let { Text(text = it, style = MaterialTheme.typography.labelSmall) }
    Text(
        text = stringResource(R.string.food_attribution_short),
        style = MaterialTheme.typography.labelSmall,
        color = OmniGartColors.colorTextLightSecondary,
        modifier = Modifier.testTag("food_attribution")
    )
}

@Composable
fun FoodSettingsBlock(core: OmniGartCore, profileId: String) {
    var offline by remember {
        mutableStateOf(core.foodLogging.settings(profileId).offlineOnly)
    }
    var erased by remember { mutableStateOf(false) }
    Text(text = stringResource(R.string.food_settings_title), style = MaterialTheme.typography.titleSmall)
    Button(
        onClick = {
            offline = !offline
            val current = core.foodLogging.settings(profileId)
            core.foodLogging.saveSettings(
                FoodSettings(
                    profileId = profileId,
                    offlineOnly = offline,
                    morningAiReviewEnabled = current.morningAiReviewEnabled,
                    morningReviewMinutes = current.morningReviewMinutes,
                    dayStartMinutes = current.dayStartMinutes ?: 240,
                    referenceOverrides = current.referenceOverrides
                )
            )
        },
        modifier = Modifier.testTag("food_offline_only")
    ) {
        Text(
            text =
            if (offline) {
                stringResource(R.string.food_offline_only)
            } else {
                stringResource(R.string.food_offline_only)
            }
        )
    }
    Button(
        onClick = {
            core.foodLogging.eraseAllAndCancelNotifications(profileId)
            erased = true
        },
        modifier = Modifier.testTag("food_erase_all")
    ) {
        Text(stringResource(R.string.food_erase_all))
    }
    if (erased) {
        Text(text = stringResource(R.string.food_erase_done), style = MaterialTheme.typography.labelSmall)
    }
    Text(
        text = stringResource(R.string.food_encryption_note),
        style = MaterialTheme.typography.labelSmall,
        color = OmniGartColors.colorTextLightSecondary
    )
    Text(
        text = stringResource(R.string.food_online_privacy_note),
        style = MaterialTheme.typography.labelSmall,
        color = OmniGartColors.colorTextLightSecondary
    )
}
