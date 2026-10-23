package com.omnigart.activity.run

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PaceCadenceTest {

    @Test
    fun paceFromKnownSpeed() {
        // 5:00 /km = 3.333… m/s
        val pace = Pace.fromSpeedMetresPerSecond(1000.0 / 300.0)!!
        assertEquals(300.0, pace.secondsPerKm, absoluteTolerance = 1e-9)
        assertEquals("5:00", pace.formatMetric())
    }

    @Test
    fun paceFromDistanceAndTime() {
        val pace = Pace.fromDistanceAndTime(5000.0, 1500.0)!! // 5k in 25:00
        assertEquals(300.0, pace.secondsPerKm, absoluteTolerance = 1e-9)
        assertNull(Pace.fromDistanceAndTime(0.0, 100.0))
    }

    @Test
    fun kilometreSplits() {
        // Constant 4:00/km over 3 km → 3 full splits
        val points = (0..30).map { i ->
            DistancePoint(
                distanceMetres = i * 100.0,
                elapsedSeconds = i * 24.0, // 100 m / 24 s = 4:00/km
            )
        }
        val splits = PaceCalculator.splits(points, splitDistanceMetres = 1000.0)
        assertTrue(splits.size >= 3)
        assertEquals(240.0, splits[0].pace.secondsPerKm, absoluteTolerance = 0.5)
        assertEquals(240.0, splits[1].pace.secondsPerKm, absoluteTolerance = 0.5)
    }

    @Test
    fun cadenceFromStepCount() {
        assertEquals(180.0, CadenceCalculator.fromStepCount(180, 60.0)!!, absoluteTolerance = 1e-9)
        assertNull(CadenceCalculator.fromStepCount(0, 60.0))
    }

    @Test
    fun cadenceFromTimestamps() {
        val steps = (0L..20L).map { it * 333L } // ~180 spm
        val cadence = CadenceCalculator.fromStepTimestamps(steps, windowMillis = 10_000L)
        assertNotNull(cadence)
        assertEquals(180.0, cadence, absoluteTolerance = 2.0)
    }
}
