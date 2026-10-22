package com.omnigart.sensors.merge

import com.omnigart.sensors.model.HeartRateSample
import com.omnigart.sensors.model.HeartRateSource
import com.omnigart.sensors.polar.OfflineHrRecording

/**
 * Merge an H10 (or Verity Sense) offline recording with a watch swim workout
 * by aligning timestamps.
 *
 * Flow modelled:
 * 1. On deck — start H10 internal recording (BLE connected).
 * 2. Swim — BLE drops; H10 keeps recording locally.
 * 3. After — reconnect, download, delete on-device copy.
 * 4. Merge — shift strap samples by [ClockOffset], then combine with watch HR.
 *
 * Conflict policy: when both sources have a sample within [dedupeWindowMillis],
 * prefer the strap ([HeartRateSource.POLAR_OFFLINE]) as the more accurate ECG
 * source during swimming.
 */
data class SwimWorkoutSegment(
    val startEpochMillis: Long,
    val endEpochMillis: Long,
    val watchHr: List<HeartRateSample>,
)

data class MergedSwimHr(
    val samples: List<HeartRateSample>,
    val offsetApplied: ClockOffset,
    val strapSampleCount: Int,
    val watchSampleCount: Int,
    val preferredStrapCount: Int,
)

object SwimSessionMerge {

    fun merge(
        offline: OfflineHrRecording,
        workout: SwimWorkoutSegment,
        offset: ClockOffset,
        dedupeWindowMillis: Long = 750L,
    ): MergedSwimHr {
        val strapAligned = offline.samples.map { s ->
            HeartRateSample(
                epochMillis = offset.applyToSource(s.epochMillis),
                bpm = s.bpm,
                source = HeartRateSource.POLAR_OFFLINE,
                rrIntervalsMs = s.rrMs,
            )
        }.filter {
            it.epochMillis in workout.startEpochMillis..workout.endEpochMillis
        }

        val watchInWindow = workout.watchHr.filter {
            it.epochMillis in workout.startEpochMillis..workout.endEpochMillis
        }.map { it.copy(source = HeartRateSource.WATCH_WORKOUT) }

        val merged = mutableListOf<HeartRateSample>()
        var preferredStrap = 0
        val strapUsed = BooleanArray(strapAligned.size)

        for (w in watchInWindow) {
            val matchIdx = strapAligned.indices
                .filter { !strapUsed[it] }
                .minByOrNull { kotlin.math.abs(strapAligned[it].epochMillis - w.epochMillis) }
            if (matchIdx != null &&
                kotlin.math.abs(strapAligned[matchIdx].epochMillis - w.epochMillis) <= dedupeWindowMillis
            ) {
                merged += strapAligned[matchIdx]
                strapUsed[matchIdx] = true
                preferredStrap++
            } else {
                merged += w
            }
        }

        for (i in strapAligned.indices) {
            if (!strapUsed[i]) merged += strapAligned[i]
        }

        val sorted = merged.sortedBy { it.epochMillis }
            .map { it.copy(source = if (it.source == HeartRateSource.POLAR_OFFLINE ||
                    it.source == HeartRateSource.WATCH_WORKOUT
            ) HeartRateSource.MERGED else it.source) }

        // Preserve provenance for debugging: re-tag from preference path.
        // Actually keep original preferred source before MERGED for callers —
        // use MERGED only as a marker that the list is the combined timeline.
        val withMergedFlag = sorted.map { sample ->
            // Prefer keeping POLAR_OFFLINE / WATCH_WORKOUT provenance on each point.
            sample.copy(
                source = when {
                    strapAligned.any {
                        it.epochMillis == sample.epochMillis && it.bpm == sample.bpm
                    } -> HeartRateSource.POLAR_OFFLINE
                    else -> HeartRateSource.WATCH_WORKOUT
                },
            )
        }

        return MergedSwimHr(
            samples = withMergedFlag.sortedBy { it.epochMillis },
            offsetApplied = offset,
            strapSampleCount = strapAligned.size,
            watchSampleCount = watchInWindow.size,
            preferredStrapCount = preferredStrap,
        )
    }
}
