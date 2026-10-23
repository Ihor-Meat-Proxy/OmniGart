package com.omnigart.activity.swim

import com.omnigart.activity.evidence.Citations
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Watch-accelerometer swim stroke and lap (turn) detection.
 *
 * Spec ([Citations.SWIM_ACCEL_SPEC]):
 * 1. Work on a single dominant wrist axis (or magnitude).
 * 2. Smooth with a short moving average (box filter).
 * 3. Stroke peaks: local maxima above `strokeThreshold ×` robust amplitude,
 *    separated by at least `minStrokePeriodMillis` (refractory).
 * 4. Turns / laps: magnitude burst ≥ `turnThreshold ×` amplitude lasting
 *    ≥ `minTurnDurationMillis`, followed by a quiet window
 *    (`postTurnQuietMillis`) before the next stroke train.
 *
 * Thresholds are starting points for synthetic fixtures; field tuning is
 * logged under OPEN_QUESTIONS A#.
 */
data class AccelSample(
    val epochMillis: Long,
    /** Dominant wrist axis in g (or m/s² — relative thresholds are scale-free). */
    val axis: Double,
)

data class SwimDetectionConfig(
    val smoothWindow: Int = 5,
    val strokeThreshold: Double = 0.45,
    val minStrokePeriodMillis: Long = 350L,
    val turnThreshold: Double = 1.8,
    val minTurnDurationMillis: Long = 200L,
    val postTurnQuietMillis: Long = 400L,
)

data class StrokeEvent(val epochMillis: Long, val amplitude: Double)

data class LapEvent(
    val epochMillis: Long,
    val peakAmplitude: Double,
    val strokesInLap: Int,
)

data class SwimDetectionResult(
    val strokes: List<StrokeEvent>,
    val laps: List<LapEvent>,
    val strokeRatePerMinute: Double?,
)

object SwimAccelDetection {
    const val SPEC_CITATION: String = Citations.SWIM_ACCEL_SPEC

    fun detect(
        samples: List<AccelSample>,
        config: SwimDetectionConfig = SwimDetectionConfig(),
    ): SwimDetectionResult {
        if (samples.size < config.smoothWindow + 2) {
            return SwimDetectionResult(emptyList(), emptyList(), null)
        }
        val sorted = samples.sortedBy { it.epochMillis }
        val smoothed = boxSmooth(sorted.map { it.axis }, config.smoothWindow)
        val amp = robustAmplitude(smoothed)
        if (amp <= 1e-9) {
            return SwimDetectionResult(emptyList(), emptyList(), null)
        }

        val strokeFloor = config.strokeThreshold * amp
        val turnFloor = config.turnThreshold * amp

        val strokes = mutableListOf<StrokeEvent>()
        var lastStrokeMs = Long.MIN_VALUE / 2
        for (i in 1 until smoothed.lastIndex) {
            val v = smoothed[i]
            if (v > smoothed[i - 1] && v >= smoothed[i + 1] && v >= strokeFloor) {
                val t = sorted[i].epochMillis
                if (t - lastStrokeMs >= config.minStrokePeriodMillis) {
                    strokes += StrokeEvent(t, v)
                    lastStrokeMs = t
                }
            }
        }

        val turns = mutableListOf<Pair<Long, Double>>()
        var i = 0
        var lastTurnMs = Long.MIN_VALUE / 2
        val minTurnGapMs = 5_000L
        while (i < smoothed.size) {
            if (abs(smoothed[i]) >= turnFloor) {
                val start = i
                var peak = abs(smoothed[i])
                while (i < smoothed.size && abs(smoothed[i]) >= turnFloor * 0.6) {
                    peak = maxOf(peak, abs(smoothed[i]))
                    i++
                }
                val turnMs = sorted[start].epochMillis
                val duration = sorted[(i - 1).coerceAtLeast(start)].epochMillis - turnMs
                if (duration >= config.minTurnDurationMillis &&
                    turnMs - lastTurnMs >= minTurnGapMs
                ) {
                    // Optional quiet check: mean |axis| in the post-turn window
                    // should drop below the stroke floor (wall contact → glide).
                    val quietEnd = turnMs + config.postTurnQuietMillis
                    val quietSamples = sorted.indices.filter {
                        sorted[it].epochMillis in turnMs..quietEnd
                    }
                    val quietMean = if (quietSamples.isEmpty()) 0.0 else {
                        quietSamples.map { abs(smoothed[it]) }.average()
                    }
                    // Accept if quiet, or if the burst itself was strong (≥ 1.5× turn floor).
                    if (quietMean < strokeFloor || peak >= turnFloor * 1.5) {
                        turns += turnMs to peak
                        lastTurnMs = turnMs
                    }
                }
            } else {
                i++
            }
        }

        // Assign strokes to laps split by accepted turns.
        val laps = mutableListOf<LapEvent>()
        var strokeCursor = 0
        var lapStartStroke = 0
        for ((turnMs, peak) in turns) {
            while (strokeCursor < strokes.size && strokes[strokeCursor].epochMillis < turnMs) {
                strokeCursor++
            }
            val count = strokeCursor - lapStartStroke
            if (count > 0) {
                laps += LapEvent(turnMs, peak, count)
            }
            lapStartStroke = strokeCursor
        }
        // Trailing incomplete length is not a lap event.

        val rate = if (strokes.size >= 2) {
            val span = (strokes.last().epochMillis - strokes.first().epochMillis).toDouble()
            if (span > 0) (strokes.size - 1) * 60_000.0 / span else null
        } else {
            null
        }

        return SwimDetectionResult(strokes, laps, rate)
    }

    /** Magnitude helper when three axes are available. */
    fun magnitude(x: Double, y: Double, z: Double): Double =
        sqrt(x * x + y * y + z * z)

    private fun boxSmooth(values: List<Double>, window: Int): List<Double> {
        if (window <= 1) return values
        val half = window / 2
        return values.indices.map { i ->
            val from = (i - half).coerceAtLeast(0)
            val to = (i + half).coerceAtMost(values.lastIndex)
            var sum = 0.0
            for (j in from..to) sum += values[j]
            sum / (to - from + 1)
        }
    }

    /** Median absolute deviation × 1.4826 as a robust amplitude scale. */
    private fun robustAmplitude(values: List<Double>): Double {
        val abs = values.map { abs(it) }.sorted()
        val median = abs[abs.size / 2]
        val deviations = abs.map { abs(it - median) }.sorted()
        val mad = deviations[deviations.size / 2]
        val scale = 1.4826 * mad
        return maxOf(scale, median, 1e-6)
    }
}

/**
 * Synthetic fixture builders for unit tests — not for production sampling.
 */
object SwimSyntheticFixtures {
    /**
     * Freestyle-like sinusoid at [strokeHz] with optional turn spikes every
     * [strokesPerLap] cycles.
     */
    fun freestyleWithTurns(
        durationSeconds: Double = 40.0,
        sampleHz: Double = 50.0,
        strokeHz: Double = 0.8,
        strokesPerLap: Int = 12,
        amplitude: Double = 1.0,
        turnAmplitude: Double = 4.0,
        startEpochMillis: Long = 0L,
    ): List<AccelSample> {
        val samples = mutableListOf<AccelSample>()
        val n = (durationSeconds * sampleHz).toInt()
        val strokePeriodSec = 1.0 / strokeHz
        // Turn windows: 250 ms spikes after every strokesPerLap cycles.
        val turnStartsSec = generateSequence(strokesPerLap * strokePeriodSec) {
            it + strokesPerLap * strokePeriodSec
        }.takeWhile { it < durationSeconds }.toList()

        for (i in 0 until n) {
            val tSec = i / sampleHz
            val phase = 2.0 * kotlin.math.PI * strokeHz * tSec
            var v = amplitude * kotlin.math.sin(phase)
            val inTurn = turnStartsSec.any { start -> tSec in start..(start + 0.25) }
            if (inTurn) v = turnAmplitude
            samples += AccelSample(
                epochMillis = startEpochMillis + (tSec * 1000.0).toLong(),
                axis = v,
            )
        }
        return samples
    }
}
