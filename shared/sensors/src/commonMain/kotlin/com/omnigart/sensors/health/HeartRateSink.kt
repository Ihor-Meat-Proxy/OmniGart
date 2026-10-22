package com.omnigart.sensors.health

import com.omnigart.sensors.model.HeartRateSample

/**
 * Platform-agnostic sink for writing heart-rate samples to the OS health store.
 *
 * Implementations (separate Health adapter PR / modules):
 * - iOS: HealthKit `HKQuantityTypeIdentifier.heartRate`
 * - Android: Health Connect `HeartRateRecord`
 *
 * This module deliberately has **no** dependency on HealthKit or Health Connect
 * SDKs. Platform adapters inject a concrete sink.
 */
interface HeartRateSink {
    /**
     * Persist [samples] into the health store.
     * @return number of samples accepted by the store (may be less if filtered).
     */
    suspend fun writeHeartRate(samples: List<HeartRateSample>): Int
}

/**
 * No-op sink for unit tests and hosts without a health store wired yet.
 */
class NoOpHeartRateSink : HeartRateSink {
    override suspend fun writeHeartRate(samples: List<HeartRateSample>): Int = 0
}

/**
 * In-memory sink that records every write — useful for tests and dry-runs.
 */
class RecordingHeartRateSink : HeartRateSink {
    private val _writes = mutableListOf<List<HeartRateSample>>()
    val writes: List<List<HeartRateSample>> get() = _writes.toList()
    val allSamples: List<HeartRateSample> get() = _writes.flatten()

    override suspend fun writeHeartRate(samples: List<HeartRateSample>): Int {
        _writes += samples.toList()
        return samples.size
    }
}
