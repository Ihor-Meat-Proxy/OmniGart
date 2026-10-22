package com.omnigart.sensors.hrv

import kotlin.math.ln
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HrvMetricsTest {

    @Test
    fun needsAtLeastTwoSamples() {
        assertNull(HrvMetrics.compute(emptyList()))
        assertNull(HrvMetrics.compute(listOf(800.0)))
    }

    @Test
    fun rmssdAndSdnnOnSimpleSeries() {
        // RR: 800, 820, 810, 830 ms
        // diffs: 20, -10, 20 → mean sq = (400+100+400)/3 = 300 → rMSSD = √300
        val rr = listOf(800.0, 820.0, 810.0, 830.0)
        val m = HrvMetrics.compute(rr)!!
        val expectedRmssd = sqrt((400.0 + 100.0 + 400.0) / 3.0)
        assertEquals(expectedRmssd, m.rmssdMs, absoluteTolerance = 1e-9)
        assertEquals(ln(expectedRmssd), m.lnRmssd, absoluteTolerance = 1e-9)

        val mean = 815.0
        val variance = listOf(800.0, 820.0, 810.0, 830.0)
            .sumOf { (it - mean) * (it - mean) } / 4.0
        assertEquals(sqrt(variance), m.sdnnMs, absoluteTolerance = 1e-9)
        assertEquals(mean, m.meanRrMs, absoluteTolerance = 1e-9)
        assertEquals(60_000.0 / mean, m.meanHrBpm, absoluteTolerance = 1e-9)
    }

    @Test
    fun constantSeriesHasZeroRmssd() {
        val m = HrvMetrics.compute(List(10) { 1000.0 })!!
        assertEquals(0.0, m.rmssdMs, absoluteTolerance = 1e-12)
        assertEquals(0.0, m.sdnnMs, absoluteTolerance = 1e-12)
        // ln(0) → -Inf
        assertTrue(m.lnRmssd.isInfinite() && m.lnRmssd < 0)
    }
}
