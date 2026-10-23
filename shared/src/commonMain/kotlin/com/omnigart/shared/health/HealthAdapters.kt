package com.omnigart.shared.health

import kotlin.time.Instant

/**
 * Platform-agnostic health data contracts.
 * Concrete adapters (HealthKit, Health Connect, Google Health API) live in platform modules later.
 * Google Fit is explicitly out of scope.
 */
interface HeartRateSample {
    val beatsPerMinute: Double
    val recordedAt: Instant
}

interface HealthDataSource {
    suspend fun latestRestingHeartRate(): HeartRateSample?

    suspend fun latestHrvLnRmssd(): Double?
}

/** Placeholder for Phase 1+ HealthKit bridging. */
interface HealthKitAdapter : HealthDataSource

/** Placeholder for Phase 1+ Health Connect bridging. */
interface HealthConnectAdapter : HealthDataSource

/** Placeholder for Phase 1+ Google Health API bridging (not Google Fit). */
interface GoogleHealthApiAdapter : HealthDataSource
