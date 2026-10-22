package com.omnigart.sensors.polar

import com.omnigart.sensors.health.HeartRateSink
import com.omnigart.sensors.merge.ClockOffset
import com.omnigart.sensors.merge.ClockOffsetEstimator
import com.omnigart.sensors.merge.MergedSwimHr
import com.omnigart.sensors.merge.SwimSessionMerge
import com.omnigart.sensors.merge.SwimWorkoutSegment
import com.omnigart.sensors.model.HeartRateSample

/**
 * Orchestrates the “swim session with strap” flow using a [PolarDeviceClient]
 * and optional [HeartRateSink].
 *
 * Steps:
 * 1. [startOnDeck] — connect, start H10 internal recording, note deck timestamp.
 * 2. Athlete swims (BLE may disconnect; recording continues on device).
 * 3. [finishAndDownload] — reconnect, stop if needed, list/download/delete.
 * 4. [mergeAndExport] — align clocks, merge with watch workout, write sink.
 */
class SwimStrapSession(
    private val client: PolarDeviceClient,
    private val sink: HeartRateSink? = null,
) {
    var deviceId: String? = null
        private set
    var exerciseId: String? = null
        private set
    var deckStartEpochMillis: Long? = null
        private set
    var downloaded: OfflineHrRecording? = null
        private set

    suspend fun startOnDeck(
        deviceId: String,
        exerciseId: String,
        sampleType: H10SampleType = H10SampleType.HR,
        nowEpochMillis: Long,
    ) {
        this.deviceId = deviceId
        this.exerciseId = exerciseId
        client.connect(deviceId)
        client.startH10Recording(
            deviceId = deviceId,
            exerciseId = exerciseId,
            sampleType = sampleType,
            interval = H10RecordingInterval.INTERVAL_1_S,
        )
        deckStartEpochMillis = nowEpochMillis
    }

    suspend fun finishAndDownload(
        deleteAfterDownload: Boolean = true,
    ): OfflineHrRecording {
        val id = requireNotNull(deviceId) { "startOnDeck not called" }
        client.connect(id)
        val status = client.h10RecordingStatus(id)
        if (status.ongoing) {
            client.stopH10Recording(id)
        }
        val entries = client.listH10Recordings(id)
        require(entries.isNotEmpty()) { "No H10 recordings found after swim" }
        val entry = entries.first()
        val recording = client.downloadH10Recording(id, entry)
        if (deleteAfterDownload) {
            client.deleteH10Recording(id, entry)
        }
        downloaded = recording
        return recording
    }

    suspend fun mergeAndExport(
        workout: SwimWorkoutSegment,
        offset: ClockOffset? = null,
        watchAnchorEpochMillis: List<Long> = emptyList(),
    ): MergedSwimHr {
        val recording = requireNotNull(downloaded) { "finishAndDownload not called" }
        val strapTimes = recording.samples.map { it.epochMillis }
        val resolved = offset
            ?: ClockOffsetEstimator.estimateFromOverlap(watchAnchorEpochMillis, strapTimes)
            ?: deckStartEpochMillis?.let { deck ->
                // Fallback: assume first strap sample corresponds to deck start.
                val first = strapTimes.minOrNull() ?: deck
                ClockOffsetEstimator.fixed(deck - first)
            }
            ?: ClockOffset(0L)

        val merged = SwimSessionMerge.merge(recording, workout, resolved)
        sink?.writeHeartRate(merged.samples)
        return merged
    }
}
