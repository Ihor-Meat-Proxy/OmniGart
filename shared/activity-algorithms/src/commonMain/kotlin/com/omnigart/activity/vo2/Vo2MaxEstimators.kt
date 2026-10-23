package com.omnigart.activity.vo2

import com.omnigart.activity.evidence.Citations

/**
 * Selectable VO₂max estimation strategies (ml·kg⁻¹·min⁻¹).
 *
 * - [UTH_SORENSEN] — HR ratio method
 * - [COOPER_12_MIN] — 12-minute run distance
 * - [ACSM_RUNNING] — ACSM running metabolic equation at a given speed/grade
 *   (steady-state oxygen cost; treat as VO₂ demand, not laboratory VO₂max,
 *   unless the bout is known maximal)
 */
enum class Vo2MaxFormula {
    UTH_SORENSEN,
    COOPER_12_MIN,
    ACSM_RUNNING,
}

data class Vo2MaxEstimate(
    val mlPerKgMin: Double,
    val formula: Vo2MaxFormula,
    val citation: String,
)

sealed class Vo2MaxInput {
    data class UthSorensen(val hrMax: Int, val hrRest: Int) : Vo2MaxInput()

    /** Distance covered in a Cooper 12-minute run, kilometres. */
    data class Cooper(val distanceKm: Double) : Vo2MaxInput()

    /**
     * ACSM running equation inputs.
     * @param speedMetresPerMin horizontal speed S (m·min⁻¹)
     * @param gradeDecimal treadmill grade G as decimal (0.01 = 1 %)
     */
    data class AcsmRunning(
        val speedMetresPerMin: Double,
        val gradeDecimal: Double = 0.0,
    ) : Vo2MaxInput()
}

object Vo2MaxEstimators {
    const val UTH_CITATION: String = Citations.UTH_SORENSEN_2004
    const val COOPER_CITATION: String = Citations.COOPER_1968
    const val ACSM_CITATION: String = Citations.ACSM_RUNNING_METABOLIC

    /** Factor from Uth et al. 2004 subgroup calibration. */
    const val UTH_FACTOR: Double = 15.3

    fun estimate(formula: Vo2MaxFormula, input: Vo2MaxInput): Vo2MaxEstimate {
        return when (formula) {
            Vo2MaxFormula.UTH_SORENSEN -> {
                val i = input as? Vo2MaxInput.UthSorensen
                    ?: error("UTH_SORENSEN requires Vo2MaxInput.UthSorensen")
                require(i.hrRest > 0 && i.hrMax > i.hrRest)
                // VO2max ≈ 15.3 × HRmax / HRrest
                Vo2MaxEstimate(
                    mlPerKgMin = UTH_FACTOR * i.hrMax / i.hrRest,
                    formula = formula,
                    citation = UTH_CITATION,
                )
            }
            Vo2MaxFormula.COOPER_12_MIN -> {
                val i = input as? Vo2MaxInput.Cooper
                    ?: error("COOPER_12_MIN requires Vo2MaxInput.Cooper")
                require(i.distanceKm > 0.0)
                // VO2max = 22.351 × km − 11.288
                Vo2MaxEstimate(
                    mlPerKgMin = 22.351 * i.distanceKm - 11.288,
                    formula = formula,
                    citation = COOPER_CITATION,
                )
            }
            Vo2MaxFormula.ACSM_RUNNING -> {
                val i = input as? Vo2MaxInput.AcsmRunning
                    ?: error("ACSM_RUNNING requires Vo2MaxInput.AcsmRunning")
                require(i.speedMetresPerMin > 0.0)
                require(i.gradeDecimal >= 0.0)
                // VO2 = 3.5 + (0.2 × S) + (0.9 × S × G)
                val vo2 = 3.5 + (0.2 * i.speedMetresPerMin) +
                    (0.9 * i.speedMetresPerMin * i.gradeDecimal)
                Vo2MaxEstimate(
                    mlPerKgMin = vo2,
                    formula = formula,
                    citation = ACSM_CITATION,
                )
            }
        }
    }

    fun availableFormulas(): List<Vo2MaxFormula> = Vo2MaxFormula.entries
}
