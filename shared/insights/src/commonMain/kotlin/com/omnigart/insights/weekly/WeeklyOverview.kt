package com.omnigart.insights.weekly

import com.omnigart.insights.EvidencedInsight
import com.omnigart.insights.InsightEvidence
import com.omnigart.insights.stats.BaselineStats
import kotlin.math.round

data class DailySnapshot(
    val readinessScore: Int?,
    val trainingLoad: Double,
    val sleepHours: Double,
)

data class WeeklyOverviewInput(
    /** Exactly 7 days preferred; fewer allowed. Newest last or first — we accept either via list order newest-first. */
    val daysNewestFirst: List<DailySnapshot>,
)

data class WeeklyOverviewResult(
    val dayCount: Int,
    val meanReadiness: Double?,
    val totalLoad: Double,
    val meanDailyLoad: Double,
    val meanSleepHours: Double,
    val trainingDays: Int,
)

object WeeklyOverview {
    fun compute(input: WeeklyOverviewInput): EvidencedInsight<WeeklyOverviewResult> {
        require(input.daysNewestFirst.isNotEmpty()) { "need at least one day" }
        val days = input.daysNewestFirst.take(7)
        val readiness = days.mapNotNull { it.readinessScore?.toDouble() }
        val loads = days.map { it.trainingLoad }
        val sleep = days.map { it.sleepHours }
        return EvidencedInsight(
            value = WeeklyOverviewResult(
                dayCount = days.size,
                meanReadiness = readiness.takeIf { it.isNotEmpty() }?.let { round1(BaselineStats.mean(it)) },
                totalLoad = round1(loads.sum()),
                meanDailyLoad = round1(BaselineStats.mean(loads)),
                meanSleepHours = round1(BaselineStats.mean(sleep)),
                trainingDays = loads.count { it > 0.0 },
            ),
            claimId = InsightEvidence.WEEKLY_OVERVIEW,
        )
    }

    private fun round1(v: Double): Double = round(v * 10.0) / 10.0
}
