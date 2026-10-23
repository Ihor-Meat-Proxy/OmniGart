package com.omnigart.shared.calc

import com.omnigart.shared.domain.BiologicalSex
import kotlin.test.Test
import kotlin.test.assertEquals

class WaterTargetTest {
    @Test
    fun base_femaleAtReferenceWeight() {
        // 60 kg woman, no training, no heat → 1.6 * 1.0 = 1.6
        assertEquals(
            1.6,
            WaterTarget.litresPerDay(BiologicalSex.FEMALE, 60.0),
            absoluteTolerance = 0.001
        )
    }

    @Test
    fun base_maleAtReferenceWeight() {
        assertEquals(
            2.0,
            WaterTarget.litresPerDay(BiologicalSex.MALE, 70.0),
            absoluteTolerance = 0.001
        )
    }

    @Test
    fun weightFactor_clamped() {
        // Very light woman: factor floor 0.85 → 1.6 * 0.85 = 1.36
        assertEquals(
            1.36,
            WaterTarget.litresPerDay(BiologicalSex.FEMALE, 40.0),
            absoluteTolerance = 0.001
        )
        // Heavy man: factor ceiling 1.25 → 2.0 * 1.25 = 2.5
        assertEquals(
            2.5,
            WaterTarget.litresPerDay(BiologicalSex.MALE, 120.0),
            absoluteTolerance = 0.001
        )
    }

    @Test
    fun trainingAndHeatAndCap() {
        // Man 70 kg + 2h training default sweat + heat → 2.0 + 1.0 + 0.3 = 3.3
        assertEquals(
            3.3,
            WaterTarget.litresPerDay(
                sex = BiologicalSex.MALE,
                weightKg = 70.0,
                trainingHours = 2.0,
                forecastHighCelsius = 30.0
            ),
            absoluteTolerance = 0.001
        )
        // Cap at 4 L
        assertEquals(
            4.0,
            WaterTarget.litresPerDay(
                sex = BiologicalSex.MALE,
                weightKg = 70.0,
                trainingHours = 10.0,
                forecastHighCelsius = 35.0
            ),
            absoluteTolerance = 0.001
        )
    }

    @Test
    fun measuredSweatRateOverridesDefault() {
        // Woman 60 kg + 1h @ 0.8 L/h → 1.6 + 0.8 = 2.4
        assertEquals(
            2.4,
            WaterTarget.litresPerDay(
                sex = BiologicalSex.FEMALE,
                weightKg = 60.0,
                trainingHours = 1.0,
                measuredSweatLitresPerHour = 0.8
            ),
            absoluteTolerance = 0.001
        )
    }
}
