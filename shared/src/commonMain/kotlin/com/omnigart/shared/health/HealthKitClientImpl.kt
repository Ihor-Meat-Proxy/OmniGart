package com.omnigart.shared.health

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Kotlin façade over [HealthKitPlatformApi] (Swift HealthKit on device / fake in tests).
 */
class HealthKitClientImpl(private val api: HealthKitPlatformApi) : HealthKitClient {
    private val mutex = Mutex()
    private val observers = mutableMapOf<String, suspend () -> Unit>()
    private var seq = 0

    override suspend fun authorizationStatus(permissions: Set<HealthPermission>): AuthStatus {
        val (read, write) = permissions.splitReadWrite()
        return api.authorizationStatus(read, write)
    }

    override suspend fun requestAuthorization(permissions: Set<HealthPermission>): AuthStatus {
        val (read, write) = permissions.splitReadWrite()
        return api.requestAuthorization(read, write)
    }

    override suspend fun readHeartRate(range: TimeRange): List<HeartRateSample> = api.queryHeartRate(range.start, range.end)

    override suspend fun writeHeartRate(samples: List<HeartRateSample>) = api.saveHeartRate(samples)

    override suspend fun readRestingHeartRate(range: TimeRange): List<HeartRateSample> = api.queryRestingHeartRate(range.start, range.end)

    override suspend fun writeRestingHeartRate(samples: List<HeartRateSample>) = api.saveRestingHeartRate(samples)

    override suspend fun readHrv(range: TimeRange): List<HrvSample> = api.queryHrv(range.start, range.end)

    override suspend fun writeHrv(samples: List<HrvSample>) = api.saveHrv(samples)

    override suspend fun readSleep(range: TimeRange): List<SleepSession> = api.querySleep(range.start, range.end)

    override suspend fun writeSleep(sessions: List<SleepSession>) = api.saveSleep(sessions)

    override suspend fun readBodyMeasurements(types: Set<HealthDataType>, range: TimeRange): List<BodyMeasurement> =
        api.queryBody(types, range.start, range.end)

    override suspend fun writeBodyMeasurements(measurements: List<BodyMeasurement>) = api.saveBody(measurements)

    override suspend fun readNutrition(range: TimeRange): List<NutritionEntry> = api.queryNutrition(range.start, range.end)

    override suspend fun writeNutrition(entries: List<NutritionEntry>) = api.saveNutrition(entries)

    override suspend fun readWater(range: TimeRange): List<WaterIntake> = api.queryWater(range.start, range.end)

    override suspend fun writeWater(entries: List<WaterIntake>) = api.saveWater(entries)

    override suspend fun readWorkouts(range: TimeRange): List<HealthWorkout> = api.queryWorkouts(range.start, range.end)

    override suspend fun writeWorkouts(workouts: List<HealthWorkout>) = api.saveWorkouts(workouts)

    override suspend fun changes(types: Set<HealthDataType>, previous: HealthChangeToken?): HealthChanges =
        api.changes(types, previous?.value)

    override suspend fun enableBackgroundDelivery(types: Set<HealthDataType>) {
        api.enableBackgroundDelivery(types)
    }

    override suspend fun startObserving(types: Set<HealthDataType>, onChange: suspend () -> Unit): String = mutex.withLock {
        val id = "hk-obs-${seq++}"
        observers[id] = onChange
        api.startObserving(types, id)
        id
    }

    override suspend fun stopObserving(handleId: String) {
        mutex.withLock {
            observers.remove(handleId)
            api.stopObserving(handleId)
        }
    }

    /** Invoked from Swift observer callbacks. */
    suspend fun dispatchObserver(handleId: String) {
        val cb = mutex.withLock { observers[handleId] }
        cb?.invoke()
    }
}

internal fun Set<HealthPermission>.splitReadWrite(): Pair<Set<HealthDataType>, Set<HealthDataType>> {
    val read = mutableSetOf<HealthDataType>()
    val write = mutableSetOf<HealthDataType>()
    forEach { perm ->
        when (perm) {
            HealthPermission.READ_WORKOUT -> read += HealthDataType.WORKOUT
            HealthPermission.WRITE_WORKOUT -> write += HealthDataType.WORKOUT
            HealthPermission.READ_HEART_RATE -> read += HealthDataType.HEART_RATE
            HealthPermission.WRITE_HEART_RATE -> write += HealthDataType.HEART_RATE
            HealthPermission.READ_HRV -> read += HealthDataType.HRV_RMSSD
            HealthPermission.WRITE_HRV -> write += HealthDataType.HRV_RMSSD
            HealthPermission.READ_RESTING_HEART_RATE -> read += HealthDataType.RESTING_HEART_RATE
            HealthPermission.WRITE_RESTING_HEART_RATE -> write += HealthDataType.RESTING_HEART_RATE
            HealthPermission.READ_SLEEP -> read += HealthDataType.SLEEP
            HealthPermission.WRITE_SLEEP -> write += HealthDataType.SLEEP
            HealthPermission.READ_BODY_MEASUREMENTS -> {
                read += HealthDataType.BODY_MASS
                read += HealthDataType.HEIGHT
                read += HealthDataType.BODY_FAT_PERCENT
                read += HealthDataType.WAIST_CIRCUMFERENCE
            }
            HealthPermission.WRITE_BODY_MEASUREMENTS -> {
                write += HealthDataType.BODY_MASS
                write += HealthDataType.HEIGHT
                write += HealthDataType.BODY_FAT_PERCENT
                write += HealthDataType.WAIST_CIRCUMFERENCE
            }
            HealthPermission.READ_NUTRITION -> read += HealthDataType.NUTRITION
            HealthPermission.WRITE_NUTRITION -> write += HealthDataType.NUTRITION
            HealthPermission.READ_WATER -> read += HealthDataType.WATER
            HealthPermission.WRITE_WATER -> write += HealthDataType.WATER
            HealthPermission.READ_HISTORY,
            HealthPermission.READ_BACKGROUND
            -> Unit
            HealthPermission.READ_LOCATION -> read += HealthDataType.LOCATION_WORKOUT
        }
    }
    return read to write
}
