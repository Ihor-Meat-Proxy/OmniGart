package com.omnigart.android.food

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.omnigart.android.R
import com.omnigart.android.nutrition.NutritionPrefsStore
import com.omnigart.design.theme.OmniGartColors
import com.omnigart.nutrition.NutritionFacade
import com.omnigart.shared.OmniGartCore
import com.omnigart.shared.domain.BiologicalSex
import com.omnigart.shared.food.models.MealGroup
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

internal data class FoodDayWindow(val startMs: Long, val endMs: Long, val dayKey: String)

internal fun foodDayBounds(dayOffset: Int): FoodDayWindow {
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
    return FoodDayWindow(start, end, key)
}

@Composable
internal fun foodMealTitle(meal: MealGroup): String = stringResource(
    when (meal) {
        MealGroup.BREAKFAST -> R.string.food_meal_breakfast
        MealGroup.LUNCH -> R.string.food_meal_lunch
        MealGroup.DINNER -> R.string.food_meal_dinner
        MealGroup.SNACKS -> R.string.food_meal_snacks
        MealGroup.CUSTOM -> R.string.food_meal_group
    }
)

@Composable
internal fun FoodDayNav(dayOffset: Int, onPrev: () -> Unit, onNext: () -> Unit) {
    val todayLabel = stringResource(R.string.food_day_today)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        TextButton(onClick = onPrev, modifier = Modifier.testTag("food_day_previous")) {
            Text(stringResource(R.string.food_day_previous))
        }
        Text(
            text = foodDayLabel(dayOffset, todayLabel),
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
internal fun FoodWeighInPrompt(tick: Int, onOpenMeasurements: () -> Unit) {
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
internal fun FoodDailySummaryBlock(
    core: OmniGartCore,
    profileId: String,
    dayOffset: Int,
    tick: Int,
    onEditGoal: () -> Unit,
    onWater: () -> Unit
) {
    val bounds = remember(dayOffset, tick) { foodDayBounds(dayOffset) }
    val diaryTotals =
        remember(dayOffset, tick, profileId) {
            core.foodLogging.totalsForDayEpoch(profileId, bounds.startMs, bounds.endMs)
        }
    val legacy =
        remember(dayOffset, tick, profileId) {
            core.foodDayTotalsForDayOffset(profileId, dayOffset)
        }
    val eaten = if (diaryTotals.entryCount > 0) diaryTotals.energyKcal else legacy.energyKcal
    val protein = if (diaryTotals.entryCount > 0) diaryTotals.proteinG else legacy.proteinG
    val carbs = if (diaryTotals.entryCount > 0) diaryTotals.carbsG else legacy.carbsG
    val fat = if (diaryTotals.entryCount > 0) diaryTotals.fatG else legacy.fatG
    val card = remember(tick, profileId) { computeFoodCard(core, profileId) }
    val target = card?.suggestedDailyKcal?.takeIf { it > 0 } ?: 0.0
    val remaining = if (target > 0) target - eaten else 0.0
    val waterMl = NutritionPrefsStore.waterMl(bounds.dayKey)
    val waterGoal = card?.waterLitres?.takeIf { it > 0 } ?: 0.0
    val kcalUnit = stringResource(R.string.nutrition_kcal_unit)

    Column(modifier = Modifier.testTag("food_totals")) {
        Text(stringResource(R.string.food_summary_title), style = MaterialTheme.typography.titleSmall)
        Text("${stringResource(R.string.food_eaten)}: ${eaten.toInt()} $kcalUnit")
        Text(
            text =
            "${stringResource(R.string.food_target)}: " +
                if (target > 0) "${target.toInt()} $kcalUnit" else "—"
        )
        Text(
            text =
            "${stringResource(R.string.food_remaining)}: " +
                if (target > 0) "${remaining.toInt()} $kcalUnit" else "—",
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
internal fun FoodMealBlock(core: OmniGartCore, profileId: String, meal: MealGroup, dayOffset: Int, tick: Int, onAdd: () -> Unit) {
    val bounds = remember(dayOffset, tick) { foodDayBounds(dayOffset) }
    val entries =
        remember(dayOffset, tick, profileId, meal) {
            core.foodLogging.entriesForDayEpoch(profileId, bounds.startMs, bounds.endMs)
                .filter { it.mealGroup == meal }
        }
    Text(text = foodMealTitle(meal), style = MaterialTheme.typography.titleSmall)
    if (entries.isEmpty()) {
        Text(
            text = stringResource(R.string.food_empty),
            color = OmniGartColors.colorTextLightSecondary,
            style = MaterialTheme.typography.bodySmall
        )
    } else {
        entries.forEach { entry ->
            Text(
                text = "${entry.name} · ${entry.energyKcal.toInt()} kcal",
                modifier = Modifier.testTag("food_entry_${entry.id}")
            )
        }
    }
    val tag = if (meal == MealGroup.BREAKFAST) "food_add" else "food_add_${meal.name.lowercase()}"
    Button(onClick = onAdd, modifier = Modifier.testTag(tag)) {
        Text(stringResource(R.string.food_add_meal))
    }
}

private fun foodDayLabel(dayOffset: Int, todayLabel: String): String {
    if (dayOffset == 0) return todayLabel
    val bounds = foodDayBounds(dayOffset)
    return SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(bounds.startMs))
}

private fun shouldPromptWeighIn(): Boolean {
    val goal = NutritionPrefsStore.loadGoal()
    if (goal.remindersPaused || !goal.weighInNudgeEnabled) return false
    val last = NutritionPrefsStore.loadWeights().lastOrNull() ?: return true
    val days = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - last.epochMs)
    return days >= 3
}

private data class FoodCardNums(
    val suggestedDailyKcal: Double,
    val waterLitres: Double,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double
)

private fun computeFoodCard(core: OmniGartCore, profileId: String): FoodCardNums? {
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
    return FoodCardNums(
        suggestedDailyKcal = result.suggestedDailyKcal ?: -1.0,
        waterLitres = result.waterLitres ?: -1.0,
        proteinG = result.proteinG ?: -1.0,
        carbsG = result.carbsG ?: -1.0,
        fatG = result.fatG ?: -1.0
    )
}
