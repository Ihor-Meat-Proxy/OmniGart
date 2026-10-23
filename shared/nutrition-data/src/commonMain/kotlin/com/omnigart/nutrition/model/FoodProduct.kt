package com.omnigart.nutrition.model

import kotlinx.serialization.Serializable

/**
 * Minimal product record for offline barcode lookup.
 * [barcode] is preferably EAN-13 / UPC-A digits without spaces.
 */
@Serializable
data class FoodProduct(
    val barcode: String,
    val name: String,
    val brands: String? = null,
    val quantity: String? = null,
    val servingSizeG: Double? = null,
    val nutrients: Nutrients = Nutrients(),
    val nutriscoreGrade: String? = null,
    val novaGroup: Int? = null,
    val categories: List<String> = emptyList(),
    val source: String = "openfoodfacts",
)
