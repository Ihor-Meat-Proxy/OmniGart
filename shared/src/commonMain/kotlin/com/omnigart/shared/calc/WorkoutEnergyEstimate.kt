package com.omnigart.shared.calc

import com.omnigart.shared.domain.BiologicalSex

/**
 * Wellness energy estimates for workouts when HealthKit has no measured samples.
 *
 * Cardio / yoga / pilates still use Compendium MET: `MET × body mass (kg) × duration (h)`.
 * Strength prefers [ResistanceWorkoutEnergy] (Keytel HR or work-based); [strengthBreakdown]
 * without sets keeps a MET fallback for empty sessions.
 * Basal kcal is the Mifflin–St Jeor daily BMR prorated over the same window.
 *
 * These figures are coaching aids for Apple Fitness / Health — not medical advice.
 * See `docs/CALORIE_MODEL.md`.
 */
object WorkoutEnergyEstimate {
    /** Compendium MET for general / traditional strength training (moderate effort). Flat fallback only. */
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

    /**
     * Strength energy. Prefer [ResistanceWorkoutEnergy.estimate] when sets / HR are available.
     * Flat [met] remains for empty sessions and backward-compatible call sites.
     */
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

    /** Resistance-aware strength estimate (HR or work-based + EPOC + basal). */
    fun strengthResistanceBreakdown(input: ResistanceEnergyInput): WorkoutEnergyBreakdown = ResistanceWorkoutEnergy.estimate(input)

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
