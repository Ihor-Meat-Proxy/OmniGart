package com.omnigart.insights.sleep

import com.omnigart.insights.EvidencedInsight
import com.omnigart.insights.InsightEvidence
import kotlin.math.max
import kotlin.math.round

data class SleepBalanceInput(
    val ageYears: Int,
    /** Last night sleep hours. */
    val lastNightHours: Double,
    /** Recent nightly sleep hours, newest first (used for debt). */
    val recentNightsNewestFirst: List<Double> = emptyList(),
    /** Optional user preference overrides consensus midpoint. */
    val preferredNeedHours: Double? = null,
)

data class SleepNeedBand(
    val minHours: Double,
    val maxHours: Double,
    val midpointHours: Double,
)

data class SleepBalanceResult(
    val need: SleepNeedBand,
    val needHoursUsed: Double,
    val lastNightBalanceHours: Double,
    /** Sum of (need − sleep) over recent nights, floored at 0 per night. */
    val debtHours: Double,
    val claimIds: List<String>,
)

object SleepBalance {
    /**
     * Adult-focused consensus bands adapted from National Sleep Foundation duration guidance
     * (Hirshkowitz et al.). Teen/child bands included for completeness.
     */
    fun consensusNeed(ageYears: Int): SleepNeedBand = when {
        ageYears < 6 -> SleepNeedBand(10.0, 13.0, 11.5)
        ageYears < 14 -> SleepNeedBand(9.0, 11.0, 10.0)
        ageYears < 18 -> SleepNeedBand(8.0, 10.0, 9.0)
        ageYears < 65 -> SleepNeedBand(7.0, 9.0, 8.0)
        else -> SleepNeedBand(7.0, 8.0, 7.5)
    }

    fun compute(input: SleepBalanceInput): EvidencedInsight<SleepBalanceResult> {
        val need = consensusNeed(input.ageYears)
        val needUsed = input.preferredNeedHours?.coerceIn(need.minHours - 1.0, need.maxHours + 1.0)
            ?: need.midpointHours
        val lastBalance = round2(input.lastNightHours - needUsed)
        val nights = if (input.recentNightsNewestFirst.isEmpty()) {
            listOf(input.lastNightHours)
        } else {
            input.recentNightsNewestFirst
        }
        val debt = nights.sumOf { max(0.0, needUsed - it) }.let { round2(it) }

        return EvidencedInsight(
            value = SleepBalanceResult(
                need = need,
                needHoursUsed = needUsed,
                lastNightBalanceHours = lastBalance,
                debtHours = debt,
                claimIds = listOf(
                    InsightEvidence.SLEEP_BALANCE_NEED,
                    InsightEvidence.SLEEP_BALANCE_DEBT,
                ),
            ),
            claimId = InsightEvidence.SLEEP_BALANCE_DEBT,
        )
    }

    private fun round2(v: Double): Double = round(v * 100.0) / 100.0
}
