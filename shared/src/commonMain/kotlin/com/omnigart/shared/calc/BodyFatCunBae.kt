package com.omnigart.shared.calc

import com.omnigart.shared.domain.BiologicalSex

/**
 * CUN-BAE body-fat percentage estimate.
 *
 * Reference: Gómez-Ambrosi J, Silva C, et al. Clinical usefulness of a new equation for
 * estimating body fat. Diabetes Care. 2012;35(2):383–388. DOI: 10.2337/dc11-1334
 *
 * BF% = −44.988 + (0.503 × age) + (10.689 × sex) + (3.172 × BMI) − (0.026 × BMI²)
 *      + (0.181 × BMI × sex) − (0.02 × BMI × age) − (0.005 × BMI² × sex)
 *      + (0.00021 × BMI² × age)
 *
 * where sex = 0 for male, 1 for female (Gómez-Ambrosi et al., 2012).
 */
object BodyFatCunBae {
    fun percent(weightKg: Double, heightCm: Double, ageYears: Int, sex: BiologicalSex): Double {
        require(weightKg > 0 && heightCm > 0 && ageYears > 0)
        val bmi = Bmi.value(weightKg, heightCm)
        val s = if (sex == BiologicalSex.FEMALE) 1.0 else 0.0
        val age = ageYears.toDouble()
        val bmi2 = bmi * bmi
        return -44.988 +
            (0.503 * age) +
            (10.689 * s) +
            (3.172 * bmi) -
            (0.026 * bmi2) +
            (0.181 * bmi * s) -
            (0.02 * bmi * age) -
            (0.005 * bmi2 * s) +
            (0.00021 * bmi2 * age)
    }

    fun percentClamped(
        weightKg: Double,
        heightCm: Double,
        ageYears: Int,
        sex: BiologicalSex
    ): Double = percent(weightKg, heightCm, ageYears, sex).coerceIn(1.0, 75.0)
}
