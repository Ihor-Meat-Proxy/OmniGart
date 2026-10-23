package com.omnigart.sensors.merge

import com.omnigart.sensors.model.HeartRateSample
import com.omnigart.sensors.model.HeartRateSource
import com.omnigart.sensors.model.MergedWorkoutHeartRate
import com.omnigart.sensors.model.OfflineRecordingPayload
import com.omnigart.sensors.model.SensorContactStatus
import com.omnigart.sensors.model.SwimWorkoutMarkers
import com.omnigart.sensors.model.WorkoutHeartRateSeries
import kotlin.math.abs
import kotlinx.datetime.Instant

/**
 * Time-aligns a Polar offline / internal recording with a watch/phone swim workout
 * (laps/strokes recorded on the wearable while BLE is unavailable underwater).
 *
 * Strategy:
 * 1. Prefer explicit workout \[startedAt, endedAt\] window.
 * 2. If recording timestamps exist, shift by [alignmentHintMs] (clock skew) then clip.
 * 3. If recording has only relative samples, place them from workout start at 1 Hz (H10 HR).
 */
object OfflineRecordingMerger {
    fun merge(
        markers: SwimWorkoutMarkers,
        recording: OfflineRecordingPayload,
        alignmentHintMs: Long = 0L,
    ): MergedWorkoutHeartRate {
        val notes = mutableListOf<String>()
        val offset = alignmentHintMs

        val hrSamples = buildHrSamples(markers, recording, offset, notes)
        val rr = recording.rrIntervalsMs.map { it.second }

        val series = WorkoutHeartRateSeries(
            workoutId = markers.workoutId,
            deviceId = recording.entry.deviceId,
            source = HeartRateSource.Merged,
            samples = hrSamples,
            rrIntervalsMs = rr,
            startedAt = markers.startedAt,
            endedAt = markers.endedAt,
        )

        return MergedWorkoutHeartRate(
            series = series,
            alignmentOffsetMs = offset,
            matchedSampleCount = hrSamples.size,
            notes = notes,
        )
    }

    /**
     * Estimate clock skew by matching recording start to workout start when both have
     * absolute timestamps; returns recording_start - workout_start in ms.
     */
    fun estimateAlignmentOffsetMs(
        workoutStart: Instant,
        recordingFirstSample: Instant?,
    ): Long {
        if (recordingFirstSample == null) return 0L
        return recordingFirstSample.toEpochMilliseconds() - workoutStart.toEpochMilliseconds()
    }

    private fun buildHrSamples(
        markers: SwimWorkoutMarkers,
        recording: OfflineRecordingPayload,
        offsetMs: Long,
        notes: MutableList<String>,
    ): List<HeartRateSample> {
        val startMs = markers.startedAt.toEpochMilliseconds()
        val endMs = markers.endedAt.toEpochMilliseconds()
        if (endMs <= startMs) {
            notes += "Workout end is not after start; returning empty series"
            return emptyList()
        }

        if (recording.heartRateBpm.isEmpty()) {
            notes += "Offline recording contained no HR samples"
            return emptyList()
        }

        val samples = mutableListOf<HeartRateSample>()
        for ((ts, bpm) in recording.heartRateBpm) {
            val shifted = Instant.fromEpochMilliseconds(ts.toEpochMilliseconds() - offsetMs)
            val t = shifted.toEpochMilliseconds()
            if (t in startMs..endMs) {
                samples += HeartRateSample(
                    timestamp = shifted,
                    bpm = bpm,
                    sensorContact = SensorContactStatus.NotSupported,
                    sourceDeviceId = recording.entry.deviceId,
                )
            }
        }
        if (samples.isEmpty()) {
            notes += "No HR samples fell inside workout window after alignment; stretching from workout start"
            return synthesizeFromStart(markers, recording)
        }
        if (markers.lapEnds.isNotEmpty()) {
            notes += "Lap markers present (${markers.lapEnds.size}); HR series clipped to workout bounds only"
        }
        return samples
    }

    private fun synthesizeFromStart(
        markers: SwimWorkoutMarkers,
        recording: OfflineRecordingPayload,
    ): List<HeartRateSample> {
        val startMs = markers.startedAt.toEpochMilliseconds()
        val endMs = markers.endedAt.toEpochMilliseconds()
        return recording.heartRateBpm.mapIndexedNotNull { index, (_, bpm) ->
            val t = startMs + index * 1000L
            if (t > endMs) null
            else HeartRateSample(
                timestamp = Instant.fromEpochMilliseconds(t),
                bpm = bpm,
                sourceDeviceId = recording.entry.deviceId,
            )
        }
    }

    fun bestOffsetByBoundaryMatch(
        workoutStart: Instant,
        workoutEnd: Instant,
        recordingSamples: List<Pair<Instant, Int>>,
        searchWindowMs: Long = 120_000L,
        stepMs: Long = 500L,
    ): Long {
        if (recordingSamples.size < 2) return 0L
        var best = 0L
        var bestScore = Long.MAX_VALUE
        var offset = -searchWindowMs
        while (offset <= searchWindowMs) {
            val first = recordingSamples.first().first.toEpochMilliseconds() - offset
            val last = recordingSamples.last().first.toEpochMilliseconds() - offset
            val score = abs(first - workoutStart.toEpochMilliseconds()) +
                abs(last - workoutEnd.toEpochMilliseconds())
            if (score < bestScore) {
                bestScore = score
                best = offset
            }
            offset += stepMs
        }
        return best
    }
}
