package com.omnigart.shared.calc

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ReadinessTest {
    private fun base(
        historyDays: Int = 20,
        hrvLnRmssd7Day: Double? = 4.0,
        hrvBaselineMean: Double? = 4.0,
        hrvBaselineSd: Double? = 0.2,
        restingHrBpm: Double? = 55.0,
        restingHr30DayAvg: Double? = 55.0,
        sleepHours: Double? = 8.0,
        sleepNeedHours: Double? = 8.0,
        sleepEfficiency: Double? = 0.9,
        yesterdayLoad: Double? = 50.0,
        load28DayAvg: Double? = 50.0,
        illness: Boolean = false
    ) = ReadinessInput(
        historyDays = historyDays,
        hrvLnRmssd7Day = hrvLnRmssd7Day,
        hrvBaselineMean = hrvBaselineMean,
        hrvBaselineSd = hrvBaselineSd,
        restingHrBpm = restingHrBpm,
        restingHr30DayAvg = restingHr30DayAvg,
        sleepHours = sleepHours,
        sleepNeedHours = sleepNeedHours,
        sleepEfficiency = sleepEfficiency,
        yesterdayLoad = yesterdayLoad,
        load28DayAvg = load28DayAvg,
        illness = illness
    )

    @Test
    fun insufficientHistory() {
        val result = Readiness.evaluate(base(historyDays = 10))
        assertIs<ReadinessResult.InsufficientData>(result)
    }

    @Test
    fun allClear_isGo() {
        val result = Readiness.evaluate(base())
        assertIs<ReadinessResult.Scored>(result)
        assertEquals(0, result.points)
        assertEquals(ReadinessBand.GO, result.band)
    }

    @Test
    fun hrvBelowSwc_scoresOne() {
        // SWC = 0.5*0.2 = 0.1; current 3.9 → delta 0.1 → +1
        val result = Readiness.evaluate(base(hrvLnRmssd7Day = 3.9))
        assertIs<ReadinessResult.Scored>(result)
        assertEquals(1, result.points)
        assertEquals(ReadinessBand.GO, result.band)
    }

    @Test
    fun hrvBelowOneSd_scoresTwo() {
        val result = Readiness.evaluate(base(hrvLnRmssd7Day = 3.8))
        assertIs<ReadinessResult.Scored>(result)
        assertEquals(2, result.points)
        assertEquals(ReadinessBand.MODERATE, result.band)
    }

    @Test
    fun restingHrAndSleepAndLoad_accumulate() {
        // +1 HR (+5), +1 sleep (<85% need), +1 load (>1.5x) = 3 → Moderate
        val result =
            Readiness.evaluate(
                base(
                    restingHrBpm = 60.0,
                    restingHr30DayAvg = 55.0,
                    sleepHours = 6.5,
                    sleepNeedHours = 8.0,
                    yesterdayLoad = 80.0,
                    load28DayAvg = 50.0
                )
            )
        assertIs<ReadinessResult.Scored>(result)
        assertEquals(3, result.points)
        assertEquals(ReadinessBand.MODERATE, result.band)
    }

    @Test
    fun shortSleep_scoresTwo_andHighLoad() {
        // sleep <5h = 2; load >2x = 2; total 4 → Easy
        val result =
            Readiness.evaluate(
                base(
                    sleepHours = 4.5,
                    yesterdayLoad = 110.0,
                    load28DayAvg = 50.0
                )
            )
        assertIs<ReadinessResult.Scored>(result)
        assertEquals(4, result.points)
        assertEquals(ReadinessBand.EASY, result.band)
    }

    @Test
    fun illness_forcesRest() {
        val result = Readiness.evaluate(base(illness = true))
        assertIs<ReadinessResult.Scored>(result)
        assertEquals(ReadinessBand.REST, result.band)
        assertTrue(result.points >= 6)
    }

    @Test
    fun lnRmssd_knownValue() {
        // ln(50) ≈ 3.912023
        assertEquals(3.912023, Readiness.lnRmssd(50.0), absoluteTolerance = 0.0001)
    }
}
