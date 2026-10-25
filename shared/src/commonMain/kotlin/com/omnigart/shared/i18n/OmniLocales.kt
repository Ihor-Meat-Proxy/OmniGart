package com.omnigart.shared.i18n

/**
 * Canonical OmniGart locales (mirrors `i18n/locales.yml`).
 *
 * Resolution order for a device/tag: exact BCP-47 match → language match → en-GB.
 * Never Russian; `ru` / `ru-*` always resolve to the en-GB fallback.
 */
object OmniLocales {
    const val SOURCE = "en-GB"
    const val FALLBACK = "en-GB"

    val REQUIRED: List<String> =
        listOf(
            "en-GB",
            "uk",
            "pl",
            "be",
            "kk",
            "ka",
            "es",
            "fr",
            "de",
            "cs",
            "zh-Hans"
        )

    private val requiredSet: Set<String> = REQUIRED.toSet()

    /** Language → canonical shipped locale when the exact tag is absent. */
    private val languageDefaults: Map<String, String> =
        mapOf(
            "en" to "en-GB",
            "uk" to "uk",
            "pl" to "pl",
            "be" to "be",
            "kk" to "kk",
            "ka" to "ka",
            "es" to "es",
            "fr" to "fr",
            "de" to "de",
            "cs" to "cs",
            // Only Simplified Chinese ships; map any zh* tag here.
            "zh" to "zh-Hans"
        )

    /**
     * Resolve a device or requested locale tag to a shipped catalog locale.
     *
     * 1. Normalize `_` → `-`
     * 2. Reject Russian (`ru` / `ru-*`) → [FALLBACK]
     * 3. Exact match against [REQUIRED]
     * 4. Language (and zh* → zh-Hans) match
     * 5. [FALLBACK] (en-GB)
     */
    fun resolve(requested: String): String {
        val normalized = requested.replace('_', '-').trim()
        if (normalized.isEmpty()) return FALLBACK

        val lower = normalized.lowercase()
        if (lower == "ru" || lower.startsWith("ru-")) return FALLBACK

        if (normalized in requiredSet) return normalized

        // zh-Hans / zh-CN / zh-SG / zh-Hans-CN etc.
        if (lower == "zh" || lower.startsWith("zh-")) {
            return "zh-Hans"
        }

        val language = normalized.substringBefore('-')
        languageDefaults[language]?.let { return it }

        return FALLBACK
    }
}
