package com.omnigart.sensors.merge

import com.omnigart.sensors.model.HeartRateSample
import com.omnigart.sensors.model.HeartRateSource
import com.omnigart.sensors.polar.H10SampleType
import com.omnigart.sensors.polar.OfflineHrRecording
import com.omnigart.sensors.polar.OfflineRecordingEntry
import com.omnigart.sensors.polar.PolarDeviceType
import com.omnigart.sensors.polar.PolarHrSample
import com.omnigart.sensors.polar.StubPolarDeviceClient
import com.omnigart.sensors.polar.SwimStrapSession
import com.omnigart.sensors.health.RecordingHeartRateSink
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class SwimSessionMergeTest {

    @Test
    fun clockOffsetMedian() {
        val ref = listOf(1_000L, 2_000L, 3_000L)
        val src = listOf(900L, 1_900L, 2_900L) // source 100 ms behind
        val offset = ClockOffsetEstimator.estimateFromOverlap(ref, src)!!
        assertEquals(100L, offset.offsetMillis)
        assertEquals(1_000L, offset.applyToSource(900L))
    }

    @Test
    fun prefersStrapWithinDedupeWindow() {
        val offline = OfflineHrRecording(
            entry = OfflineRecordingEntry(
                id = "ex1",
                path = null,
                sizeBytes = null,
                startedEpochMillis = 1_000L,
                sampleType = H10SampleType.HR,
                deviceType = PolarDeviceType.H10,
            ),
            samples = listOf(
                PolarHrSample(1_000L, 140),
                PolarHrSample(2_000L, 145),
                PolarHrSample(3_000L, 150),
            ),
        )
        val workout = SwimWorkoutSegment(
            startEpochMillis = 1_000L,
            endEpochMillis = 4_000L,
            watchHr = listOf(
                HeartRateSample(1_050L, 120, HeartRateSource.WATCH_WORKOUT),
                HeartRateSample(2_050L, 122, HeartRateSource.WATCH_WORKOUT),
                HeartRateSample(3_500L, 118, HeartRateSource.WATCH_WORKOUT),
            ),
        )
        val merged = SwimSessionMerge.merge(offline, workout, ClockOffset(0L))
        assertEquals(3, merged.strapSampleCount)
        assertTrue(merged.preferredStrapCount >= 2)
        // At ~1000 and ~2000, strap should win
        val near1k = merged.samples.first { it.epochMillis in 900L..1_100L }
        assertEquals(140, near1k.bpm)
        assertEquals(HeartRateSource.POLAR_OFFLINE, near1k.source)
    }

    @Test
    fun swimStrapSessionOrchestration() = runTest {
        val entry = OfflineRecordingEntry(
            id = "swim-1",
            path = null,
            sizeBytes = 128,
            startedEpochMillis = 10_000L,
            sampleType = H10SampleType.HR,
            deviceType = PolarDeviceType.H10,
        )
        val recording = OfflineHrRecording(
            entry = entry,
            samples = (0 until 5).map { i ->
                PolarHrSample(10_000L + i * 1_000L, 130 + i)
            },
        )
        val client = StubPolarDeviceClient(
            devices = listOf(
                com.omnigart.sensors.polar.PolarDeviceInfo(
                    "ABC123", null, "Polar H10", PolarDeviceType.H10,
                ),
            ),
        )
        client.seedRecording(recording)
        val sink = RecordingHeartRateSink()
        val session = SwimStrapSession(client, sink)

        session.startOnDeck("ABC123", "swim-1", nowEpochMillis = 10_000L)
        assertTrue(client.recordingOngoing)
        client.recordingOngoing = false // simulate stop during swim

        // Stub already has recording seeded; finish downloads it
        val downloaded = session.finishAndDownload(deleteAfterDownload = true)
        assertEquals(5, downloaded.samples.size)
        assertTrue(client.listH10Recordings("ABC123").isEmpty())

        val workout = SwimWorkoutSegment(
            startEpochMillis = 10_000L,
            endEpochMillis = 15_000L,
            watchHr = listOf(
                HeartRateSample(10_000L, 100, HeartRateSource.WATCH_WORKOUT),
                HeartRateSample(12_000L, 105, HeartRateSource.WATCH_WORKOUT),
            ),
        )
        val merged = session.mergeAndExport(workout, offset = ClockOffset(0L))
        assertTrue(merged.samples.isNotEmpty())
        assertEquals(merged.samples.size, sink.allSamples.size)
    }
}
