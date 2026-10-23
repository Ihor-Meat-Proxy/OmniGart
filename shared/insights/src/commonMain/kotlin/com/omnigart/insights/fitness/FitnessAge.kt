package com.omnigart.insights.fitness

import com.omnigart.insights.EvidencedInsight
import com.omnigart.insights.InsightEvidence
import kotlin.math.abs
import kotlin.math.round

enum class FitnessSex {
    FEMALE,
    MALE,
}

data class FitnessAgeInput(
    val chronologicalAgeYears: Int,
    val sex: FitnessSex,
    /** Estimated or measured VO2max in ml·kg⁻¹·min⁻¹. */
    val vo2maxMlKgMin: Double,
)

data class FitnessAgeResult(
    val fitnessAgeYears: Int,
    val chronologicalAgeYears: Int,
    val vo2maxMlKgMin: Double,
    val matchedPercentileLabel: String,
    val note: String,
)

/**
 * Wellness fitness-age estimate by matching VO2max to simplified age–sex median
 * cardiorespiratory fitness norms inspired by FRIEND reference standards (Kaminsky et al.).
 * Tables are intentionally coarse mid-decade medians for transparent wellness use.
 */
object FitnessAge {
    // Approximate 50th-percentile VO2max (ml/kg/min) by age decade midpoints.
    private val maleMedianByAge: List<Pair<Int, Double>> = listOf(
        20 to 48.0,
        30 to 42.4,
        40 to 37.8,
        50 to 33.0,
        60 to 28.5,
        70 to 24.0,
    )
    private val femaleMedianByAge: List<Pair<Int, Double>> = listOf(
        20 to 37.6,
        30 to 30.9,
        40 to 27.0,
        50 to 24.0,
        60 to 21.0,
        70 to 18.0,
    )

    fun compute(input: FitnessAgeInput): EvidencedInsight<FitnessAgeResult> {
        require(input.vo2maxMlKgMin > 0.0) { "VO2max must be positive" }
        require(input.chronologicalAgeYears in 15..90) { "age out of supported wellness range" }
        val table = if (input.sex == FitnessSex.MALE) maleMedianByAge else femaleMedianByAge
        val best = table.minBy { abs(it.second - input.vo2maxMlKgMin) }
        val fitnessAge = best.first
        return EvidencedInsight(
            value = FitnessAgeResult(
                fitnessAgeYears = fitnessAge,
                chronologicalAgeYears = input.chronologicalAgeYears,
                vo2maxMlKgMin = round1(input.vo2maxMlKgMin),
                matchedPercentileLabel = "approx. age-band median CRF norm",
                note = "Fitness age is a wellness comparison to CRF norms, not a medical or biological age.",
            ),
            claimId = InsightEvidence.FITNESS_AGE,
        )
    }

    fun medianVo2ForAge(sex: FitnessSex, ageYears: Int): Double {
        val table = if (sex == FitnessSex.MALE) maleMedianByAge else femaleMedianByAge
        return table.minBy { abs(it.first - ageYears) }.second
    }

    private fun round1(v: Double): Double = round(v * 10.0) / 10.0
}
