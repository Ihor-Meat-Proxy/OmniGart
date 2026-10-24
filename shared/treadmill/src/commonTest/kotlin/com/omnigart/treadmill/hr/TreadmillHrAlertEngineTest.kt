package com.omnigart.treadmill.hr

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TreadmillHrAlertEngineTest {
    private val settings = TreadmillHrSettings(maxBpm = 200, highLimitFraction = 0.95)

    @Test
    fun percentHrMaxZones() {
        assertEquals(2, TreadmillHrAlertEngine.currentZone(130, settings))
        assertEquals(5, TreadmillHrAlertEngine.currentZone(195, settings))
    }

    @Test
    fun karvonenUsesRestingWhenPresent() {
        val karvonen =
            TreadmillHrSettings(
                maxBpm = 200,
                restingBpm = 60,
                zoneModel = TreadmillHrZoneModel.KARVONEN
            )
        // Z2 Karvonen HRR 60–70% with rest 60 / max 200 → 144–158 bpm
        val zone = TreadmillHrAlertEngine.currentZone(150, karvonen)
        assertEquals(2, zone)
    }

    @Test
    fun safetyHighAndDropout() {
        val high =
            TreadmillHrAlertEngine.evaluate(
                bpm = 192,
                settings = settings,
                targetZone = null,
                effortActive = true,
                secondsSinceLastSample = 1.0,
                highStreakSeconds = 10.0,
                lowStreakSeconds = 0.0
            )
        assertTrue(high.any { it.kind == TreadmillHrAlertKind.SAFETY_HIGH })

        val drop =
            TreadmillHrAlertEngine.evaluate(
                bpm = null,
                settings = settings,
                targetZone = 3,
                effortActive = true,
                secondsSinceLastSample = 20.0,
                highStreakSeconds = 0.0,
                lowStreakSeconds = 0.0
            )
        assertTrue(drop.any { it.kind == TreadmillHrAlertKind.SENSOR_DROPOUT })
    }

    @Test
    fun programZoneAlertsAndMute() {
        val above =
            TreadmillHrAlertEngine.evaluate(
                bpm = 175,
                settings = settings,
                targetZone = 2,
                effortActive = true,
                secondsSinceLastSample = 1.0,
                highStreakSeconds = 0.0,
                lowStreakSeconds = 0.0
            )
        assertTrue(above.any { it.kind == TreadmillHrAlertKind.ABOVE_TARGET_ZONE })

        val muted = settings.copy(alertsEnabled = false)
        assertTrue(
            TreadmillHrAlertEngine.evaluate(
                bpm = 195,
                settings = muted,
                targetZone = 2,
                effortActive = true,
                secondsSinceLastSample = 30.0,
                highStreakSeconds = 30.0,
                lowStreakSeconds = 30.0
            ).isEmpty()
        )
    }

    @Test
    fun foxMaxFallback() {
        assertEquals(180, TreadmillHrSettings.foxMax(40))
    }
}
