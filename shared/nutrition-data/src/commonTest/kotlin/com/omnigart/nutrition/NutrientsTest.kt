package com.omnigart.nutrition

import com.omnigart.nutrition.model.NutrientBasis
import com.omnigart.nutrition.model.Nutrients
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NutrientsTest {
    @Test
    fun energyDerived_fromKcal() {
        val n = Nutrients(energyKcal = 100.0)
        assertEquals(418.4, n.energyKjOrDerived()!!, absoluteTolerance = 1e-9)
        assertEquals(100.0, n.energyKcalOrDerived()!!)
    }

    @Test
    fun energyDerived_fromKj() {
        val n = Nutrients(energyKj = 418.4)
        assertEquals(100.0, n.energyKcalOrDerived()!!, absoluteTolerance = 1e-9)
    }

    @Test
    fun forServing_scalesFromHundred() {
        val n =
            Nutrients(
                basis = NutrientBasis.PER_100G,
                energyKcal = 200.0,
                proteinsG = 10.0,
                saltG = 1.0,
            )
        val serving = n.forServing(50.0)
        assertEquals(100.0, serving.energyKcal)
        assertEquals(5.0, serving.proteinsG)
        assertEquals(0.5, serving.saltG)
        assertNull(serving.fatG)
    }
}
