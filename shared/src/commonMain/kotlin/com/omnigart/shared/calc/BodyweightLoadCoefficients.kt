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
    const val PUSH_UP_INCLINE: Double = 0.55
    const val PUSH_UP_DECLINE: Double = 0.70
    const val PUSH_UP_PIKE: Double = 0.70
    const val PULL_UP: Double = 1.0
    const val DIP: Double = 1.0
    const val INVERTED_ROW: Double = 0.5
    const val SQUAT_BODYWEIGHT: Double = 0.70
    const val LUNGE: Double = 0.85
    const val STEP_UP: Double = 0.90
    const val HIP_THRUST: Double = 1.0
    const val CALF_RAISE: Double = 1.0
    const val CORE_HOLD: Double = 1.0
    const val NORDIC: Double = 0.70
    const val DEFAULT: Double = 1.0

    private val byExerciseId: Map<String, Double> =
        mapOf(
            "push-up" to PUSH_UP,
            "diamond-push-up" to PUSH_UP,
            "wide-push-up" to PUSH_UP,
            "knee-push-up" to 0.50,
            "incline-push-up" to PUSH_UP_INCLINE,
            "decline-push-up" to PUSH_UP_DECLINE,
            "pike-push-up" to PUSH_UP_PIKE,
            "pull-up" to PULL_UP,
            "chin-up" to PULL_UP,
            "chest-to-bar-pull-up" to PULL_UP,
            "weighted-pull-up" to PULL_UP,
            "assisted-pull-up-band" to PULL_UP,
            "scapular-pull-up" to PULL_UP,
            "negative-pull-up" to PULL_UP,
            "archer-pull-up" to PULL_UP,
            "typewriter-pull-up" to PULL_UP,
            "dip-parallel-bar" to DIP,
            "ring-dip" to DIP,
            "weighted-dip" to DIP,
            "negative-dip" to DIP,
            "band-assisted-dip" to DIP,
            "bench-dip" to 0.55,
            "bench-dip-assisted" to 0.55,
            "inverted-row" to INVERTED_ROW,
            "bodyweight-squat" to SQUAT_BODYWEIGHT,
            "jump-squat" to SQUAT_BODYWEIGHT,
            "wall-sit" to SQUAT_BODYWEIGHT,
            "split-squat-bodyweight" to LUNGE,
            "walking-lunge" to LUNGE,
            "reverse-lunge-bodyweight" to LUNGE,
            "lateral-lunge" to LUNGE,
            "step-up" to STEP_UP,
            "glute-bridge" to HIP_THRUST,
            "hip-thrust-bodyweight" to HIP_THRUST,
            "single-leg-hip-thrust" to HIP_THRUST,
            "calf-raise-standing" to CALF_RAISE,
            "calf-raise-bodyweight" to CALF_RAISE,
            "plank" to CORE_HOLD,
            "side-plank" to CORE_HOLD,
            "side-plank-hip-abduction" to CORE_HOLD,
            "hollow-hold" to CORE_HOLD,
            "burpee" to DEFAULT,
            "mountain-climber" to CORE_HOLD,
            "nordic-curl" to NORDIC,
            "nordic-curl-assisted" to NORDIC
        )

    fun forExerciseId(exerciseId: String): Double = byExerciseId[exerciseId] ?: DEFAULT

    fun isKnownBodyweightMovement(exerciseId: String): Boolean = exerciseId in byExerciseId
}
