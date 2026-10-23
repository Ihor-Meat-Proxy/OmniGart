package com.omnigart.activity.swim

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SwolfAndAccelTest {

    @Test
    fun swolfIsStrokesPlusSeconds() {
        val s = Swolf.score(strokes = 18, timeSeconds = 22.0, lengthMetres = 25.0)
        assertEquals(40.0, s.score, absoluteTolerance = 1e-9)
        assertEquals(40.0, Swolf.average(listOf(s, Swolf.score(20, 20.0)))!!)
    }

    @Test
    fun detectsStrokesOnSyntheticFreestyle() {
        val samples = SwimSyntheticFixtures.freestyleWithTurns(
            durationSeconds = 30.0,
            sampleHz = 50.0,
            strokeHz = 1.0,
            strokesPerLap = 20, // no turns in first 20 s
            amplitude = 1.0,
            turnAmplitude = 5.0,
        )
        // Use only first 15 s (before any turn) for stroke-only check
        val early = samples.filter { it.epochMillis < 15_000 }
        val result = SwimAccelDetection.detect(
            early,
            SwimDetectionConfig(strokeThreshold = 0.3, minStrokePeriodMillis = 400L),
        )
        // ~1 Hz for 15 s → ~14–15 strokes
        assertTrue(
            result.strokes.size in 10..20,
            "expected ~15 strokes, got ${result.strokes.size}",
        )
        assertTrue((result.strokeRatePerMinute ?: 0.0) in 45.0..70.0)
    }

    @Test
    fun detectsLapsOnSyntheticTurns() {
        val samples = SwimSyntheticFixtures.freestyleWithTurns(
            durationSeconds = 45.0,
            sampleHz = 50.0,
            strokeHz = 1.0,
            strokesPerLap = 10,
            amplitude = 1.0,
            turnAmplitude = 5.0,
        )
        val result = SwimAccelDetection.detect(
            samples,
            SwimDetectionConfig(
                strokeThreshold = 0.35,
                minStrokePeriodMillis = 400L,
                turnThreshold = 2.5,
                minTurnDurationMillis = 150L,
                postTurnQuietMillis = 200L,
            ),
        )
        assertTrue(
            result.laps.isNotEmpty(),
            "expected at least one lap/turn, strokes=${result.strokes.size}",
        )
        assertTrue(result.laps.all { it.strokesInLap > 0 })
    }
}
