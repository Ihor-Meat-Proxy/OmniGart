package com.omnigart.android.food

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
import com.omnigart.android.R
import com.omnigart.android.nutrition.BodyMeasurementsSection
import com.omnigart.android.nutrition.NutritionGoalSettingsSection
import com.omnigart.design.theme.OmniGartColors
import com.omnigart.design.theme.OmniGartSpacing
import com.omnigart.shared.OmniGartCore
import com.omnigart.shared.compliance.FeatureFlags
import com.omnigart.shared.food.models.MealGroup
import com.omnigart.shared.food.nutrients.BiologicalSexBand
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
private fun FoodMorningReviewBlock(
    core: OmniGartCore,
    profileId: String,
    reviewSummary: String?,
    onSummary: (String) -> Unit
) {
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
