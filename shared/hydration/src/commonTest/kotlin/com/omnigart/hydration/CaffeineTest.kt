package com.omnigart.hydration

import com.omnigart.hydration.caffeine.CaffeineLimits
import com.omnigart.hydration.caffeine.LateCaffeineInsight
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CaffeineTest {

    @Test
    fun adultDailyAndSingleDoseLimits() {
        val ok = CaffeineLimits.assess(listOf(95.0, 63.0, 47.0))
        assertFalse(ok.exceedsDaily)
        assertFalse(ok.exceedsSingleDose)
        assertEquals(400.0, ok.dailyLimitMg)

        val over = CaffeineLimits.assess(listOf(250.0, 200.0))
        assertTrue(over.exceedsSingleDose)
        assertTrue(over.exceedsDaily)
    }

    @Test
    fun pregnancyUses200MgDaily() {
        val assessment = CaffeineLimits.assess(listOf(150.0, 60.0), pregnant = true)
        assertEquals(200.0, assessment.dailyLimitMg)
        assertTrue(assessment.exceedsDaily)
        assertTrue(assessment.pregnancyMode)
    }

    @Test
    fun lateCaffeineWithinSixHoursOfBed() {
        // Bed 23:00; coffee at 18:00 → 5 h before bed → in window
        val insight =
            LateCaffeineInsight.evaluate(
                bedtimeMinutesFromMidnight = 23 * 60,
                events =
                    listOf(
                        LateCaffeineInsight.CaffeineEvent(18 * 60, 95.0),
                        LateCaffeineInsight.CaffeineEvent(12 * 60, 63.0),
                    ),
            )
        assertTrue(insight.triggered)
        assertEquals(95.0, insight.totalMgInWindow)
        assertTrue(insight.messageEnGb!!.contains("10.5664/jcsm.3170"))
    }

    @Test
    fun lateCaffeineOutsideWindow() {
        val insight =
            LateCaffeineInsight.evaluate(
                bedtimeMinutesFromMidnight = 23 * 60,
                events = listOf(LateCaffeineInsight.CaffeineEvent(10 * 60, 95.0)),
            )
        assertFalse(insight.triggered)
    }

    @Test
    fun minutesBeforeBedWrapsMidnight() {
        // Logged 22:00, bed 01:00 → 3 hours
        assertEquals(180, LateCaffeineInsight.minutesBeforeBed(22 * 60, 60))
    }
}
