package com.omnigart.shared.calc

/**
 * Body mass index and WHO adult categories.
 *
 * Reference: World Health Organization. Obesity: preventing and managing the global epidemic.
 * WHO Technical Report Series 894. Geneva: WHO; 2000. (adult cut-offs still used in WHO fact sheets)
 */
enum class WhoBmiCategory {
    UNDERWEIGHT,
    NORMAL,
    OVERWEIGHT,
    OBESE_CLASS_I,
    OBESE_CLASS_II,
    OBESE_CLASS_III
}

object Bmi {
    /** BMI = weight(kg) / height(m)² */
    fun value(weightKg: Double, heightCm: Double): Double {
        require(weightKg > 0 && heightCm > 0)
        val heightM = heightCm / 100.0
        return weightKg / (heightM * heightM)
    }

    fun whoCategory(bmi: Double): WhoBmiCategory {
        require(bmi > 0)
        return when {
            bmi < 18.5 -> WhoBmiCategory.UNDERWEIGHT
            bmi < 25.0 -> WhoBmiCategory.NORMAL
            bmi < 30.0 -> WhoBmiCategory.OVERWEIGHT
            bmi < 35.0 -> WhoBmiCategory.OBESE_CLASS_I
            bmi < 40.0 -> WhoBmiCategory.OBESE_CLASS_II
            else -> WhoBmiCategory.OBESE_CLASS_III
        }
    }

    fun whoCategory(weightKg: Double, heightCm: Double): WhoBmiCategory =
        whoCategory(value(weightKg, heightCm))
}
