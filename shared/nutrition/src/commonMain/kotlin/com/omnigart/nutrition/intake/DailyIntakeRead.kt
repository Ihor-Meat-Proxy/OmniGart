package com.omnigart.nutrition.intake

import com.omnigart.nutrition.DayLogStatus

/**
 * Small read interface for daily intake totals and targets.
 *
 * Diary storage, barcodes, label scan, and food DB live in a separate food-logging PR.
 * Hosts implement this against their diary; the nutrition engine only consumes totals.
 */
interface DailyIntakeRead {
    /** Totals for a single local day key (yyyy-MM-dd relative to [dayStartMinutes]). */
    suspend fun dayTotals(dayKey: String): DailyIntakeTotals?

    /** Recent days newest-last, for adaptive windows. */
    suspend fun recentDays(limit: Int): List<DailyIntakeTotals>
}

data class DailyIntakeTotals(
    val dayKey: String,
    val energyKcal: Double,
    val proteinG: Double = 0.0,
    val carbsG: Double = 0.0,
    val fatG: Double = 0.0,
    val fibreG: Double = 0.0,
    val waterMl: Double = 0.0,
    val status: DayLogStatus = DayLogStatus.COMPLETE,
    /** Host may set true when the user marked an intentional fasting day. */
    val fasting: Boolean = false,
)

/**
 * Targets the food diary UI can display without owning the energy model.
 */
data class DailyMacroTargets(
    val dayKey: String,
    val energyKcal: Double,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
    val fibreG: Double,
    val waterLitres: Double,
)

interface DailyTargetRead {
    fun targetsForDay(dayKey: String, weekday: Int): DailyMacroTargets?
}
