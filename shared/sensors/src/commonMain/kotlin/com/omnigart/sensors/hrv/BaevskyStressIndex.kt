package com.omnigart.sensors.hrv

/**
 * Baevsky stress index (SI) from an RR-interval histogram.
 *
 * Canonical formula (Baevsky / space-medicine literature, widely reproduced):
 *
 * ```
 * SI = AMo / (2 × Mo × MxDMn)
 * ```
 *
 * with:
 * - **Mo** — mode of RR intervals (s), taken as the centre of the modal 50 ms bin
 * - **AMo** — mode amplitude as a **fraction** of all RR intervals (0–1), i.e. count in
 *   the modal bin divided by N (some texts express AMo in percent; then divide by 100
 *   before using the formula so units stay consistent)
 * - **MxDMn** — variation range (s): max(RR) − min(RR)
 * - Histogram bin width **50 ms** (0.05 s), the conventional Baevsky / Kubios choice
 *
 * ### Variants documented here (do not mix them blindly)
 *
 * 1. **Raw SI** — formula above; Mo and MxDMn in seconds; AMo as fraction. Typical resting
 *    values are often tens to low hundreds.
 * 2. **Percent AMo** — if AMo is stored as percent (0–100), use `AMo/100` in the formula
 *    (equivalent to writing `SI = (AMo%) / (2 × Mo × MxDMn)` only when AMo% is divided by 100).
 * 3. **Kubios-style reporting** — Kubios removes a very-low-frequency trend before SI and
 *    reports **√SI** to normalise the skewed distribution
 *    ([Kubios HRV analysis methods](https://www.kubios.com/blog/hrv-analysis-methods/)).
 *    This module exposes [sqrtSi] as an optional transform; it does **not** implement
 *    smoothness-priors detrending (open question for a later PR).
 * 4. **Mo as median** — some implementations substitute the median for the histogram mode;
 *    we keep histogram mode as default and expose [modeAsMedian] for comparison.
 *
 * Primary historical source commonly cited: Baevsky RM, Chernikova AG. Heart rate variability
 * analysis: physiological foundations and main methods. Cardiometry. 2017;(5). Review literature
 * also summarised in Shaffer & Ginsberg 2017 (DOI 10.3389/fpubh.2017.00258).
 */
data class BaevskyResult(
    val si: Double,
    val sqrtSi: Double,
    val amoFraction: Double,
    val moSeconds: Double,
    val mxDMnSeconds: Double,
    val binWidthMs: Double,
    val variant: BaevskyVariant
)

enum class BaevskyVariant {
    HistogramMode,
    MedianAsMode
}

object BaevskyStressIndex {
    const val DEFAULT_BIN_WIDTH_MS: Double = 50.0

    fun compute(rrMs: List<Double>, binWidthMs: Double = DEFAULT_BIN_WIDTH_MS, modeAsMedian: Boolean = false): BaevskyResult {
        require(rrMs.size >= 2) { "Need at least 2 RR intervals" }
        require(rrMs.all { it > 0.0 }) { "RR intervals must be positive" }
        require(binWidthMs > 0.0)

        val minRr = rrMs.min()
        val maxRr = rrMs.max()
        val mxDMnSec = (maxRr - minRr) / 1000.0
        require(mxDMnSec > 0.0) { "MxDMn is zero (all RR equal); SI undefined" }

        val moSeconds: Double
        val amoFraction: Double
        val variant: BaevskyVariant

        if (modeAsMedian) {
            val sorted = rrMs.sorted()
            val median = if (sorted.size % 2 == 0) {
                (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2.0
            } else {
                sorted[sorted.size / 2]
            }
            moSeconds = median / 1000.0
            // Amplitude: fraction of intervals within ±binWidth/2 of median
            val half = binWidthMs / 2.0
            amoFraction = rrMs.count { kotlin.math.abs(it - median) <= half }.toDouble() / rrMs.size
            variant = BaevskyVariant.MedianAsMode
        } else {
            val start = kotlin.math.floor(minRr / binWidthMs) * binWidthMs
            val bins = mutableMapOf<Int, Int>()
            for (rr in rrMs) {
                val idx = ((rr - start) / binWidthMs).toInt()
                bins[idx] = (bins[idx] ?: 0) + 1
            }
            val (modalIdx, modalCount) = bins.maxBy { it.value }
            amoFraction = modalCount.toDouble() / rrMs.size
            moSeconds = (start + (modalIdx + 0.5) * binWidthMs) / 1000.0
            variant = BaevskyVariant.HistogramMode
        }

        require(moSeconds > 0.0)
        val si = amoFraction / (2.0 * moSeconds * mxDMnSec)
        return BaevskyResult(
            si = si,
            sqrtSi = kotlin.math.sqrt(si),
            amoFraction = amoFraction,
            moSeconds = moSeconds,
            mxDMnSeconds = mxDMnSec,
            binWidthMs = binWidthMs,
            variant = variant
        )
    }
}
