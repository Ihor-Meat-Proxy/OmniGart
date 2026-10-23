package com.omnigart.body.health

class FakeBodyHealthStore : BodyHealthWriter, BodyHealthReader {
    data class StoredMass(val sample: BodyMassSample, val platformRecordId: String, val deleted: Boolean = false)

    data class StoredHeight(val sample: HeightSample, val platformRecordId: String, val deleted: Boolean = false)

    data class StoredFat(val sample: BodyFatSample, val platformRecordId: String, val deleted: Boolean = false)

    data class StoredWaist(val sample: WaistCircumferenceSample, val platformRecordId: String, val deleted: Boolean = false)

    val mass = linkedMapOf<String, StoredMass>()
    val height = linkedMapOf<String, StoredHeight>()
    val fat = linkedMapOf<String, StoredFat>()
    val waist = linkedMapOf<String, StoredWaist>()
    var foreignMassKg: Double? = null
    private var seq = 0L

    override suspend fun upsertBodyMass(sample: BodyMassSample): BodyWriteResult =
        upsert(mass, sample.identity, sample) { id, s -> StoredMass(s, id) }

    override suspend fun upsertHeight(sample: HeightSample): BodyWriteResult =
        upsert(height, sample.identity, sample) { id, s -> StoredHeight(s, id) }

    override suspend fun upsertBodyFat(sample: BodyFatSample): BodyWriteResult =
        upsert(fat, sample.identity, sample) { id, s -> StoredFat(s, id) }

    override suspend fun upsertWaist(sample: WaistCircumferenceSample): BodyWriteResult =
        upsert(waist, sample.identity, sample) { id, s -> StoredWaist(s, id) }

    override suspend fun deleteByClientRecordId(
        clientRecordId: String,
        clientRecordVersion: Long,
    ): BodyWriteResult {
        mass[clientRecordId]?.let {
            if (clientRecordVersion >= it.sample.identity.clientRecordVersion) {
                mass[clientRecordId] = it.copy(deleted = true)
                return BodyWriteResult(clientRecordId, it.platformRecordId, true)
            }
        }
        height[clientRecordId]?.let {
            if (clientRecordVersion >= it.sample.identity.clientRecordVersion) {
                height[clientRecordId] = it.copy(deleted = true)
                return BodyWriteResult(clientRecordId, it.platformRecordId, true)
            }
        }
        fat[clientRecordId]?.let {
            if (clientRecordVersion >= it.sample.identity.clientRecordVersion) {
                fat[clientRecordId] = it.copy(deleted = true)
                return BodyWriteResult(clientRecordId, it.platformRecordId, true)
            }
        }
        waist[clientRecordId]?.let {
            if (clientRecordVersion >= it.sample.identity.clientRecordVersion) {
                waist[clientRecordId] = it.copy(deleted = true)
                return BodyWriteResult(clientRecordId, it.platformRecordId, true)
            }
        }
        return BodyWriteResult(clientRecordId, null, false, "not found")
    }

    override suspend fun readLatestBodyMassKg(
        startEpochMs: Long,
        endEpochMs: Long,
        sourceFilter: BodyReadSourceFilter,
    ): Double? {
        val own =
            mass.values
                .filter { !it.deleted }
                .filter { it.sample.epochMs in startEpochMs until endEpochMs }
                .maxByOrNull { it.sample.epochMs }
                ?.sample
                ?.massKg
        return when (sourceFilter) {
            BodyReadSourceFilter.OMNIGART_ONLY -> own
            BodyReadSourceFilter.ALL_SOURCES -> own ?: foreignMassKg
        }
    }

    private fun <S, T> upsert(
        map: MutableMap<String, T>,
        identity: BodySyncIdentity,
        sample: S,
        store: (platformId: String, sample: S) -> T,
    ): BodyWriteResult where T : Any {
        @Suppress("UNCHECKED_CAST")
        val existing = map[identity.clientRecordId]
        if (existing != null) {
            val existingVersion =
                when (existing) {
                    is StoredMass -> existing.sample.identity.clientRecordVersion
                    is StoredHeight -> existing.sample.identity.clientRecordVersion
                    is StoredFat -> existing.sample.identity.clientRecordVersion
                    is StoredWaist -> existing.sample.identity.clientRecordVersion
                    else -> 0L
                }
            val platformId =
                when (existing) {
                    is StoredMass -> existing.platformRecordId
                    is StoredHeight -> existing.platformRecordId
                    is StoredFat -> existing.platformRecordId
                    is StoredWaist -> existing.platformRecordId
                    else -> "unknown"
                }
            if (existingVersion > identity.clientRecordVersion) {
                return BodyWriteResult(identity.clientRecordId, platformId, false, "stale version")
            }
            map[identity.clientRecordId] = store(platformId, sample)
            return BodyWriteResult(identity.clientRecordId, platformId, true)
        }
        val id = "fake-body-${++seq}"
        map[identity.clientRecordId] = store(id, sample)
        return BodyWriteResult(identity.clientRecordId, id, true)
    }
}
