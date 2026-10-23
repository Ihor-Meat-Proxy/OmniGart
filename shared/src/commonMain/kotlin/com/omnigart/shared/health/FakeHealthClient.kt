package com.omnigart.shared.health

import kotlin.time.Instant
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * In-memory [HealthClient] for unit tests and UI previews.
 * Also serves as [HealthKitClient] / [HealthConnectClient] stand-in.
 */
class FakeHealthClient :
    HealthKitClient,
    HealthConnectClient {
    private val mutex = Mutex()
    private val heart = mutableListOf<HeartRateSample>()
    private val resting = mutableListOf<HeartRateSample>()
    private val hrv = mutableListOf<HrvSample>()
    private val sleep = mutableListOf<SleepSession>()
    private val body = mutableListOf<BodyMeasurement>()
    private val nutrition = mutableListOf<NutritionEntry>()
    private val water = mutableListOf<WaterIntake>()
    private val workouts = mutableListOf<HealthWorkout>()
    private val granted = mutableSetOf<HealthPermission>()
    private val observers = mutableMapOf<String, suspend () -> Unit>()
    private var observerSeq = 0
    private var changeSeq = 0
    private var tokenRef: HealthChangeToken? = null

    var backgroundTypes: Set<HealthDataType> = emptySet()
        private set

    suspend fun grant(permissions: Set<HealthPermission>) = mutex.withLock {
        granted += permissions
    }

    override suspend fun authorizationStatus(permissions: Set<HealthPermission>): AuthStatus = mutex.withLock {
        when {
            permissions.isEmpty() -> AuthStatus.AUTHORIZED
            granted.containsAll(permissions) -> AuthStatus.AUTHORIZED
            granted.any { it in permissions } -> AuthStatus.PARTIAL
            granted.isEmpty() -> AuthStatus.NOT_DETERMINED
            else -> AuthStatus.DENIED
        }
    }

    override suspend fun requestAuthorization(permissions: Set<HealthPermission>): AuthStatus = mutex.withLock {
        granted += permissions
        AuthStatus.AUTHORIZED
    }

    override suspend fun readHeartRate(range: TimeRange): List<HeartRateSample> =
        mutex.withLock { heart.filter { it.recordedAt inRange range } }

    override suspend fun writeHeartRate(samples: List<HeartRateSample>) = mutex.withLock {
        heart += samples
        bumpChange()
    }

    override suspend fun readRestingHeartRate(range: TimeRange): List<HeartRateSample> =
        mutex.withLock { resting.filter { it.recordedAt inRange range } }

    override suspend fun writeRestingHeartRate(samples: List<HeartRateSample>) = mutex.withLock {
        resting += samples
        bumpChange()
    }

    override suspend fun readHrv(range: TimeRange): List<HrvSample> = mutex.withLock { hrv.filter { it.recordedAt inRange range } }

    override suspend fun writeHrv(samples: List<HrvSample>) = mutex.withLock {
        hrv += samples
        bumpChange()
    }

    override suspend fun readSleep(range: TimeRange): List<SleepSession> =
        mutex.withLock { sleep.filter { overlaps(it.start, it.end, range) } }

    override suspend fun writeSleep(sessions: List<SleepSession>) = mutex.withLock {
        sleep += sessions
        bumpChange()
    }

    override suspend fun readBodyMeasurements(types: Set<HealthDataType>, range: TimeRange): List<BodyMeasurement> = mutex.withLock {
        body.filter { it.type in types && it.recordedAt inRange range }
    }

    override suspend fun writeBodyMeasurements(measurements: List<BodyMeasurement>) = mutex.withLock {
        body += measurements
        bumpChange()
    }

    override suspend fun readNutrition(range: TimeRange): List<NutritionEntry> =
        mutex.withLock { nutrition.filter { it.recordedAt inRange range } }

    override suspend fun writeNutrition(entries: List<NutritionEntry>) = mutex.withLock {
        nutrition += entries
        bumpChange()
    }

    override suspend fun readWater(range: TimeRange): List<WaterIntake> = mutex.withLock { water.filter { it.recordedAt inRange range } }

    override suspend fun writeWater(entries: List<WaterIntake>) = mutex.withLock {
        water += entries
        bumpChange()
    }

    override suspend fun readWorkouts(range: TimeRange): List<HealthWorkout> =
        mutex.withLock { workouts.filter { overlaps(it.start, it.end, range) } }

    override suspend fun writeWorkouts(workouts: List<HealthWorkout>) = mutex.withLock {
        this.workouts += workouts
        bumpChange()
    }

    override suspend fun changes(types: Set<HealthDataType>, previous: HealthChangeToken?): HealthChanges = mutex.withLock {
        val next = HealthChangeToken(platform = "fake", value = "t-$changeSeq")
        tokenRef = next
        HealthChanges(
            upserts =
            heart.takeLast(5).map {
                HealthRecordRef(
                    it.recordedAt.toString(),
                    HealthDataType.HEART_RATE,
                    it.recordedAt
                )
            },
            nextToken = next
        )
    }

    override suspend fun enableBackgroundDelivery(types: Set<HealthDataType>) {
        backgroundTypes = types
    }

    override suspend fun startObserving(types: Set<HealthDataType>, onChange: suspend () -> Unit): String = mutex.withLock {
        val id = "obs-${observerSeq++}"
        observers[id] = onChange
        id
    }

    override suspend fun stopObserving(handleId: String) {
        mutex.withLock { observers.remove(handleId) }
    }

    override suspend fun getOrCreateChangesToken(types: Set<HealthDataType>): HealthChangeToken = mutex.withLock {
        tokenRef ?: HealthChangeToken("fake", "t-0").also { tokenRef = it }
    }

    override suspend fun revokeAllPermissions() {
        mutex.withLock { granted.clear() }
    }

    /** Test helper: fire all observers. */
    suspend fun emitObservation() {
        val copy = mutex.withLock { observers.values.toList() }
        copy.forEach { it.invoke() }
    }

    private fun bumpChange() {
        changeSeq += 1
    }
}

private infix fun Instant.inRange(range: TimeRange): Boolean = this >= range.start && this <= range.end

private fun overlaps(start: Instant, end: Instant, range: TimeRange): Boolean = start <= range.end && end >= range.start

/** Platform API fake for HealthKit bridge unit tests. */
class FakeHealthKitPlatformApi(private val store: FakeHealthClient = FakeHealthClient()) : HealthKitPlatformApi {
    val client: FakeHealthClient get() = store

    override suspend fun requestAuthorization(read: Set<HealthDataType>, write: Set<HealthDataType>): AuthStatus =
        store.requestAuthorization(
            permissions =
            (read + write).flatMap { it.toPermissions() }.toSet()
        )

    override suspend fun authorizationStatus(read: Set<HealthDataType>, write: Set<HealthDataType>): AuthStatus =
        store.authorizationStatus((read + write).flatMap { it.toPermissions() }.toSet())

    override suspend fun queryHeartRate(start: Instant, end: Instant) = store.readHeartRate(TimeRange(start, end))

    override suspend fun saveHeartRate(samples: List<HeartRateSample>) = store.writeHeartRate(samples)

    override suspend fun queryRestingHeartRate(start: Instant, end: Instant) = store.readRestingHeartRate(TimeRange(start, end))

    override suspend fun saveRestingHeartRate(samples: List<HeartRateSample>) = store.writeRestingHeartRate(samples)

    override suspend fun queryHrv(start: Instant, end: Instant) = store.readHrv(TimeRange(start, end))

    override suspend fun saveHrv(samples: List<HrvSample>) = store.writeHrv(samples)

    override suspend fun querySleep(start: Instant, end: Instant) = store.readSleep(TimeRange(start, end))

    override suspend fun saveSleep(sessions: List<SleepSession>) = store.writeSleep(sessions)

    override suspend fun queryBody(types: Set<HealthDataType>, start: Instant, end: Instant) =
        store.readBodyMeasurements(types, TimeRange(start, end))

    override suspend fun saveBody(measurements: List<BodyMeasurement>) = store.writeBodyMeasurements(measurements)

    override suspend fun queryNutrition(start: Instant, end: Instant) = store.readNutrition(TimeRange(start, end))

    override suspend fun saveNutrition(entries: List<NutritionEntry>) = store.writeNutrition(entries)

    override suspend fun queryWater(start: Instant, end: Instant) = store.readWater(TimeRange(start, end))

    override suspend fun saveWater(entries: List<WaterIntake>) = store.writeWater(entries)

    override suspend fun queryWorkouts(start: Instant, end: Instant) = store.readWorkouts(TimeRange(start, end))

    override suspend fun saveWorkouts(workouts: List<HealthWorkout>) = store.writeWorkouts(workouts)

    override suspend fun enableBackgroundDelivery(types: Set<HealthDataType>) {
        store.enableBackgroundDelivery(types)
    }

    override suspend fun startObserving(types: Set<HealthDataType>, callbackId: String) {
        // no-op in fake; observers are managed at HealthKitClientImpl level
    }

    override suspend fun stopObserving(callbackId: String) = Unit

    override suspend fun changes(types: Set<HealthDataType>, previous: String?): HealthChanges = store.changes(
        types,
        previous?.let { HealthChangeToken("healthkit", it) }
    )
}

private fun HealthDataType.toPermissions(): Set<HealthPermission> = when (this) {
    HealthDataType.WORKOUT -> setOf(
        HealthPermission.READ_WORKOUT,
        HealthPermission.WRITE_WORKOUT
    )
    HealthDataType.HEART_RATE ->
        setOf(HealthPermission.READ_HEART_RATE, HealthPermission.WRITE_HEART_RATE)
    HealthDataType.HRV_RMSSD -> setOf(HealthPermission.READ_HRV, HealthPermission.WRITE_HRV)
    HealthDataType.RESTING_HEART_RATE ->
        setOf(
            HealthPermission.READ_RESTING_HEART_RATE,
            HealthPermission.WRITE_RESTING_HEART_RATE
        )
    HealthDataType.SLEEP -> setOf(HealthPermission.READ_SLEEP, HealthPermission.WRITE_SLEEP)
    HealthDataType.BODY_MASS,
    HealthDataType.HEIGHT,
    HealthDataType.BODY_FAT_PERCENT,
    HealthDataType.WAIST_CIRCUMFERENCE
    ->
        setOf(HealthPermission.READ_BODY_MEASUREMENTS, HealthPermission.WRITE_BODY_MEASUREMENTS)
    HealthDataType.NUTRITION ->
        setOf(HealthPermission.READ_NUTRITION, HealthPermission.WRITE_NUTRITION)
    HealthDataType.WATER -> setOf(HealthPermission.READ_WATER, HealthPermission.WRITE_WATER)
    HealthDataType.LOCATION_WORKOUT -> setOf(HealthPermission.READ_LOCATION)
}
