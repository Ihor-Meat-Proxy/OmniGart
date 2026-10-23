package com.omnigart.shared.food.targets

/**
 * Read-only seam for calorie / macro targets owned by the parallel nutrition-model PR
 * (`com.omnigart.shared.nutrition`). Food logging must not reimplement TDEE, adaptive
 * energy, body measurements, or macro goal engines — only consume this interface.
 *
 * When the nutrition-model module is not merged yet, inject [StubNutritionTargetsReader].
 */
interface NutritionTargetsReader {
    /** Minutes after local midnight when the nutrition day starts (default 04:00 → 240). */
    fun dayStartMinutes(profileId: String): Int

    fun targetsForDay(profileId: String, dayKey: String): NutritionDayTargets

    /** True when the low-intake guard is active (suppress AI review suggestions). */
    fun isLowIntakeGuardActive(profileId: String, dayKey: String): Boolean = false

    fun isUnderEighteen(profileId: String): Boolean = false

    fun isPregnancyMode(profileId: String): Boolean = false
}

/**
 * Daily energy/macro targets for diary comparison.
 * Micronutrient reference intakes live in the food module (EFSA/IOM tables).
 */
data class NutritionDayTargets(
    val energyKcal: Double? = null,
    val proteinG: Double? = null,
    val carbsG: Double? = null,
    val fatG: Double? = null,
    val fibreG: Double? = null,
    val waterLitres: Double? = null,
    val dataInsufficient: Boolean = false
)

/**
 * Safe defaults until the nutrition-model PR lands. Values are gentle wellness
 * placeholders — not personalised medical advice.
 */
class StubNutritionTargetsReader(
    private val dayStartMinutes: Int = DEFAULT_DAY_START_MINUTES,
    private val defaults: NutritionDayTargets = NutritionDayTargets(
        energyKcal = 2_000.0,
        proteinG = 50.0,
        carbsG = 260.0,
        fatG = 70.0,
        fibreG = 25.0,
        waterLitres = 2.0,
        dataInsufficient = true
    )
) : NutritionTargetsReader {
    override fun dayStartMinutes(profileId: String): Int = dayStartMinutes

    override fun targetsForDay(profileId: String, dayKey: String): NutritionDayTargets = defaults

    companion object {
        const val DEFAULT_DAY_START_MINUTES: Int = 4 * 60
    }
}

/**
 * Day-key helper shared with nutrition-model day boundaries.
 * [dayStartMinutes] is minutes after local midnight.
 */
object FoodDayKeys {
    /**
     * Returns `yyyy-MM-dd` for the nutrition day containing [epochMs].
     * Uses a simple UTC-offset approximation via [utcOffsetMinutes] so commonMain
     * stays free of platform calendar APIs; callers pass the zone offset.
     */
    fun dayKey(epochMs: Long, dayStartMinutes: Int, utcOffsetMinutes: Int = 0): String {
        val localMs = epochMs + utcOffsetMinutes * 60_000L
        val dayStartMs = dayStartMinutes * 60_000L
        val shifted = localMs - dayStartMs
        val days = shifted.floorDiv(86_400_000L)
        // Civil date from days since Unix epoch in the shifted timeline.
        return civilDateFromEpochDay(days)
    }

    private fun civilDateFromEpochDay(epochDay: Long): String {
        // Algorithm from civil_from_days (Howard Hinnant), public domain.
        val z = epochDay + 719_468L
        val era = (if (z >= 0) z else z - 146_096) / 146_097
        val doe = z - era * 146_097
        val yoe = (doe - doe / 1_460 + doe / 36_524 - doe / 146_096) / 365
        val y = yoe + era * 400
        val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
        val mp = (5 * doy + 2) / 153
        val d = doy - (153 * mp + 2) / 5 + 1
        val m = mp + (if (mp < 10) 3 else -9)
        val year = y + (if (m <= 2) 1 else 0)
        return "${year.toString().padStart(4, '0')}-" +
            "${m.toString().padStart(2, '0')}-" +
            "${d.toString().padStart(2, '0')}"
    }
}
