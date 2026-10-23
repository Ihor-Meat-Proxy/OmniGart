package com.omnigart.body.health

/**
 * Platform-agnostic contracts for body measurements ↔ HealthKit / Health Connect.
 * See `docs/specs/body-health-sync.md`.
 *
 * Progress photos are **never** written to Health — local metadata only.
 */

const val BODY_SYNC_SCHEMA_VERSION = 1
const val OMNIGART_BODY_SOURCE_ID = "com.omnigart.body"

data class BodySyncIdentity(
    val clientRecordId: String,
    val clientRecordVersion: Long,
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
    val errorMessage: String? = null,
)

enum class BodyReadSourceFilter {
    OMNIGART_ONLY,
    ALL_SOURCES,
}

interface BodyHealthWriter {
    suspend fun upsertBodyMass(sample: BodyMassSample): BodyWriteResult

    suspend fun upsertHeight(sample: HeightSample): BodyWriteResult

    suspend fun upsertBodyFat(sample: BodyFatSample): BodyWriteResult

    suspend fun upsertWaist(sample: WaistCircumferenceSample): BodyWriteResult

    suspend fun deleteByClientRecordId(clientRecordId: String, clientRecordVersion: Long): BodyWriteResult
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
