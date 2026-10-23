package com.omnigart.insights.reserve

import com.omnigart.insights.EvidencedInsight
import com.omnigart.insights.InsightEvidence
import com.omnigart.insights.load.LoadTargetResult
import com.omnigart.insights.readiness.DailyReadinessResult
import com.omnigart.insights.readiness.ReadinessBand
import kotlin.math.round

data class DayReserveInput(
    val readiness: DailyReadinessResult,
    val loadTarget: LoadTargetResult,
    /** Load already accumulated today (session-RPE×min). */
    val loadSoFarToday: Double = 0.0,
)

data class DayReserveResult(
    /** 0–100 remaining headroom estimate. */
    val reserveScore: Int,
    val suggestedRemainingLoadMin: Double,
    val suggestedRemainingLoadMax: Double,
    val note: String,
)

object DayReserve {
    fun compute(input: DayReserveInput): EvidencedInsight<DayReserveResult> {
        val readinessFactor = when (input.readiness.band) {
            ReadinessBand.LOW -> 0.55
            ReadinessBand.MODERATE -> 0.85
            ReadinessBand.HIGH -> 1.0
        }
        val ratioPenalty = when (val r = input.loadTarget.acuteChronicRatio) {
            null -> 1.0
            else -> when {
                r > 1.5 -> 0.6
                r > 1.3 -> 0.75
                else -> 1.0
            }
        }
        val roomMax = (input.loadTarget.recommendedDailyMax - input.loadSoFarToday).coerceAtLeast(0.0)
        val roomMin = (input.loadTarget.recommendedDailyMin - input.loadSoFarToday).coerceAtLeast(0.0)
        val scaledMax = roomMax * readinessFactor * ratioPenalty
        val scaledMin = (roomMin * readinessFactor * ratioPenalty).coerceAtMost(scaledMax)
        val reserveScore = round(
            (100.0 * readinessFactor * ratioPenalty * (if (input.loadTarget.recommendedDailyMax <= 0) {
                0.5
            } else {
                scaledMax / input.loadTarget.recommendedDailyMax
            })).coerceIn(0.0, 100.0),
        ).toInt()

        return EvidencedInsight(
            value = DayReserveResult(
                reserveScore = reserveScore,
                suggestedRemainingLoadMin = round1(scaledMin),
                suggestedRemainingLoadMax = round1(scaledMax),
                note = "Day reserve is a wellness headroom estimate from readiness and load context, not a prescription.",
            ),
            claimId = InsightEvidence.DAY_RESERVE,
        )
    }

    private fun round1(v: Double): Double = round(v * 10.0) / 10.0
}
