package com.omnigart.shared.food.catalogue

import com.omnigart.shared.food.models.CatalogueFood
import com.omnigart.shared.food.models.FoodSource
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Bundled / downloadable USDA FoodData Central subset pack.
 * Production may ship a gzip-compressed SQLite file with the same logical rows
 * (see `content/food/fixtures/usda-demo.sqlite` and [UsdaSqliteSchema]);
 * this NDJSON pack is the portable parse path for KMP tests and first boot.
 *
 * Data is public domain (USDA). Never NCCDB.
 */
@Serializable
data class UsdaPackManifest(
    val schema: String = SCHEMA,
    val title: String,
    val datasets: List<String> = listOf("Foundation", "SR Legacy", "FNDDS"),
    val foodCount: Int,
    val foodsFile: String = "foods.ndjson",
    val license: String = "USDA FoodData Central — public domain",
    val optionalSqliteFile: String? = "usda-demo.sqlite"
) {
    companion object {
        const val SCHEMA: String = "omnigart.usda.pack/v1"
    }
}

data class UsdaPack(val manifest: UsdaPackManifest, val foods: List<CatalogueFood>)

object UsdaPackParser {
    private val json =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

    fun parseManifest(manifestJson: String): UsdaPackManifest {
        val m = json.decodeFromString(UsdaPackManifest.serializer(), manifestJson)
        require(m.schema == UsdaPackManifest.SCHEMA) { "Unsupported USDA pack schema ${m.schema}" }
        require(m.foodCount >= 0)
        return m
    }

    fun parseFoodsNdjson(ndjson: String, expectedCount: Int? = null): List<CatalogueFood> {
        val foods = ArrayList<CatalogueFood>()
        ndjson.lineSequence().forEach { raw ->
            val line = raw.trim()
            if (line.isEmpty()) return@forEach
            val row = json.decodeFromString(UsdaFoodRow.serializer(), line)
            foods += row.toCatalogue()
        }
        if (expectedCount != null) {
            require(foods.size == expectedCount) {
                "Expected $expectedCount USDA foods, got ${foods.size}"
            }
        }
        return foods
    }

    fun parsePack(manifestJson: String, foodsNdjson: String): UsdaPack {
        val manifest = parseManifest(manifestJson)
        return UsdaPack(manifest, parseFoodsNdjson(foodsNdjson, manifest.foodCount))
    }

    @Serializable
    data class UsdaFoodRow(
        val id: String,
        val name: String,
        val category: String? = null,
        val energyKcalPer100g: Double,
        val proteinGPer100g: Double = 0.0,
        val carbsGPer100g: Double = 0.0,
        val fatGPer100g: Double = 0.0,
        val fibreGPer100g: Double = 0.0,
        val sugarsGPer100g: Double = 0.0,
        val sodiumMgPer100g: Double = 0.0,
        val microsPer100g: Map<String, Double> = emptyMap(),
        val namesByLocale: Map<String, String> = emptyMap()
    ) {
        fun toCatalogue(): CatalogueFood = CatalogueFood(
            id = id,
            name = name,
            category = category,
            energyKcalPer100g = energyKcalPer100g,
            proteinGPer100g = proteinGPer100g,
            carbsGPer100g = carbsGPer100g,
            fatGPer100g = fatGPer100g,
            fibreGPer100g = fibreGPer100g,
            sugarsGPer100g = sugarsGPer100g,
            sodiumMgPer100g = sodiumMgPer100g,
            microsPer100g = microsPer100g,
            source = FoodSource.USDA,
            namesByLocale = namesByLocale
        )
    }
}

/** SQL schema for optional compressed SQLite downloads of the same catalogue. */
object UsdaSqliteSchema {
    // Not `const`: Native requires compile-time constants; trimIndent() is not one.
    val CREATE_SQL: String =
        """
        CREATE TABLE IF NOT EXISTS usda_food (
          id TEXT PRIMARY KEY NOT NULL,
          name TEXT NOT NULL,
          category TEXT,
          energy_kcal_per_100g REAL NOT NULL,
          protein_g_per_100g REAL NOT NULL DEFAULT 0,
          carbs_g_per_100g REAL NOT NULL DEFAULT 0,
          fat_g_per_100g REAL NOT NULL DEFAULT 0,
          fibre_g_per_100g REAL NOT NULL DEFAULT 0,
          sugars_g_per_100g REAL NOT NULL DEFAULT 0,
          sodium_mg_per_100g REAL NOT NULL DEFAULT 0,
          micros_json TEXT NOT NULL DEFAULT '{}',
          names_json TEXT NOT NULL DEFAULT '{}'
        );
        CREATE INDEX IF NOT EXISTS usda_food_name ON usda_food(name);
        """.trimIndent()
}

/**
 * Optional download of a larger USDA SQLite/gzip pack.
 * Platforms implement network + cache; food logging never requires a cloud account.
 */
fun interface UsdaPackDownloader {
    /** Returns local filesystem path or null if skipped / offline-only. */
    suspend fun ensureLocalPack(url: String, offlineOnly: Boolean): String?
}
