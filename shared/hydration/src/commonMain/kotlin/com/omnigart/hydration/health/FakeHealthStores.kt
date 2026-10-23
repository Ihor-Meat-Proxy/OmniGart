package com.omnigart.hydration.health

/**
 * In-memory fakes for unit tests and JVM demos. Not thread-safe across coroutines beyond
 * single-threaded test dispatchers.
 */
class FakeHydrationHealthStore : HydrationHealthWriter, HydrationHealthReader {

    data class StoredWater(
        val sample: WaterSample,
        val platformRecordId: String,
        val deleted: Boolean = false,
    )

    data class StoredCaffeine(
        val sample: CaffeineSample,
        val platformRecordId: String,
        val deleted: Boolean = false,
    )

    data class StoredCaloric(
        val sample: CaloricDrinkSample,
        val platformRecordId: String,
        val deleted: Boolean = false,
    )

    val water = linkedMapOf<String, StoredWater>()
    val caffeine = linkedMapOf<String, StoredCaffeine>()
    val caloric = linkedMapOf<String, StoredCaloric>()

    /** Foreign (non-OmniGart) water litres injected for double-count tests. */
    var foreignWaterLiters: Double = 0.0
    var foreignCaffeineMg: Double = 0.0

    private var seq = 0L

    override suspend fun upsertWater(sample: WaterSample): HealthWriteResult {
        val existing = water[sample.identity.clientRecordId]
        if (existing != null && existing.sample.identity.clientRecordVersion > sample.identity.clientRecordVersion) {
            return HealthWriteResult(
                clientRecordId = sample.identity.clientRecordId,
                platformRecordId = existing.platformRecordId,
                ok = false,
                errorMessage = "stale version",
            )
        }
        val id = existing?.platformRecordId ?: "fake-water-${++seq}"
        water[sample.identity.clientRecordId] = StoredWater(sample, id, deleted = false)
        return HealthWriteResult(sample.identity.clientRecordId, id, ok = true)
    }

    override suspend fun upsertCaffeine(sample: CaffeineSample): HealthWriteResult {
        val existing = caffeine[sample.identity.clientRecordId]
        if (existing != null && existing.sample.identity.clientRecordVersion > sample.identity.clientRecordVersion) {
            return HealthWriteResult(
                clientRecordId = sample.identity.clientRecordId,
                platformRecordId = existing.platformRecordId,
                ok = false,
                errorMessage = "stale version",
            )
        }
        val id = existing?.platformRecordId ?: "fake-caffeine-${++seq}"
        caffeine[sample.identity.clientRecordId] = StoredCaffeine(sample, id, deleted = false)
        return HealthWriteResult(sample.identity.clientRecordId, id, ok = true)
    }

    override suspend fun upsertCaloricDrink(sample: CaloricDrinkSample): HealthWriteResult {
        val existing = caloric[sample.identity.clientRecordId]
        if (existing != null && existing.sample.identity.clientRecordVersion > sample.identity.clientRecordVersion) {
            return HealthWriteResult(
                clientRecordId = sample.identity.clientRecordId,
                platformRecordId = existing.platformRecordId,
                ok = false,
                errorMessage = "stale version",
            )
        }
        val id = existing?.platformRecordId ?: "fake-food-${++seq}"
        caloric[sample.identity.clientRecordId] = StoredCaloric(sample, id, deleted = false)
        return HealthWriteResult(sample.identity.clientRecordId, id, ok = true)
    }

    override suspend fun deleteByClientRecordId(
        clientRecordId: String,
        clientRecordVersion: Long,
    ): HealthWriteResult {
        water[clientRecordId]?.let {
            if (clientRecordVersion >= it.sample.identity.clientRecordVersion) {
                water[clientRecordId] = it.copy(deleted = true)
                return HealthWriteResult(clientRecordId, it.platformRecordId, ok = true)
            }
        }
        caffeine[clientRecordId]?.let {
            if (clientRecordVersion >= it.sample.identity.clientRecordVersion) {
                caffeine[clientRecordId] = it.copy(deleted = true)
                return HealthWriteResult(clientRecordId, it.platformRecordId, ok = true)
            }
        }
        caloric[clientRecordId]?.let {
            if (clientRecordVersion >= it.sample.identity.clientRecordVersion) {
                caloric[clientRecordId] = it.copy(deleted = true)
                return HealthWriteResult(clientRecordId, it.platformRecordId, ok = true)
            }
        }
        return HealthWriteResult(clientRecordId, null, ok = false, errorMessage = "not found")
    }

    override suspend fun readWaterLiters(
        startEpochMs: Long,
        endEpochMs: Long,
        sourceFilter: HealthReadSourceFilter,
    ): Double {
        val own =
            water.values
                .filter { !it.deleted }
                .filter { it.sample.startEpochMs in startEpochMs until endEpochMs }
                .sumOf { it.sample.volumeLiters }
        return when (sourceFilter) {
            HealthReadSourceFilter.OMNIGART_ONLY -> own
            HealthReadSourceFilter.ALL_SOURCES -> own + foreignWaterLiters
        }
    }

    override suspend fun readCaffeineMg(
        startEpochMs: Long,
        endEpochMs: Long,
        sourceFilter: HealthReadSourceFilter,
    ): Double {
        val own =
            caffeine.values
                .filter { !it.deleted }
                .filter { it.sample.startEpochMs in startEpochMs until endEpochMs }
                .sumOf { it.sample.caffeineMg }
        return when (sourceFilter) {
            HealthReadSourceFilter.OMNIGART_ONLY -> own
            HealthReadSourceFilter.ALL_SOURCES -> own + foreignCaffeineMg
        }
    }
}
