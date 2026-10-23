package com.omnigart.shared.calc

import com.omnigart.shared.domain.BiologicalSex
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BmrTest {
    @Test
    fun mifflinStJeor_male_reference() {
        // 80 kg, 180 cm, 30 y, male → 10*80 + 6.25*180 - 5*30 + 5 = 800 + 1125 - 150 + 5 = 1780
        val bmr = Bmr.mifflinStJeor(80.0, 180.0, 30, BiologicalSex.MALE)
        assertEquals(1780.0, bmr, absoluteTolerance = 0.01)
    }

    @Test
    fun mifflinStJeor_female_reference() {
        // 62 kg, 168 cm, 32 y, female → 10*62 + 6.25*168 - 5*32 - 161 = 620 + 1050 - 160 - 161 = 1349
        val bmr = Bmr.mifflinStJeor(62.0, 168.0, 32, BiologicalSex.FEMALE)
        assertEquals(1349.0, bmr, absoluteTolerance = 0.01)
    }

    @Test
    fun mifflinStJeor_rejectsNonPositive() {
        assertFailsWith<IllegalArgumentException> {
            Bmr.mifflinStJeor(0.0, 170.0, 25, BiologicalSex.MALE)
        }
    }
}
