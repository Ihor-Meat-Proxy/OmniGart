package com.omnigart.sensors.merge

import com.omnigart.sensors.model.OfflineRecordingEntry
import com.omnigart.sensors.model.OfflineRecordingPayload
import com.omnigart.sensors.model.SwimWorkoutMarkers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.datetime.Instant

class OfflineRecordingMergerTest {

    @Test
    fun clipsRecordingToWorkoutWindow() {
        val start = Instant.fromEpochMilliseconds(1_000_000)
        val end = Instant.fromEpochMilliseconds(1_000_000 + 10_000)
        val markers = SwimWorkoutMarkers("w1", start, end, lapEnds = listOf(
            Instant.fromEpochMilliseconds(1_000_000 + 5_000)
        ))
        val recording = OfflineRecordingPayload(
            entry = OfflineRecordingEntry("r1", "polar-h10"),
            heartRateBpm = (0..20).map { i ->
                Instant.fromEpochMilliseconds(1_000_000L + i * 1000) to (120 + i)
            },
        )
        val merged = OfflineRecordingMerger.merge(markers, recording)
        assertEquals(11, merged.matchedSampleCount) // 0..10 inclusive
        assertEquals("w1", merged.series.workoutId)
        assertTrue(merged.notes.any { it.contains("Lap markers") })
    }

    @Test
    fun synthesisesWhenOutsideWindow() {
        val start = Instant.fromEpochMilliseconds(5_000_000)
        val end = Instant.fromEpochMilliseconds(5_010_000)
        val markers = SwimWorkoutMarkers("w2", start, end)
        val recording = OfflineRecordingPayload(
            entry = OfflineRecordingEntry("r2", "polar-h10"),
            heartRateBpm = (0..5).map { i ->
                Instant.fromEpochMilliseconds(1000L + i * 1000) to 130
            },
        )
        val merged = OfflineRecordingMerger.merge(markers, recording)
        assertTrue(merged.matchedSampleCount > 0)
        assertEquals(start, merged.series.samples.first().timestamp)
    }

    @Test
    fun alignmentOffsetEstimate() {
        val workout = Instant.fromEpochMilliseconds(10_000)
        val recording = Instant.fromEpochMilliseconds(10_500)
        assertEquals(500L, OfflineRecordingMerger.estimateAlignmentOffsetMs(workout, recording))
    }
}
