package com.omnigart.nutrition.water

import com.omnigart.nutrition.NutritionSex

/**
 * Water goal from body weight plus workouts (wellness heuristic).
 *
 * Base: 2.0 L women / 2.5 L men (EFSA total water), with a weight factor and
 * +0.5 L per training hour (or measured sweat rate when supplied).
 */
object WaterGoal {

    const val BASE_WOMEN_L = 2.0
    const val BASE_MEN_L = 2.5
    const val BASE_UNSPECIFIED_L = 2.25
    const val LITRES_PER_TRAINING_HOUR = 0.5
    const val CAP_L = 4.5

    fun litres(
        sex: NutritionSex,
        weightKg: Double,
        trainingHoursToday: Double = 0.0,
        measuredSweatLitresPerHour: Double? = null,
    ): Double {
        require(weightKg > 0.0)
        val base =
            when (sex) {
                NutritionSex.FEMALE -> BASE_WOMEN_L
                NutritionSex.MALE -> BASE_MEN_L
                NutritionSex.UNSPECIFIED -> BASE_UNSPECIFIED_L
            }
        val ref = if (sex == NutritionSex.FEMALE) 60.0 else 70.0
        val weightFactor = (weightKg / ref).coerceIn(0.85, 1.25)
        val sweat = measuredSweatLitresPerHour ?: LITRES_PER_TRAINING_HOUR
        val training = trainingHoursToday.coerceAtLeast(0.0) * sweat
        return (base * weightFactor + training).coerceAtMost(CAP_L)
    }
}
