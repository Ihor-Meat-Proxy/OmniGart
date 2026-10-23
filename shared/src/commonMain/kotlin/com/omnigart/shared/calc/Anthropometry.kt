package com.omnigart.shared.calc

/**
 * Simple anthropometric ratios.
 *
 * Waist-to-height: Ashwell M, Gibson S. Waist-to-height ratio as an indicator of
 * ‘early health risk’. BMJ Open 2016;6:e010159 (guideline discussion of 0.5 cut-off).
 *
 * Waist-to-hip: WHO. Waist Circumference and Waist-Hip Ratio: Report of a WHO Expert
 * Consultation. Geneva: WHO; 2008.
 */
object Anthropometry {
    fun waistToHeightRatio(waistCm: Double, heightCm: Double): Double {
        require(waistCm > 0 && heightCm > 0)
        return waistCm / heightCm
    }

    fun waistToHipRatio(waistCm: Double, hipCm: Double): Double {
        require(waistCm > 0 && hipCm > 0)
        return waistCm / hipCm
    }
}
