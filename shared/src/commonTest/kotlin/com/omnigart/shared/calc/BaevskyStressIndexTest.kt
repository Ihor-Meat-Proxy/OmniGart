package com.omnigart.shared.calc

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class BaevskyStressIndexTest {
    @Test
    fun workedExample_50msBins() {
        // Ten RR intervals: eight in 800–850 bin, one 700, one 900.
        // Modal bin start 800, Mo = 0.825 s, AMo = 80%, MxDMn = 0.200 s
        // SI = 80 / (2 * 0.825 * 0.200) = 80 / 0.33 ≈ 242.424
        val rr =
            listOf(
                700.0,
                810.0,
                820.0,
                830.0,
                840.0,
                815.0,
                825.0,
                835.0,
                845.0,
                900.0
            )
        val result = BaevskyStressIndex.fromRrIntervalsMs(rr)
        assertEquals(800, result.modalBinStartMs)
        assertEquals(0.825, result.modeSeconds, absoluteTolerance = 1e-9)
        assertEquals(80.0, result.amplitudePercent, absoluteTolerance = 1e-9)
        assertEquals(0.2, result.variationRangeSeconds, absoluteTolerance = 1e-9)
        assertEquals(242.424242, result.stressIndex, absoluteTolerance = 0.001)
    }

    @Test
    fun rejectsIdenticalIntervals() {
        assertFailsWith<IllegalArgumentException> {
            BaevskyStressIndex.fromRrIntervalsMs(listOf(800.0, 800.0, 800.0))
        }
    }

    @Test
    fun requiresAtLeastTwo() {
        assertFailsWith<IllegalArgumentException> {
            BaevskyStressIndex.fromRrIntervalsMs(listOf(800.0))
        }
    }

    @Test
    fun binWidthConstant() {
        assertEquals(50, BaevskyStressIndex.BIN_WIDTH_MS)
        assertTrue(BaevskyStressIndex.nearestBinStartMs(824.0) in setOf(800, 850))
    }
}
