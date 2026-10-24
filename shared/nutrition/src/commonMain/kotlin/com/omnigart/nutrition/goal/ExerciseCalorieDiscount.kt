package com.omnigart.nutrition.goal

/**
 * When the user opts to add exercise calories to the target, count only a fraction
 * (about 0.72 by default), scaled lower for higher body fat.
 */
object ExerciseCalorieDiscount {

    const val DEFAULT_FACTOR = 0.72

    /**
     * @param bodyFatPercent optional 0–100; higher body fat lowers the credited fraction.
     */
    fun factor(bodyFatPercent: Double?, base: Double = DEFAULT_FACTOR): Double {
        val b = base.coerceIn(0.0, 1.0)
        if (bodyFatPercent == null) return b
        val bf = bodyFatPercent.coerceIn(0.0, 60.0)
        // Scale: at 15% keep base; at 40% reduce toward ~0.55×base band.
        val scale =
            when {
                bf <= 15.0 -> 1.0
                bf >= 40.0 -> 0.55
                else -> 1.0 - (bf - 15.0) / (40.0 - 15.0) * 0.45
            }
        return (b * scale).coerceIn(0.3, 1.0)
    }

    fun creditedKcal(
        exerciseKcal: Double,
        bodyFatPercent: Double?,
        base: Double = DEFAULT_FACTOR,
    ): Double = exerciseKcal.coerceAtLeast(0.0) * factor(bodyFatPercent, base)
}
