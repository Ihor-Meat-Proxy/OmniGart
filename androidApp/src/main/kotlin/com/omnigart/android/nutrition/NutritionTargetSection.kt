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
import com.omnigart.shared.OmniGartCore
import com.omnigart.shared.domain.BiologicalSex

@Composable
fun NutritionTargetSection(
    core: OmniGartCore,
    profileId: String,
    onOpenMeasurements: () -> Unit,
) {
    val profile = remember(profileId) { core.profiles.get(profileId) }
    var suggested by remember { mutableStateOf("—") }
    var waterText by remember { mutableStateOf("—") }
    var confidenceRes by remember { mutableStateOf(R.string.nutrition_confidence_rough) }
    var reasonResIds by remember { mutableStateOf(listOf<Int>()) }
    var floorWarn by remember { mutableStateOf(false) }
    var supportive by remember { mutableStateOf(false) }

    LaunchedEffect(profileId, profile) {
        val p = profile ?: return@LaunchedEffect
        val store = NutritionPrefsStore
        val goal = store.loadGoal()
        val weights = store.loadWeights()
        val bodyFat = store.loadBodyFat().lastOrNull()?.bodyFatPercent
        val previous = store.lastTargetKcal()
        val result =
            NutritionFacade.computeForProfile(
                sexCode = if (p.biologicalSex == BiologicalSex.MALE) "MALE" else "FEMALE",
                ageYears = core.ageYears(p),
                heightCm = p.heightCm,
                weightKg = weights.lastOrNull()?.massKg ?: p.weightKg,
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
                trainingHoursToday = 0.0,
            )
        val kcal = NutritionFacade.suggestedKcalOrNull(result)
        suggested =
            if (kcal > 0) {
                store.saveLastTargetKcal(kcal)
                kcal.toInt().toString()
            } else {
                "—"
            }
        waterText =
            result.waterLitres?.let { w ->
                String.format("%.1f L", w)
            } ?: "—"
        confidenceRes =
            when (result.confidence) {
                ConfidenceLevel.ROUGH -> R.string.nutrition_confidence_rough
                ConfidenceLevel.GOOD -> R.string.nutrition_confidence_good
                ConfidenceLevel.PERSONALISED -> R.string.nutrition_confidence_personalised
            }
        reasonResIds = result.changeReasons.mapNotNull { reasonStringRes(it.key) }
        floorWarn = result.floorWarning?.belowFloor == true
        supportive = result.lowIntakeSupportive
    }

    Column(
        modifier = Modifier.fillMaxWidth().testTag("nutrition_target_card"),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = stringResource(R.string.nutrition_target_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text =
                if (suggested == "—") {
                    suggested
                } else {
                    "$suggested ${stringResource(R.string.nutrition_kcal_unit)}"
                },
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.testTag("nutrition_target_kcal"),
        )
        Text(
            text = "${stringResource(R.string.nutrition_water_goal)}: $waterText",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.testTag("nutrition_water_goal"),
        )
        Text(
            text = stringResource(R.string.nutrition_target_disclaimer),
            color = OmniGartColors.colorTextLightSecondary,
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            text =
                "${stringResource(R.string.nutrition_confidence_title)}: " +
                    stringResource(confidenceRes),
            modifier = Modifier.testTag("nutrition_confidence"),
        )
        if (reasonResIds.isNotEmpty()) {
            Text(
                text = stringResource(R.string.nutrition_reason_title),
                style = MaterialTheme.typography.titleSmall,
            )
            reasonResIds.forEach { resId ->
                Text(
                    text = stringResource(resId),
                    style = MaterialTheme.typography.bodySmall,
                    color = OmniGartColors.colorTextLightSecondary,
                )
            }
        }
        if (floorWarn) {
            Text(
                text = stringResource(R.string.nutrition_floor_warning),
                color = Color.Red,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.testTag("nutrition_floor_warning"),
            )
        }
        if (supportive) {
            Text(
                text = stringResource(R.string.nutrition_low_intake_title),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = stringResource(R.string.nutrition_low_intake_body),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        TextButton(
            onClick = onOpenMeasurements,
            modifier = Modifier.testTag("nutrition_open_measurements"),
        ) {
            Text(stringResource(R.string.nutrition_open_measurements))
        }
    }
}

private fun reasonStringRes(key: String): Int? =
    when (key) {
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
