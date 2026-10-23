package com.omnigart.insights

/**
 * Shared framing: every insight is a wellness estimate, not a medical claim.
 */
object WellnessDisclaimer {
    const val EN_GB: String =
        "Wellness estimate only — not a medical diagnosis, treatment, or clinical advice."

    const val UK: String =
        "Лише оцінка добробуту — не медичний діагноз, лікування чи клінічна порада."

    fun text(locale: InsightLocale = InsightLocale.EN_GB): String = when (locale) {
        InsightLocale.EN_GB -> EN_GB
        InsightLocale.UK -> UK
    }
}

enum class InsightLocale {
    EN_GB,
    UK,
}
