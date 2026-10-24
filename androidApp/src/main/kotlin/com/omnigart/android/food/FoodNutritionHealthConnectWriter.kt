package com.omnigart.android.food

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Mass
import com.omnigart.shared.food.health.DietaryNutritionSample
import com.omnigart.shared.food.health.FoodHealthWriteResult
import com.omnigart.shared.food.health.FoodNutritionHealthWriter
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Append-only Health Connect nutrition writer.
 * Exist-check by [Metadata.clientRecordId]; never calls deleteRecords/updateRecords.
 */
class FoodNutritionHealthConnectWriter(private val client: HealthConnectClient) : FoodNutritionHealthWriter {
    override suspend fun appendDietary(sample: DietaryNutritionSample): FoodHealthWriteResult = withContext(Dispatchers.IO) {
        val syncId = sample.identity.syncIdentifier
        val start = Instant.ofEpochMilli(sample.startEpochMs)
        val end = Instant.ofEpochMilli(sample.endEpochMs).plusMillis(1)
        if (clientRecordIdExists(syncId, start, end)) {
            return@withContext FoodHealthWriteResult(
                syncIdentifier = syncId,
                platformRecordId = null,
                skippedAlreadyPresent = true,
                ok = true
            )
        }
        val meta =
            Metadata.manualEntry(
                clientRecordId = syncId,
                clientRecordVersion = sample.identity.syncVersion.coerceAtLeast(1L)
            )
        val record =
            NutritionRecord(
                startTime = start,
                startZoneOffset = null,
                endTime = end,
                endZoneOffset = null,
                name = sample.displayName,
                energy = sample.energyKcal?.let { Energy.kilocalories(it) },
                protein = sample.proteinG?.let { Mass.grams(it) },
                totalCarbohydrate = sample.carbohydrateG?.let { Mass.grams(it) },
                totalFat = sample.fatG?.let { Mass.grams(it) },
                dietaryFiber = sample.fibreG?.let { Mass.grams(it) },
                sugar = sample.sugarsG?.let { Mass.grams(it) },
                metadata = meta
            )
        return@withContext try {
            client.insertRecords(listOf(record))
            FoodHealthWriteResult(
                syncIdentifier = syncId,
                platformRecordId = syncId,
                ok = true
            )
        } catch (e: IllegalArgumentException) {
            writeFailure(syncId, e.message)
        } catch (e: SecurityException) {
            writeFailure(syncId, e.message)
        } catch (e: IllegalStateException) {
            writeFailure(syncId, e.message)
        }
    }

    private fun writeFailure(syncId: String, message: String?): FoodHealthWriteResult = FoodHealthWriteResult(
        syncIdentifier = syncId,
        platformRecordId = null,
        ok = false,
        errorMessage = message
    )

    private suspend fun clientRecordIdExists(clientRecordId: String, start: Instant, end: Instant): Boolean {
        val response =
            client.readRecords(
                ReadRecordsRequest(
                    recordType = NutritionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start.minusSeconds(1), end.plusSeconds(1))
                )
            )
        return response.records.any { it.metadata.clientRecordId == clientRecordId }
    }
}
