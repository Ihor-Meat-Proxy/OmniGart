package com.omnigart.hydration.health

/**
 * In-memory fakes for unit tests and JVM demos. Not thread-safe across coroutines beyond
 * single-threaded test dispatchers.
 *
 * Append-only: first write wins for a [HealthSyncIdentity.clientRecordId]; no deletes.
 */
class FakeHydrationHealthStore : HydrationHealthWriter, HydrationHealthReader {

    data class StoredWater(
        val sample: WaterSample,
        val platformRecordId: String,
    )

    data class StoredCaffeine(
        val sample: CaffeineSample,
        val platformRecordId: String,
    )

    data class StoredCaloric(
        val sample: CaloricDrinkSample,
        val platformRecordId: String,
    )

    val water = linkedMapOf<String, StoredWater>()
    val caffeine = linkedMapOf<String, StoredCaffeine>()
    val caloric = linkedMapOf<String, StoredCaloric>()

    /** Foreign (non-OmniGart) water litres injected for double-count tests. */
    var foreignWaterLiters: Double = 0.0
    var foreignCaffeineMg: Double = 0.0

    private var seq = 0L

    override suspend fun appendWater(sample: WaterSample): HealthWriteResult {
        val existing = water[sample.identity.clientRecordId]
        if (existing != null) {
            return HealthWriteResult(
                clientRecordId = sample.identity.clientRecordId,
                platformRecordId = existing.platformRecordId,
                ok = true,
                skippedAlreadyPresent = true,
            )
        }
        val id = "fake-water-${++seq}"
        water[sample.identity.clientRecordId] = StoredWater(sample, id)
        return HealthWriteResult(sample.identity.clientRecordId, id, ok = true)
    }

    override suspend fun appendCaffeine(sample: CaffeineSample): HealthWriteResult {
        val existing = caffeine[sample.identity.clientRecordId]
        if (existing != null) {
            return HealthWriteResult(
                clientRecordId = sample.identity.clientRecordId,
                platformRecordId = existing.platformRecordId,
                ok = true,
                skippedAlreadyPresent = true,
            )
        }
        val id = "fake-caffeine-${++seq}"
        caffeine[sample.identity.clientRecordId] = StoredCaffeine(sample, id)
        return HealthWriteResult(sample.identity.clientRecordId, id, ok = true)
    }

    override suspend fun appendCaloricDrink(sample: CaloricDrinkSample): HealthWriteResult {
        val existing = caloric[sample.identity.clientRecordId]
        if (existing != null) {
            return HealthWriteResult(
                clientRecordId = sample.identity.clientRecordId,
                platformRecordId = existing.platformRecordId,
                ok = true,
                skippedAlreadyPresent = true,
            )
        }
        val id = "fake-food-${++seq}"
        caloric[sample.identity.clientRecordId] = StoredCaloric(sample, id)
        return HealthWriteResult(sample.identity.clientRecordId, id, ok = true)
    }

    override suspend fun readWaterLiters(
        startEpochMs: Long,
        endEpochMs: Long,
        sourceFilter: HealthReadSourceFilter,
    ): Double {
        val own =
            water.values
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
                .filter { it.sample.startEpochMs in startEpochMs until endEpochMs }
                .sumOf { it.sample.caffeineMg }
        return when (sourceFilter) {
            HealthReadSourceFilter.OMNIGART_ONLY -> own
            HealthReadSourceFilter.ALL_SOURCES -> own + foreignCaffeineMg
        }
    }
}
