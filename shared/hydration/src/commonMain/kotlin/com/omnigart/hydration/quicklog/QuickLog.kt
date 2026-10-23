package com.omnigart.hydration.quicklog

import com.omnigart.hydration.models.Beverage

data class ServingPreset(
    val id: String,
    val beverageId: String,
    val volumeMl: Int,
    val labelEnGb: String,
)

data class QuickLogAction(
    val type: String,
    val beverageId: String,
    val volumeMl: Int,
)

/**
 * Favourites, recents, and notification action payloads for one-tap logging.
 */
object QuickLogLogic {

    const val ACTION_ADD_WATER_250 = "hydration.add_water_250"
    const val ACTION_ADD_COFFEE_DEFAULT = "hydration.add_coffee_default"
    const val ACTION_ADD_PRESET = "hydration.add_preset"

    val DEFAULT_WATER_PRESETS_ML: List<Int> = listOf(100, 250, 330, 500)

    fun defaultPresets(waterBeverageId: String = "water"): List<ServingPreset> =
        DEFAULT_WATER_PRESETS_ML.map { ml ->
            ServingPreset(
                id = "water-$ml",
                beverageId = waterBeverageId,
                volumeMl = ml,
                labelEnGb = "+$ml ml water",
            )
        }

    fun notificationPayloadWater250(waterBeverageId: String = "water"): QuickLogAction =
        QuickLogAction(ACTION_ADD_WATER_250, waterBeverageId, 250)

    fun notificationPayloadCoffee(coffee: Beverage): QuickLogAction =
        QuickLogAction(ACTION_ADD_COFFEE_DEFAULT, coffee.id, coffee.defaultServingMl)

    fun notificationPayloadPreset(preset: ServingPreset): QuickLogAction =
        QuickLogAction(ACTION_ADD_PRESET, preset.beverageId, preset.volumeMl)

    /**
     * Recents: most recently used beverage ids, de-duplicated, capped.
     * [ ChronologicalUsage ] is newest-first beverage ids (repeats allowed).
     */
    fun recents(chronologicalUsageNewestFirst: List<String>, limit: Int = 8): List<String> {
        val seen = linkedSetOf<String>()
        for (id in chronologicalUsageNewestFirst) {
            if (seen.add(id) && seen.size >= limit) break
        }
        return seen.toList()
    }

    fun favouritesInCatalogueOrder(
        favouriteIds: List<String>,
        catalogueIds: List<String>,
    ): List<String> {
        val fav = favouriteIds.toSet()
        return catalogueIds.filter { it in fav }
    }

    fun mergeQuickTargets(
        favourites: List<String>,
        recents: List<String>,
        limit: Int = 6,
    ): List<String> {
        val out = linkedSetOf<String>()
        for (id in favourites + recents) {
            out.add(id)
            if (out.size >= limit) break
        }
        return out.toList()
    }
}
