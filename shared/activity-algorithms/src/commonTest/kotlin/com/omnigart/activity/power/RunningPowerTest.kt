package com.omnigart.activity.power

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RunningPowerTest {

    @Test
    fun flatRunningPower() {
        // 70 kg at 3.333 m/s (5:00/km), Cr=3.8 → P = 70 * 3.333 * 3.8 ≈ 886 W
        val est = RunningPower.estimate(70.0, 1000.0 / 300.0)!!
        assertEquals(70.0 * (1000.0 / 300.0) * 3.8, est.watts, absoluteTolerance = 0.1)
        assertEquals(est.watts / 70.0, est.wattsPerKg, absoluteTolerance = 1e-9)
    }

    @Test
    fun uphillIncreasesPower() {
        val flat = RunningPower.estimate(70.0, 3.0, grade = 0.0)!!
        val hill = RunningPower.estimate(70.0, 3.0, grade = 0.05)!!
        assertTrue(hill.watts > flat.watts)
    }

    @Test
    fun fromPaceMatchesDirect() {
        val a = RunningPower.fromPaceSecondsPerKm(65.0, 300.0)!!
        val b = RunningPower.estimate(65.0, 1000.0 / 300.0)!!
        assertEquals(b.watts, a.watts, absoluteTolerance = 1e-9)
    }

    @Test
    fun rejectsNonPositive() {
        assertNull(RunningPower.estimate(0.0, 3.0))
        assertNull(RunningPower.estimate(70.0, 0.0))
    }
}
