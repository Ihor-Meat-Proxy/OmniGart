package com.omnigart.activity.hr

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HeartRateZonesTest {

    @Test
    fun percentHrMaxBounds() {
        val zones = HeartRateZones.boundsBpm(hrMax = 200, model = ZoneModel.PERCENT_HR_MAX)
        assertEquals(5, zones.size)
        assertEquals(100, zones[0].lowerBpm) // 50%
        assertEquals(120, zones[0].upperBpm) // 60%
        assertEquals(180, zones[4].lowerBpm)
        assertEquals(200, zones[4].upperBpm)
    }

    @Test
    fun karvonenThreshold() {
        // HRrest=60, HRmax=180, 70% HRR → 60 + 0.7*(120) = 144
        val bpm = HeartRateZones.thresholdBpm(
            0.70, hrMax = 180, hrRest = 60, model = ZoneModel.PERCENT_HR_RESERVE,
        )
        assertEquals(144, bpm)
    }

    @Test
    fun timeInZoneAccumulation() {
        val zones = HeartRateZones.boundsBpm(200)
        val samples = listOf(
            HrSample(0, 110),    // Z1
            HrSample(1000, 110),
            HrSample(2000, 150), // Z3
            HrSample(5000, 150),
            HrSample(6000, 190), // Z5
        )
        val tiz = HeartRateZones.timeInZone(samples, zones, defaultSamplePeriodMillis = 1000L)
        assertEquals(5, tiz.size)
        assertTrue(tiz[0].durationMillis >= 1000L)
        assertTrue(tiz[2].durationMillis >= 3000L)
        assertTrue(tiz[4].durationMillis >= 1000L)
        assertEquals(0L, tiz[1].durationMillis) // no Z2 samples
    }
}
