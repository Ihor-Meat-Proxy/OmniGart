package com.omnigart.nutrition.pack

import com.omnigart.nutrition.model.FoodProduct
import kotlinx.serialization.Serializable

/**
 * Open Food Facts offline pack — versioned, hashable, CDN-friendly.
 *
 * Typical layout inside a signed content-plane `off` pack archive:
 * ```
 * manifest.json
 * products.ndjson   # one FoodProduct JSON object per line
 * ATTRIBUTION.txt
 * ```
 */
@Serializable
data class OffPackManifest(
    val schema: String = SCHEMA,
    val id: String = "food-offline",
    val version: String,
    val minAppVersion: String = "0.1.0",
    val createdAt: String,
    val locale: String? = null,
    val productCount: Int,
    val productsFile: String = "products.ndjson",
    val productsSha256: String,
    val productsSize: Long,
    val license: OffLicenseNotice = OffLicenseNotice(),
    val source: OffSourceMeta = OffSourceMeta(),
) {
    companion object {
        const val SCHEMA: String = "omnigart.off-pack/1"
    }
}

@Serializable
data class OffLicenseNotice(
    val name: String = "ODbL-1.0",
    val odblUrl: String = "https://opendatacommons.org/licenses/odbl/1.0/",
    val attribution: String =
        "Data from Open Food Facts — https://world.openfoodfacts.org/ — " +
            "available under the Open Database Licence (ODbL).",
    val shareAlike: Boolean = true,
)

@Serializable
data class OffSourceMeta(
    val provider: String = "openfoodfacts",
    val dumpTag: String? = null,
    val country: String? = null,
    val extractedAt: String? = null,
)

@Serializable
data class OffPack(
    val manifest: OffPackManifest,
    val products: List<FoodProduct>,
)
