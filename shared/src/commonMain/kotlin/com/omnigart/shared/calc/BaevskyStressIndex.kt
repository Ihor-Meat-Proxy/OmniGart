package com.omnigart.shared.calc

import kotlin.math.roundToInt

/**
 * Baevsky stress index (SI) from RR intervals.
 *
 * SI = AMo / (2 × Mo × MxDMn)
 *
 * with standard **50 ms** histogram binning:
 * - Mo — mode (centre of the modal bin), in **seconds**
 * - AMo — amplitude of the mode, **percent** of intervals in the modal bin (0–100)
 * - MxDMn — variation range (RR_max − RR_min), in **seconds**
 *
 * Reference framing: Baevsky RM, et al. Methodical recommendations on analysis of heart rate
 * variability under use of different electrocardiographic systems. (Russian cardiology methodology;
 * English summaries commonly cite the SI formula above.) See docs/evidence/citations.json.
 */
data class BaevskyResult(
    val stressIndex: Double,
    val modeSeconds: Double,
    val amplitudePercent: Double,
    val variationRangeSeconds: Double,
    val modalBinStartMs: Int,
    val sampleCount: Int
)

object BaevskyStressIndex {
    const val BIN_WIDTH_MS = 50

    fun fromRrIntervalsMs(rrMs: List<Double>): BaevskyResult {
        require(rrMs.size >= 2) { "Need at least 2 RR intervals" }
        require(rrMs.all { it > 0 }) { "RR intervals must be positive" }

        val min = rrMs.min()
        val max = rrMs.max()
        val bins = LinkedHashMap<Int, Int>()
        rrMs.forEach { rr ->
            val start = ((rr / BIN_WIDTH_MS).toInt()) * BIN_WIDTH_MS
            bins[start] = (bins[start] ?: 0) + 1
        }
        val modalEntry = bins.maxBy { it.value }
        val modalCount = modalEntry.value
        val modalBinStart = modalEntry.key
        val moSeconds = (modalBinStart + BIN_WIDTH_MS / 2.0) / 1000.0
        val amoPercent = 100.0 * modalCount / rrMs.size
        val mxdmnSeconds = (max - min) / 1000.0
        require(mxdmnSeconds > 0) { "MxDMn must be > 0 (all RR identical)" }
        val si = amoPercent / (2.0 * moSeconds * mxdmnSeconds)
        return BaevskyResult(
            stressIndex = si,
            modeSeconds = moSeconds,
            amplitudePercent = amoPercent,
            variationRangeSeconds = mxdmnSeconds,
            modalBinStartMs = modalBinStart,
            sampleCount = rrMs.size
        )
    }

    /** Convenience for integer millisecond RR series. */
    fun fromRrIntervalsMs(rrMs: IntArray): BaevskyResult = fromRrIntervalsMs(rrMs.map { it.toDouble() })

    fun nearestBinStartMs(rrMs: Double): Int = ((rrMs / BIN_WIDTH_MS).roundToInt()) * BIN_WIDTH_MS
}
