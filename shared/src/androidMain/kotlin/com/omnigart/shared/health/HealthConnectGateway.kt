package com.omnigart.shared.health

/**
 * Narrow Android gateway so [HealthConnectClientImpl] stays testable without a live HC APK.
 * Production wiring uses [SdkHealthConnectGateway]; tests use [FakeHealthClient] directly.
 */
interface HealthConnectGateway {
    suspend fun getGrantedPermissions(): Set<String>

    fun permissionStrings(permissions: Set<HealthPermission>): Set<String>

    suspend fun readHeartRate(range: TimeRange): List<HeartRateSample>

    suspend fun writeHeartRate(samples: List<HeartRateSample>)

    suspend fun readRestingHeartRate(range: TimeRange): List<HeartRateSample>

    suspend fun writeRestingHeartRate(samples: List<HeartRateSample>)

    suspend fun readHrv(range: TimeRange): List<HrvSample>

    suspend fun writeHrv(samples: List<HrvSample>)

    suspend fun readSleep(range: TimeRange): List<SleepSession>

    suspend fun writeSleep(sessions: List<SleepSession>)

    suspend fun readBody(types: Set<HealthDataType>, range: TimeRange): List<BodyMeasurement>

    suspend fun writeBody(measurements: List<BodyMeasurement>)

    suspend fun readNutrition(range: TimeRange): List<NutritionEntry>

    suspend fun writeNutrition(entries: List<NutritionEntry>)

    suspend fun readWater(range: TimeRange): List<WaterIntake>

    suspend fun writeWater(entries: List<WaterIntake>)

    suspend fun readWorkouts(range: TimeRange): List<HealthWorkout>

    suspend fun writeWorkouts(workouts: List<HealthWorkout>)

    suspend fun getChangesToken(types: Set<HealthDataType>): String

    suspend fun getChanges(token: String): HealthChanges
}
