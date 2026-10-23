package com.omnigart.shared.calc

import kotlin.math.ln
import kotlin.math.sqrt

/**
 * Training readiness heuristic (OmniGart Phase 0).
 *
 * Composite score from HRV, resting HR, sleep, yesterday's load, and illness flag.
 * Requires at least [MIN_HISTORY_DAYS] calendar days of data; otherwise returns
 * [ReadinessResult.InsufficientData].
 *
 * Scoring (points accumulate):
 * - HRV: 7-day mean ln(rMSSD) vs 60-day baseline; below SWC (0.5·SD) → +1; below 1·SD → +2
 * - Resting HR vs 30-day average: +5 bpm → +1; +8 bpm → +2
 * - Sleep: &lt;85% of need or efficiency &lt;80% → +1; &lt;5 h → +2
 * - Yesterday's load vs 28-day daily average: &gt;1.5× → +1; &gt;2× → +2
 * - Illness → Rest (override)
 *
 * Bands: 0–1 Go, 2–3 Moderate, 4–5 Easy, 6+ Rest.
 *
 * HRV SWC concept draws on Buchheit M. Monitoring training status with HR measures.
 * Front Physiol. 2014;5:73. The exact band thresholds are OmniGart product rules
 * (see docs/OPEN_QUESTIONS.md).
 */
enum class ReadinessBand {
    GO,
    MODERATE,
    EASY,
    REST,
    INSUFFICIENT_DATA
}

sealed class ReadinessResult {
    data class Scored(val points: Int, val band: ReadinessBand) : ReadinessResult()

    data object InsufficientData : ReadinessResult()
}

data class ReadinessInput(
    /** Calendar days of available monitoring history. */
    val historyDays: Int,
    /** Mean ln(rMSSD) over the last 7 days. */
    val hrvLnRmssd7Day: Double?,
    /** Baseline mean ln(rMSSD) over ~60 days. */
    val hrvBaselineMean: Double?,
    /** Baseline SD of ln(rMSSD) over ~60 days. */
    val hrvBaselineSd: Double?,
    /** Today's (or latest) resting HR. */
    val restingHrBpm: Double?,
    /** 30-day average resting HR. */
    val restingHr30DayAvg: Double?,
    /** Last night's sleep duration in hours. */
    val sleepHours: Double?,
    /** Individual sleep need in hours (e.g. 8.0). */
    val sleepNeedHours: Double?,
    /** Sleep efficiency 0–1 (time asleep / time in bed). */
    val sleepEfficiency: Double?,
    /** Yesterday's training load (arbitrary consistent units). */
    val yesterdayLoad: Double?,
    /** Mean daily load over the last 28 days. */
    val load28DayAvg: Double?,
    val illness: Boolean = false
)

object Readiness {
    const val MIN_HISTORY_DAYS = 14

    fun evaluate(input: ReadinessInput): ReadinessResult {
        if (input.historyDays < MIN_HISTORY_DAYS) {
            return ReadinessResult.InsufficientData
        }
        if (input.illness) {
            return ReadinessResult.Scored(points = 6, band = ReadinessBand.REST)
        }

        var points = 0
        points += hrvPoints(input)
        points += restingHrPoints(input)
        points += sleepPoints(input)
        points += loadPoints(input)

        val band =
            when {
                points <= 1 -> ReadinessBand.GO
                points <= 3 -> ReadinessBand.MODERATE
                points <= 5 -> ReadinessBand.EASY
                else -> ReadinessBand.REST
            }
        return ReadinessResult.Scored(points = points, band = band)
    }

    /** Helper for building ln(rMSSD) from raw rMSSD (ms). */
    fun lnRmssd(rmssdMs: Double): Double {
        require(rmssdMs > 0)
        return ln(rmssdMs)
    }

    fun sampleStdDev(values: List<Double>): Double {
        require(values.size >= 2)
        val mean = values.average()
        val variance = values.sumOf { (it - mean) * (it - mean) } / (values.size - 1)
        return sqrt(variance)
    }

    private fun hrvPoints(input: ReadinessInput): Int {
        val current = input.hrvLnRmssd7Day ?: return 0
        val mean = input.hrvBaselineMean ?: return 0
        val sd = input.hrvBaselineSd ?: return 0
        if (sd <= 0) return 0
        val delta = mean - current // positive when current is below baseline
        val swc = 0.5 * sd
        return when {
            delta >= sd -> 2
            delta >= swc -> 1
            else -> 0
        }
    }

    private fun restingHrPoints(input: ReadinessInput): Int {
        val current = input.restingHrBpm ?: return 0
        val avg = input.restingHr30DayAvg ?: return 0
        val delta = current - avg
        return when {
            delta >= 8.0 -> 2
            delta >= 5.0 -> 1
            else -> 0
        }
    }

    private fun sleepPoints(input: ReadinessInput): Int {
        val hours = input.sleepHours ?: return 0
        if (hours < 5.0) return 2
        val need = input.sleepNeedHours
        val efficiency = input.sleepEfficiency
        val underNeed = need != null && hours < 0.85 * need
        val lowEfficiency = efficiency != null && efficiency < 0.80
        return if (underNeed || lowEfficiency) 1 else 0
    }

    private fun loadPoints(input: ReadinessInput): Int {
        val yesterday = input.yesterdayLoad ?: return 0
        val avg = input.load28DayAvg ?: return 0
        if (avg <= 0) return 0
        val ratio = yesterday / avg
        return when {
            ratio > 2.0 -> 2
            ratio > 1.5 -> 1
            else -> 0
        }
    }
}
