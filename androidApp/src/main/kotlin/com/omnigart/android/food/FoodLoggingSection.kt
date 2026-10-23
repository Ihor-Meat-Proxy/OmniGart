package com.omnigart.android.food

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.omnigart.android.R
import com.omnigart.design.theme.OmniGartColors
import com.omnigart.design.theme.OmniGartSpacing
import com.omnigart.shared.OmniGartCore
import com.omnigart.shared.compliance.FeatureFlags
import com.omnigart.shared.food.models.CatalogueFood
import com.omnigart.shared.food.models.MealGroup
import com.omnigart.shared.food.models.QuickMacroAdd
import com.omnigart.shared.food.nutrients.BiologicalSexBand
import com.omnigart.shared.food.review.OnDeviceAiAvailability
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

/**
 * Food tab body: quick add, catalogue search, gaps hint, morning review.
 * Kept out of [com.omnigart.android.MainActivity] to avoid merge conflicts with
 * Insights / treadmill / in-workout PRs.
 */
@Composable
fun FoodLoggingSection(core: OmniGartCore, profileId: String) {
    var tick by remember { mutableIntStateOf(0) }
    var reviewSummary by remember { mutableStateOf<String?>(null) }
    val totals = remember(tick, profileId) { core.foodDayTotals(profileId) }

    Column(verticalArrangement = Arrangement.spacedBy(OmniGartSpacing.space2)) {
        Text(
            text = stringResource(R.string.food_intro),
            style = MaterialTheme.typography.bodySmall,
            color = OmniGartColors.colorTextLightSecondary
        )
        FoodTotalsBlock(totals.energyKcal, totals.proteinG, totals.carbsG, totals.fatG)
        FoodQuickAddBlock(core, profileId) { tick += 1 }
        FoodSearchBlock(core, profileId) { tick += 1 }
        Text(
            text = stringResource(R.string.food_gaps_hint),
            style = MaterialTheme.typography.bodySmall,
            color = OmniGartColors.colorTextLightSecondary,
            modifier = Modifier.testTag("food_gaps")
        )
        FoodMorningReviewBlock(
            core = core,
            profileId = profileId,
            reviewSummary = reviewSummary,
            onSummary = { reviewSummary = it }
        )
        Text(
            text = stringResource(R.string.food_attribution_short),
            style = MaterialTheme.typography.labelSmall,
            color = OmniGartColors.colorTextLightSecondary,
            modifier = Modifier.testTag("food_attribution")
        )
    }
}

@Composable
private fun FoodTotalsBlock(energy: Double, protein: Double, carbs: Double, fat: Double) {
    Text(
        text = stringResource(R.string.food_totals_title),
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.testTag("food_totals")
    )
    Text(
        text =
        stringResource(
            R.string.food_totals_line,
            energy.toInt(),
            protein.toInt(),
            carbs.toInt(),
            fat.toInt()
        )
    )
}

@Composable
private fun FoodQuickAddBlock(core: OmniGartCore, profileId: String, onLogged: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var energy by remember { mutableStateOf("250") }
    OutlinedTextField(
        value = name,
        onValueChange = { name = it },
        label = { Text(stringResource(R.string.food_name_placeholder)) },
        modifier = Modifier.fillMaxWidth().testTag("food_name")
    )
    OutlinedTextField(
        value = energy,
        onValueChange = { energy = it },
        label = { Text(stringResource(R.string.food_energy_kcal)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth().testTag("food_energy")
    )
    Button(
        onClick = {
            val trimmed = name.trim()
            if (trimmed.isEmpty()) return@Button
            val kcal = energy.toDoubleOrNull() ?: 0.0
            core.foodLogging.quickAdd(profileId, QuickMacroAdd(trimmed, kcal), MealGroup.SNACKS)
            core.logFood(profileId, trimmed, kcal, 0.0, 0.0, 0.0)
            name = ""
            onLogged()
        },
        modifier = Modifier.testTag("food_add")
    ) {
        Text(stringResource(R.string.food_add))
    }
}

@Composable
private fun FoodSearchBlock(core: OmniGartCore, profileId: String, onLogged: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val results =
        remember(query) {
            if (query.isBlank()) emptyList() else core.foodLogging.search(profileId, query, "en-GB")
        }
    OutlinedTextField(
        value = query,
        onValueChange = { query = it },
        label = { Text(stringResource(R.string.food_search_placeholder)) },
        modifier = Modifier.fillMaxWidth().testTag("food_search")
    )
    results.take(8).forEach { food ->
        CatalogueResultButton(core, profileId, food, onLogged)
    }
}

@Composable
private fun CatalogueResultButton(core: OmniGartCore, profileId: String, food: CatalogueFood, onLogged: () -> Unit) {
    Button(
        onClick = {
            core.foodLogging.logFromCatalogue(
                profileId = profileId,
                foodId = food.id,
                grams = 100.0,
                mealGroup = MealGroup.BREAKFAST,
                locale = "en-GB"
            )
            core.logFood(
                profileId,
                food.name,
                food.energyKcalPer100g,
                food.proteinGPer100g,
                food.carbsGPer100g,
                food.fatGPer100g
            )
            onLogged()
        },
        modifier = Modifier.testTag("food_catalogue_${food.id}")
    ) {
        Text("${food.name} · ${food.energyKcalPer100g.toInt()} kcal/100g")
    }
}

@Composable
private fun FoodMorningReviewBlock(core: OmniGartCore, profileId: String, reviewSummary: String?, onSummary: (String) -> Unit) {
    val aiNote =
        if (FeatureFlags.morningFoodReviewEnabled) {
            stringResource(R.string.food_review_ai_ready)
        } else {
            stringResource(R.string.food_ai_unavailable)
        }
    Text(
        text = aiNote,
        style = MaterialTheme.typography.bodySmall,
        color = OmniGartColors.colorTextLightSecondary
    )
    Button(
        onClick = { onSummary(runMorningReview(core, profileId)) },
        modifier = Modifier.testTag("food_review_run")
    ) {
        Text(stringResource(R.string.food_review_run))
    }
    reviewSummary?.let {
        Text(text = it, modifier = Modifier.testTag("food_review_summary"))
        Text(
            text = stringResource(R.string.food_review_footer),
            style = MaterialTheme.typography.labelSmall,
            color = OmniGartColors.colorTextLightSecondary
        )
    }
}

private fun runMorningReview(core: OmniGartCore, profileId: String): String {
    val end = Clock.System.now()
    val start = end - 1.days
    val dayKey = core.foodLogging.dayKeyFor(profileId, start)
    val age = core.profiles.get(profileId)?.let { core.ageYears(it) } ?: 30
    return core.foodLogging.morningReview(
        profileId = profileId,
        dayKey = dayKey,
        dayStart = start,
        dayEnd = end,
        locale = "en-GB",
        ageYears = age,
        sex = BiologicalSexBand.UNSPECIFIED,
        availability =
        OnDeviceAiAvailability(
            deviceEligible = false,
            featureEnabled = false,
            modelReady = false,
            reasonIfUnavailable = "On-device model not ready on this device"
        )
    ).summary
}
