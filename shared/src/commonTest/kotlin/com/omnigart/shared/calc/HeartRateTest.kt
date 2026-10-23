package com.omnigart.shared.calc

import kotlin.test.Test
import kotlin.test.assertEquals

class HeartRateTest {
    @Test
    fun tanakaMax_age40() {
        // 208 - 0.7*40 = 180
        assertEquals(180.0, HeartRate.tanakaMax(40), absoluteTolerance = 0.01)
    }

    @Test
    fun karvonenTarget_midZone() {
        // rest 60, max 180, 70% → 60 + 0.7*(120) = 144
        assertEquals(144.0, HeartRate.karvonenTarget(60.0, 180.0, 0.70), absoluteTolerance = 0.01)
    }

    @Test
    fun karvonenFiveZones_bounds() {
        val zones = HeartRate.karvonenFiveZones(60.0, 180.0)
        assertEquals(5, zones.size)
        assertEquals("Z1", zones[0].name)
        assertEquals(120.0, zones[0].lowerBpm, absoluteTolerance = 0.01) // 50%
        assertEquals(132.0, zones[0].upperBpm, absoluteTolerance = 0.01) // 60%
        assertEquals(180.0, zones[4].upperBpm, absoluteTolerance = 0.01)
    }

    @Test
    fun karvonenThreeZones_bounds() {
        val zones = HeartRate.karvonenThreeZones(60.0, 180.0)
        assertEquals(3, zones.size)
        assertEquals("Easy", zones[0].name)
        assertEquals(120.0, zones[0].lowerBpm, absoluteTolerance = 0.01)
        assertEquals(144.0, zones[0].upperBpm, absoluteTolerance = 0.01)
        assertEquals("Hard", zones[2].name)
    }

    @Test
    fun percentHrMaxFiveZones() {
        val zones = HeartRate.percentHrMaxFiveZones(180.0)
        assertEquals(90.0, zones[0].lowerBpm, absoluteTolerance = 0.01)
        assertEquals(108.0, zones[0].upperBpm, absoluteTolerance = 0.01)
        assertEquals(180.0, zones[4].upperBpm, absoluteTolerance = 0.01)
    }
}
