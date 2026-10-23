package com.omnigart.activity.vo2

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class Vo2MaxEstimatorsTest {

    @Test
    fun uthSorensenWorkedExample() {
        // HRmax=190, HRrest=50 → 15.3 * 190/50 = 58.14
        val est = Vo2MaxEstimators.estimate(
            Vo2MaxFormula.UTH_SORENSEN,
            Vo2MaxInput.UthSorensen(hrMax = 190, hrRest = 50),
        )
        assertEquals(15.3 * 190.0 / 50.0, est.mlPerKgMin, absoluteTolerance = 1e-9)
        assertTrue(est.citation.contains("Uth"))
    }

    @Test
    fun cooperTwelveMinute() {
        // 3.0 km → 22.351*3 − 11.288 = 55.765
        val est = Vo2MaxEstimators.estimate(
            Vo2MaxFormula.COOPER_12_MIN,
            Vo2MaxInput.Cooper(distanceKm = 3.0),
        )
        assertEquals(22.351 * 3.0 - 11.288, est.mlPerKgMin, absoluteTolerance = 1e-9)
    }

    @Test
    fun acsmRunningFlat() {
        // S = 200 m/min (~7.5 mph), G = 0 → VO2 = 3.5 + 40 = 43.5
        val est = Vo2MaxEstimators.estimate(
            Vo2MaxFormula.ACSM_RUNNING,
            Vo2MaxInput.AcsmRunning(speedMetresPerMin = 200.0, gradeDecimal = 0.0),
        )
        assertEquals(43.5, est.mlPerKgMin, absoluteTolerance = 1e-9)
    }

    @Test
    fun acsmRunningWithGrade() {
        // S=160, G=0.05 → 3.5 + 32 + 7.2 = 42.7
        val est = Vo2MaxEstimators.estimate(
            Vo2MaxFormula.ACSM_RUNNING,
            Vo2MaxInput.AcsmRunning(160.0, 0.05),
        )
        assertEquals(3.5 + 32.0 + 7.2, est.mlPerKgMin, absoluteTolerance = 1e-9)
    }

    @Test
    fun wrongInputTypeFails() {
        assertFailsWith<IllegalStateException> {
            Vo2MaxEstimators.estimate(
                Vo2MaxFormula.UTH_SORENSEN,
                Vo2MaxInput.Cooper(3.0),
            )
        }
    }

    @Test
    fun allFormulasListed() {
        assertEquals(3, Vo2MaxEstimators.availableFormulas().size)
    }
}
