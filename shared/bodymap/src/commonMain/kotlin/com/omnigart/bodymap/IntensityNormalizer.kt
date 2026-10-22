package com.omnigart.bodymap

import kotlin.math.ceil

/**
 * Maps continuous effective-set loads into five intensity levels.
 *
 * Default strategy: equal-width bins relative to the session (or week) maximum.
 * Idle (0) is reserved for zero load. When [max] is 0, everything stays IDLE.
 */
object IntensityNormalizer {
    fun levelFor(value: Double, max: Double): IntensityLevel {
        if (value <= 0.0 || max <= 0.0) return IntensityLevel.IDLE
        val ratio = (value / max).coerceIn(0.0, 1.0)
        // Map (0,1] → 1..5 with equal width bins
        val band = ceil(ratio * 5.0).toInt().coerceIn(1, 5)
        return IntensityLevel.fromLevel(band)
    }

    /**
     * Landmark-relative normalisation for weekly volume heatmaps.
     * ratio ≥ 1.0 → L5, 0 → IDLE, otherwise proportional.
     */
    fun levelForLandmarkRatio(ratio: Double?): IntensityLevel {
        if (ratio == null || ratio <= 0.0) return IntensityLevel.IDLE
        val band = ceil(ratio.coerceIn(0.0, 1.0) * 5.0).toInt().coerceIn(1, 5)
        // Over-landmark still caps at L5 (already coerced)
        return IntensityLevel.fromLevel(if (ratio >= 1.0) 5 else band)
    }
}
