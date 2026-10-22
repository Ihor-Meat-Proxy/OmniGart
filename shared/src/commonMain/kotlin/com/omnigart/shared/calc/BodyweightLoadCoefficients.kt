package com.omnigart.shared.calc

/**
 * Provisional bodyweight → external-load equivalents for volume load.
 *
 * These are product constants for Phase 1 until movement-specific citations are verified
 * (see docs/OPEN_QUESTIONS.md). Do not invent DOIs; refine coefficients when sources exist.
 *
 * Typical interpretation: fraction of body mass supported by the working limbs for a
 * standard variation (e.g. push-up hands/feet on floor).
 */
object BodyweightLoadCoefficients {
    const val PUSH_UP: Double = 0.64
    const val PULL_UP: Double = 1.0
    const val DIP: Double = 1.0
    const val INVERTED_ROW: Double = 0.5
    const val DEFAULT: Double = 1.0

    private val byExerciseId: Map<String, Double> =
        mapOf(
            "push-up" to PUSH_UP,
            "diamond-push-up" to PUSH_UP,
            "incline-push-up" to PUSH_UP,
            "knee-push-up" to PUSH_UP,
            "pull-up" to PULL_UP,
            "chin-up" to PULL_UP,
            "chest-to-bar-pull-up" to PULL_UP,
            "weighted-pull-up" to PULL_UP,
            "assisted-pull-up-band" to PULL_UP,
            "dip-parallel-bar" to DIP,
            "ring-dip" to DIP,
            "bench-dip-assisted" to DIP,
            "inverted-row" to INVERTED_ROW
        )

    fun forExerciseId(exerciseId: String): Double = byExerciseId[exerciseId] ?: DEFAULT
}
