package com.omnigart.shared.calc

import com.omnigart.shared.domain.BiologicalSex

/**
 * Daily drinking-water target heuristic.
 *
 * EFSA base total water: 2.0 L/day women, 2.5 L/day men
 * (EFSA Panel on Dietetic Products, Nutrition and Allergies. Scientific Opinion on Dietary
 * Reference Values for water. EFSA Journal 2010;8(3):1459. DOI: 10.2903/j.efsa.2010.1459).
 *
 * Approximately 80% of total water comes from beverages → drink base 1.6 L (women) / 2.0 L (men).
 *
 * Adjustments (OmniGart Phase 0 heuristic; see docs/OPEN_QUESTIONS.md):
 * - Weight factor: bodyWeight / 60 kg (women) or / 70 kg (men), clamped to [0.85, 1.25]
 * - Training: +0.5 L per training hour, unless [measuredSweatLitresPerHour] is supplied
 * - Heat: +0.3 L if forecast high &gt; 27 °C
 * - Cap: 4.0 L
 */
object WaterTarget {
    const val EFSA_TOTAL_WOMEN_L = 2.0
    const val EFSA_TOTAL_MEN_L = 2.5
    const val DRINK_FRACTION = 0.80
    const val DRINK_BASE_WOMEN_L = 1.6
    const val DRINK_BASE_MEN_L = 2.0
    const val DEFAULT_TRAINING_LITRES_PER_HOUR = 0.5
    const val HEAT_BONUS_L = 0.3
    const val HEAT_THRESHOLD_C = 27.0
    const val CAP_L = 4.0

    fun litresPerDay(
        sex: BiologicalSex,
        weightKg: Double,
        trainingHours: Double = 0.0,
        forecastHighCelsius: Double? = null,
        measuredSweatLitresPerHour: Double? = null
    ): Double {
        require(weightKg > 0)
        require(trainingHours >= 0)
        measuredSweatLitresPerHour?.let { require(it >= 0) }

        val base =
            when (sex) {
                BiologicalSex.FEMALE -> DRINK_BASE_WOMEN_L
                BiologicalSex.MALE -> DRINK_BASE_MEN_L
            }
        val referenceWeight = if (sex == BiologicalSex.FEMALE) 60.0 else 70.0
        val weightFactor = (weightKg / referenceWeight).coerceIn(0.85, 1.25)
        val sweatRate = measuredSweatLitresPerHour ?: DEFAULT_TRAINING_LITRES_PER_HOUR
        val trainingBonus = trainingHours * sweatRate
        val heatBonus =
            if (forecastHighCelsius != null && forecastHighCelsius > HEAT_THRESHOLD_C) {
                HEAT_BONUS_L
            } else {
                0.0
            }
        return (base * weightFactor + trainingBonus + heatBonus).coerceAtMost(CAP_L)
    }
}
