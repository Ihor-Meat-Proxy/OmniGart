package com.omnigart.shared.calc

import kotlin.test.Test
import kotlin.test.assertEquals

class OneRepMaxTest {
    @Test
    fun epley_5reps() {
        // 100 * (1 + 5/30) = 116.666...
        assertEquals(116.6667, OneRepMax.epley(100.0, 5), absoluteTolerance = 0.001)
    }

    @Test
    fun brzycki_5reps() {
        // 100 * 36 / (37-5) = 3600/32 = 112.5
        assertEquals(112.5, OneRepMax.brzycki(100.0, 5), absoluteTolerance = 0.001)
    }

    @Test
    fun epley_oneRepIsIdentity() {
        assertEquals(100.0, OneRepMax.epley(100.0, 1), absoluteTolerance = 0.001)
    }

    @Test
    fun fromRir_addsReserveToEpley() {
        // 100 kg × 5 reps @ RIR 2 → effective 7 → 100*(1+7/30)=123.333
        assertEquals(123.3333, OneRepMax.fromRir(100.0, 5, 2.0), absoluteTolerance = 0.001)
    }
}
