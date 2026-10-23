package com.omnigart.shared.food.catalogue

import com.omnigart.shared.food.barcode.BarcodeCheckDigit
import com.omnigart.shared.food.models.CatalogueFood
import com.omnigart.shared.food.models.FoodSource

/**
 * Bundled / downloaded nutrient catalogue.
 * Online lookups (Open Food Facts Search-a-licious) are optional and must be
 * gated by offline-only mode — queries leave the device when online.
 */
interface FoodCatalogue {
    fun findByBarcode(barcode: String): CatalogueFood?

    fun search(query: String, locale: String, limit: Int = 25): List<CatalogueFood>

    fun all(): List<CatalogueFood>

    val size: Int
}

/** Tiny in-memory USDA-style foundation slice for tests and offline demos. */
class BundledUsdaCatalogue(private val foods: List<CatalogueFood>) : FoodCatalogue {
    private val byBarcode: Map<String, CatalogueFood> =
        foods.mapNotNull { f ->
            f.barcode?.let { BarcodeCheckDigit.normalize(it) to f }
        }.toMap()

    override val size: Int get() = foods.size

    override fun findByBarcode(barcode: String): CatalogueFood? = byBarcode[BarcodeCheckDigit.normalize(barcode)]

    override fun search(query: String, locale: String, limit: Int): List<CatalogueFood> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return emptyList()
        return foods
            .asSequence()
            .filter {
                it.displayName(locale).lowercase().contains(q) ||
                    it.name.lowercase().contains(q) ||
                    it.brands?.lowercase()?.contains(q) == true
            }.take(limit)
            .toList()
    }

    override fun all(): List<CatalogueFood> = foods

    companion object {
        /** Public-domain style synthetic foundation foods (not a USDA dump). */
        fun demo(): BundledUsdaCatalogue = BundledUsdaCatalogue(
            listOf(
                CatalogueFood(
                    id = "usda-demo-oats",
                    name = "Oats, rolled, dry",
                    category = "cereal",
                    energyKcalPer100g = 379.0,
                    proteinGPer100g = 13.0,
                    carbsGPer100g = 67.0,
                    fatGPer100g = 6.5,
                    fibreGPer100g = 10.0,
                    microsPer100g =
                    mapOf(
                        "iron_mg" to 4.0,
                        "magnesium_mg" to 138.0,
                        "thiamin_mg" to 0.5
                    ),
                    source = FoodSource.USDA,
                    namesByLocale =
                    mapOf(
                        "en-GB" to "Oats, rolled, dry",
                        "uk" to "Вівсяні пластівці, сухі"
                    )
                ),
                CatalogueFood(
                    id = "usda-demo-spinach",
                    name = "Spinach, raw",
                    category = "vegetable",
                    energyKcalPer100g = 23.0,
                    proteinGPer100g = 2.9,
                    carbsGPer100g = 3.6,
                    fatGPer100g = 0.4,
                    fibreGPer100g = 2.2,
                    microsPer100g =
                    mapOf(
                        "vitamin_a_rae_ug" to 469.0,
                        "vitamin_c_mg" to 28.0,
                        "vitamin_k_ug" to 483.0,
                        "iron_mg" to 2.7,
                        "folate_ug" to 194.0
                    ),
                    source = FoodSource.USDA
                ),
                CatalogueFood(
                    id = "usda-demo-yogurt",
                    name = "Yogurt, plain, low fat",
                    category = "dairy",
                    energyKcalPer100g = 63.0,
                    proteinGPer100g = 5.3,
                    carbsGPer100g = 7.0,
                    fatGPer100g = 1.6,
                    microsPer100g =
                    mapOf(
                        "calcium_mg" to 183.0,
                        "vitamin_b12_ug" to 0.6,
                        "riboflavin_mg" to 0.2
                    ),
                    source = FoodSource.USDA
                ),
                CatalogueFood(
                    id = "usda-demo-salmon",
                    name = "Salmon, Atlantic, cooked",
                    category = "fish",
                    energyKcalPer100g = 206.0,
                    proteinGPer100g = 22.0,
                    carbsGPer100g = 0.0,
                    fatGPer100g = 12.0,
                    microsPer100g =
                    mapOf(
                        "vitamin_d_ug" to 11.0,
                        "vitamin_b12_ug" to 2.8,
                        "selenium_ug" to 41.0
                    ),
                    source = FoodSource.USDA
                ),
                CatalogueFood(
                    id = "off-demo-banana",
                    name = "Banana",
                    barcode = "0000000004011",
                    category = "fruit",
                    energyKcalPer100g = 89.0,
                    proteinGPer100g = 1.1,
                    carbsGPer100g = 23.0,
                    fatGPer100g = 0.3,
                    fibreGPer100g = 2.6,
                    microsPer100g =
                    mapOf(
                        "potassium_mg" to 358.0,
                        "vitamin_b6_mg" to 0.4,
                        "vitamin_c_mg" to 8.7
                    ),
                    source = FoodSource.OPEN_FOOD_FACTS
                )
            )
        )
    }
}

/**
 * Composite: prefer offline packs, optionally fall back to an online search.
 * Never calls [online] when [offlineOnly] is true.
 */
class CompositeFoodCatalogue(
    private val offline: FoodCatalogue,
    private val online: FoodCatalogue? = null,
    private val offlineOnly: Boolean = false
) : FoodCatalogue {
    override val size: Int get() = offline.size

    override fun findByBarcode(barcode: String): CatalogueFood? {
        offline.findByBarcode(barcode)?.let { return it }
        if (offlineOnly) return null
        return online?.findByBarcode(barcode)
    }

    override fun search(query: String, locale: String, limit: Int): List<CatalogueFood> {
        val local = offline.search(query, locale, limit)
        if (local.size >= limit || offlineOnly || online == null) return local
        val remote = online.search(query, locale, limit - local.size)
        val seen = local.map { it.id }.toHashSet()
        return local + remote.filter { seen.add(it.id) }
    }

    override fun all(): List<CatalogueFood> = offline.all()
}

/**
 * Privacy note for UI: online lookups send the query string to Open Food Facts.
 */
object FoodCataloguePrivacy {
    const val ONLINE_LOOKUP_NOTE_EN_GB: String =
        "Online food search sends your query to Open Food Facts. " +
            "Turn on offline-only mode to keep lookups on this device."
}
