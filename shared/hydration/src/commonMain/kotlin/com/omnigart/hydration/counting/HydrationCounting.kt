package com.omnigart.hydration.counting

import com.omnigart.hydration.models.Beverage
import com.omnigart.hydration.models.DrinkLogEntry
import com.omnigart.hydration.models.HydrationCountingMode

/**
 * Pure helpers that turn logged volumes into progress toward a daily beverage target.
 */
object HydrationCounting {

    /**
     * Contribution of one log line toward the daily target under [mode].
     * Always returns millilitres of “counted” fluid (never negative).
     */
    fun countedMl(
        entry: DrinkLogEntry,
        beverage: Beverage,
        mode: HydrationCountingMode,
    ): Double {
        require(entry.beverageId == beverage.id) {
            "Entry beverageId ${entry.beverageId} does not match ${beverage.id}"
        }
        val volume = entry.volumeMl.coerceAtLeast(0.0)
        return when (mode) {
            HydrationCountingMode.RAW_VOLUME -> volume
            HydrationCountingMode.WATER_ONLY -> if (beverage.isPlainWater) volume else 0.0
            HydrationCountingMode.HYDRATION_ADJUSTED -> volume * beverage.hydrationFactor.coerceAtLeast(0.0)
        }
    }

    fun totalCountedMl(
        entries: List<DrinkLogEntry>,
        beveragesById: Map<String, Beverage>,
        mode: HydrationCountingMode,
    ): Double =
        entries.sumOf { entry ->
            val beverage = beveragesById[entry.beverageId] ?: return@sumOf 0.0
            countedMl(entry, beverage, mode)
        }

    fun totalRawVolumeMl(entries: List<DrinkLogEntry>): Double =
        entries.sumOf { it.volumeMl.coerceAtLeast(0.0) }

    fun totalCaffeineMg(
        entries: List<DrinkLogEntry>,
        beveragesById: Map<String, Beverage>,
    ): Double =
        entries.sumOf { entry ->
            val beverage = beveragesById[entry.beverageId] ?: return@sumOf 0.0
            beverage.caffeineMgForVolume(entry.volumeMl.coerceAtLeast(0.0))
        }

    fun progressFraction(
        countedMl: Double,
        targetMl: Double,
    ): Double {
        if (targetMl <= 0.0) return 0.0
        return (countedMl / targetMl).coerceAtLeast(0.0)
    }
}
