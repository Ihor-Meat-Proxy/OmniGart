package com.omnigart.hydration.health

/**
 * Platform-agnostic contracts for writing hydration/nutrition samples.
 * Platform modules implement these against HealthKit / Health Connect.
 *
 * Policy (see docs/specs/hydration-health-sync.md):
 * - Write **raw** water/fluid volume to Health platforms.
 * - Keep BHI-adjusted totals internal to OmniGart.
 * - Filter foreign sources on read to avoid double counting.
 */

const val HYDRATION_SYNC_SCHEMA_VERSION = 1
const val OMNIGART_HEALTH_SOURCE_ID = "com.omnigart.hydration"

data class HealthSyncIdentity(
    /** Stable id for this OmniGart drink log (maps to HK metadata / clientRecordId). */
    val clientRecordId: String,
    /** Monotonic version for idempotent upsert/delete. */
    val clientRecordVersion: Long,
    val schemaVersion: Int = HYDRATION_SYNC_SCHEMA_VERSION,
)

data class WaterSample(
    val identity: HealthSyncIdentity,
    /** Raw volume in litres (HealthKit dietaryWater / HC HydrationRecord). */
    val volumeLiters: Double,
    val startEpochMs: Long,
    val endEpochMs: Long = startEpochMs,
)

data class CaffeineSample(
    val identity: HealthSyncIdentity,
    /** Milligrams. */
    val caffeineMg: Double,
    val startEpochMs: Long,
    val endEpochMs: Long = startEpochMs,
)

data class NutritionMacros(
    val energyKcal: Double,
    val proteinG: Double,
    val carbohydrateG: Double,
    val fatG: Double,
    val sugarsG: Double = 0.0,
    val sodiumMg: Double = 0.0,
)

/**
 * Caloric drink payload.
 * HealthKit: HKCorrelation of food type with quantity samples + metadata.
 * Health Connect: NutritionRecord with matching nutrients.
 */
data class CaloricDrinkSample(
    val identity: HealthSyncIdentity,
    val beverageId: String,
    val displayName: String,
    val volumeMl: Double,
    val macros: NutritionMacros,
    val caffeineMg: Double = 0.0,
    val startEpochMs: Long,
    val endEpochMs: Long = startEpochMs,
    val metadata: Map<String, String> = emptyMap(),
)

data class HealthWriteResult(
    val clientRecordId: String,
    val platformRecordId: String?,
    val ok: Boolean,
    val errorMessage: String? = null,
)

enum class HealthReadSourceFilter {
    /** Only samples authored by OmniGart. */
    OMNIGART_ONLY,

    /** All sources (caller must de-duplicate carefully). */
    ALL_SOURCES,
}

interface HydrationHealthWriter {
    suspend fun upsertWater(sample: WaterSample): HealthWriteResult

    suspend fun upsertCaffeine(sample: CaffeineSample): HealthWriteResult

    suspend fun upsertCaloricDrink(sample: CaloricDrinkSample): HealthWriteResult

    suspend fun deleteByClientRecordId(clientRecordId: String, clientRecordVersion: Long): HealthWriteResult
}

interface HydrationHealthReader {
    suspend fun readWaterLiters(
        startEpochMs: Long,
        endEpochMs: Long,
        sourceFilter: HealthReadSourceFilter = HealthReadSourceFilter.OMNIGART_ONLY,
    ): Double

    suspend fun readCaffeineMg(
        startEpochMs: Long,
        endEpochMs: Long,
        sourceFilter: HealthReadSourceFilter = HealthReadSourceFilter.OMNIGART_ONLY,
    ): Double
}

/**
 * Why we write raw volume (not BHI-adjusted) to Health:
 * HealthKit `dietaryWater` and Health Connect `HydrationRecord` represent ingested water volume.
 * BHI is a research index of relative retention, not a quantity type those APIs define.
 * Writing adjusted millilitres would silently disagree with other apps and inflate/deflate
 * system totals. OmniGart therefore stores raw litres externally and keeps BHI progress private.
 */
object HealthSyncPolicy {
    const val WRITE_RAW_VOLUME_RATIONALE_EN_GB =
        "Write raw ingested volume to Health platforms. BHI-adjusted progress stays inside OmniGart " +
            "because HealthKit dietaryWater and Health Connect HydrationRecord model volume, not retention index."
}
