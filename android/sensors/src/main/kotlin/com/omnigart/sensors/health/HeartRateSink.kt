package com.omnigart.sensors.health

/**
 * Platform-agnostic sink for writing HR into Health Connect.
 * Concrete `HealthConnectHeartRateSink` lives in the Health adapter module —
 * this library only defines the contract so there is no compile dependency.
 */
interface HeartRateSink {
    suspend fun writeHeartRate(samples: List<HeartRateSampleDto>): Int
}

data class HeartRateSampleDto(
    val epochMillis: Long,
    val bpm: Int,
    val source: String,
)

class NoOpHeartRateSink : HeartRateSink {
    override suspend fun writeHeartRate(samples: List<HeartRateSampleDto>): Int = 0
}

class RecordingHeartRateSink : HeartRateSink {
    private val _writes = mutableListOf<List<HeartRateSampleDto>>()
    val writes: List<List<HeartRateSampleDto>> get() = _writes.toList()

    override suspend fun writeHeartRate(samples: List<HeartRateSampleDto>): Int {
        _writes += samples.toList()
        return samples.size
    }
}
