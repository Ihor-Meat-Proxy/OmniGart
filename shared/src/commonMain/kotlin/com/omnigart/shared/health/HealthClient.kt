package com.omnigart.shared.health

import kotlin.time.Instant

/**
 * Unified local health store contract used by HealthKit and Health Connect adapters.
 * Google Health API uses [GoogleHealthApiClient] (cloud OAuth) and can sync into the same domain models.
 */
interface HealthClient {
    suspend fun authorizationStatus(permissions: Set<HealthPermission>): AuthStatus

    /** Request the given permissions. Returns the resulting status. */
    suspend fun requestAuthorization(permissions: Set<HealthPermission>): AuthStatus

    suspend fun readHeartRate(range: TimeRange): List<HeartRateSample>

    suspend fun writeHeartRate(samples: List<HeartRateSample>)

    suspend fun readRestingHeartRate(range: TimeRange): List<HeartRateSample>

    suspend fun writeRestingHeartRate(samples: List<HeartRateSample>)

    suspend fun readHrv(range: TimeRange): List<HrvSample>

    suspend fun writeHrv(samples: List<HrvSample>)

    suspend fun readSleep(range: TimeRange): List<SleepSession>

    suspend fun writeSleep(sessions: List<SleepSession>)

    suspend fun readBodyMeasurements(types: Set<HealthDataType>, range: TimeRange): List<BodyMeasurement>

    suspend fun writeBodyMeasurements(measurements: List<BodyMeasurement>)

    suspend fun readNutrition(range: TimeRange): List<NutritionEntry>

    suspend fun writeNutrition(entries: List<NutritionEntry>)

    suspend fun readWater(range: TimeRange): List<WaterIntake>

    suspend fun writeWater(entries: List<WaterIntake>)

    suspend fun readWorkouts(range: TimeRange): List<HealthWorkout>

    /**
     * Append-only workout write. Implementations must not delete or update existing Health /
     * Health Connect records; skip any [HealthWorkout.id] that is already present.
     */
    suspend fun writeWorkouts(workouts: List<HealthWorkout>)

    /** Incremental sync; [previous] null starts a new token. */
    suspend fun changes(types: Set<HealthDataType>, previous: HealthChangeToken?): HealthChanges
}

/** Marker for the Apple HealthKit-backed client (implemented on appleMain + Swift bridge). */
interface HealthKitClient : HealthClient {
    /** Enable background delivery / observer queries for [types]. */
    suspend fun enableBackgroundDelivery(types: Set<HealthDataType>)

    /** Register an observer; returns a cancellation handle id. */
    suspend fun startObserving(types: Set<HealthDataType>, onChange: suspend () -> Unit): String

    suspend fun stopObserving(handleId: String)
}

/** Marker for the Android Health Connect-backed client. */
interface HealthConnectClient : HealthClient {
    /** Persist / restore a Health Connect changes token for [types]. */
    suspend fun getOrCreateChangesToken(types: Set<HealthDataType>): HealthChangeToken

    suspend fun revokeAllPermissions()
}

/**
 * Bridge injected from Swift so Kotlin `HealthKitClientImpl` can call HK APIs.
 * Simulator tests provide [FakeHealthKitPlatformApi] or sample injectors.
 */
interface HealthKitPlatformApi {
    suspend fun requestAuthorization(read: Set<HealthDataType>, write: Set<HealthDataType>): AuthStatus

    suspend fun authorizationStatus(read: Set<HealthDataType>, write: Set<HealthDataType>): AuthStatus

    suspend fun queryHeartRate(start: Instant, end: Instant): List<HeartRateSample>

    suspend fun saveHeartRate(samples: List<HeartRateSample>)

    suspend fun queryRestingHeartRate(start: Instant, end: Instant): List<HeartRateSample>

    suspend fun saveRestingHeartRate(samples: List<HeartRateSample>)

    suspend fun queryHrv(start: Instant, end: Instant): List<HrvSample>

    suspend fun saveHrv(samples: List<HrvSample>)

    suspend fun querySleep(start: Instant, end: Instant): List<SleepSession>

    suspend fun saveSleep(sessions: List<SleepSession>)

    suspend fun queryBody(types: Set<HealthDataType>, start: Instant, end: Instant): List<BodyMeasurement>

    suspend fun saveBody(measurements: List<BodyMeasurement>)

    suspend fun queryNutrition(start: Instant, end: Instant): List<NutritionEntry>

    suspend fun saveNutrition(entries: List<NutritionEntry>)

    suspend fun queryWater(start: Instant, end: Instant): List<WaterIntake>

    suspend fun saveWater(entries: List<WaterIntake>)

    suspend fun queryWorkouts(start: Instant, end: Instant): List<HealthWorkout>

    suspend fun saveWorkouts(workouts: List<HealthWorkout>)

    suspend fun enableBackgroundDelivery(types: Set<HealthDataType>)

    suspend fun startObserving(types: Set<HealthDataType>, callbackId: String)

    suspend fun stopObserving(callbackId: String)

    suspend fun changes(types: Set<HealthDataType>, previous: String?): HealthChanges
}
