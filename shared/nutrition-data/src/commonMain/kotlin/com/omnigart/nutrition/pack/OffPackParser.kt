package com.omnigart.nutrition.pack

import com.omnigart.nutrition.model.FoodProduct
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * Parses an OmniGart OFF offline pack from in-memory strings
 * (apps supply file bytes after download / unzip).
 */
class OffPackParser(
    private val json: Json = defaultJson,
) {
    fun parseManifest(manifestJson: String): OffPackManifest {
        val manifest = json.decodeFromString(OffPackManifest.serializer(), manifestJson)
        require(manifest.schema == OffPackManifest.SCHEMA) {
            "Unsupported OFF pack schema: ${manifest.schema}"
        }
        require(manifest.productCount >= 0) { "productCount must be >= 0" }
        require(manifest.productsFile.isNotBlank()) { "productsFile required" }
        require(manifest.productsSha256.matches(SHA256_HEX)) {
            "productsSha256 must be 64 lowercase hex chars"
        }
        return manifest
    }

    /**
     * Parse NDJSON products file. Blank lines are ignored.
     * When [expectedCount] is set, enforces exact cardinality.
     */
    fun parseProductsNdjson(
        ndjson: String,
        expectedCount: Int? = null,
    ): List<FoodProduct> {
        val products = ArrayList<FoodProduct>()
        ndjson.lineSequence().forEachIndexed { index, raw ->
            val line = raw.trim()
            if (line.isEmpty()) return@forEachIndexed
            try {
                products += json.decodeFromString(FoodProduct.serializer(), line)
            } catch (e: SerializationException) {
                throw OffPackParseException("Invalid product JSON at line ${index + 1}: ${e.message}", e)
            }
        }
        if (expectedCount != null && products.size != expectedCount) {
            throw OffPackParseException(
                "Expected $expectedCount products, parsed ${products.size}",
            )
        }
        return products
    }

    fun parsePack(manifestJson: String, productsNdjson: String): OffPack {
        val manifest = parseManifest(manifestJson)
        val products = parseProductsNdjson(productsNdjson, expectedCount = manifest.productCount)
        return OffPack(manifest = manifest, products = products)
    }

    companion object {
        private val SHA256_HEX = Regex("^[a-f0-9]{64}$")

        val defaultJson: Json = Json {
            ignoreUnknownKeys = true
            isLenient = false
            explicitNulls = false
        }
    }
}

class OffPackParseException(
    message: String,
    cause: Throwable? = null,
) : IllegalArgumentException(message, cause)
