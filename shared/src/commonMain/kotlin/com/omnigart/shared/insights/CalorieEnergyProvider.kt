package com.omnigart.shared.insights

/**
 * Seam for the calorie / energy model owned by the parallel logging PR.
 *
 * Phone Insights uses this interface so calorie-based training-load fallback
 * works when that API lands; until then [StubCalorieEnergyProvider] returns null
 * and Insights falls through to duration × RPE.
 */
interface CalorieEnergyProvider {
    /**
     * Estimated active kilocalories for a completed session.
     * Return null when the provider cannot estimate (caller uses next fallback).
     */
    fun estimateActiveKcal(activityKey: String, durationSeconds: Long, weightKg: Double?, averageHeartRateBpm: Double?): Double?
}

/** Default until the calorie-model PR exposes a real implementation. */
class StubCalorieEnergyProvider : CalorieEnergyProvider {
    override fun estimateActiveKcal(activityKey: String, durationSeconds: Long, weightKg: Double?, averageHeartRateBpm: Double?): Double? =
        null
}
