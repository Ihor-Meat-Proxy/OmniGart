package com.omnigart.shared.calc

import com.omnigart.shared.domain.BiologicalSex
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class WorkoutEnergyEstimateTest {
    @Test
    fun activeKcal_metTimesMassTimesHours() {
        // 5 MET × 80 kg × 1 h = 400 kcal
        val kcal = WorkoutEnergyEstimate.activeKcal(met = 5.0, weightKg = 80.0, durationHours = 1.0)
        assertEquals(400.0, kcal, absoluteTolerance = 0.01)
    }

    @Test
    fun activeKcal_scalesWithDuration() {
        val hour = WorkoutEnergyEstimate.activeKcal(5.0, 70.0, 1.0)
        val half = WorkoutEnergyEstimate.activeKcal(5.0, 70.0, 0.5)
        assertEquals(hour / 2.0, half, absoluteTolerance = 0.01)
    }

    @Test
    fun basalKcal_proratesDailyBmr() {
        // 1800 kcal/day × 2 h / 24 = 150
        val basal = WorkoutEnergyEstimate.basalKcal(bmrKcalPerDay = 1800.0, durationHours = 2.0)
        assertEquals(150.0, basal, absoluteTolerance = 0.01)
    }

    @Test
    fun totalKcal_sumsActiveAndBasal() {
        assertEquals(550.0, WorkoutEnergyEstimate.totalKcal(400.0, 150.0), absoluteTolerance = 0.01)
    }

    @Test
    fun strengthBreakdown_usesMifflinAndStrengthMet() {
        // Female 62 kg / 168 cm / 32 y → BMR 1349; 0.5 h session
        val breakdown =
            WorkoutEnergyEstimate.strengthBreakdown(
                weightKg = 62.0,
                heightCm = 168.0,
                ageYears = 32,
                sex = BiologicalSex.FEMALE,
                durationHours = 0.5
            )
        val expectedActive = 5.0 * 62.0 * 0.5 // 155
        val expectedBasal = 1349.0 * (0.5 / 24.0)
        assertEquals(expectedActive, breakdown.activeKcal, absoluteTolerance = 0.01)
        assertEquals(expectedBasal, breakdown.basalKcal, absoluteTolerance = 0.05)
        assertEquals(
            expectedActive + expectedBasal,
            breakdown.totalKcal,
            absoluteTolerance = 0.05
        )
        assertTrue(breakdown.totalKcal > breakdown.activeKcal)
    }

    @Test
    fun activeKcal_rejectsNonPositiveInputs() {
        assertFailsWith<IllegalArgumentException> {
            WorkoutEnergyEstimate.activeKcal(met = 0.0, weightKg = 70.0, durationHours = 1.0)
        }
        assertFailsWith<IllegalArgumentException> {
            WorkoutEnergyEstimate.activeKcal(met = 5.0, weightKg = -1.0, durationHours = 1.0)
        }
        assertFailsWith<IllegalArgumentException> {
            WorkoutEnergyEstimate.activeKcal(met = 5.0, weightKg = 70.0, durationHours = -0.1)
        }
    }
}
