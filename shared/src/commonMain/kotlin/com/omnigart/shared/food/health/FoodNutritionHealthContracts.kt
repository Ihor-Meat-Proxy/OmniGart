package com.omnigart.shared.food.health

/**
 * Append-only dietary sample writes to HealthKit / Health Connect.
 *
 * Policy (same as workouts — see docs/HEALTH_DATA_MAPPING.md):
 * - Exist-check by sync id / clientRecordId before insert; if present, skip.
 * - Never delete or update platform records (CI: scripts/check_no_health_platform_mutate.sh).
 * - Local diary edits stay in OmniGart.
 */
const val FOOD_NUTRITION_SYNC_SCHEMA_VERSION = 1
const val OMNIGART_FOOD_SOURCE_ID = "com.omnigart.food"

data class FoodSyncIdentity(
    /** Stable id, e.g. `omnigart.food.<localId>`. */
    val syncIdentifier: String,
    /** First insert = 1; bump only for user restore after platform deletion. */
    val syncVersion: Long = 1L,
    val schemaVersion: Int = FOOD_NUTRITION_SYNC_SCHEMA_VERSION
)

data class DietaryNutritionSample(
    val identity: FoodSyncIdentity,
    val displayName: String,
    val energyKcal: Double?,
    val proteinG: Double? = null,
    val carbohydrateG: Double? = null,
    val fatG: Double? = null,
    val fibreG: Double? = null,
    val sugarsG: Double? = null,
    val sodiumMg: Double? = null,
    val startEpochMs: Long,
    val endEpochMs: Long = startEpochMs
)

data class FoodHealthWriteResult(
    val syncIdentifier: String,
    val platformRecordId: String?,
    val skippedAlreadyPresent: Boolean = false,
    val ok: Boolean,
    val errorMessage: String? = null
)

/**
 * Platform writers must implement exist-check + insert only.
 * Do not call HealthKit delete APIs or Health Connect deleteRecords/updateRecords.
 */
interface FoodNutritionHealthWriter {
    suspend fun appendDietary(sample: DietaryNutritionSample): FoodHealthWriteResult
}

/** Fake for unit tests — records samples, simulates exist-check skip. */
class FakeFoodNutritionHealthWriter : FoodNutritionHealthWriter {
    val written = mutableListOf<DietaryNutritionSample>()
    private val presentIds = mutableSetOf<String>()

    fun markPresent(syncIdentifier: String) {
        presentIds += syncIdentifier
    }

    override suspend fun appendDietary(sample: DietaryNutritionSample): FoodHealthWriteResult {
        val id = sample.identity.syncIdentifier
        if (id in presentIds) {
            return FoodHealthWriteResult(
                syncIdentifier = id,
                platformRecordId = null,
                skippedAlreadyPresent = true,
                ok = true
            )
        }
        written += sample
        presentIds += id
        return FoodHealthWriteResult(
            syncIdentifier = id,
            platformRecordId = "fake-$id",
            ok = true
        )
    }
}

object FoodHealthSyncIds {
    fun forEntry(localId: String): String = "omnigart.food.$localId"
}
