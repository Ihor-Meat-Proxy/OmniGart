package com.omnigart.activity.hr

import com.omnigart.activity.evidence.Citations
import kotlin.math.roundToInt

/**
 * Zone 2 heart-rate band for easy aerobic running (wellness training guidance).
 *
 * **Default:** % of heart-rate reserve (Karvonen) using the Z2 slice of the
 * five-zone table ([HeartRateZones.DEFAULT_FIVE_ZONE] index 2 → 60–70% HRR).
 * Alternatives: %HRmax Z2 band, or a fraction of lactate-threshold HR (LTHR).
 */
enum class Zone2Method {
    /** HRrest + % × (HRmax − HRrest); default product choice. */
    HRR_KARVONEN,

    /** Fraction of maximal HR (same Z2 cut-points as [HeartRateZones.DEFAULT_FIVE_ZONE]). */
    PERCENT_HR_MAX,

    /** Fraction of measured/estimated lactate-threshold heart rate. */
    LTHR,
}

data class Zone2Profile(
    val method: Zone2Method,
    val lowerBpm: Int,
    val upperBpm: Int,
    /** Inclusive lower / exclusive-or-equal upper depending on caller hysteresis. */
    val lowerFraction: Double,
    val upperFraction: Double,
    val citation: String,
) {
    init {
        require(lowerBpm > 0 && upperBpm > lowerBpm) {
            "Zone 2 band must be a positive bpm interval"
        }
        require(lowerFraction in 0.0..1.0 && upperFraction in 0.0..1.0 && lowerFraction < upperFraction)
    }

    fun contains(bpm: Int, hysteresisBpm: Int = 0): Boolean {
        val pad = hysteresisBpm.coerceAtLeast(0)
        return bpm >= (lowerBpm - pad) && bpm <= (upperBpm + pad)
    }
}

object Zone2 {
    /** Z2 slice of the default five-zone table (60–70%). */
    val DEFAULT_LOWER_FRACTION: Double = HeartRateZones.DEFAULT_FIVE_ZONE[1].lowerFraction
    val DEFAULT_UPPER_FRACTION: Double = HeartRateZones.DEFAULT_FIVE_ZONE[1].upperFraction

    /**
     * LTHR-relative band used when [Zone2Method.LTHR] is selected.
     * Provisional coaching band (see OPEN_QUESTIONS W3); not a medical threshold.
     */
    const val LTHR_LOWER_FRACTION: Double = 0.80
    const val LTHR_UPPER_FRACTION: Double = 0.90

    const val DEFAULT_CITATION: String = Citations.KARVONEN_1957
    const val LTHR_CITATION: String = Citations.SEILER_INTENSITY_2010

    fun profile(
        method: Zone2Method = Zone2Method.HRR_KARVONEN,
        hrMax: Int,
        hrRest: Int,
        lthrBpm: Int? = null,
        lowerFraction: Double = DEFAULT_LOWER_FRACTION,
        upperFraction: Double = DEFAULT_UPPER_FRACTION,
    ): Zone2Profile {
        require(hrMax > 0)
        require(hrRest >= 0 && hrRest < hrMax)
        return when (method) {
            Zone2Method.HRR_KARVONEN -> {
                val lo = HeartRateZones.thresholdBpm(lowerFraction, hrMax, hrRest, ZoneModel.PERCENT_HR_RESERVE)
                val hi = HeartRateZones.thresholdBpm(upperFraction, hrMax, hrRest, ZoneModel.PERCENT_HR_RESERVE)
                Zone2Profile(method, lo, hi, lowerFraction, upperFraction, Citations.KARVONEN_1957)
            }
            Zone2Method.PERCENT_HR_MAX -> {
                val lo = HeartRateZones.thresholdBpm(lowerFraction, hrMax, hrRest, ZoneModel.PERCENT_HR_MAX)
                val hi = HeartRateZones.thresholdBpm(upperFraction, hrMax, hrRest, ZoneModel.PERCENT_HR_MAX)
                Zone2Profile(method, lo, hi, lowerFraction, upperFraction, Citations.KARVONEN_1957)
            }
            Zone2Method.LTHR -> {
                val lthr = requireNotNull(lthrBpm) { "lthrBpm required for LTHR Zone 2" }
                require(lthr > 0)
                val lo = (lthr * LTHR_LOWER_FRACTION).roundToInt()
                val hi = (lthr * LTHR_UPPER_FRACTION).roundToInt()
                Zone2Profile(
                    method,
                    lo,
                    hi.coerceAtLeast(lo + 1),
                    LTHR_LOWER_FRACTION,
                    LTHR_UPPER_FRACTION,
                    Citations.SEILER_INTENSITY_2010,
                )
            }
        }
    }

    /** Convenience: default Karvonen Zone 2 for a user profile. */
    fun karvonen(hrMax: Int, hrRest: Int): Zone2Profile =
        profile(Zone2Method.HRR_KARVONEN, hrMax = hrMax, hrRest = hrRest)
}
