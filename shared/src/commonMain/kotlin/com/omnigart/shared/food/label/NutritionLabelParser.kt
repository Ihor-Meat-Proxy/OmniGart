package com.omnigart.shared.food.label

import com.omnigart.shared.food.models.ServingNutrients

/**
 * Heuristic parser for on-device OCR text from a nutrition label
 * (Vision on iOS / ML Kit text recognition on Android).
 *
 * Extracts common per-serving / per 100 g energy and macro lines.
 * Wellness aid only — users should confirm before logging.
 */
object NutritionLabelParser {
    private val energyKcal =
        Regex("""(?i)(?:energy|calories?)\s*[:=]?\s*(\d+(?:[.,]\d+)?)\s*(?:kcal|cal)\b""")
    private val energyKj =
        Regex("""(?i)(?:energy)?\s*[:=]?\s*(\d+(?:[.,]\d+)?)\s*kJ\b""")
    private val protein =
        Regex("""(?i)proteins?\s*[:=]?\s*(\d+(?:[.,]\d+)?)\s*g\b""")
    private val carbs =
        Regex("""(?i)(?:carbohydrates?|carbs?)\s*[:=]?\s*(\d+(?:[.,]\d+)?)\s*g\b""")
    private val fat =
        Regex("""(?i)(?:total\s+)?fat\s*[:=]?\s*(\d+(?:[.,]\d+)?)\s*g\b""")
    private val fibre =
        Regex("""(?i)(?:fibre|fiber)\s*[:=]?\s*(\d+(?:[.,]\d+)?)\s*g\b""")
    private val sugars =
        Regex("""(?i)sugars?\s*[:=]?\s*(\d+(?:[.,]\d+)?)\s*g\b""")
    private val sodium =
        Regex("""(?i)sodium\s*[:=]?\s*(\d+(?:[.,]\d+)?)\s*mg\b""")
    private val salt =
        Regex("""(?i)salt\s*[:=]?\s*(\d+(?:[.,]\d+)?)\s*g\b""")

    data class ParseResult(val nutrients: ServingNutrients, val matchedFields: Set<String>, val rawSnippet: String)

    fun parse(ocrText: String): ParseResult? {
        val text = ocrText.replace('\u00A0', ' ')
        if (text.isBlank()) return null
        val matched = linkedSetOf<String>()
        var kcal = firstDouble(energyKcal, text)?.also { matched += "energyKcal" }
        if (kcal == null) {
            val kj = firstDouble(energyKj, text)
            if (kj != null) {
                kcal = kj / 4.184
                matched += "energyKj"
            }
        }
        val proteinG = firstDouble(protein, text)?.also { matched += "proteinG" } ?: 0.0
        val carbsG = firstDouble(carbs, text)?.also { matched += "carbsG" } ?: 0.0
        val fatG = firstDouble(fat, text)?.also { matched += "fatG" } ?: 0.0
        val fibreG = firstDouble(fibre, text)?.also { matched += "fibreG" } ?: 0.0
        val sugarsG = firstDouble(sugars, text)?.also { matched += "sugarsG" } ?: 0.0
        var sodiumMg = firstDouble(sodium, text)?.also { matched += "sodiumMg" }
        if (sodiumMg == null) {
            val saltG = firstDouble(salt, text)
            if (saltG != null) {
                sodiumMg = saltG * 400.0 // ~Na in salt
                matched += "saltG"
            }
        }
        if (kcal == null && matched.isEmpty()) return null
        return ParseResult(
            nutrients =
            ServingNutrients(
                energyKcal = kcal ?: 0.0,
                proteinG = proteinG,
                carbsG = carbsG,
                fatG = fatG,
                fibreG = fibreG,
                sugarsG = sugarsG,
                sodiumMg = sodiumMg ?: 0.0
            ),
            matchedFields = matched,
            rawSnippet = text.take(280)
        )
    }

    private fun firstDouble(regex: Regex, text: String): Double? {
        val m = regex.find(text) ?: return null
        return m.groupValues[1].replace(',', '.').toDoubleOrNull()
    }
}
