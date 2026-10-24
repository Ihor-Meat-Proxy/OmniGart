package com.omnigart.shared.calc

import com.omnigart.shared.domain.BiologicalSex

/**
 * Wellness energy estimates for workouts when HealthKit has no measured samples.
 *
 * Active kcal uses the Compendium MET model: `MET × body mass (kg) × duration (h)`.
 * Basal kcal is the Mifflin–St Jeor daily BMR prorated over the same window.
 *
 * These figures are coaching aids for Apple Fitness / Health — not medical advice.
 */
object WorkoutEnergyEstimate {
    /** Compendium MET for general / traditional strength training (moderate effort). */
    const val STRENGTH_MET: Double = 5.0

    /** Hatha / general yoga (Compendium ~2.5). */
    const val YOGA_MET: Double = 2.5

    /** Mat pilates (Compendium ~3.0). */
    const val PILATES_MET: Double = 3.0

    /**
     * Active energy in kilocalories.
     *
     * @param met metabolic equivalent for the activity
     * @param weightKg body mass in kilograms
     * @param durationHours session length in hours (≥ 0)
     */
    fun activeKcal(met: Double, weightKg: Double, durationHours: Double): Double {
        require(met > 0.0) { "met must be positive" }
        require(weightKg > 0.0) { "weightKg must be positive" }
        require(durationHours >= 0.0) { "durationHours must be non-negative" }
        return met * weightKg * durationHours
    }

    /** Basal kilocalories for [durationHours] from a daily BMR. */
    fun basalKcal(bmrKcalPerDay: Double, durationHours: Double): Double {
        require(bmrKcalPerDay > 0.0) { "bmrKcalPerDay must be positive" }
        require(durationHours >= 0.0) { "durationHours must be non-negative" }
        return bmrKcalPerDay * (durationHours / 24.0)
    }

    fun totalKcal(activeKcal: Double, basalKcal: Double): Double {
        require(activeKcal >= 0.0 && basalKcal >= 0.0)
        return activeKcal + basalKcal
    }

    fun breakdown(
        met: Double,
        weightKg: Double,
        heightCm: Double,
        ageYears: Int,
        sex: BiologicalSex,
        durationHours: Double
    ): WorkoutEnergyBreakdown {
        val active = activeKcal(met = met, weightKg = weightKg, durationHours = durationHours)
        val bmr = Bmr.mifflinStJeor(weightKg, heightCm, ageYears, sex)
        val basal = basalKcal(bmr, durationHours)
        return WorkoutEnergyBreakdown(
            activeKcal = active,
            basalKcal = basal,
            totalKcal = totalKcal(active, basal)
        )
    }

    fun strengthBreakdown(
        weightKg: Double,
        heightCm: Double,
        ageYears: Int,
        sex: BiologicalSex,
        durationHours: Double,
        met: Double = STRENGTH_MET
    ): WorkoutEnergyBreakdown = breakdown(
        met = met,
        weightKg = weightKg,
        heightCm = heightCm,
        ageYears = ageYears,
        sex = sex,
        durationHours = durationHours
    )

    fun yogaBreakdown(
        weightKg: Double,
        heightCm: Double,
        ageYears: Int,
        sex: BiologicalSex,
        durationHours: Double
    ): WorkoutEnergyBreakdown = breakdown(YOGA_MET, weightKg, heightCm, ageYears, sex, durationHours)

    fun pilatesBreakdown(
        weightKg: Double,
        heightCm: Double,
        ageYears: Int,
        sex: BiologicalSex,
        durationHours: Double
    ): WorkoutEnergyBreakdown = breakdown(PILATES_MET, weightKg, heightCm, ageYears, sex, durationHours)
}

data class WorkoutEnergyBreakdown(val activeKcal: Double, val basalKcal: Double, val totalKcal: Double)
