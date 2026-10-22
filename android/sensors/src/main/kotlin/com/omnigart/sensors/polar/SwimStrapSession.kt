package com.omnigart.sensors.polar

import com.omnigart.sensors.health.HeartRateSampleDto
import com.omnigart.sensors.health.HeartRateSink
import com.polar.sdk.api.PolarH10OfflineExerciseApi
import kotlinx.coroutines.flow.toList

/**
 * Android swim-with-strap orchestration over [PolarDeviceAdapter].
 */
class SwimStrapSession(
    private val polar: PolarDeviceAdapter,
    private val sink: HeartRateSink? = null,
) {
    var deviceId: String? = null
        private set
    var exerciseId: String? = null
        private set
    var deckStartEpochMillis: Long? = null
        private set

    suspend fun startOnDeck(
        deviceId: String,
        exerciseId: String,
        nowEpochMillis: Long,
        sampleType: PolarH10OfflineExerciseApi.SampleType =
            PolarH10OfflineExerciseApi.SampleType.HR,
    ) {
        this.deviceId = deviceId
        this.exerciseId = exerciseId
        polar.connect(deviceId)
        polar.startH10Recording(deviceId, exerciseId, sampleType)
        deckStartEpochMillis = nowEpochMillis
    }

    suspend fun finishAndDownload(
        deleteAfterDownload: Boolean = true,
    ): List<HeartRateSampleDto> {
        val id = requireNotNull(deviceId) { "startOnDeck first" }
        polar.connect(id)
        val status = polar.h10RecordingStatus(id)
        if (status.first) {
            polar.stopH10Recording(id)
        }
        val entries = polar.listH10Exercises(id).toList()
        val entry = entries.firstOrNull()
            ?: error("No H10 recordings found after swim")
        val data = polar.fetchH10Exercise(id, entry)
        val start = entry.date?.time
            ?: deckStartEpochMillis
            ?: System.currentTimeMillis()
        // HR offline samples are 1 Hz — map index → epoch.
        val samples = data.samples.mapIndexed { index, bpm ->
            HeartRateSampleDto(
                epochMillis = start + index * 1000L,
                bpm = bpm,
                source = "polar_offline",
            )
        }
        if (deleteAfterDownload) {
            polar.removeH10Exercise(id, entry)
        }
        return samples
    }

    suspend fun mergeAndExport(
        strapSamples: List<HeartRateSampleDto>,
        watchSamples: List<HeartRateSampleDto>,
        offsetMillis: Long,
        workoutStartMillis: Long,
        workoutEndMillis: Long,
        dedupeWindowMillis: Long = 750L,
    ): List<HeartRateSampleDto> {
        val strap = strapSamples.map {
            it.copy(epochMillis = it.epochMillis + offsetMillis)
        }.filter { it.epochMillis in workoutStartMillis..workoutEndMillis }

        val watch = watchSamples.filter {
            it.epochMillis in workoutStartMillis..workoutEndMillis
        }

        val used = BooleanArray(strap.size)
        val merged = mutableListOf<HeartRateSampleDto>()
        for (w in watch) {
            val matchIdx = strap.indices
                .filter { !used[it] }
                .minByOrNull { kotlin.math.abs(strap[it].epochMillis - w.epochMillis) }
            if (matchIdx != null &&
                kotlin.math.abs(strap[matchIdx].epochMillis - w.epochMillis) <= dedupeWindowMillis
            ) {
                merged += strap[matchIdx]
                used[matchIdx] = true
            } else {
                merged += w
            }
        }
        for (i in strap.indices) {
            if (!used[i]) merged += strap[i]
        }
        val sorted = merged.sortedBy { it.epochMillis }
        sink?.writeHeartRate(sorted)
        return sorted
    }
}
