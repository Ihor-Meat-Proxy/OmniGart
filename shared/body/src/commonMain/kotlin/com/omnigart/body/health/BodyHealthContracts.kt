package com.omnigart.body.health

/**
 * Platform-agnostic contracts for body measurements ↔ HealthKit / Health Connect.
 * See `docs/specs/body-health-sync.md`.
 *
 * Progress photos are **never** written to Health — local metadata only.
 * Platform writes are **append-only**: exist-check by [BodySyncIdentity.clientRecordId],
 * insert only when absent — never upsert, update, or delete platform records.
 */

const val BODY_SYNC_SCHEMA_VERSION = 1
const val OMNIGART_BODY_SOURCE_ID = "com.omnigart.body"

/** Fixed platform metadata version — never bump; bumping enables silent Health Connect rewrites. */
const val BODY_CLIENT_RECORD_VERSION = 1L

data class BodySyncIdentity(
    val clientRecordId: String,
    /** Always [BODY_CLIENT_RECORD_VERSION] for platform stamps; not used for upsert. */
    val clientRecordVersion: Long = BODY_CLIENT_RECORD_VERSION,
    val schemaVersion: Int = BODY_SYNC_SCHEMA_VERSION,
)

data class BodyMassSample(
    val identity: BodySyncIdentity,
    /** Kilograms. */
    val massKg: Double,
    val epochMs: Long,
)

data class HeightSample(
    val identity: BodySyncIdentity,
    /** Metres (HealthKit/HC native). */
    val heightMeters: Double,
    val epochMs: Long,
)

data class BodyFatSample(
    val identity: BodySyncIdentity,
    /** Percentage 0–100. */
    val bodyFatPercent: Double,
    val epochMs: Long,
    /** Method id string for metadata (e.g. cun_bae). */
    val methodId: String,
)

data class WaistCircumferenceSample(
    val identity: BodySyncIdentity,
    /** Metres. */
    val waistMeters: Double,
    val epochMs: Long,
)

data class BodyWriteResult(
    val clientRecordId: String,
    val platformRecordId: String?,
    val ok: Boolean,
    val skippedAlreadyPresent: Boolean = false,
    val errorMessage: String? = null,
)

enum class BodyReadSourceFilter {
    OMNIGART_ONLY,
    ALL_SOURCES,
}

/**
 * Append-only platform writer. Implementations must exist-check [BodySyncIdentity.clientRecordId]
 * and skip if present. Never call HealthKit delete APIs or Health Connect deleteRecords/updateRecords.
 */
interface BodyHealthWriter {
    suspend fun appendBodyMass(sample: BodyMassSample): BodyWriteResult

    suspend fun appendHeight(sample: HeightSample): BodyWriteResult

    suspend fun appendBodyFat(sample: BodyFatSample): BodyWriteResult

    suspend fun appendWaist(sample: WaistCircumferenceSample): BodyWriteResult
}

interface BodyHealthReader {
    suspend fun readLatestBodyMassKg(
        startEpochMs: Long,
        endEpochMs: Long,
        sourceFilter: BodyReadSourceFilter = BodyReadSourceFilter.OMNIGART_ONLY,
    ): Double?
}

object BodyHealthSyncPolicy {
    const val PHOTOS_LOCAL_ONLY_EN_GB =
        "Progress photo binaries and metadata stay on-device. Do not create HealthKit or Health Connect records for photos."
}
