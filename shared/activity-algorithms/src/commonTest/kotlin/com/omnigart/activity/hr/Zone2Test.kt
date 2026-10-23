package com.omnigart.activity.hr

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class Zone2Test {
    @Test
    fun karvonenDefaultMatchesFiveZoneZ2() {
        // rest 60, max 180 → HRR 120; 60–70% → 132–144
        val z2 = Zone2.karvonen(hrMax = 180, hrRest = 60)
        assertEquals(Zone2Method.HRR_KARVONEN, z2.method)
        assertEquals(132, z2.lowerBpm)
        assertEquals(144, z2.upperBpm)
        assertTrue(z2.contains(140))
        assertFalse(z2.contains(120))
    }

    @Test
    fun percentHrMaxBand() {
        val z2 =
            Zone2.profile(
                method = Zone2Method.PERCENT_HR_MAX,
                hrMax = 200,
                hrRest = 50,
            )
        assertEquals(120, z2.lowerBpm) // 60%
        assertEquals(140, z2.upperBpm) // 70%
    }

    @Test
    fun lthrBand() {
        val z2 =
            Zone2.profile(
                method = Zone2Method.LTHR,
                hrMax = 190,
                hrRest = 55,
                lthrBpm = 170,
            )
        assertEquals(136, z2.lowerBpm) // 0.80 * 170
        assertEquals(153, z2.upperBpm) // 0.90 * 170
        assertTrue(z2.contains(145, hysteresisBpm = 0))
    }

    @Test
    fun hysteresisWidensBand() {
        val z2 = Zone2.karvonen(180, 60)
        assertFalse(z2.contains(130))
        assertTrue(z2.contains(130, hysteresisBpm = 3))
    }
}
