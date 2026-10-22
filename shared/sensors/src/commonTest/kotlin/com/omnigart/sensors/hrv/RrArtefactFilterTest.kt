package com.omnigart.sensors.hrv

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RrArtefactFilterTest {

    @Test
    fun dropsAbsoluteOutliers() {
        val rr = listOf(800.0, 810.0, 50.0, 820.0, 800.0) // 50 ms non-physiologic
        val result = RrArtefactFilter.correct(
            rr,
            ArtefactFilterConfig(interpolate = false, relativeThreshold = 0.5),
        )
        assertEquals(listOf(2), result.rejectedIndices)
        assertEquals(4, result.acceptedCount)
        assertTrue(result.correctedMs.none { it == 50.0 })
    }

    @Test
    fun dropsRelativeOutliersAndInterpolates() {
        val rr = listOf(800.0, 810.0, 1500.0, 820.0, 800.0)
        val result = RrArtefactFilter.correct(
            rr,
            ArtefactFilterConfig(window = 2, relativeThreshold = 0.20, interpolate = true),
        )
        assertTrue(2 in result.rejectedIndices)
        assertEquals(5, result.correctedMs.size)
        // Interpolated value should sit between neighbours (~815)
        assertEquals(815.0, result.correctedMs[2], absoluteTolerance = 1.0)
    }

    @Test
    fun cleanSeriesUnchanged() {
        val rr = listOf(800.0, 805.0, 810.0, 800.0, 795.0)
        val result = RrArtefactFilter.correct(rr)
        assertTrue(result.rejectedIndices.isEmpty())
        assertEquals(rr, result.correctedMs)
    }
}
