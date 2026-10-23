package com.omnigart.nutrition.macros

import com.omnigart.nutrition.NutritionGoalKind

/**
 * Macro targets from energy and body mass.
 *
 * Protein: 1.6 g/kg maintain; 1.6–2.0 gain; 1.8–2.2 lose (goal or reference weight if obese).
 * Fat ≥20% energy and ≥0.5 g/kg. Carbs = remainder, ≥130 g.
 * Fibre 14 g/1000 kcal (25 g minimum). Saturated fat and free sugars guidance &lt;10% (surfaced as copy).
 */
object MacroTargets {

    const val PROTEIN_MAINTAIN = 1.6
    const val PROTEIN_GAIN_MIN = 1.6
    const val PROTEIN_GAIN_MAX = 2.0
    const val PROTEIN_LOSS_MIN = 1.8
    const val PROTEIN_LOSS_MAX = 2.2
    const val FAT_MIN_ENERGY_FRACTION = 0.20
    const val FAT_MIN_G_PER_KG = 0.5
    const val CARBS_MIN_G = 130.0
    const val FIBRE_PER_1000_KCAL = 14.0
    const val FIBRE_MIN_G = 25.0
    const val KCAL_PER_G_PROTEIN = 4.0
    const val KCAL_PER_G_CARBS = 4.0
    const val KCAL_PER_G_FAT = 9.0
    const val OBESE_BMI = 30.0

    data class Result(
        val proteinG: Double,
        val fatG: Double,
        val carbsG: Double,
        val fibreG: Double,
        val referenceWeightKg: Double,
    )

    fun compute(
        energyKcal: Double,
        weightKg: Double,
        heightCm: Double?,
        goalKind: NutritionGoalKind,
        targetWeightKg: Double? = null,
    ): Result {
        val bmi =
            if (heightCm != null && heightCm > 0.0) {
                val m = heightCm / 100.0
                weightKg / (m * m)
            } else {
                null
            }
        val ref =
            when {
                bmi != null && bmi >= OBESE_BMI && targetWeightKg != null -> targetWeightKg
                bmi != null && bmi >= OBESE_BMI -> {
                    // Reference: BMI 25 at same height
                    val m = heightCm!! / 100.0
                    25.0 * m * m
                }
                else -> weightKg
            }

        val proteinPerKg =
            when (goalKind) {
                NutritionGoalKind.MAINTAIN -> PROTEIN_MAINTAIN
                NutritionGoalKind.GAIN -> (PROTEIN_GAIN_MIN + PROTEIN_GAIN_MAX) / 2.0
                NutritionGoalKind.LOSE -> (PROTEIN_LOSS_MIN + PROTEIN_LOSS_MAX) / 2.0
            }
        val proteinG = proteinPerKg * ref
        val proteinKcal = proteinG * KCAL_PER_G_PROTEIN

        val fatFromEnergy = (energyKcal * FAT_MIN_ENERGY_FRACTION) / KCAL_PER_G_FAT
        val fatFromMass = FAT_MIN_G_PER_KG * weightKg
        val fatG = maxOf(fatFromEnergy, fatFromMass)
        val fatKcal = fatG * KCAL_PER_G_FAT

        val remaining = (energyKcal - proteinKcal - fatKcal).coerceAtLeast(0.0)
        var carbsG = remaining / KCAL_PER_G_CARBS
        if (carbsG < CARBS_MIN_G) {
            carbsG = CARBS_MIN_G
        }

        val fibreG = maxOf(FIBRE_MIN_G, energyKcal / 1000.0 * FIBRE_PER_1000_KCAL)

        return Result(
            proteinG = proteinG,
            fatG = fatG,
            carbsG = carbsG,
            fibreG = fibreG,
            referenceWeightKg = ref,
        )
    }
}
