package com.omnigart.insights.habits

import com.omnigart.insights.EvidencedInsight
import com.omnigart.insights.InsightEvidence
import com.omnigart.insights.stats.BaselineStats
import kotlin.math.sqrt

data class HabitPair(
    /** Metric on habit-present days (e.g. readiness, sleep hours). */
    val withHabit: Double,
    /** Matched metric on habit-absent comparison days. */
    val withoutHabit: Double,
)

data class HabitEffectInput(
    val pairs: List<HabitPair>,
    val habitLabel: String,
    val metricLabel: String,
)

enum class HabitConfidence {
    VERY_LOW,
    LOW,
    MODERATE,
}

data class HabitEffectResult(
    val habitLabel: String,
    val metricLabel: String,
    val nPairs: Int,
    val meanDifference: Double,
    /** Approximate standard error of the mean paired difference. */
    val standardError: Double,
    val confidence: HabitConfidence,
    val caveat: String,
)

object HabitEffectAnalysis {
    private const val CAVEAT: String =
        "Exploratory within-person contrast only. Small or noisy samples stay low-confidence; not causal proof."

    fun compute(input: HabitEffectInput): EvidencedInsight<HabitEffectResult> {
        require(input.pairs.isNotEmpty()) { "need at least one paired observation" }
        val diffs = input.pairs.map { it.withHabit - it.withoutHabit }
        val meanDiff = BaselineStats.mean(diffs)
        val sd = BaselineStats.sampleSd(diffs)
        val se = if (diffs.size < 2) Double.POSITIVE_INFINITY else sd / sqrt(diffs.size.toDouble())
        val confidence = when {
            diffs.size < 5 -> HabitConfidence.VERY_LOW
            diffs.size < 14 || se > kotlin.math.abs(meanDiff) -> HabitConfidence.LOW
            else -> HabitConfidence.MODERATE
        }
        return EvidencedInsight(
            value = HabitEffectResult(
                habitLabel = input.habitLabel,
                metricLabel = input.metricLabel,
                nPairs = diffs.size,
                meanDifference = meanDiff,
                standardError = if (se.isFinite()) se else Double.NaN,
                confidence = confidence,
                caveat = CAVEAT,
            ),
            claimId = InsightEvidence.HABIT_EFFECT,
        )
    }
}
