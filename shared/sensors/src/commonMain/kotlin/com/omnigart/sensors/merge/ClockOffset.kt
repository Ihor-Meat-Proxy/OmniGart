package com.omnigart.sensors.merge

/**
 * Estimate and apply a constant clock offset between two timelines.
 *
 * Swim-with-strap flow: the H10 offline recording timestamps come from the
 * strap clock (or recording start + sample index × period), while the watch
 * workout uses the watch clock. A single offset (watch − strap) aligns them.
 *
 * [estimateFromOverlap] uses the median of pairwise differences over a short
 * overlap window where both series have samples (e.g. deck start / post-swim
 * reconnect). Callers can also supply a known [fixedOffsetMillis].
 */
data class ClockOffset(
    /** Add this to strap/source timestamps to land on the watch/reference clock. */
    val offsetMillis: Long,
) {
    fun applyToSource(epochMillis: Long): Long = epochMillis + offsetMillis
}

object ClockOffsetEstimator {
    /**
     * Estimate offset = median(ref[i] − src[j]) for nearest-neighbour pairs
     * within [maxPairDeltaMillis].
     */
    fun estimateFromOverlap(
        referenceEpochMillis: List<Long>,
        sourceEpochMillis: List<Long>,
        maxPairDeltaMillis: Long = 5_000L,
    ): ClockOffset? {
        if (referenceEpochMillis.isEmpty() || sourceEpochMillis.isEmpty()) return null

        val diffs = mutableListOf<Long>()
        for (src in sourceEpochMillis) {
            val nearest = referenceEpochMillis.minByOrNull { kotlin.math.abs(it - src) } ?: continue
            val delta = nearest - src
            if (kotlin.math.abs(delta) <= maxPairDeltaMillis) {
                diffs += delta
            }
        }
        if (diffs.isEmpty()) return null
        diffs.sort()
        val mid = diffs.size / 2
        val median = if (diffs.size % 2 == 0) {
            (diffs[mid - 1] + diffs[mid]) / 2
        } else {
            diffs[mid]
        }
        return ClockOffset(median)
    }

    fun fixed(offsetMillis: Long): ClockOffset = ClockOffset(offsetMillis)
}
