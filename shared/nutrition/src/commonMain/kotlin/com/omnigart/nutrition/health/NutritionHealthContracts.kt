package com.omnigart.nutrition.health

import com.omnigart.nutrition.body.LocalWeightEntry
import com.omnigart.nutrition.body.WeighInSyncIds

/**
 * Read-only HealthKit / Health Connect access for nutrition inputs.
 * Weigh-in writes are append-only (exist-check sync id, insert only — never delete/update).
 */

data class HealthWeightSample(
    val massKg: Double,
    val epochMs: Long,
    val sourceBundle: String? = null,
)

data class HealthHeightSample(
    val heightCm: Double,
    val epochMs: Long,
)

data class HealthBodyFatSample(
    val bodyFatPercent: Double,
    val epochMs: Long,
)

interface NutritionHealthReader {
    suspend fun readLatestWeightKg(startEpochMs: Long, endEpochMs: Long): HealthWeightSample?

    suspend fun readLatestHeightCm(startEpochMs: Long, endEpochMs: Long): HealthHeightSample?

    suspend fun readLatestBodyFatPercent(
        startEpochMs: Long,
        endEpochMs: Long,
    ): HealthBodyFatSample?
}

data class WeighInWriteResult(
    val syncIdentifier: String,
    val platformRecordId: String?,
    val skippedAlreadyPresent: Boolean,
    val ok: Boolean,
)

/**
 * Append-only platform weigh-in writer.
 * Implementations must exist-check [LocalWeightEntry.syncIdentifier] and skip if present.
 * Never call HealthKit delete / Health Connect deleteRecords or updateRecords.
 */
interface NutritionWeighInWriter {
    suspend fun appendIfAbsent(entry: LocalWeightEntry): WeighInWriteResult
}

object NutritionWeighInPolicy {
    fun prepareForPlatform(entry: LocalWeightEntry): LocalWeightEntry? {
        if (!WeighInSyncIds.shouldAttemptPlatformWrite(entry)) return null
        return if (entry.syncIdentifier.isBlank()) {
            entry.copy(syncIdentifier = WeighInSyncIds.syncIdentifier(entry.id))
        } else {
            entry
        }
    }
}

/** In-memory fake for unit tests — append-only, no deletes. */
class FakeNutritionHealth : NutritionHealthReader, NutritionWeighInWriter {
    var weight: HealthWeightSample? = null
    var height: HealthHeightSample? = null
    var bodyFat: HealthBodyFatSample? = null
    private val platformIds = linkedSetOf<String>()
    val written = mutableListOf<LocalWeightEntry>()

    override suspend fun readLatestWeightKg(
        startEpochMs: Long,
        endEpochMs: Long,
    ): HealthWeightSample? =
        weight?.takeIf { it.epochMs in startEpochMs until endEpochMs }

    override suspend fun readLatestHeightCm(
        startEpochMs: Long,
        endEpochMs: Long,
    ): HealthHeightSample? =
        height?.takeIf { it.epochMs in startEpochMs until endEpochMs }

    override suspend fun readLatestBodyFatPercent(
        startEpochMs: Long,
        endEpochMs: Long,
    ): HealthBodyFatSample? =
        bodyFat?.takeIf { it.epochMs in startEpochMs until endEpochMs }

    override suspend fun appendIfAbsent(entry: LocalWeightEntry): WeighInWriteResult {
        val prepared = NutritionWeighInPolicy.prepareForPlatform(entry)
            ?: return WeighInWriteResult(entry.syncIdentifier, null, skippedAlreadyPresent = true, ok = true)
        if (platformIds.contains(prepared.syncIdentifier)) {
            return WeighInWriteResult(
                prepared.syncIdentifier,
                prepared.syncIdentifier,
                skippedAlreadyPresent = true,
                ok = true,
            )
        }
        platformIds += prepared.syncIdentifier
        written += prepared
        return WeighInWriteResult(
            prepared.syncIdentifier,
            "fake-${prepared.syncIdentifier}",
            skippedAlreadyPresent = false,
            ok = true,
        )
    }
}
