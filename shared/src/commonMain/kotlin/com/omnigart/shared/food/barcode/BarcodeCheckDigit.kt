package com.omnigart.shared.food.barcode

/**
 * Retail barcode normalisation and GS1 check-digit validation (UPC-A / EAN-8 / EAN-13).
 */
object BarcodeCheckDigit {
    fun normalize(raw: String): String = raw.filter { it.isDigit() }

    fun isValid(raw: String): Boolean {
        val digits = normalize(raw)
        return when (digits.length) {
            8, 12, 13 -> checkDigitMatches(digits)
            else -> false
        }
    }

    /** Returns the expected check digit (0–9) for the payload without check digit. */
    fun expectedCheckDigit(payloadWithoutCheck: String): Int {
        val digits = normalize(payloadWithoutCheck)
        require(digits.isNotEmpty()) { "empty barcode payload" }
        var sum = 0
        // Right-to-left: odd positions ×3, even ×1 (GS1).
        for (i in digits.indices) {
            val d = digits[digits.lastIndex - i].digitToInt()
            sum += if (i % 2 == 0) d * 3 else d
        }
        return (10 - (sum % 10)) % 10
    }

    private fun checkDigitMatches(digits: String): Boolean {
        val payload = digits.dropLast(1)
        val actual = digits.last().digitToInt()
        return expectedCheckDigit(payload) == actual
    }
}
