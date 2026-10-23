package com.omnigart.nutrition

import com.omnigart.nutrition.lookup.InMemoryBarcodeIndex
import com.omnigart.nutrition.lookup.normalizeBarcode
import com.omnigart.nutrition.pack.OffPackParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BarcodeLookupTest {
    private val parser = OffPackParser()

    private val pack =
        parser.parsePack(
            """
            {
              "schema": "omnigart.off-pack/1",
              "id": "food-offline",
              "version": "1.0.0",
              "createdAt": "1970-01-01T00:00:00.000Z",
              "productCount": 2,
              "productsFile": "products.ndjson",
              "productsSha256": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
              "productsSize": 1,
              "license": {
                "name": "ODbL-1.0",
                "odblUrl": "https://opendatacommons.org/licenses/odbl/1.0/",
                "attribution": "Data from Open Food Facts",
                "shareAlike": true
              }
            }
            """.trimIndent(),
            """
            {"barcode":"3017620422003","name":"Spread","nutrients":{"energyKcal":539.0}}
            {"barcode":"5449000000996","name":"Cola","nutrients":{"basis":"per_100ml","energyKcal":42.0}}
            """.trimIndent(),
        )

    @Test
    fun normalizeBarcode_stripsSpacesAndPunctuation() {
        assertEquals("3017620422003", normalizeBarcode("3017 6204-2200 3"))
    }

    @Test
    fun findByBarcode_hitsAndMisses() {
        val index = InMemoryBarcodeIndex.fromPack(pack)
        assertEquals(2, index.size)
        assertEquals("Spread", index.findByBarcode("3017 620422003")?.name)
        assertEquals("Cola", index.findByBarcode("5449000000996")?.name)
        assertNull(index.findByBarcode("0000000000000"))
        assertNull(index.findByBarcode("not-a-barcode"))
    }
}
