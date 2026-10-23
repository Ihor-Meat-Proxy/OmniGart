package com.omnigart.shared.calc

import com.omnigart.shared.domain.BiologicalSex
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BodyFatCunBaeTest {
    @Test
    fun percent_male_workedExample() {
        // BMI = 70 / 1.75^2 = 22.857142...
        // Hand-evaluated CUN-BAE for male, age 30:
        val bf = BodyFatCunBae.percent(70.0, 175.0, 30, BiologicalSex.MALE)
        // Hand-evaluated from Gómez-Ambrosi 2012 coefficients at BMI≈22.857
        assertEquals(18.598, bf, absoluteTolerance = 0.02)
    }

    @Test
    fun percent_femaleHigherThanMaleAtSameAnthropometry() {
        val male = BodyFatCunBae.percent(70.0, 175.0, 30, BiologicalSex.MALE)
        val female = BodyFatCunBae.percent(70.0, 175.0, 30, BiologicalSex.FEMALE)
        assertTrue(female > male)
    }
}
