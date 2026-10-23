package com.omnigart.shared.calc

import kotlin.test.Test
import kotlin.test.assertEquals

class AnthropometryTest {
    @Test
    fun waistToHeight() {
        assertEquals(0.5, Anthropometry.waistToHeightRatio(85.0, 170.0), absoluteTolerance = 0.0001)
    }

    @Test
    fun waistToHip() {
        assertEquals(0.8, Anthropometry.waistToHipRatio(80.0, 100.0), absoluteTolerance = 0.0001)
    }
}
