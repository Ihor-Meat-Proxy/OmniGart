package com.omnigart.hydration.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class BeverageCategory {
    @SerialName("water") WATER,
    @SerialName("sparkling_water") SPARKLING_WATER,
    @SerialName("coffee") COFFEE,
    @SerialName("tea") TEA,
    @SerialName("milk") MILK,
    @SerialName("plant_milk") PLANT_MILK,
    @SerialName("juice") JUICE,
    @SerialName("soft_drink") SOFT_DRINK,
    @SerialName("sports_drink") SPORTS_DRINK,
    @SerialName("fermented_dairy") FERMENTED_DAIRY,
    @SerialName("beer") BEER,
    @SerialName("wine") WINE,
}

/**
 * How logged volume contributes toward the daily beverage target.
 *
 * Default is [HYDRATION_ADJUSTED]: multiply volume by the Beverage Hydration Index
 * (Maughan et al. 2016, DOI 10.3945/ajcn.115.114769). Coffee is treated as ≈ water
 * for net fluid balance in habituated drinkers (Killer et al. 2014,
 * DOI 10.1371/journal.pone.0084154).
 */
@Serializable
enum class HydrationCountingMode {
    /** volume_ml × beverage.hydrationFactor (BHI). Evidence-based default. */
    @SerialName("hydration_adjusted") HYDRATION_ADJUSTED,

    /** Sum of logged volumes with no BHI weighting. */
    @SerialName("raw_volume") RAW_VOLUME,

    /** Count only beverages whose category is water or sparkling water. */
    @SerialName("water_only") WATER_ONLY,
}

@Serializable
data class LocalizedNameKeys(
    val enGB: String,
    val uk: String,
)

@Serializable
data class MacrosPer100ml(
    /** Kilocalories per 100 ml. */
    val energyKcal: Double,
    val proteinG: Double,
    val carbohydrateG: Double,
    val fatG: Double,
    val sugarsG: Double = 0.0,
    val sodiumMg: Double = 0.0,
)

@Serializable
data class EvidenceRef(
    val kind: String,
    val id: String,
    val label: String,
    val supports: List<String> = emptyList(),
)

@Serializable
data class Beverage(
    val id: String,
    val nameKeys: LocalizedNameKeys,
    val category: BeverageCategory,
    /** Default serving size in millilitres. */
    val defaultServingMl: Int,
    /** Typical caffeine for [defaultServingMl], milligrams (EFSA 2015 / USDA FDC). */
    val caffeineMgPerServing: Double,
    /** Nutrition density per 100 ml (USDA FoodData Central where available). */
    val macrosPer100ml: MacrosPer100ml,
    /**
     * Beverage Hydration Index relative to still water (= 1.0).
     * Values from Maughan 2016 where measured; otherwise documented estimates.
     */
    val hydrationFactor: Double,
    val evidenceRefs: List<EvidenceRef> = emptyList(),
    /** True when [hydrationFactor] is extrapolated, not measured in Maughan 2016. */
    val hydrationFactorEstimated: Boolean = false,
    val notes: String? = null,
) {
    fun caffeineMgForVolume(volumeMl: Double): Double {
        if (defaultServingMl <= 0) return 0.0
        return caffeineMgPerServing * (volumeMl / defaultServingMl.toDouble())
    }

    fun macrosForVolume(volumeMl: Double): MacrosPer100ml {
        val scale = volumeMl / 100.0
        return MacrosPer100ml(
            energyKcal = macrosPer100ml.energyKcal * scale,
            proteinG = macrosPer100ml.proteinG * scale,
            carbohydrateG = macrosPer100ml.carbohydrateG * scale,
            fatG = macrosPer100ml.fatG * scale,
            sugarsG = macrosPer100ml.sugarsG * scale,
            sodiumMg = macrosPer100ml.sodiumMg * scale,
        )
    }

    val isPlainWater: Boolean
        get() = category == BeverageCategory.WATER || category == BeverageCategory.SPARKLING_WATER
}

@Serializable
data class DrinkLogEntry(
    val id: String,
    val beverageId: String,
    /** Instant of logging, epoch milliseconds UTC. */
    val loggedAtEpochMs: Long,
    val volumeMl: Double,
    val source: String = "manual",
)

/**
 * Copy shown next to the counting-mode picker (ⓘ).
 */
object CountingModeInfo {
    const val DEFAULT_MODE = "hydration_adjusted"

    const val INFO_EN_GB =
        "Hydration-adjusted counting multiplies each drink’s volume by its Beverage " +
            "Hydration Index (BHI). Still water is 1.0. Full-fat and skimmed milk scored " +
            "about 1.5–1.6 over two hours versus water; sparkling water, tea, coffee, cola, " +
            "sports drink and lager were not different from water (Maughan et al., Am J Clin Nutr " +
            "2016; DOI 10.3945/ajcn.115.114769). Moderate coffee also matched water for net fluid " +
            "balance in habituated drinkers (Killer et al., PLOS ONE 2014; DOI " +
            "10.1371/journal.pone.0084154). Raw volume ignores BHI. Water-only ignores non-water drinks."

    const val INFO_UK =
        "Підрахунок з поправкою на гідратацію множить об’єм напою на індекс гідратації (BHI). " +
            "Звичайна вода = 1,0. У Maughan et al. (2016) молоко давало вищий BHI (~1,5–1,6), " +
            "а газована вода, чай, кава, кола, спортивний напій і лагер не відрізнялися від води. " +
            "Помірне споживання кави в звиклих людей також відповідало воді (Killer et al., 2014). " +
            "«Сирий об’єм» ігнорує BHI. «Лише вода» враховує тільки воду та газовану воду."
}
