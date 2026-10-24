package com.omnigart.shared.insights

/**
 * ATL/CTL training-load ratio with a soft “sweet spot” band (wellness guide only).
 */
object TrainingLoadRatio {
    const val SWEET_MIN: Double = 0.8
    const val SWEET_MAX: Double = 1.3

    enum class Band {
        LOW,
        SWEET,
        ELEVATED,
        UNKNOWN
    }

    data class Result(
        val ratio: Double?,
        val band: Band,
        /** Short en-GB wellness explanation key suffix / copy for UI. */
        val explanationKey: String
    )

    fun compute(atl: Double, ctl: Double): Result {
        if (ctl < 1e-6) {
            return Result(null, Band.UNKNOWN, "insights_tlr_need_fitness")
        }
        val ratio = atl / ctl
        val band =
            when {
                ratio < SWEET_MIN -> Band.LOW
                ratio <= SWEET_MAX -> Band.SWEET
                else -> Band.ELEVATED
            }
        val key =
            when (band) {
                Band.LOW -> "insights_tlr_low"
                Band.SWEET -> "insights_tlr_sweet"
                Band.ELEVATED -> "insights_tlr_elevated"
                Band.UNKNOWN -> "insights_tlr_need_fitness"
            }
        return Result(ratio, band, key)
    }
}
