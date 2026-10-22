package com.omnigart.sensors.hrv

/**
 * RR-interval artefact correction.
 *
 * Primary method: a simplified threshold filter inspired by
 * Lipponen & Tarvainen (2019) — “A robust electrocardiogram-based method for
 * heart rate variability analysis”, Physiol Meas 40(5):055002,
 * doi:10.1088/1361-6579/aabec2.
 *
 * The full Lipponen–Tarvainen classifier uses a two-sided median of the dRR
 * series and quartile-deviation thresholds to label ectopic / missing /
 * misaligned beats. This implementation keeps the same spirit with a
 * documented, testable subset:
 *
 * 1. Drop non-physiologic absolute RR values outside [rrMinMs, rrMaxMs].
 * 2. Relative to a local median of [window] neighbours, drop intervals whose
 *    absolute relative deviation exceeds [relativeThreshold] (default 0.20 =
 *    20 %, a common Kubios-style medium correction threshold).
 * 3. Optionally interpolate dropped samples by linear bridging so the
 *    corrected series length stays usable for SDNN / rMSSD windows.
 *
 * Cite Lipponen & Tarvainen (2019) when describing this correction in product
 * copy; document any threshold changes in OPEN_QUESTIONS.
 */
data class ArtefactFilterConfig(
    /** Absolute lower bound for a valid RR (ms). Default 300 ms ≈ 200 bpm. */
    val rrMinMs: Double = 300.0,
    /** Absolute upper bound for a valid RR (ms). Default 2000 ms ≈ 30 bpm. */
    val rrMaxMs: Double = 2000.0,
    /** Half-window (neighbours each side) for local median. */
    val window: Int = 5,
    /** Max allowed |RR − median| / median. */
    val relativeThreshold: Double = 0.20,
    /** When true, replace rejected samples with linear interpolation. */
    val interpolate: Boolean = true,
)

data class ArtefactFilterResult(
    /** Corrected RR series in milliseconds (same length if interpolate=true). */
    val correctedMs: List<Double>,
    /** Indices into the original series that were rejected. */
    val rejectedIndices: List<Int>,
    val acceptedCount: Int,
    val rejectedCount: Int,
)

object RrArtefactFilter {

    fun correct(
        rrMs: List<Double>,
        config: ArtefactFilterConfig = ArtefactFilterConfig(),
    ): ArtefactFilterResult {
        if (rrMs.isEmpty()) {
            return ArtefactFilterResult(emptyList(), emptyList(), 0, 0)
        }

        val rejected = mutableListOf<Int>()
        val keep = BooleanArray(rrMs.size) { true }

        for (i in rrMs.indices) {
            val v = rrMs[i]
            if (v < config.rrMinMs || v > config.rrMaxMs || !v.isFinite()) {
                keep[i] = false
                rejected += i
                continue
            }
            val median = localMedian(rrMs, i, config.window, keepMask = null)
            if (median > 0.0) {
                val rel = kotlin.math.abs(v - median) / median
                if (rel > config.relativeThreshold) {
                    keep[i] = false
                    rejected += i
                }
            }
        }

        val corrected = if (config.interpolate) {
            interpolate(rrMs, keep)
        } else {
            rrMs.filterIndexed { index, _ -> keep[index] }
        }

        val accepted = keep.count { it }
        return ArtefactFilterResult(
            correctedMs = corrected,
            rejectedIndices = rejected,
            acceptedCount = accepted,
            rejectedCount = rejected.size,
        )
    }

    private fun localMedian(
        values: List<Double>,
        index: Int,
        window: Int,
        keepMask: BooleanArray?,
    ): Double {
        val from = (index - window).coerceAtLeast(0)
        val to = (index + window).coerceAtMost(values.lastIndex)
        val sample = mutableListOf<Double>()
        for (i in from..to) {
            if (i == index) continue
            if (keepMask != null && !keepMask[i]) continue
            val v = values[i]
            if (v.isFinite()) sample += v
        }
        if (sample.isEmpty()) return values[index]
        sample.sort()
        val mid = sample.size / 2
        return if (sample.size % 2 == 0) {
            (sample[mid - 1] + sample[mid]) / 2.0
        } else {
            sample[mid]
        }
    }

    private fun interpolate(values: List<Double>, keep: BooleanArray): List<Double> {
        val out = values.toMutableList()
        var i = 0
        while (i < out.size) {
            if (keep[i]) {
                i++
                continue
            }
            var j = i
            while (j < out.size && !keep[j]) j++
            val leftIdx = i - 1
            val rightIdx = if (j < out.size) j else -1
            val leftVal = if (leftIdx >= 0) out[leftIdx] else null
            val rightVal = if (rightIdx >= 0) out[rightIdx] else null
            when {
                leftVal != null && rightVal != null -> {
                    val span = (rightIdx - leftIdx).toDouble()
                    for (k in i until j) {
                        val t = (k - leftIdx) / span
                        out[k] = leftVal + t * (rightVal - leftVal)
                    }
                }
                leftVal != null -> for (k in i until j) out[k] = leftVal
                rightVal != null -> for (k in i until j) out[k] = rightVal
                else -> { /* leave as-is */ }
            }
            i = j
        }
        return out
    }
}
