package com.omnigart.activity.hr

import com.omnigart.activity.evidence.Citations

/**
 * Heart-rate zone models and time-in-zone aggregation.
 *
 * Default five-zone scheme uses % of HRmax (common coaching bands).
 * Karvonen / heart-rate reserve (HRR) option uses
 * `HRrest + % × (HRmax − HRrest)` ([Citations.KARVONEN_1957]).
 */
enum class ZoneModel {
    /** Zones as fractions of HRmax. */
    PERCENT_HR_MAX,

    /** Zones as fractions of HRR (Karvonen). */
    PERCENT_HR_RESERVE,
}

data class ZoneDefinition(
    val index: Int,
    val name: String,
    /** Inclusive lower bound as fraction 0–1 of the chosen model base. */
    val lowerFraction: Double,
    /** Exclusive upper bound (1.0 inclusive for the top zone). */
    val upperFraction: Double,
)

data class ZoneBoundsBpm(
    val definition: ZoneDefinition,
    val lowerBpm: Int,
    val upperBpm: Int,
)

data class HrSample(
    val epochMillis: Long,
    val bpm: Int,
)

data class TimeInZone(
    val zone: ZoneBoundsBpm,
    val durationMillis: Long,
) {
    val durationSeconds: Double get() = durationMillis / 1000.0
}

object HeartRateZones {
    /** Classic 5-zone %HRmax bands (approx. recovery → anaerobic). */
    val DEFAULT_FIVE_ZONE: List<ZoneDefinition> = listOf(
        ZoneDefinition(1, "Z1", 0.50, 0.60),
        ZoneDefinition(2, "Z2", 0.60, 0.70),
        ZoneDefinition(3, "Z3", 0.70, 0.80),
        ZoneDefinition(4, "Z4", 0.80, 0.90),
        ZoneDefinition(5, "Z5", 0.90, 1.00),
    )

    const val KARVONEN_CITATION: String = Citations.KARVONEN_1957

    fun boundsBpm(
        hrMax: Int,
        hrRest: Int = 0,
        model: ZoneModel = ZoneModel.PERCENT_HR_MAX,
        definitions: List<ZoneDefinition> = DEFAULT_FIVE_ZONE,
    ): List<ZoneBoundsBpm> {
        require(hrMax > 0)
        require(hrRest >= 0 && hrRest < hrMax)
        return definitions.map { def ->
            val lower = thresholdBpm(def.lowerFraction, hrMax, hrRest, model)
            val upper = thresholdBpm(def.upperFraction, hrMax, hrRest, model)
            ZoneBoundsBpm(def, lower, upper)
        }
    }

    fun thresholdBpm(
        fraction: Double,
        hrMax: Int,
        hrRest: Int,
        model: ZoneModel,
    ): Int {
        require(fraction in 0.0..1.0)
        return when (model) {
            ZoneModel.PERCENT_HR_MAX -> (hrMax * fraction).toInt()
            ZoneModel.PERCENT_HR_RESERVE -> {
                // Karvonen: HRrest + fraction × (HRmax − HRrest)
                (hrRest + fraction * (hrMax - hrRest)).toInt()
            }
        }
    }

    /**
     * Accumulate time in each zone from a sorted (or unsorted) HR series.
     * Uses forward differences between consecutive samples; the last sample
     * contributes [defaultSamplePeriodMillis] when it is the only gap estimate.
     */
    fun timeInZone(
        samples: List<HrSample>,
        zones: List<ZoneBoundsBpm>,
        defaultSamplePeriodMillis: Long = 1000L,
    ): List<TimeInZone> {
        if (samples.isEmpty() || zones.isEmpty()) {
            return zones.map { TimeInZone(it, 0L) }
        }
        val sorted = samples.sortedBy { it.epochMillis }
        val totals = LongArray(zones.size)

        for (i in sorted.indices) {
            val duration = if (i < sorted.lastIndex) {
                (sorted[i + 1].epochMillis - sorted[i].epochMillis).coerceAtLeast(0L)
            } else {
                defaultSamplePeriodMillis
            }
            val bpm = sorted[i].bpm
            val zoneIdx = zones.indexOfFirst { z ->
                if (z.definition.index == zones.last().definition.index) {
                    bpm >= z.lowerBpm && bpm <= z.upperBpm
                } else {
                    bpm >= z.lowerBpm && bpm < z.upperBpm
                }
            }
            if (zoneIdx >= 0) totals[zoneIdx] += duration
        }

        return zones.mapIndexed { idx, z -> TimeInZone(z, totals[idx]) }
    }
}
