package com.omnigart.android.food

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.omnigart.android.R
import com.omnigart.android.nutrition.BodyMeasurementsSection
import com.omnigart.android.nutrition.NutritionGoalSettingsSection
import com.omnigart.android.nutrition.NutritionPrefsStore
import com.omnigart.design.theme.OmniGartColors
import com.omnigart.design.theme.OmniGartSpacing
import com.omnigart.nutrition.NutritionFacade
import com.omnigart.shared.OmniGartCore
import com.omnigart.shared.compliance.FeatureFlags
import com.omnigart.shared.domain.BiologicalSex
import com.omnigart.shared.food.models.CatalogueFood
import com.omnigart.shared.food.models.MealGroup
import com.omnigart.shared.food.models.QuickMacroAdd
import com.omnigart.shared.food.models.QuickServing
import com.omnigart.shared.food.nutrients.BiologicalSexBand
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

/**
 * Food tab body: day nav, summary, per-meal add, goals, water, weigh-in prompt.
 */
@Composable
fun FoodLoggingSection(core: OmniGartCore, profileId: String) {
    var tick by remember { mutableIntStateOf(0) }
    var dayOffset by remember { mutableIntStateOf(0) }
    var addMeal by remember { mutableStateOf<MealGroup?>(null) }
    var showGoalEditor by remember { mutableStateOf(false) }
    var showMeasurements by remember { mutableStateOf(false) }
    var reviewSummary by remember { mutableStateOf<String?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(OmniGartSpacing.space2)) {
        Text(
            text = stringResource(R.string.food_intro),
            style = MaterialTheme.typography.bodySmall,
            color = OmniGartColors.colorTextLightSecondary
        )
        FoodDayNav(
            dayOffset = dayOffset,
            onPrev = {
                dayOffset -= 1
                tick += 1
            },
            onNext = {
                dayOffset = (dayOffset + 1).coerceAtMost(0)
                tick += 1
            }
        )
        FoodWeighInPrompt(tick = tick) { showMeasurements = true }
        if (showMeasurements) {
            BodyMeasurementsSection()
            TextButton(
                onClick = {
                    showMeasurements = false
                    tick += 1
                }
            ) {
                Text(stringResource(R.string.tab_food))
            }
        }
        FoodDailySummaryBlock(
            core = core,
            profileId = profileId,
            dayOffset = dayOffset,
            tick = tick,
            onEditGoal = { showGoalEditor = true },
            onWater = { tick += 1 }
        )
        if (showGoalEditor) {
            NutritionGoalSettingsSection()
            TextButton(onClick = { showGoalEditor = false }) {
                Text(stringResource(R.string.food_edit_goal))
            }
        }
        MealGroup.entries.filter { it != MealGroup.CUSTOM }.forEach { meal ->
            FoodMealBlock(
                core = core,
                profileId = profileId,
                meal = meal,
                dayOffset = dayOffset,
                tick = tick,
                onAdd = { addMeal = meal }
            )
        }
        addMeal?.let { meal ->
            FoodAddMealPanel(
                core = core,
                profileId = profileId,
                meal = meal,
                onLogged = {
                    tick += 1
                    addMeal = null
                },
                onDismiss = { addMeal = null }
            )
        }
        FoodBestSourcesBlock(core, profileId)
        FoodToolsBlock(core, profileId) { tick += 1 }
        FoodMorningReviewBlock(
            core = core,
            profileId = profileId,
            reviewSummary = reviewSummary,
            onSummary = { reviewSummary = it }
        )
        FoodSettingsBlock(core, profileId)
    }
}

@Composable
private fun FoodDayNav(dayOffset: Int, onPrev: () -> Unit, onNext: () -> Unit) {
    val todayLabel = stringResource(R.string.food_day_today)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        TextButton(onClick = onPrev, modifier = Modifier.testTag("food_day_previous")) {
            Text(stringResource(R.string.food_day_previous))
        }
        Text(
            text = dayLabel(dayOffset, todayLabel),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.testTag("food_day_label")
        )
        TextButton(
            onClick = onNext,
            enabled = dayOffset < 0,
            modifier = Modifier.testTag("food_day_next")
        ) {
            Text(stringResource(R.string.food_day_next))
        }
    }
}

@Composable
private fun FoodWeighInPrompt(tick: Int, onOpenMeasurements: () -> Unit) {
    val show = remember(tick) { shouldPromptWeighIn() }
    if (!show) return
    Text(
        text = stringResource(R.string.food_weigh_in_prompt),
        style = MaterialTheme.typography.bodySmall,
        color = OmniGartColors.colorTextLightSecondary
    )
    TextButton(
        onClick = onOpenMeasurements,
        modifier = Modifier.testTag("food_weigh_in_action")
    ) {
        Text(stringResource(R.string.food_weigh_in_action))
    }
}

@Composable
private fun FoodDailySummaryBlock(
    core: OmniGartCore,
    profileId: String,
    dayOffset: Int,
    tick: Int,
    onEditGoal: () -> Unit,
    onWater: () -> Unit
) {
    val bounds = remember(dayOffset, tick) { dayBounds(dayOffset) }
    val diaryTotals =
        remember(dayOffset, tick, profileId) {
            core.foodLogging.totalsForDayEpoch(profileId, bounds.startMs, bounds.endMs)
        }
    val legacy =
        remember(dayOffset, tick, profileId) {
            core.foodDayTotalsForDayOffset(profileId, dayOffset)
        }
    val eaten =
        if (diaryTotals.entryCount > 0) {
            diaryTotals.energyKcal
        } else {
            legacy.energyKcal
        }
    val protein = if (diaryTotals.entryCount > 0) diaryTotals.proteinG else legacy.proteinG
    val carbs = if (diaryTotals.entryCount > 0) diaryTotals.carbsG else legacy.carbsG
    val fat = if (diaryTotals.entryCount > 0) diaryTotals.fatG else legacy.fatG
    val card = remember(tick, profileId) { computeCard(core, profileId) }
    val target = card?.suggestedDailyKcal?.takeIf { it > 0 } ?: 0.0
    val remaining = if (target > 0) target - eaten else 0.0
    val waterMl = NutritionPrefsStore.waterMl(bounds.dayKey)
    val waterGoal = card?.waterLitres?.takeIf { it > 0 } ?: 0.0

    Column(modifier = Modifier.testTag("food_totals")) {
        Text(stringResource(R.string.food_summary_title), style = MaterialTheme.typography.titleSmall)
        Text("${stringResource(R.string.food_eaten)}: ${eaten.toInt()} ${stringResource(R.string.nutrition_kcal_unit)}")
        Text(
            text =
            "${stringResource(R.string.food_target)}: " +
                if (target > 0) {
                    "${target.toInt()} ${stringResource(R.string.nutrition_kcal_unit)}"
                } else {
                    "—"
                }
        )
        Text(
            text =
            "${stringResource(R.string.food_remaining)}: " +
                if (target > 0) {
                    "${remaining.toInt()} ${stringResource(R.string.nutrition_kcal_unit)}"
                } else {
                    "—"
                },
            modifier = Modifier.testTag("food_remaining")
        )
        Text(
            text =
            String.format(
                Locale.US,
                stringResource(R.string.food_macro_line),
                protein,
                card?.proteinG?.takeIf { it > 0 } ?: 0.0,
                carbs,
                card?.carbsG?.takeIf { it > 0 } ?: 0.0,
                fat,
                card?.fatG?.takeIf { it > 0 } ?: 0.0
            ),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.testTag("food_macro_vs_target")
        )
        Text(
            text =
            String.format(
                Locale.US,
                stringResource(R.string.food_water_logged),
                waterMl,
                waterGoal
            ),
            modifier = Modifier.testTag("food_water_logged")
        )
        Button(
            onClick = {
                NutritionPrefsStore.addWaterMl(bounds.dayKey, 250.0)
                onWater()
            },
            modifier = Modifier.testTag("food_water_add_250")
        ) {
            Text(stringResource(R.string.food_water_add_250))
        }
        TextButton(onClick = onEditGoal, modifier = Modifier.testTag("food_edit_goal")) {
            Text(stringResource(R.string.food_edit_goal))
        }
    }
}

@Composable
private fun FoodMealBlock(
    core: OmniGartCore,
    profileId: String,
    meal: MealGroup,
    dayOffset: Int,
    tick: Int,
    onAdd: () -> Unit
) {
    val bounds = remember(dayOffset, tick) { dayBounds(dayOffset) }
    val entries =
        remember(dayOffset, tick, profileId, meal) {
            core.foodLogging.entriesForDayEpoch(profileId, bounds.startMs, bounds.endMs)
                .filter { it.mealGroup == meal }
        }
    Text(text = mealTitle(meal), style = MaterialTheme.typography.titleSmall)
    if (entries.isEmpty()) {
        Text(
            text = stringResource(R.string.food_empty),
            color = OmniGartColors.colorTextLightSecondary,
            style = MaterialTheme.typography.bodySmall
        )
    } else {
        entries.forEach { entry ->
            Text(
                text =
                "${entry.name} · ${entry.energyKcal.toInt()} kcal",
                modifier = Modifier.testTag("food_entry_${entry.id}")
            )
        }
    }
    // UITest looks for food_add without scrolling; attach to breakfast only.
    val tag = if (meal == MealGroup.BREAKFAST) "food_add" else "food_add_${meal.name.lowercase()}"
    Button(onClick = onAdd, modifier = Modifier.testTag(tag)) {
        Text(stringResource(R.string.food_add_meal))
    }
}

@Composable
private fun FoodAddMealPanel(
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

    Text(text = mealTitle(meal), style = MaterialTheme.typography.titleMedium)
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
    FoodRecentsBlock(core, profileId, meal, onLogged)
    Text(stringResource(R.string.food_quick_add_title), style = MaterialTheme.typography.titleSmall)
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
            val p = protein.toDoubleOrNull() ?: 0.0
            val c = carbs.toDoubleOrNull() ?: 0.0
            val f = fat.toDoubleOrNull() ?: 0.0
            core.foodLogging.quickAdd(profileId, QuickMacroAdd(trimmed, kcal, p, c, f), meal)
            core.logFood(profileId, trimmed, kcal, p, c, f)
            onLogged()
        },
        modifier = Modifier.testTag("food_add_confirm")
    ) {
        Text(stringResource(R.string.food_add))
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
private fun FoodRecentsBlock(core: OmniGartCore, profileId: String, meal: MealGroup, onLogged: () -> Unit) {
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
        availability = FoodGeminiNanoAvailability.check()
    ).summary
}

@Composable
private fun mealTitle(meal: MealGroup): String =
    stringResource(
        when (meal) {
            MealGroup.BREAKFAST -> R.string.food_meal_breakfast
            MealGroup.LUNCH -> R.string.food_meal_lunch
            MealGroup.DINNER -> R.string.food_meal_dinner
            MealGroup.SNACKS -> R.string.food_meal_snacks
            MealGroup.CUSTOM -> R.string.food_meal_group
        }
    )

private data class DayWindow(val startMs: Long, val endMs: Long, val dayKey: String)

private fun dayBounds(dayOffset: Int): DayWindow {
    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    cal.add(Calendar.DAY_OF_YEAR, dayOffset)
    val start = cal.timeInMillis
    cal.add(Calendar.DAY_OF_YEAR, 1)
    val end = cal.timeInMillis
    val key = SimpleDateFormat("yyyy-MM-dd", Locale.UK).format(Date(start))
    return DayWindow(start, end, key)
}

private fun dayLabel(dayOffset: Int, todayLabel: String): String {
    if (dayOffset == 0) return todayLabel
    val bounds = dayBounds(dayOffset)
    return SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(bounds.startMs))
}

private fun shouldPromptWeighIn(): Boolean {
    val goal = NutritionPrefsStore.loadGoal()
    if (goal.remindersPaused || !goal.weighInNudgeEnabled) return false
    val last = NutritionPrefsStore.loadWeights().lastOrNull() ?: return true
    val days = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - last.epochMs)
    return days >= 3
}

private data class CardNums(
    val suggestedDailyKcal: Double,
    val waterLitres: Double,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double
)

private fun computeCard(core: OmniGartCore, profileId: String): CardNums? {
    val profile = core.profiles.get(profileId) ?: return null
    val store = NutritionPrefsStore
    val goal = store.loadGoal()
    val weights = store.loadWeights()
    val bodyFat = store.loadBodyFat().lastOrNull()?.bodyFatPercent
    val previous = store.lastTargetKcal()
    val result =
        NutritionFacade.computeForProfile(
            sexCode = if (profile.biologicalSex == BiologicalSex.MALE) "MALE" else "FEMALE",
            ageYears = core.ageYears(profile),
            heightCm = profile.heightCm,
            weightKg = weights.lastOrNull()?.massKg ?: profile.weightKg,
            bodyFatPercent = bodyFat ?: 0.0,
            hasBodyFat = bodyFat != null,
            goalKindCode = goal.kind,
            rateFractionPerWeek = goal.rateFractionPerWeek,
            targetWeightKg = goal.targetWeightKg ?: 0.0,
            hasTargetWeight = goal.targetWeightKg != null,
            modeCode = goal.mode,
            manualDailyKcal = goal.manualDailyKcal ?: 0.0,
            hasManualDaily = goal.manualDailyKcal != null,
            addExerciseCalories = goal.addExerciseCalories,
            exerciseKcalToday = 0.0,
            previousTargetKcal = previous ?: 0.0,
            hasPreviousTarget = previous != null,
            recentWeightKg = weights.map { it.massKg }.toDoubleArray(),
            recentWeightEpochMs = weights.map { it.epochMs }.toLongArray(),
            meanSteps = 8_000,
            dayStartMinutes = goal.dayStartMinutes,
            trainingHoursToday = 0.0
        )
    return CardNums(
        suggestedDailyKcal = result.suggestedDailyKcal ?: -1.0,
        waterLitres = result.waterLitres ?: -1.0,
        proteinG = result.proteinG ?: -1.0,
        carbsG = result.carbsG ?: -1.0,
        fatG = result.fatG ?: -1.0
    )
}
