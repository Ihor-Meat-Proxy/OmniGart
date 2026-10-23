package com.omnigart.shared.food.catalogue

import com.omnigart.shared.food.barcode.BarcodeCheckDigit
import com.omnigart.shared.food.models.CatalogueFood
import com.omnigart.shared.food.models.FoodSource
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Open Food Facts online lookup: Search-a-licious first, legacy CGI / product API fallback.
 * Callers must honour offline-only mode (do not invoke when offline-only is on).
 * Privacy: the query / barcode leaves the device.
 */
class OffOnlineSearchClient(
    private val http: HttpClient,
    private val json: Json =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
) {
    suspend fun findByBarcode(barcode: String): CatalogueFood? {
        val code = BarcodeCheckDigit.normalize(barcode)
        if (code.isEmpty() || !BarcodeCheckDigit.isValid(code)) return null
        val response = http.get("https://world.openfoodfacts.org/api/v2/product/$code.json")
        if (!response.status.isSuccess()) return null
        return parseProductV2(response.bodyAsText(), code)
    }

    /**
     * Search-a-licious, then legacy `search.pl` if empty or HTTP failure.
     * [locale] selects preferred `product_name_<lang>` when present.
     */
    suspend fun search(query: String, locale: String, limit: Int = 25): List<CatalogueFood> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()
        val primary =
            runCatching { searchAlicious(q, locale, limit) }.getOrDefault(emptyList())
        if (primary.isNotEmpty()) return primary
        return runCatching { searchLegacy(q, locale, limit) }.getOrDefault(emptyList())
    }

    private suspend fun searchAlicious(query: String, locale: String, limit: Int): List<CatalogueFood> {
        val lang = locale.substringBefore('-').lowercase()
        val url =
            "https://search.openfoodfacts.org/search?q=${encode(query)}&page_size=$limit&langs=$lang"
        val response = http.get(url)
        if (!response.status.isSuccess()) return emptyList()
        val body = response.bodyAsText()
        val alicious =
            runCatching { json.decodeFromString(SearchAliciousResponse.serializer(), body) }.getOrNull()
        if (alicious != null && alicious.hits.isNotEmpty()) {
            return alicious.hits.mapNotNull { it.toCatalogue(locale) }
        }
        return parseProductsArray(body, locale, limit)
    }

    private suspend fun searchLegacy(query: String, locale: String, limit: Int): List<CatalogueFood> {
        val url =
            "https://world.openfoodfacts.org/cgi/search.pl?" +
                "search_terms=${encode(query)}&search_simple=1&action=process&json=1&page_size=$limit"
        val response = http.get(url)
        if (!response.status.isSuccess()) return emptyList()
        return parseProductsArray(response.bodyAsText(), locale, limit)
    }

    private fun parseProductsArray(body: String, locale: String, limit: Int): List<CatalogueFood> {
        val root = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull() ?: return emptyList()
        val arr = root["hits"] ?: root["products"] ?: return emptyList()
        val elements = runCatching { arr.jsonArray }.getOrNull() ?: return emptyList()
        return elements.take(limit).mapNotNull { el ->
            val obj = el.jsonObject
            val hit =
                SearchHit(
                    code = obj["code"]?.jsonPrimitive?.content,
                    productName =
                    localizedName(obj, locale)
                        ?: obj["product_name"]?.jsonPrimitive?.content,
                    brands = obj["brands"]?.jsonPrimitive?.content,
                    nutriments = nutrimentMap(obj["nutriments"]?.jsonObject)
                )
            hit.toCatalogue(locale)
        }
    }

    private fun parseProductV2(body: String, barcode: String): CatalogueFood? {
        val root = json.parseToJsonElement(body).jsonObject
        val product = root["product"]?.jsonObject ?: return null
        val name =
            localizedName(product, "en-GB")
                ?: product["product_name"]?.jsonPrimitive?.content
                ?: return null
        val nutriments = nutrimentMap(product["nutriments"]?.jsonObject)
        fun n(key: String): Double = nutriments[key] ?: 0.0
        return CatalogueFood(
            id = "off-$barcode",
            name = name,
            brands = product["brands"]?.jsonPrimitive?.content,
            barcode = barcode,
            energyKcalPer100g =
            n("energy-kcal_100g").takeIf { it > 0 }
                ?: (n("energy_100g") / 4.184),
            proteinGPer100g = n("proteins_100g"),
            carbsGPer100g = n("carbohydrates_100g"),
            fatGPer100g = n("fat_100g"),
            fibreGPer100g = n("fiber_100g"),
            sugarsGPer100g = n("sugars_100g"),
            sodiumMgPer100g = n("sodium_100g") * 1000.0,
            source = FoodSource.OPEN_FOOD_FACTS
        )
    }

    private fun localizedName(product: JsonObject, locale: String): String? {
        val lang = locale.substringBefore('-').lowercase()
        for (k in listOf("product_name_$lang", "product_name_en", "product_name")) {
            val v = product[k]?.jsonPrimitive?.content
            if (!v.isNullOrBlank()) return v
        }
        return null
    }

    private fun nutrimentMap(obj: JsonObject?): Map<String, Double?> {
        if (obj == null) return emptyMap()
        return obj.mapValues { (_, v) -> v.jsonPrimitive.doubleOrNull }
    }

    private fun encode(value: String): String = buildString {
        for (c in value) {
            when {
                c.isLetterOrDigit() || c in "-._~" -> append(c)
                c == ' ' -> append("%20")
                else -> append('%').append(c.code.toString(16).uppercase().padStart(2, '0'))
            }
        }
    }

    @Serializable
    private data class SearchAliciousResponse(val hits: List<SearchHit> = emptyList())

    @Serializable
    private data class SearchHit(
        val code: String? = null,
        @SerialName("product_name") val productName: String? = null,
        val brands: String? = null,
        val nutriments: Map<String, Double?>? = null
    ) {
        fun toCatalogue(locale: String): CatalogueFood? {
            val name = productName?.takeIf { it.isNotBlank() } ?: return null
            fun n(key: String): Double = nutriments?.get(key) ?: 0.0
            return CatalogueFood(
                id = "off-${code ?: name}",
                name = name,
                brands = brands,
                barcode = code,
                energyKcalPer100g = n("energy-kcal_100g"),
                proteinGPer100g = n("proteins_100g"),
                carbsGPer100g = n("carbohydrates_100g"),
                fatGPer100g = n("fat_100g"),
                fibreGPer100g = n("fiber_100g"),
                sugarsGPer100g = n("sugars_100g"),
                source = FoodSource.OPEN_FOOD_FACTS,
                namesByLocale = mapOf(locale to name)
            )
        }
    }
}
