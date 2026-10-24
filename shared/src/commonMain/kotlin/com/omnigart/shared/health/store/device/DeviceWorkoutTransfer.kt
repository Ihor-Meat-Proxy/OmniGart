package com.omnigart.shared.health.store.device

import com.omnigart.shared.health.WorkoutActivityType
import com.omnigart.shared.health.store.HealthDataMapper
import com.omnigart.shared.health.store.HealthRecordOrigin
import com.omnigart.shared.health.store.HealthSyncIdentity
import com.omnigart.shared.health.store.LocalHealthWorkout
import com.omnigart.shared.health.store.LocalHeartRatePoint
import com.omnigart.shared.health.store.LocalWorkoutSet
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Wire format for watch / Wear → phone workout delivery.
 *
 * - Apple Watch → iPhone: WCSession `transferUserInfo` / `transferFile`
 * - Wear OS → phone: Wearable Data Layer (`DataClient`) + ack via `MessageClient`
 *
 * Documented in `docs/HEALTH_DATA_MAPPING.md` § Device transfer.
 *
 * Schema v2 adds optional [sets] for per-set strength logging. v1 payloads (no sets)
 * remain accepted by [isSupportedSchemaVersion].
 */
@Serializable
data class DeviceWorkoutTransferDocument(
    val schema: String = SCHEMA_ID,
    val schemaVersion: Int = SCHEMA_VERSION,
    val localId: String,
    val syncIdentifier: String,
    val syncVersion: Long = 1,
    val activityTypeName: String,
    val startEpochMs: Long,
    val endEpochMs: Long,
    val activeEnergyKcal: Double? = null,
    val basalEnergyKcal: Double? = null,
    val distanceMetres: Double? = null,
    val averageHeartRateBpm: Double? = null,
    val maxHeartRateBpm: Double? = null,
    val averageCadenceSpm: Double? = null,
    val heartRateEpochMs: List<Long> = emptyList(),
    val heartRateBpm: List<Double> = emptyList(),
    /** Per-set strength log (schema ≥ 2). */
    val sets: List<DeviceWorkoutTransferSet> = emptyList(),
    /** `APPLE_WATCH` or `WEAR_OS`. */
    val sourceDevice: String,
    val sourceBundle: String = HealthDataMapper.SOURCE_BUNDLE_OMNIGART,
    val sourceName: String = "OmniGart"
) {
    init {
        require(localId.isNotBlank())
        require(syncIdentifier.isNotBlank())
        require(syncVersion >= 1)
        require(endEpochMs >= startEpochMs)
        require(heartRateEpochMs.size == heartRateBpm.size)
        require(sourceDevice == SOURCE_APPLE_WATCH || sourceDevice == SOURCE_WEAR_OS)
        require(isSupportedSchemaVersion(schemaVersion)) {
            "Unsupported schemaVersion $schemaVersion"
        }
    }

    companion object {
        const val SCHEMA_ID = "omnigart.device.workout.transfer"

        /** Current writer version (includes sets). */
        const val SCHEMA_VERSION = 2
        const val MIN_SUPPORTED_SCHEMA_VERSION = 1
        const val SOURCE_APPLE_WATCH = "APPLE_WATCH"
        const val SOURCE_WEAR_OS = "WEAR_OS"

        /** WCSession / Data Layer dictionary key for the UTF-8 JSON payload. */
        const val PAYLOAD_KEY = "omnigart.workout.transfer.json"

        /** Data Layer path prefix: `/omnigart/workout/transfer/<syncId>`. */
        const val DATA_PATH_PREFIX = "/omnigart/workout/transfer/"

        /** Message path for phone → Wear ack. */
        const val ACK_PATH = "/omnigart/workout/ack"

        fun isSupportedSchemaVersion(version: Int): Boolean = version in MIN_SUPPORTED_SCHEMA_VERSION..SCHEMA_VERSION
    }
}

@Serializable
data class DeviceWorkoutTransferSet(
    val id: String,
    val exerciseKey: String,
    val setIndex: Int,
    val reps: Int? = null,
    val loadKg: Double? = null,
    val durationSeconds: Int? = null,
    val distanceMetres: Double? = null,
    val rir: Double? = null,
    val rpe: Double? = null,
    val isWarmup: Boolean = false,
    val isDropSet: Boolean = false,
    val isFailure: Boolean = false,
    val note: String? = null
)

enum class DeviceTransferIngestResult {
    /** Inserted into the phone local store. */
    INSERTED,

    /** Sync id already present — idempotent no-op. */
    ALREADY_PRESENT,

    /** Payload rejected (bad schema / foreign). */
    REJECTED
}

/** Whether the phone should ack Wear after an ingest attempt. */
fun shouldAckWearAfterIngest(result: DeviceTransferIngestResult): Boolean =
    result == DeviceTransferIngestResult.INSERTED || result == DeviceTransferIngestResult.ALREADY_PRESENT

fun shouldAckWearAfterIngest(resultName: String): Boolean = resultName == DeviceTransferIngestResult.INSERTED.name ||
    resultName == DeviceTransferIngestResult.ALREADY_PRESENT.name

object DeviceWorkoutTransferCodec {
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    fun encode(document: DeviceWorkoutTransferDocument): String = json.encodeToString(document)

    fun decode(utf8: String): DeviceWorkoutTransferDocument = json.decodeFromString(utf8)

    fun dataPathFor(syncIdentifier: String): String = DeviceWorkoutTransferDocument.DATA_PATH_PREFIX + syncIdentifier

    fun syncIdentifierFromDataPath(path: String): String? {
        if (!path.startsWith(DeviceWorkoutTransferDocument.DATA_PATH_PREFIX)) return null
        val id = path.removePrefix(DeviceWorkoutTransferDocument.DATA_PATH_PREFIX)
        return id.takeIf { it.isNotBlank() }
    }

    fun toLocalWorkout(document: DeviceWorkoutTransferDocument, clock: Clock = Clock.System): LocalHealthWorkout {
        require(document.schema == DeviceWorkoutTransferDocument.SCHEMA_ID)
        require(DeviceWorkoutTransferDocument.isSupportedSchemaVersion(document.schemaVersion))
        val activity =
            runCatching { WorkoutActivityType.valueOf(document.activityTypeName) }
                .getOrDefault(WorkoutActivityType.OTHER)
        val hr =
            document.heartRateEpochMs.indices.map { i ->
                LocalHeartRatePoint(
                    recordedAt = Instant.fromEpochMilliseconds(document.heartRateEpochMs[i]),
                    beatsPerMinute = document.heartRateBpm[i]
                )
            }
        val sets =
            document.sets.map { s ->
                LocalWorkoutSet(
                    id = s.id,
                    exerciseKey = s.exerciseKey,
                    setIndex = s.setIndex,
                    reps = s.reps,
                    loadKg = s.loadKg,
                    durationSeconds = s.durationSeconds,
                    distanceMetres = s.distanceMetres,
                    rir = s.rir,
                    rpe = s.rpe,
                    isWarmup = s.isWarmup,
                    isDropSet = s.isDropSet,
                    isFailure = s.isFailure,
                    note = s.note
                )
            }
        return LocalHealthWorkout(
            id = document.localId,
            identity =
            HealthSyncIdentity(
                syncIdentifier = document.syncIdentifier,
                syncVersion = document.syncVersion.coerceAtLeast(1)
            ),
            activityType = activity,
            startAt = Instant.fromEpochMilliseconds(document.startEpochMs),
            endAt = Instant.fromEpochMilliseconds(document.endEpochMs),
            activeEnergyKcal = document.activeEnergyKcal,
            basalEnergyKcal = document.basalEnergyKcal,
            distanceMetres = document.distanceMetres,
            averageHeartRateBpm = document.averageHeartRateBpm,
            maxHeartRateBpm = document.maxHeartRateBpm,
            averageCadenceSpm = document.averageCadenceSpm,
            origin = HealthRecordOrigin.OMNIGART,
            sourceBundle = document.sourceBundle,
            sourceName = document.sourceName,
            updatedAt = clock.now(),
            heartRateSeries = hr,
            sets = sets
        )
    }

    fun fromLocalWorkout(workout: LocalHealthWorkout, sourceDevice: String): DeviceWorkoutTransferDocument = DeviceWorkoutTransferDocument(
        localId = workout.id,
        syncIdentifier = workout.identity.syncIdentifier,
        syncVersion = workout.identity.syncVersion,
        activityTypeName = workout.activityType.name,
        startEpochMs = workout.startAt.toEpochMilliseconds(),
        endEpochMs = workout.endAt.toEpochMilliseconds(),
        activeEnergyKcal = workout.activeEnergyKcal,
        basalEnergyKcal = workout.basalEnergyKcal,
        distanceMetres = workout.distanceMetres,
        averageHeartRateBpm = workout.averageHeartRateBpm,
        maxHeartRateBpm = workout.maxHeartRateBpm,
        averageCadenceSpm = workout.averageCadenceSpm,
        heartRateEpochMs = workout.heartRateSeries.map { it.recordedAt.toEpochMilliseconds() },
        heartRateBpm = workout.heartRateSeries.map { it.beatsPerMinute },
        sets =
        workout.sets.map { s ->
            DeviceWorkoutTransferSet(
                id = s.id,
                exerciseKey = s.exerciseKey,
                setIndex = s.setIndex,
                reps = s.reps,
                loadKg = s.loadKg,
                durationSeconds = s.durationSeconds,
                distanceMetres = s.distanceMetres,
                rir = s.rir,
                rpe = s.rpe,
                isWarmup = s.isWarmup,
                isDropSet = s.isDropSet,
                isFailure = s.isFailure,
                note = s.note
            )
        },
        sourceDevice = sourceDevice,
        sourceBundle = workout.sourceBundle ?: HealthDataMapper.SOURCE_BUNDLE_OMNIGART,
        sourceName = workout.sourceName ?: "OmniGart"
    )
}
