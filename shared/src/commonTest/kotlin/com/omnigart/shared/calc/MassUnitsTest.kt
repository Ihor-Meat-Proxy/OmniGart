package com.omnigart.shared.calc

import com.omnigart.shared.domain.UnitSystem
import kotlin.test.Test
import kotlin.test.assertEquals

class MassUnitsTest {
    @Test
    fun kgToLb_roundTrip() {
        val kg = 100.0
        val lb = MassUnits.kgToDisplay(kg, MassUnit.LB)
        assertEquals(220.462, lb, absoluteTolerance = 0.001)
        assertEquals(kg, MassUnits.displayToKg(lb, MassUnit.LB), absoluteTolerance = 0.0001)
    }

    @Test
    fun fromUnitSystem() {
        assertEquals(MassUnit.KG, MassUnits.fromUnitSystem(UnitSystem.METRIC))
        assertEquals(MassUnit.LB, MassUnits.fromUnitSystem(UnitSystem.IMPERIAL))
    }

    @Test
    fun kgIdentity() {
        assertEquals(42.5, MassUnits.kgToDisplay(42.5, MassUnit.KG), absoluteTolerance = 0.0)
    }
}
