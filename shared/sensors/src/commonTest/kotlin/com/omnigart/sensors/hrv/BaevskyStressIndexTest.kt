package com.omnigart.sensors.hrv

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BaevskyStressIndexTest {

    @Test
    fun workedExampleComponentsYieldSi100() {
        // Classic worked example: Mo=0.80 s, AMo=40 %, MxDMn=0.25 s → SI=100
        val si = BaevskyStressIndex.fromComponents(
            modeSeconds = 0.80,
            amplitudeOfModePercent = 40.0,
            variationRangeSeconds = 0.25,
        )
        assertEquals(100.0, si, absoluteTolerance = 1e-9)
        assertEquals(100, BaevskyStressIndex.displayValue(si))
    }

    @Test
    fun histogramProducesExpectedModeAndAmo() {
        // 10 intervals: eight in 775–825 ms bin (bin 15 for 50 ms → 750–800? )
        // bin = floor(rr / 50)
        // 800 → bin 16 (800/50=16) centre = 16.5*50 = 825 ms = 0.825 s
        // Use values that all land in bin 16: 800..849
        val rr = listOf(
            800.0, 810.0, 820.0, 830.0, 805.0, 815.0, 825.0, 835.0, // 8 in bin 16
            900.0, // bin 18
            700.0, // bin 14
        )
        val result = BaevskyStressIndex.compute(rr)!!
        assertEquals(0.825, result.modeSeconds, absoluteTolerance = 1e-12)
        assertEquals(80.0, result.amplitudeOfModePercent, absoluteTolerance = 1e-9)
        // min=700, max=900 → MxDMn = 0.200 s
        assertEquals(0.200, result.variationRangeSeconds, absoluteTolerance = 1e-12)
        // SI = 80 / (2 * 0.825 * 0.200) = 80 / 0.330 ≈ 242.424
        val expected = 80.0 / (2.0 * 0.825 * 0.200)
        assertEquals(expected, result.stressIndex, absoluteTolerance = 1e-6)
    }

    @Test
    fun identicalIntervalsUndefined() {
        assertNull(BaevskyStressIndex.compute(List(20) { 800.0 }))
    }

    @Test
    fun tooFewSamples() {
        assertNull(BaevskyStressIndex.compute(listOf(800.0)))
    }

    @Test
    fun elevatedSiWhenNarrowRangeAndHighAmo() {
        // Tight cluster around 600 ms with one outlier → high SI (sympathetic)
        val rr = List(19) { 600.0 } + listOf(650.0)
        val result = BaevskyStressIndex.compute(rr)!!
        assertTrue(result.stressIndex > 200.0, "expected elevated SI, got ${result.stressIndex}")
    }
}
