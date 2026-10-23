package com.omnigart.insights.notes

import com.omnigart.insights.EvidencedInsight
import com.omnigart.insights.InsightEvidence
import com.omnigart.insights.stats.BaselineStats
import kotlin.math.abs

data class MetricPoint(
    val id: String,
    val label: String,
    val value: Double,
    /** History newest-first for the same metric. */
    val historyNewestFirst: List<Double>,
    val windowDays: Int = 14,
    /** Absolute z threshold beyond which a note is raised. */
    val zThreshold: Double = 1.5,
)

enum class DeviationDirection {
    ABOVE,
    BELOW,
}

data class OutOfRangeNote(
    val metricId: String,
    val label: String,
    val value: Double,
    val baselineMean: Double,
    val zScore: Double,
    val direction: DeviationDirection,
)

data class OutOfRangeResult(
    val notes: List<OutOfRangeNote>,
    val evaluatedCount: Int,
)

object OutOfRangeNotes {
    fun compute(metrics: List<MetricPoint>): EvidencedInsight<OutOfRangeResult> {
        val notes = mutableListOf<OutOfRangeNote>()
        var evaluated = 0
        for (m in metrics) {
            val baseline = BaselineStats.rollingBaseline(m.historyNewestFirst, m.windowDays) ?: continue
            if (baseline.n < 3 || baseline.sd < 1e-9) continue
            evaluated += 1
            val z = BaselineStats.zScore(m.value, baseline)
            if (abs(z) >= m.zThreshold) {
                notes += OutOfRangeNote(
                    metricId = m.id,
                    label = m.label,
                    value = m.value,
                    baselineMean = baseline.mean,
                    zScore = z,
                    direction = if (z > 0) DeviationDirection.ABOVE else DeviationDirection.BELOW,
                )
            }
        }
        return EvidencedInsight(
            value = OutOfRangeResult(notes = notes, evaluatedCount = evaluated),
            claimId = InsightEvidence.OUT_OF_RANGE,
        )
    }
}
