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
    const val SHELL_PLACEHOLDER = "shell_placeholder"
    const val WATCH_START = "watch_start"
    const val WATCH_SUBTITLE = "watch_subtitle"
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
            StringKeys.SHELL_PLACEHOLDER to "Phase 0 shell — data from shared core",
            StringKeys.WATCH_START to "Start",
            StringKeys.WATCH_SUBTITLE to "Phase 0 — shared core"
        )

    private val uk: Map<String, String> =
        mapOf(
            StringKeys.APP_NAME to "OmniGart",
            StringKeys.TAB_FOOD to "Їжа",
            StringKeys.TAB_WORKOUTS to "Тренування",
            StringKeys.TAB_RECOVERY to "Відновлення",
            StringKeys.TAB_PLANNER to "Планувальник",
            StringKeys.TAB_PROFILE to "Профіль",
            StringKeys.SHELL_PLACEHOLDER to "Оболонка Phase 0 — дані зі спільного ядра",
            StringKeys.WATCH_START to "Старт",
            StringKeys.WATCH_SUBTITLE to "Phase 0 — спільне ядро"
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
