package com.omnigart.nutrition.body

/**
 * Local-first body measurement store. Platform sync is optional and append-only for weigh-ins.
 */
interface BodyMeasurementStore {
    fun listWeights(): List<LocalWeightEntry>

    fun listMeasurements(): List<LocalMeasurementEntry>

    fun listBodyFat(): List<LocalBodyFatEntry>

    fun appendWeight(entry: LocalWeightEntry): LocalWeightEntry

    fun appendMeasurement(entry: LocalMeasurementEntry): LocalMeasurementEntry

    fun appendBodyFat(entry: LocalBodyFatEntry): LocalBodyFatEntry

    fun markWeightPlatformLinked(id: String)

    fun latestSnapshot(heightCm: Double?): LocalBodySnapshot
}

class InMemoryBodyMeasurementStore : BodyMeasurementStore {
    private val weights = linkedMapOf<String, LocalWeightEntry>()
    private val measurements = linkedMapOf<String, LocalMeasurementEntry>()
    private val bodyFat = linkedMapOf<String, LocalBodyFatEntry>()

    override fun listWeights(): List<LocalWeightEntry> =
        weights.values.sortedBy { it.epochMs }

    override fun listMeasurements(): List<LocalMeasurementEntry> =
        measurements.values.sortedBy { it.epochMs }

    override fun listBodyFat(): List<LocalBodyFatEntry> =
        bodyFat.values.sortedBy { it.epochMs }

    override fun appendWeight(entry: LocalWeightEntry): LocalWeightEntry {
        val withId =
            if (entry.syncIdentifier.isBlank()) {
                entry.copy(syncIdentifier = WeighInSyncIds.syncIdentifier(entry.id))
            } else {
                entry
            }
        // Append-only: never overwrite an existing id.
        if (weights.containsKey(withId.id)) {
            return weights.getValue(withId.id)
        }
        weights[withId.id] = withId
        return withId
    }

    override fun appendMeasurement(entry: LocalMeasurementEntry): LocalMeasurementEntry {
        if (measurements.containsKey(entry.id)) return measurements.getValue(entry.id)
        measurements[entry.id] = entry
        return entry
    }

    override fun appendBodyFat(entry: LocalBodyFatEntry): LocalBodyFatEntry {
        if (bodyFat.containsKey(entry.id)) return bodyFat.getValue(entry.id)
        bodyFat[entry.id] = entry
        return entry
    }

    override fun markWeightPlatformLinked(id: String) {
        val existing = weights[id] ?: return
        weights[id] = existing.copy(platformLinked = true)
    }

    override fun latestSnapshot(heightCm: Double?): LocalBodySnapshot {
        val w = listWeights().lastOrNull()?.massKg
        val bf = listBodyFat().lastOrNull()?.bodyFatPercent
        val bySite =
            MeasurementSite.entries.associateWith { site ->
                listMeasurements().lastOrNull { it.site == site }?.valueCm
            }.filterValues { it != null }.mapValues { it.value!! }
        val waist = bySite[MeasurementSite.WAIST]
        val whtr =
            if (waist != null && heightCm != null && heightCm > 0.0) {
                BodyMetricsDisplay.waistToHeightRatio(waist, heightCm)
            } else {
                null
            }
        return LocalBodySnapshot(
            weightKg = w,
            heightCm = heightCm,
            bodyFatPercent = bf,
            circumferencesCm = bySite,
            waistToHeightRatio = whtr,
        )
    }
}
