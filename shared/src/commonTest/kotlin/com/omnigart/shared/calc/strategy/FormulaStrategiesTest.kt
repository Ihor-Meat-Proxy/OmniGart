package com.omnigart.shared.calc.strategy

import com.omnigart.shared.domain.BiologicalSex
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FormulaStrategiesTest {
    @Test
    fun defaultsMatchPhase0MifflinAndTanaka() {
        val prefs = FormulaPreferences()
        assertEquals(
            1780.0,
            prefs.bmr.bmrKcal(80.0, 180.0, 30, BiologicalSex.MALE),
            absoluteTolerance = 0.01
        )
        assertEquals(180.0, prefs.hrMax.hrMax(40), absoluteTolerance = 0.01)
    }

    @Test
    fun harrisBenedictAndFoxAreSelectable() {
        val prefs =
            FormulaCatalog.preferencesFromKeys(
                bmrKey = "harris_benedict",
                hrMaxKey = "fox",
                oneRepMaxKey = "brzycki",
                hrZonesKey = "karvonen_3"
            )
        val bmr = prefs.bmr.bmrKcal(80.0, 180.0, 30, BiologicalSex.MALE)
        // 88.362 + 13.397*80 + 4.799*180 - 5.677*30 = 88.362 + 1071.76 + 863.82 - 170.31 = 1853.632
        assertEquals(1853.632, bmr, absoluteTolerance = 0.01)
        assertEquals(180.0, prefs.hrMax.hrMax(40), absoluteTolerance = 0.01)
        assertEquals(112.5, prefs.oneRepMax.e1rm(100.0, 5), absoluteTolerance = 0.001)
        assertEquals(3, prefs.hrZones.zones(60.0, 180.0).size)
    }

    @Test
    fun catalogContainsExpectedKeys() {
        assertTrue(FormulaCatalog.bmrOptions.containsKey("mifflin_st_jeor"))
        assertTrue(FormulaCatalog.hrMaxOptions.containsKey("tanaka"))
        assertTrue(FormulaCatalog.bodyFatOptions.containsKey("cun_bae"))
    }
}
