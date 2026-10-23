package com.omnigart.shared.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient as HcSdk
import androidx.health.connect.client.permission.HealthPermission as HcPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.ChangesTokenRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Length
import androidx.health.connect.client.units.Mass
import androidx.health.connect.client.units.Volume
import java.time.Instant as JavaInstant
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Production Health Connect gateway.
 * Device/emulator with Health Connect required — unverified on Linux CI.
 */
class SdkHealthConnectGateway(
    context: Context,
    private val clientProvider: () -> HcSdk = { HcSdk.getOrCreate(context.applicationContext) }
) : HealthConnectGateway {
    private val client get() = clientProvider()

    override suspend fun getGrantedPermissions(): Set<String> =
        withContext(Dispatchers.IO) { client.permissionController.getGrantedPermissions() }

    override fun permissionStrings(permissions: Set<HealthPermission>): Set<String> {
        val out = mutableSetOf<String>()
        permissions.forEach { perm ->
            when (perm) {
                HealthPermission.READ_HEART_RATE ->
                    out += HcPermission.getReadPermission(HeartRateRecord::class)
                HealthPermission.WRITE_HEART_RATE ->
                    out += HcPermission.getWritePermission(HeartRateRecord::class)
                HealthPermission.READ_RESTING_HEART_RATE ->
                    out += HcPermission.getReadPermission(RestingHeartRateRecord::class)
                HealthPermission.WRITE_RESTING_HEART_RATE ->
                    out += HcPermission.getWritePermission(RestingHeartRateRecord::class)
                HealthPermission.READ_HRV ->
                    out += HcPermission.getReadPermission(HeartRateVariabilityRmssdRecord::class)
                HealthPermission.WRITE_HRV ->
                    out += HcPermission.getWritePermission(HeartRateVariabilityRmssdRecord::class)
                HealthPermission.READ_SLEEP ->
                    out += HcPermission.getReadPermission(SleepSessionRecord::class)
                HealthPermission.WRITE_SLEEP ->
                    out += HcPermission.getWritePermission(SleepSessionRecord::class)
                HealthPermission.READ_WATER ->
                    out += HcPermission.getReadPermission(HydrationRecord::class)
                HealthPermission.WRITE_WATER ->
                    out += HcPermission.getWritePermission(HydrationRecord::class)
                HealthPermission.READ_WORKOUT ->
                    out += HcPermission.getReadPermission(ExerciseSessionRecord::class)
                HealthPermission.WRITE_WORKOUT ->
                    out += HcPermission.getWritePermission(ExerciseSessionRecord::class)
                HealthPermission.READ_NUTRITION ->
                    out += HcPermission.getReadPermission(NutritionRecord::class)
                HealthPermission.WRITE_NUTRITION ->
                    out += HcPermission.getWritePermission(NutritionRecord::class)
                HealthPermission.READ_BODY_MEASUREMENTS -> {
                    out += HcPermission.getReadPermission(WeightRecord::class)
                    out += HcPermission.getReadPermission(HeightRecord::class)
                }
                HealthPermission.WRITE_BODY_MEASUREMENTS -> {
                    out += HcPermission.getWritePermission(WeightRecord::class)
                    out += HcPermission.getWritePermission(HeightRecord::class)
                }
                HealthPermission.READ_HISTORY,
                HealthPermission.READ_BACKGROUND,
                HealthPermission.READ_LOCATION
                -> Unit
            }
        }
        return out
    }

    override suspend fun readHeartRate(range: TimeRange): List<HeartRateSample> = withContext(Dispatchers.IO) {
        client.readRecords(
            ReadRecordsRequest(HeartRateRecord::class, range.toFilter())
        ).records.flatMap { record ->
            record.samples.map {
                HeartRateSample(it.beatsPerMinute.toDouble(), it.time.toKotlin())
            }
        }
    }

    override suspend fun writeHeartRate(samples: List<HeartRateSample>) {
        withContext(Dispatchers.IO) {
            val records =
                samples.map { sample ->
                    val t = sample.recordedAt.toJava()
                    HeartRateRecord(
                        startTime = t,
                        startZoneOffset = null,
                        endTime = t.plusMillis(1),
                        endZoneOffset = null,
                        samples =
                        listOf(
                            HeartRateRecord.Sample(t, sample.beatsPerMinute.toLong())
                        ),
                        metadata = Metadata.manualEntry()
                    )
                }
            if (records.isNotEmpty()) client.insertRecords(records)
        }
    }

    override suspend fun readRestingHeartRate(range: TimeRange): List<HeartRateSample> = withContext(Dispatchers.IO) {
        client.readRecords(
            ReadRecordsRequest(RestingHeartRateRecord::class, range.toFilter())
        ).records.map {
            HeartRateSample(it.beatsPerMinute.toDouble(), it.time.toKotlin())
        }
    }

    override suspend fun writeRestingHeartRate(samples: List<HeartRateSample>) {
        withContext(Dispatchers.IO) {
            client.insertRecords(
                samples.map {
                    RestingHeartRateRecord(
                        time = it.recordedAt.toJava(),
                        zoneOffset = null,
                        beatsPerMinute = it.beatsPerMinute.toLong(),
                        metadata = Metadata.manualEntry()
                    )
                }
            )
        }
    }

    override suspend fun readHrv(range: TimeRange): List<HrvSample> = withContext(Dispatchers.IO) {
        client.readRecords(
            ReadRecordsRequest(HeartRateVariabilityRmssdRecord::class, range.toFilter())
        ).records.map {
            HrvSample(it.heartRateVariabilityMillis, it.time.toKotlin())
        }
    }

    override suspend fun writeHrv(samples: List<HrvSample>) {
        withContext(Dispatchers.IO) {
            client.insertRecords(
                samples.map {
                    HeartRateVariabilityRmssdRecord(
                        time = it.recordedAt.toJava(),
                        zoneOffset = null,
                        heartRateVariabilityMillis = it.rmssdMs,
                        metadata = Metadata.manualEntry()
                    )
                }
            )
        }
    }

    override suspend fun readSleep(range: TimeRange): List<SleepSession> = withContext(Dispatchers.IO) {
        client.readRecords(
            ReadRecordsRequest(SleepSessionRecord::class, range.toFilter())
        ).records.map {
            SleepSession(it.metadata.id, it.startTime.toKotlin(), it.endTime.toKotlin())
        }
    }

    override suspend fun writeSleep(sessions: List<SleepSession>) {
        withContext(Dispatchers.IO) {
            client.insertRecords(
                sessions.map {
                    SleepSessionRecord(
                        startTime = it.start.toJava(),
                        startZoneOffset = null,
                        endTime = it.end.toJava(),
                        endZoneOffset = null,
                        metadata = Metadata.manualEntry()
                    )
                }
            )
        }
    }

    override suspend fun readBody(types: Set<HealthDataType>, range: TimeRange): List<BodyMeasurement> = withContext(Dispatchers.IO) {
        val out = mutableListOf<BodyMeasurement>()
        if (HealthDataType.BODY_MASS in types) {
            client.readRecords(ReadRecordsRequest(WeightRecord::class, range.toFilter()))
                .records.forEach {
                    out +=
                        BodyMeasurement(
                            HealthDataType.BODY_MASS,
                            it.weight.inKilograms,
                            MeasurementUnit.KILOGRAM,
                            it.time.toKotlin()
                        )
                }
        }
        if (HealthDataType.HEIGHT in types) {
            client.readRecords(ReadRecordsRequest(HeightRecord::class, range.toFilter()))
                .records.forEach {
                    out +=
                        BodyMeasurement(
                            HealthDataType.HEIGHT,
                            it.height.inMeters * 100.0,
                            MeasurementUnit.CENTIMETRE,
                            it.time.toKotlin()
                        )
                }
        }
        out
    }

    override suspend fun writeBody(measurements: List<BodyMeasurement>) {
        withContext(Dispatchers.IO) {
            val records =
                measurements.mapNotNull { m ->
                    when (m.type) {
                        HealthDataType.BODY_MASS ->
                            WeightRecord(
                                time = m.recordedAt.toJava(),
                                zoneOffset = null,
                                weight = Mass.kilograms(m.value),
                                metadata = Metadata.manualEntry()
                            )
                        HealthDataType.HEIGHT ->
                            HeightRecord(
                                time = m.recordedAt.toJava(),
                                zoneOffset = null,
                                height = Length.meters(m.value / 100.0),
                                metadata = Metadata.manualEntry()
                            )
                        else -> null
                    }
                }
            if (records.isNotEmpty()) client.insertRecords(records)
        }
    }

    override suspend fun readNutrition(range: TimeRange): List<NutritionEntry> = withContext(Dispatchers.IO) {
        client.readRecords(ReadRecordsRequest(NutritionRecord::class, range.toFilter()))
            .records.map {
                NutritionEntry(
                    id = it.metadata.id,
                    recordedAt = it.startTime.toKotlin(),
                    energyKcal = it.energy?.inKilocalories,
                    name = it.name
                )
            }
    }

    override suspend fun writeNutrition(entries: List<NutritionEntry>) {
        // NutritionRecord construction is version-sensitive; device QA follow-up.
    }

    override suspend fun readWater(range: TimeRange): List<WaterIntake> = withContext(Dispatchers.IO) {
        client.readRecords(ReadRecordsRequest(HydrationRecord::class, range.toFilter()))
            .records.map {
                WaterIntake(it.metadata.id, it.volume.inMilliliters, it.startTime.toKotlin())
            }
    }

    override suspend fun writeWater(entries: List<WaterIntake>) {
        withContext(Dispatchers.IO) {
            client.insertRecords(
                entries.map {
                    val t = it.recordedAt.toJava()
                    HydrationRecord(
                        startTime = t,
                        startZoneOffset = null,
                        endTime = t.plusMillis(1),
                        endZoneOffset = null,
                        volume = Volume.milliliters(it.volumeMl),
                        metadata = Metadata.manualEntry()
                    )
                }
            )
        }
    }

    override suspend fun readWorkouts(range: TimeRange): List<HealthWorkout> = withContext(Dispatchers.IO) {
        client.readRecords(ReadRecordsRequest(ExerciseSessionRecord::class, range.toFilter()))
            .records.map {
                val omnigartId =
                    it.metadata.clientRecordId
                        ?.removePrefix("omnigart.workout.")
                        ?.takeIf { id -> id.isNotBlank() }
                HealthWorkout(
                    id = omnigartId ?: it.metadata.id,
                    activityType = activityTypeFor(it.exerciseType),
                    start = it.startTime.toKotlin(),
                    end = it.endTime.toKotlin(),
                    sourceName = it.title
                )
            }
    }

    override suspend fun writeWorkouts(workouts: List<HealthWorkout>) {
        withContext(Dispatchers.IO) {
            // Append-only: never HC record delete/update APIs. Skip when clientRecordId exists.
            // Restore uses a bumped clientRecordVersion after platform deletion.
            val toWrite = mutableListOf<Record>()
            for (workout in workouts) {
                val syncId = "omnigart.workout.${workout.id}"
                if (clientRecordIdExists(syncId, workout.start, workout.end)) continue
                val meta =
                    Metadata.manualEntry(
                        clientRecordId = syncId,
                        clientRecordVersion = workout.clientRecordVersion.coerceAtLeast(1L)
                    )
                val start = workout.start.toJava()
                val end = workout.end.toJava()
                toWrite +=
                    ExerciseSessionRecord(
                        startTime = start,
                        startZoneOffset = null,
                        endTime = end,
                        endZoneOffset = null,
                        exerciseType = exerciseTypeFor(workout.activityType),
                        title = workout.sourceName,
                        metadata = meta
                    )
                workout.energyKcal?.takeIf { it > 0 }?.let { kcal ->
                    toWrite +=
                        ActiveCaloriesBurnedRecord(
                            startTime = start,
                            startZoneOffset = null,
                            endTime = end,
                            endZoneOffset = null,
                            energy = Energy.kilocalories(kcal),
                            metadata = Metadata.manualEntry()
                        )
                }
                workout.distanceMetres?.takeIf { it > 0 }?.let { metres ->
                    toWrite +=
                        DistanceRecord(
                            startTime = start,
                            startZoneOffset = null,
                            endTime = end,
                            endZoneOffset = null,
                            distance = Length.meters(metres),
                            metadata = Metadata.manualEntry()
                        )
                }
                if (workout.heartRateSamples.isNotEmpty()) {
                    val samples =
                        workout.heartRateSamples
                            .filter { it.beatsPerMinute > 0 }
                            .map {
                                HeartRateRecord.Sample(
                                    it.recordedAt.toJava(),
                                    it.beatsPerMinute.toLong()
                                )
                            }
                    if (samples.isNotEmpty()) {
                        toWrite +=
                            HeartRateRecord(
                                startTime = samples.first().time,
                                startZoneOffset = null,
                                endTime = samples.last().time.plusMillis(1),
                                endZoneOffset = null,
                                samples = samples,
                                metadata = Metadata.manualEntry()
                            )
                    }
                }
            }
            if (toWrite.isNotEmpty()) client.insertRecords(toWrite)
        }
    }

    /**
     * True when Health Connect already has an [ExerciseSessionRecord] with [clientRecordId].
     * Used to skip inserts (append-only; never HC update APIs).
     */
    suspend fun clientRecordIdExists(clientRecordId: String, start: Instant, end: Instant): Boolean {
        val padMs = 60_000L
        val from = JavaInstant.ofEpochMilli((start.toEpochMilliseconds() - padMs).coerceAtLeast(0))
        val to = JavaInstant.ofEpochMilli(end.toEpochMilliseconds() + padMs)
        val records =
            client.readRecords(
                ReadRecordsRequest(
                    ExerciseSessionRecord::class,
                    TimeRangeFilter.between(from, to)
                )
            ).records
        return records.any { it.metadata.clientRecordId == clientRecordId }
    }

    override suspend fun getChangesToken(types: Set<HealthDataType>): String = withContext(Dispatchers.IO) {
        val recordTypes =
            buildSet {
                if (HealthDataType.HEART_RATE in types) add(HeartRateRecord::class)
                if (HealthDataType.SLEEP in types) add(SleepSessionRecord::class)
                if (HealthDataType.WORKOUT in types) add(ExerciseSessionRecord::class)
                if (HealthDataType.WATER in types) add(HydrationRecord::class)
                if (isEmpty()) add(HeartRateRecord::class)
            }
        client.getChangesToken(ChangesTokenRequest(recordTypes))
    }

    override suspend fun getChanges(token: String): HealthChanges = withContext(Dispatchers.IO) {
        val response = client.getChanges(token)
        // HC 1.1 ChangesResponse exposes opaque change list; map defensively by class name
        // so we do not hard-depend on nested type names that shift between SDK revisions.
        val upserts = mutableListOf<HealthRecordRef>()
        val deletions = mutableListOf<String>()
        response.changes.forEach { change ->
            val name = change::class.simpleName.orEmpty()
            when {
                name.contains("Upsert", ignoreCase = true) -> {
                    val id =
                        runCatching {
                            val record = change.javaClass.getMethod("getRecord").invoke(change)
                            val metadata = record.javaClass.getMethod(
                                "getMetadata"
                            ).invoke(record)
                            metadata.javaClass.getMethod("getId").invoke(metadata) as String
                        }.getOrElse { "unknown" }
                    upserts +=
                        HealthRecordRef(
                            id = id,
                            type = HealthDataType.HEART_RATE,
                            recordedAt = Instant.fromEpochMilliseconds(0)
                        )
                }
                name.contains("Deletion", ignoreCase = true) -> {
                    val id =
                        runCatching {
                            change.javaClass.getMethod("getRecordId").invoke(change) as String
                        }.getOrElse { "unknown" }
                    deletions += id
                }
            }
        }
        HealthChanges(
            upserts = upserts,
            deletions = deletions,
            nextToken = HealthChangeToken("health_connect", response.nextChangesToken)
        )
    }

    companion object {
        fun isAvailable(context: Context): Boolean = HcSdk.getSdkStatus(context) == HcSdk.SDK_AVAILABLE
    }
}

private fun TimeRange.toFilter(): TimeRangeFilter = TimeRangeFilter.between(start.toJava(), end.toJava())

private fun Instant.toJava(): JavaInstant = JavaInstant.ofEpochMilli(toEpochMilliseconds())

private fun JavaInstant.toKotlin(): Instant = Instant.fromEpochMilliseconds(toEpochMilli())

private fun exerciseTypeFor(type: WorkoutActivityType): Int = when (type) {
    WorkoutActivityType.STRENGTH -> ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING
    WorkoutActivityType.RUNNING -> ExerciseSessionRecord.EXERCISE_TYPE_RUNNING
    WorkoutActivityType.CYCLING -> ExerciseSessionRecord.EXERCISE_TYPE_BIKING
    WorkoutActivityType.SWIMMING -> ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL
    WorkoutActivityType.WALKING -> ExerciseSessionRecord.EXERCISE_TYPE_WALKING
    WorkoutActivityType.HIIT -> ExerciseSessionRecord.EXERCISE_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING
    WorkoutActivityType.YOGA -> ExerciseSessionRecord.EXERCISE_TYPE_YOGA
    WorkoutActivityType.PILATES -> ExerciseSessionRecord.EXERCISE_TYPE_PILATES
    WorkoutActivityType.OTHER -> ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT
}

private fun activityTypeFor(exerciseType: Int): WorkoutActivityType = when (exerciseType) {
    ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING -> WorkoutActivityType.STRENGTH
    ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
    ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL -> WorkoutActivityType.RUNNING
    ExerciseSessionRecord.EXERCISE_TYPE_BIKING,
    ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY -> WorkoutActivityType.CYCLING
    ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL,
    ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_OPEN_WATER -> WorkoutActivityType.SWIMMING
    ExerciseSessionRecord.EXERCISE_TYPE_WALKING,
    ExerciseSessionRecord.EXERCISE_TYPE_HIKING -> WorkoutActivityType.WALKING
    ExerciseSessionRecord.EXERCISE_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING -> WorkoutActivityType.HIIT
    ExerciseSessionRecord.EXERCISE_TYPE_YOGA -> WorkoutActivityType.YOGA
    ExerciseSessionRecord.EXERCISE_TYPE_PILATES -> WorkoutActivityType.PILATES
    else -> WorkoutActivityType.OTHER
}
