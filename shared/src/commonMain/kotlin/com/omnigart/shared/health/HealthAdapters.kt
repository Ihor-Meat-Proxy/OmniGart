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

/** HealthKit bridging (platform module; gated by FeatureFlags.healthSyncEnabled). */
interface HealthKitAdapter : HealthDataSource

/** Health Connect bridging (platform module; gated by FeatureFlags.healthSyncEnabled). */
interface HealthConnectAdapter : HealthDataSource

/** Google Health API bridging (not Google Fit; gated by FeatureFlags.healthSyncEnabled). */
interface GoogleHealthApiAdapter : HealthDataSource
