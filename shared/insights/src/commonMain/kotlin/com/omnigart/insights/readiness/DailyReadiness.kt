package com.omnigart.insights.readiness

import com.omnigart.insights.EvidencedInsight
import com.omnigart.insights.InsightEvidence
import com.omnigart.insights.stats.BaselineStats
import com.omnigart.insights.stats.RollingBaseline
import kotlin.math.ln
import kotlin.math.round

enum class ReadinessBand {
    LOW,
    MODERATE,
    HIGH,
}

data class DailyReadinessInput(
    /** Morning rMSSD in milliseconds (must be > 0). */
    val rmssdMs: Double,
    /** Prior morning rMSSD values, newest first (same units). */
    val rmssdHistoryNewestFirst: List<Double>,
    /** Resting heart rate in bpm. */
    val restingHrBpm: Double,
    /** Prior resting HR values, newest first. */
    val restingHrHistoryNewestFirst: List<Double>,
    /** Last night's sleep hours. */
    val sleepHours: Double,
    /** Preferred or consensus sleep need hours. */
    val sleepNeedHours: Double,
)

data class DailyReadinessResult(
    /** 0–100 wellness readiness score. */
    val score: Int,
    val band: ReadinessBand,
    val lnRmssd: Double,
    val baseline7: RollingBaseline?,
    val baseline60: RollingBaseline?,
    val hrvZ7: Double?,
    val hrvZ60: Double?,
    val rhrZ7: Double?,
    val sleepRatio: Double,
)

object DailyReadiness {
    private const val WINDOW_7 = 7
    private const val WINDOW_60 = 60

    fun lnRmssd(rmssdMs: Double): Double {
        require(rmssdMs > 0.0) { "rMSSD must be positive" }
        return ln(rmssdMs)
    }

    fun compute(input: DailyReadinessInput): EvidencedInsight<DailyReadinessResult> {
        val lnToday = lnRmssd(input.rmssdMs)
        val lnHistory = input.rmssdHistoryNewestFirst.filter { it > 0 }.map { ln(it) }
        val b7 = BaselineStats.rollingBaseline(lnHistory, WINDOW_7)
        val b60 = BaselineStats.rollingBaseline(lnHistory, WINDOW_60)
        val rhr7 = BaselineStats.rollingBaseline(input.restingHrHistoryNewestFirst, WINDOW_7)

        val hrvZ7 = b7?.let { BaselineStats.zScore(lnToday, it) }
        val hrvZ60 = b60?.let { BaselineStats.zScore(lnToday, it) }
        val rhrZ = rhr7?.let { BaselineStats.zScore(input.restingHrBpm, it) }
        val sleepRatio = if (input.sleepNeedHours > 0) {
            (input.sleepHours / input.sleepNeedHours).coerceIn(0.0, 1.5)
        } else {
            1.0
        }

        // Higher ln rMSSD vs baseline and lower RHR vs baseline tend to raise readiness;
        // sleep ratio near 1 supports recovery. Weights are a transparent wellness heuristic.
        val hrvComponent = when {
            hrvZ7 == null -> 50.0
            else -> (50.0 + 12.0 * hrvZ7 + 6.0 * (hrvZ60 ?: hrvZ7)).coerceIn(0.0, 100.0)
        }
        val rhrComponent = when {
            rhrZ == null -> 50.0
            else -> (50.0 - 10.0 * rhrZ).coerceIn(0.0, 100.0)
        }
        val sleepComponent = (sleepRatio * 100.0).coerceIn(0.0, 100.0)
        val score = round(0.45 * hrvComponent + 0.25 * rhrComponent + 0.30 * sleepComponent).toInt()
            .coerceIn(0, 100)
        val band = when {
            score < 40 -> ReadinessBand.LOW
            score < 70 -> ReadinessBand.MODERATE
            else -> ReadinessBand.HIGH
        }

        return EvidencedInsight(
            value = DailyReadinessResult(
                score = score,
                band = band,
                lnRmssd = lnToday,
                baseline7 = b7,
                baseline60 = b60,
                hrvZ7 = hrvZ7,
                hrvZ60 = hrvZ60,
                rhrZ7 = rhrZ,
                sleepRatio = sleepRatio,
            ),
            claimId = InsightEvidence.DAILY_READINESS,
        )
    }
}
