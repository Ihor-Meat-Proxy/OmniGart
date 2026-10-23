package com.omnigart.android.nutrition

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.omnigart.android.R
import com.omnigart.design.theme.OmniGartColors
import com.omnigart.nutrition.ConfidenceLevel
import com.omnigart.nutrition.NutritionFacade
import com.omnigart.nutrition.NutritionTargetResult
import com.omnigart.shared.OmniGartCore
import com.omnigart.shared.domain.BiologicalSex
import com.omnigart.shared.domain.UserProfile
import java.util.Locale

private data class TargetCardState(
    val suggested: String = "—",
    val waterText: String = "—",
    val confidenceRes: Int = R.string.nutrition_confidence_rough,
    val reasonResIds: List<Int> = emptyList(),
    val floorWarn: Boolean = false,
    val supportive: Boolean = false
)

@Composable
fun NutritionTargetSection(core: OmniGartCore, profileId: String, onOpenMeasurements: () -> Unit) {
    val profile = remember(profileId) { core.profiles.get(profileId) }
    var state by remember { mutableStateOf(TargetCardState()) }

    LaunchedEffect(profileId, profile) {
        val p = profile ?: return@LaunchedEffect
        state = buildTargetCardState(core, p)
    }

    TargetCardContent(state = state, onOpenMeasurements = onOpenMeasurements)
}

@Composable
private fun TargetCardContent(state: TargetCardState, onOpenMeasurements: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().testTag("nutrition_target_card"),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(stringResource(R.string.nutrition_target_title), style = MaterialTheme.typography.titleMedium)
        val energy =
            if (state.suggested == "—") {
                state.suggested
            } else {
                "${state.suggested} ${stringResource(R.string.nutrition_kcal_unit)}"
            }
        Text(
            text = energy,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.testTag("nutrition_target_kcal")
        )
        Text(
            text = "${stringResource(R.string.nutrition_water_goal)}: ${state.waterText}",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.testTag("nutrition_water_goal")
        )
        Text(
            text = stringResource(R.string.nutrition_target_disclaimer),
            color = OmniGartColors.colorTextLightSecondary,
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text =
            "${stringResource(R.string.nutrition_confidence_title)}: " +
                stringResource(state.confidenceRes),
            modifier = Modifier.testTag("nutrition_confidence")
        )
        TargetReasons(state.reasonResIds)
        if (state.floorWarn) {
            Text(
                text = stringResource(R.string.nutrition_floor_warning),
                color = Color.Red,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.testTag("nutrition_floor_warning")
            )
        }
        if (state.supportive) {
            Text(stringResource(R.string.nutrition_low_intake_title), style = MaterialTheme.typography.titleSmall)
            Text(stringResource(R.string.nutrition_low_intake_body), style = MaterialTheme.typography.bodySmall)
        }
        TextButton(
            onClick = onOpenMeasurements,
            modifier = Modifier.testTag("nutrition_open_measurements")
        ) {
            Text(stringResource(R.string.nutrition_open_measurements))
        }
    }
}

@Composable
private fun TargetReasons(reasonResIds: List<Int>) {
    if (reasonResIds.isEmpty()) return
    Text(stringResource(R.string.nutrition_reason_title), style = MaterialTheme.typography.titleSmall)
    reasonResIds.forEach { resId ->
        Text(
            text = stringResource(resId),
            style = MaterialTheme.typography.bodySmall,
            color = OmniGartColors.colorTextLightSecondary
        )
    }
}

private fun buildTargetCardState(core: OmniGartCore, profile: UserProfile): TargetCardState {
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
    return mapResult(store, result)
}

private fun mapResult(store: NutritionPrefsStore, result: NutritionTargetResult): TargetCardState {
    val kcal = NutritionFacade.suggestedKcalOrNull(result)
    val suggested =
        if (kcal > 0) {
            store.saveLastTargetKcal(kcal)
            kcal.toInt().toString()
        } else {
            "—"
        }
    val water =
        result.waterLitres?.let { String.format(Locale.US, "%.1f L", it) } ?: "—"
    val confidence =
        when (result.confidence) {
            ConfidenceLevel.ROUGH -> R.string.nutrition_confidence_rough
            ConfidenceLevel.GOOD -> R.string.nutrition_confidence_good
            ConfidenceLevel.PERSONALISED -> R.string.nutrition_confidence_personalised
        }
    return TargetCardState(
        suggested = suggested,
        waterText = water,
        confidenceRes = confidence,
        reasonResIds = result.changeReasons.mapNotNull { reasonStringRes(it.key) },
        floorWarn = result.floorWarning?.belowFloor == true,
        supportive = result.lowIntakeSupportive
    )
}

private fun reasonStringRes(key: String): Int? = when (key) {
    "nutrition_reason_no_weight" -> R.string.nutrition_reason_no_weight
    "nutrition_reason_rough_bmr" -> R.string.nutrition_reason_rough_bmr
    "nutrition_reason_baseline" -> R.string.nutrition_reason_baseline
    "nutrition_reason_adaptive_ewma" -> R.string.nutrition_reason_adaptive_ewma
    "nutrition_reason_energy_balance" -> R.string.nutrition_reason_energy_balance
    "nutrition_reason_paused_insufficient" -> R.string.nutrition_reason_paused_insufficient
    "nutrition_reason_logging_break" -> R.string.nutrition_reason_logging_break
    "nutrition_reason_floor" -> R.string.nutrition_reason_floor
    "nutrition_reason_auto_raise" -> R.string.nutrition_reason_auto_raise
    "nutrition_reason_exercise_credit" -> R.string.nutrition_reason_exercise_credit
    else -> null
}
