package com.omnigart.android.food

import com.omnigart.shared.food.barcode.BarcodeCheckDigit
import com.omnigart.shared.food.label.NutritionLabelParser
import com.omnigart.shared.food.models.ServingNutrients

/**
 * Barcode check-digit + label OCR parse helpers.
 * Live CameraX / ML Kit TextRecognizer wiring attaches here when
 * [com.omnigart.shared.compliance.FeatureFlags.foodBarcodeEnabled] is on.
 */
object FoodLabelBarcodeHelpers {
    fun validateBarcode(raw: String): String? {
        val digits = BarcodeCheckDigit.normalize(raw)
        return digits.takeIf { BarcodeCheckDigit.isValid(it) }
    }

    fun parseLabelOcr(text: String): ServingNutrients? = NutritionLabelParser.parse(text)?.nutrients
}
