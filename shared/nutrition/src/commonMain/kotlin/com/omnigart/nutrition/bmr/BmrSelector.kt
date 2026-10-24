package com.omnigart.nutrition.bmr

import com.omnigart.nutrition.BmrMethod
import com.omnigart.nutrition.NutritionSex

/**
 * BMR selection with documented fallbacks.
 *
 * Defaults: Mifflin–St Jeor. Trusted body fat → Katch–McArdle, or Cunningham if athletic.
 * Missing height or age → Henry/Oxford weight-only (labelled rough). No weight → no BMR.
 * Unspecified sex → average of male and female equations.
 */
object BmrSelector {

    data class Result(
        val bmrKcal: Double,
        val method: BmrMethod,
        val roughEstimate: Boolean,
    )

    fun select(
        weightKg: Double?,
        heightCm: Double?,
        ageYears: Int?,
        sex: NutritionSex,
        bodyFatPercent: Double?,
        trustedBodyFat: Boolean,
        athletic: Boolean,
    ): Result? {
        if (weightKg == null || weightKg <= 0.0) return null

        if (trustedBodyFat && bodyFatPercent != null && bodyFatPercent in 1.0..60.0) {
            val lbm = leanBodyMassKg(weightKg, bodyFatPercent)
            return if (athletic) {
                Result(cunningham(lbm), BmrMethod.CUNNINGHAM, roughEstimate = false)
            } else {
                Result(katchMcArdle(lbm), BmrMethod.KATCH_MCARDLE, roughEstimate = false)
            }
        }

        if (heightCm == null || heightCm <= 0.0 || ageYears == null || ageYears <= 0) {
            return Result(
                henryOxfordWeightOnly(weightKg, sex),
                BmrMethod.HENRY_OXFORD_WEIGHT_ONLY,
                roughEstimate = true,
            )
        }

        return Result(
            mifflinForSex(weightKg, heightCm, ageYears, sex),
            BmrMethod.MIFFLIN_ST_JEOR,
            roughEstimate = false,
        )
    }

    fun leanBodyMassKg(weightKg: Double, bodyFatPercent: Double): Double =
        weightKg * (1.0 - bodyFatPercent / 100.0)

    /** Katch–McArdle: BMR = 370 + 21.6 × LBM(kg). */
    fun katchMcArdle(leanBodyMassKg: Double): Double = 370.0 + 21.6 * leanBodyMassKg

    /** Cunningham: BMR = 500 + 22 × LBM(kg). */
    fun cunningham(leanBodyMassKg: Double): Double = 500.0 + 22.0 * leanBodyMassKg

    fun mifflinStJeor(weightKg: Double, heightCm: Double, ageYears: Int, sex: NutritionSex): Double {
        val base = 10.0 * weightKg + 6.25 * heightCm - 5.0 * ageYears
        return when (sex) {
            NutritionSex.MALE -> base + 5.0
            NutritionSex.FEMALE -> base - 161.0
            NutritionSex.UNSPECIFIED -> {
                val male = base + 5.0
                val female = base - 161.0
                (male + female) / 2.0
            }
        }
    }

    private fun mifflinForSex(
        weightKg: Double,
        heightCm: Double,
        ageYears: Int,
        sex: NutritionSex,
    ): Double = mifflinStJeor(weightKg, heightCm, ageYears, sex)

    /**
     * Henry/Oxford adult weight-only approximation (FAO/WHO-aligned family), used only when
     * height or age is missing. Labelled as a rough estimate in the UI.
     */
    fun henryOxfordWeightOnly(weightKg: Double, sex: NutritionSex): Double {
        val male = 14.2 * weightKg + 593.0
        val female = 10.4 * weightKg + 615.0
        return when (sex) {
            NutritionSex.MALE -> male
            NutritionSex.FEMALE -> female
            NutritionSex.UNSPECIFIED -> (male + female) / 2.0
        }
    }
}
