package com.omnigart.nutrition.tdee

import com.omnigart.nutrition.DayLogStatus
import com.omnigart.nutrition.IntakeDay
import com.omnigart.nutrition.WeightPoint
import kotlin.math.abs

/**
 * Adaptive TDEE estimators.
 *
 * 1) Classic EWMA weight-slope form (alpha 0.1, ≥14 days / ≥10 weigh-ins / ≥80% logged).
 * 2) Energy-balance expenditure over a 21-day window: intake minus stored-energy change,
 *    with a variable fat/lean kcal share by rate of change.
 */
object AdaptiveTdee {

    const val EWMA_ALPHA = 0.1
    const val KCAL_PER_KG_CLASSIC = 7700.0
    const val MIN_DAYS = 14
    const val MIN_WEIGH_INS = 10
    const val MIN_LOG_FRACTION = 0.80
    const val BLEND_FULL_DAYS = 28.0
    const val MAX_WEEKLY_TARGET_DELTA_KCAL = 100.0

    const val ENERGY_BALANCE_WINDOW_DAYS = 21
    const val FAT_KCAL_PER_KG = 7700.0
    const val LEAN_KCAL_PER_KG = 1800.0

    data class EwmaResult(
        val adaptiveTdeeKcal: Double,
        val weightSlopeKgPerDay: Double,
        val meanIntakeKcal: Double,
        val blendWeight: Double,
        val eligible: Boolean,
    )

    data class EnergyBalanceResult(
        val expenditureKcal: Double,
        val meanIntakeKcal: Double,
        val storedEnergyKcalPerDay: Double,
        val fatShare: Double,
        val rateKgPerWeek: Double,
        val eligible: Boolean,
    )

    fun ewmaAdaptive(
        intakeDays: List<IntakeDay>,
        weights: List<WeightPoint>,
        windowDays: Int = MIN_DAYS,
    ): EwmaResult? {
        val complete = completeIntakeDays(intakeDays)
        if (complete.size < (windowDays * MIN_LOG_FRACTION).toInt()) {
            return EwmaResult(0.0, 0.0, 0.0, 0.0, eligible = false)
        }
        if (weights.size < MIN_WEIGH_INS) {
            return EwmaResult(0.0, 0.0, 0.0, 0.0, eligible = false)
        }
        val spanDays = daySpan(weights)
        if (spanDays < windowDays) {
            return EwmaResult(0.0, 0.0, 0.0, 0.0, eligible = false)
        }

        val meanIntake = complete.mapNotNull { it.energyKcal }.average()
        val slope = ewmaWeightSlopeKgPerDay(weights) ?: return null
        val adaptive = meanIntake - slope * KCAL_PER_KG_CLASSIC
        val blend = (spanDays / BLEND_FULL_DAYS).coerceIn(0.0, 1.0)
        return EwmaResult(
            adaptiveTdeeKcal = adaptive,
            weightSlopeKgPerDay = slope,
            meanIntakeKcal = meanIntake,
            blendWeight = blend,
            eligible = true,
        )
    }

    /**
     * Energy-balance TDEE over [ENERGY_BALANCE_WINDOW_DAYS]:
     * expenditure ≈ mean intake − Δstored / days.
     * Stored energy uses a fat/lean mix that depends on the weekly rate of change
     * (slower loss or faster gain → higher fat share).
     */
    fun energyBalance21(
        intakeDays: List<IntakeDay>,
        weights: List<WeightPoint>,
        windowDays: Int = ENERGY_BALANCE_WINDOW_DAYS,
    ): EnergyBalanceResult? {
        val complete = completeIntakeDays(intakeDays)
        if (complete.isEmpty() || weights.size < 2) {
            return EnergyBalanceResult(0.0, 0.0, 0.0, 0.0, 0.0, eligible = false)
        }
        val ordered = weights.sortedBy { it.epochMs }
        val spanMs = ordered.last().epochMs - ordered.first().epochMs
        val spanDays = spanMs / 86_400_000.0
        if (spanDays < windowDays * 0.75) {
            return EnergyBalanceResult(0.0, 0.0, 0.0, 0.0, 0.0, eligible = false)
        }

        val meanIntake = complete.mapNotNull { it.energyKcal }.average()
        val deltaKg = ordered.last().massKg - ordered.first().massKg
        val rateKgPerWeek = deltaKg / spanDays * 7.0
        val fatShare = fatShareForRate(rateKgPerWeek)
        val kcalPerKg = fatShare * FAT_KCAL_PER_KG + (1.0 - fatShare) * LEAN_KCAL_PER_KG
        val storedPerDay = (deltaKg * kcalPerKg) / spanDays
        val expenditure = meanIntake - storedPerDay
        return EnergyBalanceResult(
            expenditureKcal = expenditure,
            meanIntakeKcal = meanIntake,
            storedEnergyKcalPerDay = storedPerDay,
            fatShare = fatShare,
            rateKgPerWeek = rateKgPerWeek,
            eligible = expenditure > 0.0,
        )
    }

    /**
     * Fat share of mass change: slower losses and faster gains bias toward fat tissue energy density.
     * Loss ≥1%/wk equivalent (~fast) → lower fat share; near-zero loss → higher fat share.
     */
    fun fatShareForRate(rateKgPerWeek: Double): Double =
        when {
            rateKgPerWeek <= -1.0 -> 0.65
            rateKgPerWeek <= -0.5 -> 0.75
            rateKgPerWeek < 0.0 -> 0.85
            rateKgPerWeek < 0.25 -> 0.70
            rateKgPerWeek < 0.5 -> 0.80
            else -> 0.90
        }

    fun blendWithBaseline(
        baselineTdee: Double,
        adaptive: Double,
        blendWeight: Double,
    ): Double {
        val w = blendWeight.coerceIn(0.0, 1.0)
        return baselineTdee * (1.0 - w) + adaptive * w
    }

    /** Cap absolute weekly change of the published target (design choice: 100 kcal/week). */
    fun applyWeeklyCap(
        previousTargetKcal: Double?,
        proposedKcal: Double,
        maxDelta: Double = MAX_WEEKLY_TARGET_DELTA_KCAL,
    ): Double {
        if (previousTargetKcal == null) return proposedKcal
        val delta = proposedKcal - previousTargetKcal
        return if (abs(delta) <= maxDelta) {
            proposedKcal
        } else {
            previousTargetKcal + maxDelta * if (delta >= 0) 1.0 else -1.0
        }
    }

    fun ewmaWeightSlopeKgPerDay(weights: List<WeightPoint>, alpha: Double = EWMA_ALPHA): Double? {
        if (weights.size < 2) return null
        val ordered = weights.sortedBy { it.epochMs }
        var prevSmooth = ordered.first().massKg
        var prevT = ordered.first().epochMs
        val slopes = mutableListOf<Double>()
        for (i in 1 until ordered.size) {
            val p = ordered[i]
            val smooth = alpha * p.massKg + (1.0 - alpha) * prevSmooth
            val days = (p.epochMs - prevT) / 86_400_000.0
            if (days > 0.0) {
                slopes += (smooth - prevSmooth) / days
            }
            prevSmooth = smooth
            prevT = p.epochMs
        }
        if (slopes.isEmpty()) return null
        // EWMA of successive slopes
        var s = slopes.first()
        for (i in 1 until slopes.size) {
            s = alpha * slopes[i] + (1.0 - alpha) * s
        }
        return s
    }

    fun completeIntakeDays(days: List<IntakeDay>): List<IntakeDay> =
        days.filter {
            it.status == DayLogStatus.COMPLETE &&
                it.energyKcal != null &&
                it.energyKcal >= 0.0
        }

    private fun daySpan(weights: List<WeightPoint>): Double {
        if (weights.size < 2) return 0.0
        val ordered = weights.sortedBy { it.epochMs }
        return (ordered.last().epochMs - ordered.first().epochMs) / 86_400_000.0
    }
}
