package com.omnigart.insights.load

import com.omnigart.insights.EvidencedInsight
import com.omnigart.insights.InsightEvidence
import com.omnigart.insights.stats.BaselineStats
import kotlin.math.round

data class SessionLoad(
    /** Foster session-RPE (typically 0–10). */
    val sessionRpe: Double,
    /** Duration in minutes. */
    val durationMinutes: Double,
) {
    /** Internal load = sRPE × duration (Foster). */
    val load: Double get() = sessionRpe * durationMinutes
}

data class LoadTargetInput(
    /** Daily loads newest-first (sum of session loads that day); include 0 for rest days. */
    val dailyLoadsNewestFirst: List<Double>,
    val acuteDays: Int = 7,
    val chronicDays: Int = 28,
)

data class LoadTargetResult(
    val acuteMean: Double,
    val chronicMean: Double,
    /** Acute:chronic ratio; null when chronic mean is ~0. */
    val acuteChronicRatio: Double?,
    val recommendedDailyMin: Double,
    val recommendedDailyMax: Double,
    val cautionNote: String,
)

object LoadTarget {
    private const val CAUTION: String =
        "Acute:chronic ratios are soft wellness guides only; they are not a universal injury predictor."

    fun compute(input: LoadTargetInput): EvidencedInsight<LoadTargetResult> {
        require(input.acuteDays > 0 && input.chronicDays > input.acuteDays) {
            "chronicDays must exceed acuteDays"
        }
        val acuteWindow = input.dailyLoadsNewestFirst.take(input.acuteDays)
        val chronicWindow = input.dailyLoadsNewestFirst.take(input.chronicDays)
        require(acuteWindow.isNotEmpty()) { "need at least one daily load point" }

        val acuteMean = BaselineStats.mean(acuteWindow)
        val chronicMean = if (chronicWindow.isEmpty()) acuteMean else BaselineStats.mean(chronicWindow)
        val ratio = if (chronicMean < 1e-9) null else acuteMean / chronicMean

        // Soft daily range around chronic mean (±20%), nudged down if acute is already elevated.
        val span = (chronicMean * 0.20).coerceAtLeast(10.0)
        var min = (chronicMean - span).coerceAtLeast(0.0)
        var max = chronicMean + span
        if (ratio != null && ratio > 1.3) {
            max = chronicMean
            min = (chronicMean * 0.5).coerceAtLeast(0.0)
        }

        return EvidencedInsight(
            value = LoadTargetResult(
                acuteMean = round2(acuteMean),
                chronicMean = round2(chronicMean),
                acuteChronicRatio = ratio?.let { round2(it) },
                recommendedDailyMin = round2(min),
                recommendedDailyMax = round2(max),
                cautionNote = CAUTION,
            ),
            claimId = InsightEvidence.LOAD_TARGET,
        )
    }

    fun sessionLoad(sessionRpe: Double, durationMinutes: Double): Double =
        SessionLoad(sessionRpe, durationMinutes).load

    private fun round2(v: Double): Double = round(v * 100.0) / 100.0
}
