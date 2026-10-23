package com.omnigart.hydration

import com.omnigart.hydration.norms.DailyNormCalculator
import com.omnigart.hydration.norms.NormMethod
import com.omnigart.hydration.norms.NormProfile
import com.omnigart.hydration.norms.NormSex
import com.omnigart.hydration.norms.WorkoutIntensity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DailyNormTest {

    @Test
    fun efsaDefaultFemaleBeverageTarget() {
        val result =
            DailyNormCalculator.calculate(
                method = NormMethod.EFSA_2010,
                profile = NormProfile(sex = NormSex.FEMALE),
            )
        assertEquals(2000.0, result.baselineTotalWaterMl)
        assertEquals(1600.0, result.baselineBeverageMl)
        assertEquals(1600.0, result.beverageTargetMl)
        assertEquals(NormMethod.EFSA_2010, DailyNormCalculator.defaultMethod())
    }

    @Test
    fun efsaMaleWithPregnancyAndLactationExtras() {
        val result =
            DailyNormCalculator.calculate(
                profile = NormProfile(sex = NormSex.MALE, pregnant = true, lactating = true),
            )
        // Pregnancy/lactation extras apply when opted in regardless of sex enum.
        assertEquals(2000.0 + 300.0 + 700.0, result.beverageTargetMl)
    }

    @Test
    fun iomMaleBaseline() {
        val result =
            DailyNormCalculator.calculate(
                method = NormMethod.IOM_2004,
                profile = NormProfile(sex = NormSex.MALE),
            )
        assertEquals(3700.0, result.baselineTotalWaterMl)
        assertEquals(2960.0, result.baselineBeverageMl)
    }

    @Test
    fun weightHeuristicIsLabelledWeak() {
        val result =
            DailyNormCalculator.calculate(
                method = NormMethod.WEIGHT_HEURISTIC_35ML_KG,
                profile = NormProfile(bodyMassKg = 70.0, sex = NormSex.UNSPECIFIED),
            )
        assertEquals(70.0 * 35.0, result.baselineTotalWaterMl)
        assertTrue(result.notesEnGb.any { it.contains("WEAK", ignoreCase = true) })
        assertTrue(result.methodLabelEnGb.contains("weak", ignoreCase = true))
    }

    @Test
    fun measuredSweatLossFromWeighIn() {
        // Lost 0.5 kg, drank 0.4 L, no urine → sweat 0.9 L
        val ml =
            DailyNormCalculator.measuredSweatLossMl(
                preExerciseMassKg = 80.0,
                postExerciseMassKg = 79.5,
                fluidConsumedDuringMl = 400.0,
            )
        assertEquals(900.0, ml, absoluteTolerance = 1e-9)
    }

    @Test
    fun intensityHeuristicModerateHour() {
        val adj = DailyNormCalculator.estimatedSweatLossMl(60, WorkoutIntensity.MODERATE)
        assertEquals(800.0, adj.sweatLossMl)
    }

    @Test
    fun hyponatraemiaWarnsAboveCeilingAndRate() {
        val norm =
            DailyNormCalculator.calculate(profile = NormProfile(sex = NormSex.FEMALE))
        val assessment =
            DailyNormCalculator.assessHyponatraemiaRisk(
                beverageIntakeMl = 6500.0,
                norm = norm,
                sustainedHourlyIntakeMl = 1200.0,
                duringExerciseIntakeMl = 1000.0,
                duringExerciseSweatLossMl = 400.0,
            )
        assertTrue(assessment.warn)
        assertTrue(assessment.reasons.size >= 2)
        assertTrue(assessment.citation.contains("10.1136/bjsports-2015-095004"))
    }

    @Test
    fun hyponatraemiaQuietWhenWithinTarget() {
        val norm =
            DailyNormCalculator.calculate(profile = NormProfile(sex = NormSex.FEMALE))
        val assessment =
            DailyNormCalculator.assessHyponatraemiaRisk(
                beverageIntakeMl = 1500.0,
                norm = norm,
            )
        assertFalse(assessment.warn)
    }
}
