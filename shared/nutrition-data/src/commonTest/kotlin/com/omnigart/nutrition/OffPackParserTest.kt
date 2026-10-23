package com.omnigart.nutrition

import com.omnigart.nutrition.model.NutrientBasis
import com.omnigart.nutrition.pack.OffPackParseException
import com.omnigart.nutrition.pack.OffPackParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class OffPackParserTest {
    private val parser = OffPackParser()

    private val manifestJson =
        """
        {
          "schema": "omnigart.off-pack/1",
          "id": "food-offline",
          "version": "1.0.0-fixture",
          "minAppVersion": "0.1.0",
          "createdAt": "1970-01-01T00:00:00.000Z",
          "productCount": 2,
          "productsFile": "products.ndjson",
          "productsSha256": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
          "productsSize": 12,
          "license": {
            "name": "ODbL-1.0",
            "odblUrl": "https://opendatacommons.org/licenses/odbl/1.0/",
            "attribution": "Data from Open Food Facts",
            "shareAlike": true
          },
          "source": { "provider": "openfoodfacts" }
        }
        """.trimIndent()

    private val productsNdjson =
        """
        {"barcode":"111","name":"A","nutrients":{"basis":"per_100g","energyKcal":100.0,"proteinsG":5.0}}
        {"barcode":"222","name":"B","nutrients":{"basis":"per_100ml","energyKcal":40.0,"sugarsG":9.0}}
        """.trimIndent()

    @Test
    fun parseManifest_readsLicenseAndCounts() {
        val m = parser.parseManifest(manifestJson)
        assertEquals("omnigart.off-pack/1", m.schema)
        assertEquals(2, m.productCount)
        assertEquals("ODbL-1.0", m.license.name)
        assertTrue(m.license.attribution.contains("Open Food Facts"))
    }

    @Test
    fun parsePack_loadsProducts() {
        val pack = parser.parsePack(manifestJson, productsNdjson)
        assertEquals(2, pack.products.size)
        assertEquals("A", pack.products[0].name)
        assertEquals(NutrientBasis.PER_100ML, pack.products[1].nutrients.basis)
    }

    @Test
    fun parsePack_rejectsCountMismatch() {
        val badManifest = manifestJson.replace("\"productCount\": 2", "\"productCount\": 3")
        assertFailsWith<OffPackParseException> {
            parser.parsePack(badManifest, productsNdjson)
        }
    }

    @Test
    fun parseManifest_rejectsUnknownSchema() {
        val bad = manifestJson.replace("omnigart.off-pack/1", "omnigart.off-pack/9")
        assertFailsWith<IllegalArgumentException> {
            parser.parseManifest(bad)
        }
    }
}
