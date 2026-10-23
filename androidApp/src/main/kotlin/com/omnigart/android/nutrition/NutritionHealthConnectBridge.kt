package com.omnigart.android.nutrition

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Mass
import java.time.Instant

/**
 * Health Connect read-only for weight / height / body fat, plus append-only weigh-ins.
 * Never calls deleteRecords or updateRecords.
 */
object NutritionHealthConnectBridge {
    suspend fun latestWeightKg(context: Context): Double? {
        val client = clientOrNull(context) ?: return null
        return latestWeight(client)?.weight?.inKilograms
    }

    suspend fun latestHeightCm(context: Context): Double? {
        val client = clientOrNull(context) ?: return null
        val meters = latestHeight(client)?.height?.inMeters ?: return null
        return meters * 100.0
    }

    suspend fun latestBodyFatPercent(context: Context): Double? {
        val client = clientOrNull(context) ?: return null
        return latestBodyFat(client)?.percentage?.value
    }

    suspend fun appendWeighInIfAbsent(context: Context, localId: String, massKg: Double): Boolean {
        val client = clientOrNull(context) ?: return false
        val syncId = "omnigart.weight.$localId"
        if (existsWeight(client, syncId)) return true
        val now = Instant.now()
        val record =
            WeightRecord(
                weight = Mass.kilograms(massKg),
                time = now,
                zoneOffset = null,
                metadata =
                    Metadata.manualEntry(
                        clientRecordId = syncId,
                        clientRecordVersion = 1L,
                    ),
            )
        // Insert only — never updateRecords / deleteRecords.
        client.insertRecords(listOf(record))
        return true
    }

    private fun clientOrNull(context: Context): HealthConnectClient? =
        runCatching { HealthConnectClient.getOrCreate(context) }.getOrNull()

    private suspend fun latestWeight(client: HealthConnectClient): WeightRecord? {
        val response =
            client.readRecords(
                ReadRecordsRequest(
                    recordType = WeightRecord::class,
                    timeRangeFilter = TimeRangeFilter.after(Instant.EPOCH),
                ),
            )
        return response.records.maxByOrNull { it.time }
    }

    private suspend fun latestHeight(client: HealthConnectClient): HeightRecord? {
        val response =
            client.readRecords(
                ReadRecordsRequest(
                    recordType = HeightRecord::class,
                    timeRangeFilter = TimeRangeFilter.after(Instant.EPOCH),
                ),
            )
        return response.records.maxByOrNull { it.time }
    }

    private suspend fun latestBodyFat(client: HealthConnectClient): BodyFatRecord? {
        val response =
            client.readRecords(
                ReadRecordsRequest(
                    recordType = BodyFatRecord::class,
                    timeRangeFilter = TimeRangeFilter.after(Instant.EPOCH),
                ),
            )
        return response.records.maxByOrNull { it.time }
    }

    private suspend fun existsWeight(client: HealthConnectClient, syncId: String): Boolean {
        val response =
            client.readRecords(
                ReadRecordsRequest(
                    recordType = WeightRecord::class,
                    timeRangeFilter = TimeRangeFilter.after(Instant.EPOCH),
                ),
            )
        return response.records.any { it.metadata.clientRecordId == syncId }
    }
}
