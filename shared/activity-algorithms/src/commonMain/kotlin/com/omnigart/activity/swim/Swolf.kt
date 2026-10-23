package com.omnigart.activity.swim

import com.omnigart.activity.evidence.Citations

/**
 * SWOLF (swim golf) = stroke count + elapsed seconds for one pool length.
 * Lower is more efficient. Coaching metric — see [Citations.SWOLF_DEFINITION].
 */
data class SwolfScore(
    val strokes: Int,
    val timeSeconds: Double,
    val score: Double,
    val lengthMetres: Double,
) {
    init {
        require(strokes >= 0)
        require(timeSeconds >= 0.0)
        require(lengthMetres > 0.0)
    }
}

object Swolf {
    const val CITATION: String = Citations.SWOLF_DEFINITION

    fun score(strokes: Int, timeSeconds: Double, lengthMetres: Double = 25.0): SwolfScore {
        require(strokes >= 0) { "strokes must be non-negative" }
        require(timeSeconds >= 0.0) { "time must be non-negative" }
        require(lengthMetres > 0.0) { "length must be positive" }
        return SwolfScore(
            strokes = strokes,
            timeSeconds = timeSeconds,
            score = strokes + timeSeconds,
            lengthMetres = lengthMetres,
        )
    }

    /** Average SWOLF across completed lengths. */
    fun average(scores: List<SwolfScore>): Double? {
        if (scores.isEmpty()) return null
        return scores.sumOf { it.score } / scores.size
    }
}
