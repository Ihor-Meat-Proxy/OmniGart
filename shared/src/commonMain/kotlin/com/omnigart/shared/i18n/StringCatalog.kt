package com.omnigart.shared.i18n

/**
 * Stable UI string keys. Platform UIs may resolve via Android resources / String Catalog;
 * this catalog is the shared source of truth for keys and the en-GB fallback table.
 *
 * Locales: all [OmniLocales.REQUIRED] (en-GB source). Never `ru`.
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
    const val ONBOARDING_WELCOME_TITLE = "onboarding_welcome_title"
    const val ONBOARDING_WELCOME_BODY = "onboarding_welcome_body"
    const val ONBOARDING_NEXT = "onboarding_next"
    const val ONBOARDING_BACK = "onboarding_back"
    const val ONBOARDING_SKIP = "onboarding_skip"
    const val ONBOARDING_FINISH = "onboarding_finish"
    const val ONBOARDING_GOAL_TITLE = "onboarding_goal_title"
    const val ONBOARDING_SEX_TITLE = "onboarding_sex_title"
    const val ONBOARDING_BIRTH_TITLE = "onboarding_birth_title"
    const val ONBOARDING_HEIGHT_TITLE = "onboarding_height_title"
    const val ONBOARDING_WEIGHT_TITLE = "onboarding_weight_title"
    const val ONBOARDING_EXPERIENCE_TITLE = "onboarding_experience_title"
    const val ONBOARDING_UNITS_TITLE = "onboarding_units_title"
    const val ONBOARDING_ACTIVITY_TITLE = "onboarding_activity_title"
    const val PERM_EXPLAINER_HEALTH_TITLE = "perm_explainer_health_title"
    const val PERM_EXPLAINER_HEALTH_BODY = "perm_explainer_health_body"
    const val PERM_CONTINUE = "perm_continue"
    const val PERM_NOT_NOW = "perm_not_now"
    const val INFO_SHEET_TITLE = "info_sheet_title"
    const val INFO_SHEET_BODY = "info_sheet_body"

    const val CALC_SETTINGS_TITLE = "calc_settings_title"
    const val CALC_SETTINGS_INTRO = "calc_settings_intro"
    const val CALC_MODE_SIMPLE = "calc_mode_simple"
    const val CALC_MODE_ADVANCED = "calc_mode_advanced"
    const val CALC_DOMAIN_BMR = "calc_domain_bmr"
    const val CALC_DOMAIN_HR_MAX = "calc_domain_hr_max"
    const val CALC_DOMAIN_BODY_FAT = "calc_domain_body_fat"
    const val CALC_DOMAIN_ONE_RM = "calc_domain_one_rm"
    const val CALC_DOMAIN_HR_ZONES = "calc_domain_hr_zones"

    const val FORMULA_BMR_MIFFLIN_ST_JEOR_TITLE = "formula.bmr.mifflin_st_jeor.title"
    const val FORMULA_BMR_MIFFLIN_ST_JEOR_EXPLAIN = "formula.bmr.mifflin_st_jeor.explain"
    const val FORMULA_BMR_HARRIS_BENEDICT_TITLE = "formula.bmr.harris_benedict.title"
    const val FORMULA_BMR_HARRIS_BENEDICT_EXPLAIN = "formula.bmr.harris_benedict.explain"
    const val FORMULA_HR_MAX_TANAKA_TITLE = "formula.hr_max.tanaka.title"
    const val FORMULA_HR_MAX_TANAKA_EXPLAIN = "formula.hr_max.tanaka.explain"
    const val FORMULA_HR_MAX_FOX_TITLE = "formula.hr_max.fox.title"
    const val FORMULA_HR_MAX_FOX_EXPLAIN = "formula.hr_max.fox.explain"
    const val FORMULA_BODY_FAT_CUN_BAE_TITLE = "formula.body_fat.cun_bae.title"
    const val FORMULA_BODY_FAT_CUN_BAE_EXPLAIN = "formula.body_fat.cun_bae.explain"
    const val FORMULA_ONE_RM_EPLEY_TITLE = "formula.one_rm.epley.title"
    const val FORMULA_ONE_RM_EPLEY_EXPLAIN = "formula.one_rm.epley.explain"
    const val FORMULA_ONE_RM_BRZYCKI_TITLE = "formula.one_rm.brzycki.title"
    const val FORMULA_ONE_RM_BRZYCKI_EXPLAIN = "formula.one_rm.brzycki.explain"
    const val FORMULA_HR_ZONES_KARVONEN_5_TITLE = "formula.hr_zones.karvonen_5.title"
    const val FORMULA_HR_ZONES_KARVONEN_5_EXPLAIN = "formula.hr_zones.karvonen_5.explain"
    const val FORMULA_HR_ZONES_KARVONEN_3_TITLE = "formula.hr_zones.karvonen_3.title"
    const val FORMULA_HR_ZONES_KARVONEN_3_EXPLAIN = "formula.hr_zones.karvonen_3.explain"
    const val FORMULA_HR_ZONES_PERCENT_HRMAX_5_TITLE = "formula.hr_zones.percent_hrmax_5.title"
    const val FORMULA_HR_ZONES_PERCENT_HRMAX_5_EXPLAIN = "formula.hr_zones.percent_hrmax_5.explain"

    const val WORKOUT_START = "workout_start"
    const val WORKOUT_END = "workout_end"
    const val WORKOUT_ADD_SET = "workout_add_set"
    const val WORKOUT_ACTIVE = "workout_active"
    const val WORKOUT_IDLE = "workout_idle"
    const val WORKOUT_EXERCISE = "workout_exercise"
    const val WORKOUT_WEIGHT_KG = "workout_weight_kg"
    const val WORKOUT_REPS = "workout_reps"
    const val WORKOUT_RIR = "workout_rir"
    const val WORKOUT_WARMUP = "workout_warmup"
    const val WORKOUT_INCLUDE_WARMUPS = "workout_include_warmups"
    const val WORKOUT_SUMMARY_TITLE = "workout_summary_title"
    const val WORKOUT_VOLUME_LOAD = "workout_volume_load"
    const val WORKOUT_TOTAL_WEIGHT_LIFTED = "workout_total_weight_lifted"
    const val WORKOUT_HARD_SETS = "workout_hard_sets"
    const val WORKOUT_PER_EXERCISE = "workout_per_exercise"
    const val WORKOUT_INTRO = "workout_intro"
    const val WORKOUT_SUMMARY_DURATION = "workout_summary_duration"
    const val WORKOUT_SUMMARY_ACTIVE_KCAL = "workout_summary_active_kcal"
    const val WORKOUT_SUMMARY_TOTAL_KCAL = "workout_summary_total_kcal"
    const val WORKOUT_SUMMARY_AVG_HR = "workout_summary_avg_hr"
    const val WORKOUT_SUMMARY_MAX_HR = "workout_summary_max_hr"
    const val WORKOUT_SUMMARY_ENERGY_ESTIMATE = "workout_summary_energy_estimate"
    const val WORKOUT_SUMMARY_EFFORT = "workout_summary_effort"
    const val WORKOUT_SUMMARY_HR_RECOVERY = "workout_summary_hr_recovery"
    const val WORKOUT_START_ON_WATCH = "workout_start_on_watch"
    const val WORKOUT_WATCH_STARTED = "workout_watch_started"
    const val WORKOUT_WATCH_UNAVAILABLE = "workout_watch_unavailable"
    const val WORKOUT_START_IPHONE_ESTIMATE = "workout_start_iphone_estimate"
    const val WORKOUT_YOGA_TITLE = "workout_yoga_title"
    const val WORKOUT_PILATES_TITLE = "workout_pilates_title"
    const val WORKOUT_MINDFUL_BODY_INTRO = "workout_mindful_body_intro"
    const val WORKOUT_MINDFUL_MINUTES_TOGGLE = "workout_mindful_minutes_toggle"
    const val WORKOUT_MINDFUL_MINUTES_BODY = "workout_mindful_minutes_body"
    const val WORKOUT_MINDFUL_SETTINGS_TITLE = "workout_mindful_settings_title"
    const val WORKOUT_FINISH = "workout_finish"
    const val WORKOUT_REST_TIMER = "workout_rest_timer"
    const val WORKOUT_NOTE = "workout_note"
    const val WORKOUT_PROGRESSION = "workout_progression"
    const val WORKOUT_COL_SET = "workout_col_set"
    const val WORKOUT_COL_PREVIOUS = "workout_col_previous"
    const val WORKOUT_COL_KG = "workout_col_kg"
    const val WORKOUT_COL_LB = "workout_col_lb"
    const val WORKOUT_COL_REPS = "workout_col_reps"
    const val WORKOUT_COL_DONE = "workout_col_done"
    const val WORKOUT_REPLACE_EXERCISE = "workout_replace_exercise"
    const val WORKOUT_REMOVE_EXERCISE = "workout_remove_exercise"
    const val WORKOUT_MARK_WARMUP = "workout_mark_warmup"
    const val WORKOUT_MARK_DROP = "workout_mark_drop"
    const val WORKOUT_MARK_FAILURE = "workout_mark_failure"
    const val WORKOUT_RECORD_RPE = "workout_record_rpe"
    const val WORKOUT_SET_MENU = "workout_set_menu"
    const val WORKOUT_WEIGHT_LB = "workout_weight_lb"
    const val WORKOUT_BODY_MAP = "workout_body_map"
    const val WORKOUT_EXPORT_WORKOUTKIT = "workout_export_workoutkit"
    const val WORKOUT_WORKOUTKIT_LIMIT = "workout_workoutkit_limit"
    const val WORKOUT_SETS_COUNT = "workout_sets_count"
    const val WORKOUT_VOLUME_SHORT = "workout_volume_short"

    const val DATA_PRIVACY_TITLE = "data_privacy_title"
    const val DATA_PRIVACY_INTRO = "data_privacy_intro"
    const val DATA_PRIVACY_EXPORT = "data_privacy_export"
    const val DATA_PRIVACY_IMPORT_JSON = "data_privacy_import_json"
}

object StringCatalog {
    const val SOURCE_LOCALE = OmniLocales.SOURCE
    const val FALLBACK_LOCALE = OmniLocales.FALLBACK

    private val catalogs: Map<String, Map<String, String>> = StringCatalogTables.catalogs

    private val sourceTable: Map<String, String> = catalogs.getValue(SOURCE_LOCALE)

    /**
     * Resolve [locale] to a shipped catalog tag (device → language → en-GB; never `ru`).
     */
    fun resolveLocale(locale: String): String = OmniLocales.resolve(locale)

    fun t(key: String, locale: String = FALLBACK_LOCALE): String {
        val resolved = resolveLocale(locale)
        val primary = catalogs[resolved]
        return primary?.get(key) ?: sourceTable[key] ?: key
    }

    fun supportedLocales(): Set<String> = OmniLocales.REQUIRED.toSet()
}
