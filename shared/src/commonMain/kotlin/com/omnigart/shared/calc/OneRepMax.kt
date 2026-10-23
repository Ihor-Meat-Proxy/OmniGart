package com.omnigart.shared.calc

/**
 * Estimated one-repetition maximum (e1RM) formulae.
 *
 * Epley B. Poundage chart. In: Boyd Epley Workout. Lincoln, NE: Body Enterprises; 1985.
 * e1RM = w × (1 + r/30)
 *
 * Brzycki M. Strength testing — predicting a one-rep max from reps-to-fatigue.
 * J Phys Educ Recreat Dance. 1993;64(1):88–90.
 * e1RM = w × 36 / (37 − r)   (valid for r &lt; 37; typically used for ≤10 reps)
 *
 * RIR-based: treat performed reps + RIR as an estimated reps-to-failure, then apply Epley.
 */
object OneRepMax {
    fun epley(weightKg: Double, reps: Int): Double {
        require(weightKg > 0 && reps >= 1)
        if (reps == 1) return weightKg
        return weightKg * (1.0 + reps / 30.0)
    }

    fun brzycki(weightKg: Double, reps: Int): Double {
        require(weightKg > 0 && reps >= 1)
        require(reps < 37) { "Brzycki undefined for reps >= 37" }
        if (reps == 1) return weightKg
        return weightKg * 36.0 / (37.0 - reps)
    }

    /**
     * Estimates 1RM from a set stopped with [rir] reps in reserve, using Epley on
     * estimated failure reps = performed reps + RIR.
     */
    fun fromRir(weightKg: Double, reps: Int, rir: Double): Double {
        require(weightKg > 0 && reps >= 1)
        require(rir in 0.0..10.0)
        val estimatedFailureReps = (reps + rir).toInt().coerceAtLeast(1)
        // Prefer fractional Epley with non-integer effective reps
        val effectiveReps = reps + rir
        if (effectiveReps <= 1.0) return weightKg
        return weightKg * (1.0 + effectiveReps / 30.0)
    }
}
