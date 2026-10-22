package com.omnigart.shared.i18n

/**
 * Stable UI string keys. Platform UIs may resolve via Android resources / String Catalog;
 * this catalog is the shared source of truth for keys and the en-GB fallback table.
 *
 * Locales: en-GB (source), uk. Never `ru`.
 */
object StringKeys {
    const val APP_NAME = "app_name"
    const val TAB_FOOD = "tab_food"
    const val TAB_WORKOUTS = "tab_workouts"
    const val TAB_RECOVERY = "tab_recovery"
    const val TAB_PLANNER = "tab_planner"
    const val TAB_PROFILE = "tab_profile"
    const val SHELL_INTRO = "shell_intro"
    const val WATCH_START = "watch_start"
    const val WATCH_SUBTITLE = "watch_subtitle"
    const val WELLNESS_DISCLAIMER = "wellness_disclaimer"
    const val INFO_SHEET_NOT_MEDICAL_ADVICE = "info_sheet_not_medical_advice"
    const val HEALTH_WARNING_CHEST_PAIN = "health_warning_chest_pain"
    const val ONBOARDING_WELLNESS_TITLE = "onboarding_wellness_title"
    const val INFO_SHEET_TITLE = "info_sheet_title"
    const val INFO_SHEET_BODY = "info_sheet_body"
}

object StringCatalog {
    const val SOURCE_LOCALE = "en-GB"
    const val FALLBACK_LOCALE = "en-GB"

    private val enGb: Map<String, String> =
        mapOf(
            StringKeys.APP_NAME to "OmniGart",
            StringKeys.TAB_FOOD to "Food",
            StringKeys.TAB_WORKOUTS to "Workouts",
            StringKeys.TAB_RECOVERY to "Recovery",
            StringKeys.TAB_PLANNER to "Planner",
            StringKeys.TAB_PROFILE to "Profile",
            StringKeys.SHELL_INTRO to "Wellness estimates from your shared OmniGart profile",
            StringKeys.WATCH_START to "Start",
            StringKeys.WATCH_SUBTITLE to "Wellness estimates on your wrist",
            StringKeys.WELLNESS_DISCLAIMER to
                "OmniGart provides wellness estimates only. Readiness, stress, HRV, Baevsky index, " +
                "body-fat estimates, hydration targets, and training plans are not medical advice. " +
                "Consult a qualified professional for health concerns.",
            StringKeys.INFO_SHEET_NOT_MEDICAL_ADVICE to "Not medical advice. Consult a professional.",
            StringKeys.HEALTH_WARNING_CHEST_PAIN to
                "Stop and seek emergency help if you feel chest pain, severe dizziness, or " +
                "unusual shortness of breath during activity.",
            StringKeys.ONBOARDING_WELLNESS_TITLE to "Wellness, not medical care",
            StringKeys.INFO_SHEET_TITLE to "About this estimate",
            StringKeys.INFO_SHEET_BODY to
                "Figures such as readiness, stress, HRV, Baevsky index, body fat, and hydration " +
                "are wellness estimates based on your inputs and device samples. Plans are " +
                "coaching suggestions, not prescriptions."
        )

    private val uk: Map<String, String> =
        mapOf(
            StringKeys.APP_NAME to "OmniGart",
            StringKeys.TAB_FOOD to "Їжа",
            StringKeys.TAB_WORKOUTS to "Тренування",
            StringKeys.TAB_RECOVERY to "Відновлення",
            StringKeys.TAB_PLANNER to "Планувальник",
            StringKeys.TAB_PROFILE to "Профіль",
            StringKeys.SHELL_INTRO to "Оцінки самопочуття з вашого спільного профілю OmniGart",
            StringKeys.WATCH_START to "Старт",
            StringKeys.WATCH_SUBTITLE to "Оцінки самопочуття на зап’ясті",
            StringKeys.WELLNESS_DISCLAIMER to
                "OmniGart надає лише оцінки самопочуття. Готовність, стрес, ВСР, індекс Баєвського, " +
                "оцінка жиру в тілі, цілі гідратації та плани тренувань не є медичною консультацією. " +
                "Звертайтеся до кваліфікованого фахівця щодо питань здоров’я.",
            StringKeys.INFO_SHEET_NOT_MEDICAL_ADVICE to
                "Це не медична консультація. Зверніться до фахівця.",
            StringKeys.HEALTH_WARNING_CHEST_PAIN to
                "Зупиніться і зверніться по екстрену допомогу, якщо під час активності відчуваєте " +
                "біль у грудях, сильне запаморочення або незвичну задишку.",
            StringKeys.ONBOARDING_WELLNESS_TITLE to "Самопочуття, не медицина",
            StringKeys.INFO_SHEET_TITLE to "Про цю оцінку",
            StringKeys.INFO_SHEET_BODY to
                "Показники на кшталт готовності, стресу, ВСР, індексу Баєвського, жиру в тілі та " +
                "гідратації — це оцінки самопочуття на основі ваших даних і зразків із пристрою. " +
                "Плани — тренерські пропозиції, а не приписи лікаря."
        )

    private val catalogs: Map<String, Map<String, String>> =
        mapOf(
            "en-GB" to enGb,
            "en" to enGb,
            "uk" to uk
        )

    fun t(key: String, locale: String = FALLBACK_LOCALE): String {
        val normalized = locale.replace('_', '-')
        val primary = catalogs[normalized] ?: catalogs[normalized.substringBefore('-')]
        return primary?.get(key) ?: enGb[key] ?: key
    }

    fun supportedLocales(): Set<String> = setOf("en-GB", "uk")
}
