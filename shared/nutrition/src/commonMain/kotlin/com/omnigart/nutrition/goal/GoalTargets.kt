package com.omnigart.nutrition.goal

import com.omnigart.nutrition.NutritionGoalKind
import com.omnigart.nutrition.NutritionGoalSettings
import kotlin.math.abs

/**
 * Goal rate helpers and near-target taper.
 *
 * Defaults: loss 0.5%/wk (max 1%/wk); gain 0.25–0.5%/wk.
 * Weekly-rate energy equivalent: 1100 kcal/day per kg/week.
 * Taper: linear from full deficit/surplus at 5 kg away down to 0 at 1 kg away.
 */
object GoalTargets {

    const val DEFAULT_LOSS_FRACTION = 0.005
    const val MAX_LOSS_FRACTION = 0.01
    const val MIN_GAIN_FRACTION = 0.0025
    const val MAX_GAIN_FRACTION = 0.005
    const val KCAL_PER_DAY_PER_KG_PER_WEEK = 1100.0
    const val TAPER_FULL_KG = 5.0
    const val TAPER_ZERO_KG = 1.0
    const val AUTO_RAISE_LOSS_FRACTION = 0.01
    const val AUTO_RAISE_WEEKS = 2

    fun clampRate(kind: NutritionGoalKind, rateFractionPerWeek: Double): Double =
        when (kind) {
            NutritionGoalKind.MAINTAIN -> 0.0
            NutritionGoalKind.LOSE -> rateFractionPerWeek.coerceIn(0.0, MAX_LOSS_FRACTION)
            NutritionGoalKind.GAIN -> rateFractionPerWeek.coerceIn(MIN_GAIN_FRACTION, MAX_GAIN_FRACTION)
        }

    fun defaultRate(kind: NutritionGoalKind): Double =
        when (kind) {
            NutritionGoalKind.MAINTAIN -> 0.0
            NutritionGoalKind.LOSE -> DEFAULT_LOSS_FRACTION
            NutritionGoalKind.GAIN -> MIN_GAIN_FRACTION
        }

    /**
     * Daily energy delta from TDEE for the chosen rate (negative = deficit).
     * Uses 1100 kcal/day per kg/week × |rate| × weight.
     */
    fun dailyDeltaKcal(weightKg: Double, kind: NutritionGoalKind, rateFractionPerWeek: Double): Double {
        val rate = clampRate(kind, rateFractionPerWeek)
        if (kind == NutritionGoalKind.MAINTAIN || rate == 0.0) return 0.0
        val kgPerWeek = abs(rate) * weightKg
        val magnitude = kgPerWeek * KCAL_PER_DAY_PER_KG_PER_WEEK
        return when (kind) {
            NutritionGoalKind.LOSE -> -magnitude
            NutritionGoalKind.GAIN -> magnitude
            NutritionGoalKind.MAINTAIN -> 0.0
        }
    }

    /**
     * Linear taper of the goal delta: full effect when |current − target| ≥ 5 kg,
     * zero when within 1 kg, linear in between.
     */
    fun taperFactor(currentWeightKg: Double, targetWeightKg: Double?): Double {
        if (targetWeightKg == null) return 1.0
        val distance = abs(currentWeightKg - targetWeightKg)
        if (distance >= TAPER_FULL_KG) return 1.0
        if (distance <= TAPER_ZERO_KG) return 0.0
        return (distance - TAPER_ZERO_KG) / (TAPER_FULL_KG - TAPER_ZERO_KG)
    }

    fun taperedDailyDelta(
        weightKg: Double,
        settings: NutritionGoalSettings,
    ): Double {
        val raw = dailyDeltaKcal(weightKg, settings.kind, settings.rateFractionPerWeek)
        return raw * taperFactor(weightKg, settings.targetWeightKg)
    }

    /**
     * If loss exceeds 1%/wk for [AUTO_RAISE_WEEKS] consecutive weeks, suggest raising the target.
     */
    fun shouldAutoRaiseTarget(
        weeklyLossFractions: List<Double>,
        threshold: Double = AUTO_RAISE_LOSS_FRACTION,
        weeks: Int = AUTO_RAISE_WEEKS,
    ): Boolean {
        if (weeklyLossFractions.size < weeks) return false
        return weeklyLossFractions.takeLast(weeks).all { it > threshold }
    }

    fun applyGoalToTdee(tdeeKcal: Double, weightKg: Double, settings: NutritionGoalSettings): Double =
        tdeeKcal + taperedDailyDelta(weightKg, settings)
}
