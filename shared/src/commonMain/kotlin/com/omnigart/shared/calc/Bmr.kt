package com.omnigart.shared.calc

import com.omnigart.shared.domain.BiologicalSex

/**
 * Basal metabolic rate estimates.
 *
 * Reference: Mifflin MD, St Jeor ST, et al. A new predictive equation for resting energy
 * expenditure in healthy individuals. Am J Clin Nutr. 1990;51(2):241–247.
 * DOI: 10.1093/ajcn/51.2.241
 */
object Bmr {
    /**
     * Mifflin–St Jeor BMR in kilocalories per day.
     *
     * Men:   BMR = 10·weight(kg) + 6.25·height(cm) − 5·age + 5
     * Women: BMR = 10·weight(kg) + 6.25·height(cm) − 5·age − 161
     */
    fun mifflinStJeor(weightKg: Double, heightCm: Double, ageYears: Int, sex: BiologicalSex): Double {
        require(weightKg > 0 && heightCm > 0 && ageYears > 0)
        val base = 10.0 * weightKg + 6.25 * heightCm - 5.0 * ageYears
        return when (sex) {
            BiologicalSex.MALE -> base + 5.0
            BiologicalSex.FEMALE -> base - 161.0
        }
    }
}
