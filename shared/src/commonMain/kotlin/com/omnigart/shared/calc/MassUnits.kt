package com.omnigart.shared.calc

import com.omnigart.shared.domain.UnitSystem

/**
 * Display mass unit for volume load and related summaries.
 * Canonical storage remains kilograms everywhere else.
 */
enum class MassUnit {
    KG,
    LB
}

object MassUnits {
    /** International avoirdupois pound. */
    const val KG_PER_LB: Double = 0.45359237

    fun fromUnitSystem(units: UnitSystem): MassUnit = when (units) {
        UnitSystem.METRIC -> MassUnit.KG
        UnitSystem.IMPERIAL -> MassUnit.LB
    }

    fun kgToDisplay(kg: Double, unit: MassUnit): Double = when (unit) {
        MassUnit.KG -> kg
        MassUnit.LB -> kg / KG_PER_LB
    }

    fun displayToKg(value: Double, unit: MassUnit): Double = when (unit) {
        MassUnit.KG -> value
        MassUnit.LB -> value * KG_PER_LB
    }
}
