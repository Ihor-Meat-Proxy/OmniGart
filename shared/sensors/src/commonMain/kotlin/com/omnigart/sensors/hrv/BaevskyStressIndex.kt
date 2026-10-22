package com.omnigart.sensors.hrv

import kotlin.math.roundToInt

/**
 * Baevsky stress index (SI) from an RR-interval series.
 *
 * Formula (Baevsky / “mathematical analysis of heart rhythm” tradition):
 *
 * ```
 * SI = AMo / (2 · Mo · MxDMn)
 * ```
 *
 * where, using a histogram of RR intervals with [binWidthMs] bins (default 50 ms):
 * - **Mo** (mode) — centre of the modal bin, in **seconds**
 * - **AMo** (amplitude of mode) — percentage of intervals falling in the modal bin (0–100)
 * - **MxDMn** (variation range) — RR_max − RR_min, in **seconds**
 *
 * Typical resting SI ≈ 50–150; elevated values indicate sympathetic dominance.
 * Units matter: Mo and MxDMn must be in seconds when AMo is a percentage, otherwise
 * SI is off by orders of magnitude.
 *
 * Reference framing: Baevsky RM, Berseneva AP. Use of heart rate variability
 * analysis in occupational medicine / space medicine literature (SI formula as
 * above). Worked example in tests: Mo=0.80 s, AMo=40 %, MxDMn=0.25 s → SI=100.
 */
data class BaevskyResult(
    val stressIndex: Double,
    val modeSeconds: Double,
    val amplitudeOfModePercent: Double,
    val variationRangeSeconds: Double,
    val binWidthMs: Double,
    val histogram: Map<Int, Int>,
)

object BaevskyStressIndex {
    const val DEFAULT_BIN_WIDTH_MS: Double = 50.0

    fun compute(
        rrMs: List<Double>,
        binWidthMs: Double = DEFAULT_BIN_WIDTH_MS,
    ): BaevskyResult? {
        if (rrMs.size < 2) return null
        require(binWidthMs > 0.0) { "binWidthMs must be positive" }
        require(rrMs.all { it.isFinite() && it > 0.0 }) {
            "RR intervals must be finite and positive"
        }

        val min = rrMs.min()
        val max = rrMs.max()
        val mxDMnSeconds = (max - min) / 1000.0
        if (mxDMnSeconds <= 0.0) {
            // All intervals identical — variation range is zero; SI undefined.
            return null
        }

        val histogram = linkedMapOf<Int, Int>()
        for (rr in rrMs) {
            val bin = (rr / binWidthMs).toInt()
            histogram[bin] = (histogram[bin] ?: 0) + 1
        }

        val modalEntry = histogram.maxBy { it.value }
        val modalBin = modalEntry.key
        val modalCount = modalEntry.value

        // Mode = centre of modal bin (ms → s).
        val modeMs = (modalBin + 0.5) * binWidthMs
        val modeSeconds = modeMs / 1000.0

        val amoPercent = 100.0 * modalCount / rrMs.size

        val si = amoPercent / (2.0 * modeSeconds * mxDMnSeconds)

        return BaevskyResult(
            stressIndex = si,
            modeSeconds = modeSeconds,
            amplitudeOfModePercent = amoPercent,
            variationRangeSeconds = mxDMnSeconds,
            binWidthMs = binWidthMs,
            histogram = histogram.toSortedMap(),
        )
    }

    /**
     * Direct SI from already-derived Mo / AMo / MxDMn (seconds / percent / seconds).
     * Useful for verifying published worked examples.
     */
    fun fromComponents(
        modeSeconds: Double,
        amplitudeOfModePercent: Double,
        variationRangeSeconds: Double,
    ): Double {
        require(modeSeconds > 0.0 && variationRangeSeconds > 0.0) {
            "Mo and MxDMn must be positive"
        }
        return amplitudeOfModePercent / (2.0 * modeSeconds * variationRangeSeconds)
    }

    /** Round SI for display (nearest integer is conventional). */
    fun displayValue(si: Double): Int = si.roundToInt()
}
