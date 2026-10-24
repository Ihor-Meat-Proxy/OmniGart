package com.omnigart.android.food

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import com.omnigart.android.R
import com.omnigart.design.theme.OmniGartColors
import com.omnigart.shared.OmniGartCore
import com.omnigart.shared.food.models.CatalogueFood
import com.omnigart.shared.food.models.MealGroup
import com.omnigart.shared.food.models.QuickMacroAdd
import com.omnigart.shared.food.models.QuickServing

@Composable
internal fun FoodAddMealPanel(
    core: OmniGartCore,
    profileId: String,
    meal: MealGroup,
    onLogged: () -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var portion by remember { mutableStateOf("100") }
    var name by remember { mutableStateOf("") }
    var energy by remember { mutableStateOf("250") }
    var protein by remember { mutableStateOf("20") }
    var carbs by remember { mutableStateOf("25") }
    var fat by remember { mutableStateOf("8") }
    var barcode by remember { mutableStateOf("") }
    val results =
        remember(query) {
            if (query.isBlank()) emptyList() else core.foodLogging.search(profileId, query, "en-GB")
        }

    Text(text = foodMealTitle(meal), style = MaterialTheme.typography.titleMedium)
    OutlinedTextField(
        value = query,
        onValueChange = { query = it },
        label = { Text(stringResource(R.string.food_search_placeholder)) },
        modifier = Modifier.fillMaxWidth().testTag("food_search")
    )
    OutlinedTextField(
        value = portion,
        onValueChange = { portion = it },
        label = { Text(stringResource(R.string.food_portion_grams)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth().testTag("food_portion_grams")
    )
    results.take(8).forEach { food ->
        CatalogueResultRow(core, profileId, food, meal, portion, onLogged)
    }
    FoodAddMealRecents(core, profileId, meal, onLogged)
    FoodAddMealQuickAdd(
        name = name,
        onName = { name = it },
        energy = energy,
        onEnergy = { energy = it },
        protein = protein,
        carbs = carbs,
        fat = fat,
        meal = meal,
        core = core,
        profileId = profileId,
        onLogged = onLogged
    )
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
            val grams = portion.toDoubleOrNull() ?: 100.0
            core.foodLogging.logFromCatalogue(profileId, food.id, grams, meal, "en-GB")
            core.logFood(
                profileId,
                food.name,
                food.energyKcalPer100g * grams / 100.0,
                food.proteinGPer100g * grams / 100.0,
                food.carbsGPer100g * grams / 100.0,
                food.fatGPer100g * grams / 100.0
            )
            onLogged()
        },
        modifier = Modifier.testTag("food_barcode_lookup")
    ) {
        Text(stringResource(R.string.food_barcode_lookup))
    }
    TextButton(onClick = onDismiss) { Text(stringResource(R.string.onboarding_skip)) }
}

@Composable
private fun FoodAddMealQuickAdd(
    name: String,
    onName: (String) -> Unit,
    energy: String,
    onEnergy: (String) -> Unit,
    protein: String,
    carbs: String,
    fat: String,
    meal: MealGroup,
    core: OmniGartCore,
    profileId: String,
    onLogged: () -> Unit
) {
    Text(stringResource(R.string.food_quick_add_title), style = MaterialTheme.typography.titleSmall)
    OutlinedTextField(
        value = name,
        onValueChange = onName,
        label = { Text(stringResource(R.string.food_name_placeholder)) },
        modifier = Modifier.fillMaxWidth().testTag("food_name")
    )
    OutlinedTextField(
        value = energy,
        onValueChange = onEnergy,
        label = { Text(stringResource(R.string.food_energy_kcal)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth().testTag("food_energy")
    )
    Button(
        onClick = {
            val trimmed = name.trim()
            if (trimmed.isEmpty()) return@Button
            val kcal = energy.toDoubleOrNull() ?: 0.0
            val proteinG = protein.toDoubleOrNull() ?: 0.0
            val carbsG = carbs.toDoubleOrNull() ?: 0.0
            val fatG = fat.toDoubleOrNull() ?: 0.0
            core.foodLogging.quickAdd(profileId, QuickMacroAdd(trimmed, kcal, proteinG, carbsG, fatG), meal)
            core.logFood(profileId, trimmed, kcal, proteinG, carbsG, fatG)
            onLogged()
        },
        modifier = Modifier.testTag("food_add_confirm")
    ) {
        Text(stringResource(R.string.food_add))
    }
}

@Composable
private fun CatalogueResultRow(
    core: OmniGartCore,
    profileId: String,
    food: CatalogueFood,
    meal: MealGroup,
    portion: String,
    onLogged: () -> Unit
) {
    Column(modifier = Modifier.testTag("food_catalogue_${food.id}")) {
        Text("${food.name} · ${food.energyKcalPer100g.toInt()} kcal/100g")
        Row {
            TextButton(
                onClick = {
                    core.foodLogging.logQuickServing(profileId, food.id, QuickServing.PER_100_G, meal, "en-GB")
                    core.logFood(
                        profileId,
                        food.name,
                        food.energyKcalPer100g,
                        food.proteinGPer100g,
                        food.carbsGPer100g,
                        food.fatGPer100g
                    )
                    onLogged()
                }
            ) { Text(stringResource(R.string.food_serving_100g)) }
            TextButton(
                onClick = {
                    val grams = portion.toDoubleOrNull() ?: 100.0
                    core.foodLogging.logFromCatalogue(profileId, food.id, grams, meal, "en-GB")
                    core.logFood(
                        profileId,
                        food.name,
                        food.energyKcalPer100g * grams / 100.0,
                        food.proteinGPer100g * grams / 100.0,
                        food.carbsGPer100g * grams / 100.0,
                        food.fatGPer100g * grams / 100.0
                    )
                    onLogged()
                }
            ) { Text(stringResource(R.string.food_portion_grams)) }
        }
    }
}

@Composable
private fun FoodAddMealRecents(core: OmniGartCore, profileId: String, meal: MealGroup, onLogged: () -> Unit) {
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
                    core.foodLogging.repeatRecent(profileId, entry.id, meal)
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
