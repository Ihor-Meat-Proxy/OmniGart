package com.omnigart.nutrition.lookup

import com.omnigart.nutrition.model.FoodProduct
import com.omnigart.nutrition.pack.OffPack

/** Normalizes retail barcodes for map keys (digits only). */
fun normalizeBarcode(raw: String): String =
    raw.filter { it.isDigit() }

/**
 * Barcode → product lookup. Implementations may be in-memory, SQLDelight, etc.
 */
interface BarcodeLookup {
    fun findByBarcode(barcode: String): FoodProduct?

    fun contains(barcode: String): Boolean = findByBarcode(barcode) != null

    /** Approximate catalogue size when known. */
    val size: Int
}

/** Immutable hash-map index built from an [OffPack]. */
class InMemoryBarcodeIndex private constructor(
    private val byBarcode: Map<String, FoodProduct>,
) : BarcodeLookup {
    override val size: Int get() = byBarcode.size

    override fun findByBarcode(barcode: String): FoodProduct? {
        val key = normalizeBarcode(barcode)
        if (key.isEmpty()) return null
        return byBarcode[key]
    }

    companion object {
        fun fromPack(pack: OffPack): InMemoryBarcodeIndex =
            fromProducts(pack.products)

        fun fromProducts(products: Iterable<FoodProduct>): InMemoryBarcodeIndex {
            val map = LinkedHashMap<String, FoodProduct>()
            for (product in products) {
                val key = normalizeBarcode(product.barcode)
                require(key.isNotEmpty()) { "Product missing numeric barcode: ${product.name}" }
                // Last write wins on duplicates — mirrors typical dump import behaviour.
                map[key] = product.copy(barcode = key)
            }
            return InMemoryBarcodeIndex(map)
        }
    }
}
