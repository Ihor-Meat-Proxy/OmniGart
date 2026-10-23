package com.omnigart.shared.calc

import kotlin.test.Test
import kotlin.test.assertEquals

class BmiTest {
    @Test
    fun value_knownReference() {
        // 70 kg / (1.75 m)^2 = 22.857...
        val bmi = Bmi.value(70.0, 175.0)
        assertEquals(22.857, bmi, absoluteTolerance = 0.001)
    }

    @Test
    fun whoCategories_boundaries() {
        assertEquals(WhoBmiCategory.UNDERWEIGHT, Bmi.whoCategory(18.49))
        assertEquals(WhoBmiCategory.NORMAL, Bmi.whoCategory(18.5))
        assertEquals(WhoBmiCategory.NORMAL, Bmi.whoCategory(24.99))
        assertEquals(WhoBmiCategory.OVERWEIGHT, Bmi.whoCategory(25.0))
        assertEquals(WhoBmiCategory.OBESE_CLASS_I, Bmi.whoCategory(30.0))
        assertEquals(WhoBmiCategory.OBESE_CLASS_II, Bmi.whoCategory(35.0))
        assertEquals(WhoBmiCategory.OBESE_CLASS_III, Bmi.whoCategory(40.0))
    }
}
