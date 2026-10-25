package com.omnigart.body.health

/**
 * In-memory fake for unit tests. Append-only: first write wins for a clientRecordId; no deletes.
 */
class FakeBodyHealthStore : BodyHealthWriter, BodyHealthReader {
    data class StoredMass(val sample: BodyMassSample, val platformRecordId: String)

    data class StoredHeight(val sample: HeightSample, val platformRecordId: String)

    data class StoredFat(val sample: BodyFatSample, val platformRecordId: String)

    data class StoredWaist(val sample: WaistCircumferenceSample, val platformRecordId: String)

    val mass = linkedMapOf<String, StoredMass>()
    val height = linkedMapOf<String, StoredHeight>()
    val fat = linkedMapOf<String, StoredFat>()
    val waist = linkedMapOf<String, StoredWaist>()
    var foreignMassKg: Double? = null
    private var seq = 0L

    override suspend fun appendBodyMass(sample: BodyMassSample): BodyWriteResult =
        append(mass, sample.identity.clientRecordId, sample) { id, s -> StoredMass(s, id) }

    override suspend fun appendHeight(sample: HeightSample): BodyWriteResult =
        append(height, sample.identity.clientRecordId, sample) { id, s -> StoredHeight(s, id) }

    override suspend fun appendBodyFat(sample: BodyFatSample): BodyWriteResult =
        append(fat, sample.identity.clientRecordId, sample) { id, s -> StoredFat(s, id) }

    override suspend fun appendWaist(sample: WaistCircumferenceSample): BodyWriteResult =
        append(waist, sample.identity.clientRecordId, sample) { id, s -> StoredWaist(s, id) }

    override suspend fun readLatestBodyMassKg(
        startEpochMs: Long,
        endEpochMs: Long,
        sourceFilter: BodyReadSourceFilter,
    ): Double? {
        val own =
            mass.values
                .filter { it.sample.epochMs in startEpochMs until endEpochMs }
                .maxByOrNull { it.sample.epochMs }
                ?.sample
                ?.massKg
        return when (sourceFilter) {
            BodyReadSourceFilter.OMNIGART_ONLY -> own
            BodyReadSourceFilter.ALL_SOURCES -> own ?: foreignMassKg
        }
    }

    private fun <S, T> append(
        map: MutableMap<String, T>,
        clientRecordId: String,
        sample: S,
        store: (platformId: String, sample: S) -> T,
    ): BodyWriteResult where T : Any {
        val existing = map[clientRecordId]
        if (existing != null) {
            val platformId =
                when (existing) {
                    is StoredMass -> existing.platformRecordId
                    is StoredHeight -> existing.platformRecordId
                    is StoredFat -> existing.platformRecordId
                    is StoredWaist -> existing.platformRecordId
                    else -> "unknown"
                }
            return BodyWriteResult(
                clientRecordId = clientRecordId,
                platformRecordId = platformId,
                ok = true,
                skippedAlreadyPresent = true,
            )
        }
        val id = "fake-body-${++seq}"
        map[clientRecordId] = store(id, sample)
        return BodyWriteResult(clientRecordId, id, ok = true)
    }
}
