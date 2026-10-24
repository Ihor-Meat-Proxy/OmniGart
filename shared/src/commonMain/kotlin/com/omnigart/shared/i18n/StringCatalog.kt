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
            StringKeys.ONBOARDING_WELCOME_TITLE to "Welcome to OmniGart",
            StringKeys.ONBOARDING_WELCOME_BODY to
                "A few questions so we can personalise wellness estimates on your device. " +
                "You can change these later in Profile.",
            StringKeys.ONBOARDING_NEXT to "Next",
            StringKeys.ONBOARDING_BACK to "Back",
            StringKeys.ONBOARDING_SKIP to "Skip",
            StringKeys.ONBOARDING_FINISH to "Finish",
            StringKeys.ONBOARDING_GOAL_TITLE to "What is your main goal?",
            StringKeys.ONBOARDING_SEX_TITLE to "Body map figure",
            StringKeys.ONBOARDING_BIRTH_TITLE to "Date of birth",
            StringKeys.ONBOARDING_HEIGHT_TITLE to "Height",
            StringKeys.ONBOARDING_WEIGHT_TITLE to "Weight",
            StringKeys.ONBOARDING_EXPERIENCE_TITLE to "Strength training experience",
            StringKeys.ONBOARDING_UNITS_TITLE to "Preferred units",
            StringKeys.ONBOARDING_ACTIVITY_TITLE to "Typical daily activity",
            StringKeys.PERM_EXPLAINER_HEALTH_TITLE to "Apple Health / Health Connect",
            StringKeys.PERM_EXPLAINER_HEALTH_BODY to
                "OmniGart can read and write wellness samples — heart rate, HRV, sleep, workouts, " +
                "active and resting energy, heart-rate recovery, workout effort, optional mindful " +
                "minutes, body mass, and hydration — so sessions can appear in Apple Fitness with " +
                "duration and calories. Data is not used for ads or sent to unrelated third parties. " +
                "You can continue without granting access.",
            StringKeys.PERM_CONTINUE to "Continue",
            StringKeys.PERM_NOT_NOW to "Not now",
            StringKeys.INFO_SHEET_TITLE to "About this estimate",
            StringKeys.INFO_SHEET_BODY to
                "Figures such as readiness, stress, HRV, Baevsky index, body fat, and hydration " +
                "are wellness estimates based on your inputs and device samples. Plans are " +
                "coaching suggestions, not prescriptions.",
            StringKeys.CALC_SETTINGS_TITLE to "Calculations",
            StringKeys.CALC_SETTINGS_INTRO to
                "Choose how OmniGart estimates wellness figures such as energy use, heart-rate " +
                "zones, body fat, and strength.",
            StringKeys.CALC_MODE_SIMPLE to "Simple",
            StringKeys.CALC_MODE_ADVANCED to "Advanced",
            StringKeys.CALC_DOMAIN_BMR to "BMR",
            StringKeys.CALC_DOMAIN_HR_MAX to "Max heart rate",
            StringKeys.CALC_DOMAIN_BODY_FAT to "Body fat",
            StringKeys.CALC_DOMAIN_ONE_RM to "One-rep max",
            StringKeys.CALC_DOMAIN_HR_ZONES to "Heart-rate zones",
            StringKeys.FORMULA_BMR_MIFFLIN_ST_JEOR_TITLE to "Mifflin–St Jeor",
            StringKeys.FORMULA_BMR_MIFFLIN_ST_JEOR_EXPLAIN to
                "Widely used resting energy estimate from weight, height, age, and sex.",
            StringKeys.FORMULA_BMR_HARRIS_BENEDICT_TITLE to "Harris–Benedict (revised)",
            StringKeys.FORMULA_BMR_HARRIS_BENEDICT_EXPLAIN to
                "Classic resting energy estimate; useful as an alternate wellness check.",
            StringKeys.FORMULA_HR_MAX_TANAKA_TITLE to "Tanaka",
            StringKeys.FORMULA_HR_MAX_TANAKA_EXPLAIN to
                "Age-based max heart-rate estimate (208 − 0.7×age).",
            StringKeys.FORMULA_HR_MAX_FOX_TITLE to "Fox (220 − age)",
            StringKeys.FORMULA_HR_MAX_FOX_EXPLAIN to
                "Traditional max heart-rate rule of thumb from age alone.",
            StringKeys.FORMULA_BODY_FAT_CUN_BAE_TITLE to "CUN-BAE",
            StringKeys.FORMULA_BODY_FAT_CUN_BAE_EXPLAIN to
                "Body-fat percentage estimate from weight, height, age, and sex.",
            StringKeys.FORMULA_ONE_RM_EPLEY_TITLE to "Epley",
            StringKeys.FORMULA_ONE_RM_EPLEY_EXPLAIN to
                "Estimates one-rep max from a submaximal lift and rep count.",
            StringKeys.FORMULA_ONE_RM_BRZYCKI_TITLE to "Brzycki",
            StringKeys.FORMULA_ONE_RM_BRZYCKI_EXPLAIN to
                "Alternate one-rep max estimate that weights higher-rep sets differently.",
            StringKeys.FORMULA_HR_ZONES_KARVONEN_5_TITLE to "Karvonen (5 zones)",
            StringKeys.FORMULA_HR_ZONES_KARVONEN_5_EXPLAIN to
                "Five training zones from heart-rate reserve (resting to max).",
            StringKeys.FORMULA_HR_ZONES_KARVONEN_3_TITLE to "Karvonen (3 zones)",
            StringKeys.FORMULA_HR_ZONES_KARVONEN_3_EXPLAIN to
                "Simpler three-zone heart-rate reserve layout.",
            StringKeys.FORMULA_HR_ZONES_PERCENT_HRMAX_5_TITLE to "% HRmax (5 zones)",
            StringKeys.FORMULA_HR_ZONES_PERCENT_HRMAX_5_EXPLAIN to
                "Five zones as percentages of estimated max heart rate.",
            StringKeys.WORKOUT_START to "Start workout",
            StringKeys.WORKOUT_END to "End workout",
            StringKeys.WORKOUT_ADD_SET to "Add set",
            StringKeys.WORKOUT_ACTIVE to "Workout in progress",
            StringKeys.WORKOUT_IDLE to "No active workout",
            StringKeys.WORKOUT_EXERCISE to "Exercise",
            StringKeys.WORKOUT_WEIGHT_KG to "Weight (kg)",
            StringKeys.WORKOUT_REPS to "Reps",
            StringKeys.WORKOUT_RIR to "RIR",
            StringKeys.WORKOUT_WARMUP to "Warm-up set",
            StringKeys.WORKOUT_INCLUDE_WARMUPS to "Count warm-ups in volume",
            StringKeys.WORKOUT_SUMMARY_TITLE to "Session summary",
            StringKeys.WORKOUT_VOLUME_LOAD to "Volume load",
            StringKeys.WORKOUT_TOTAL_WEIGHT_LIFTED to "Total weight lifted",
            StringKeys.WORKOUT_HARD_SETS to "Hard sets",
            StringKeys.WORKOUT_PER_EXERCISE to "Per exercise",
            StringKeys.WORKOUT_INTRO to
                "Log strength sets for wellness training estimates. Figures are coaching aids, " +
                "not medical advice.",
            StringKeys.WORKOUT_SUMMARY_DURATION to "Duration",
            StringKeys.WORKOUT_SUMMARY_ACTIVE_KCAL to "Active kcal",
            StringKeys.WORKOUT_SUMMARY_TOTAL_KCAL to "Total kcal",
            StringKeys.WORKOUT_SUMMARY_AVG_HR to "Average heart rate",
            StringKeys.WORKOUT_SUMMARY_MAX_HR to "Max heart rate",
            StringKeys.WORKOUT_SUMMARY_ENERGY_ESTIMATE to
                "Energy is a wellness estimate (heart rate or work-based), not a medical measurement.",
            StringKeys.WORKOUT_SUMMARY_EFFORT to "Effort score",
            StringKeys.WORKOUT_SUMMARY_HR_RECOVERY to "Heart-rate recovery",
            StringKeys.WORKOUT_START_ON_WATCH to "Start on Apple Watch",
            StringKeys.WORKOUT_WATCH_STARTED to "Watch workout starting…",
            StringKeys.WORKOUT_WATCH_UNAVAILABLE to "Watch unavailable — use iPhone estimate below.",
            StringKeys.WORKOUT_START_IPHONE_ESTIMATE to "Log on iPhone (estimate)",
            StringKeys.WORKOUT_YOGA_TITLE to "Yoga",
            StringKeys.WORKOUT_PILATES_TITLE to "Pilates",
            StringKeys.WORKOUT_MINDFUL_BODY_INTRO to
                "Prefer a live watch session for heart rate and energy. On iPhone alone, OmniGart " +
                "saves a MET wellness estimate labelled as such.",
            StringKeys.WORKOUT_MINDFUL_MINUTES_TOGGLE to "Save mindful minutes for yoga",
            StringKeys.WORKOUT_MINDFUL_MINUTES_BODY to
                "Optional. Off by default. Writes a mindful session only when you enable this.",
            StringKeys.WORKOUT_MINDFUL_SETTINGS_TITLE to "Yoga mindful minutes",
            StringKeys.WORKOUT_FINISH to "Finish",
            StringKeys.WORKOUT_REST_TIMER to "Rest",
            StringKeys.WORKOUT_NOTE to "Note",
            StringKeys.WORKOUT_PROGRESSION to "Target",
            StringKeys.WORKOUT_COL_SET to "SET",
            StringKeys.WORKOUT_COL_PREVIOUS to "PREVIOUS",
            StringKeys.WORKOUT_COL_KG to "KG",
            StringKeys.WORKOUT_COL_LB to "LB",
            StringKeys.WORKOUT_COL_REPS to "REPS",
            StringKeys.WORKOUT_COL_DONE to "✓",
            StringKeys.WORKOUT_REPLACE_EXERCISE to "Replace exercise",
            StringKeys.WORKOUT_REMOVE_EXERCISE to "Remove exercise",
            StringKeys.WORKOUT_MARK_WARMUP to "Mark as warm-up",
            StringKeys.WORKOUT_MARK_DROP to "Mark as drop set",
            StringKeys.WORKOUT_MARK_FAILURE to "Mark as failure",
            StringKeys.WORKOUT_RECORD_RPE to "Record RPE",
            StringKeys.WORKOUT_SET_MENU to "Set options",
            StringKeys.WORKOUT_WEIGHT_LB to "Weight (lb)",
            StringKeys.WORKOUT_BODY_MAP to "Muscles",
            StringKeys.WORKOUT_EXPORT_WORKOUTKIT to "Add to Apple Workout",
            StringKeys.WORKOUT_WORKOUTKIT_LIMIT to
                "Apple’s Workout app cannot accept weight or rep entry from other apps. " +
                "OmniGart exports each set as a work step and each rest as recovery so you can " +
                "follow the routine there; log load × reps in OmniGart on iPhone or Apple Watch.",
            StringKeys.WORKOUT_SETS_COUNT to "Sets",
            StringKeys.WORKOUT_VOLUME_SHORT to "Volume",
            StringKeys.DATA_PRIVACY_TITLE to "Data and privacy",
            StringKeys.DATA_PRIVACY_INTRO to
                "OmniGart keeps a full local copy of your workouts and samples on this device. " +
                "Export stays on-device. Platform sync is optional and never required for logging.",
            StringKeys.DATA_PRIVACY_EXPORT to "Export health data",
            StringKeys.DATA_PRIVACY_IMPORT_JSON to "Import OmniGart JSON"
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
            StringKeys.ONBOARDING_WELCOME_TITLE to "Ласкаво просимо до OmniGart",
            StringKeys.ONBOARDING_WELCOME_BODY to
                "Кілька запитань, щоб налаштувати оцінки самопочуття на вашому пристрої. " +
                "Пізніше можна змінити в Профілі.",
            StringKeys.ONBOARDING_NEXT to "Далі",
            StringKeys.ONBOARDING_BACK to "Назад",
            StringKeys.ONBOARDING_SKIP to "Пропустити",
            StringKeys.ONBOARDING_FINISH to "Завершити",
            StringKeys.ONBOARDING_GOAL_TITLE to "Яка ваша головна мета?",
            StringKeys.ONBOARDING_SEX_TITLE to "Фігура на карті тіла",
            StringKeys.ONBOARDING_BIRTH_TITLE to "Дата народження",
            StringKeys.ONBOARDING_HEIGHT_TITLE to "Зріст",
            StringKeys.ONBOARDING_WEIGHT_TITLE to "Вага",
            StringKeys.ONBOARDING_EXPERIENCE_TITLE to "Досвід силових тренувань",
            StringKeys.ONBOARDING_UNITS_TITLE to "Зручні одиниці",
            StringKeys.ONBOARDING_ACTIVITY_TITLE to "Типова щоденна активність",
            StringKeys.PERM_EXPLAINER_HEALTH_TITLE to "Apple Health / Health Connect",
            StringKeys.PERM_EXPLAINER_HEALTH_BODY to
                "OmniGart може читати й записувати зразки самопочуття — пульс, ВСР, сон, тренування, " +
                "активну й базальну енергію, відновлення пульсу, оцінку зусилля, за бажанням хвилини " +
                "усвідомленості, масу тіла та гідратацію — щоб сесії з’являлися в Apple Fitness. " +
                "Дані не для реклами й не передаються стороннім. Можна продовжити без доступу.",
            StringKeys.PERM_CONTINUE to "Продовжити",
            StringKeys.PERM_NOT_NOW to "Не зараз",
            StringKeys.INFO_SHEET_TITLE to "Про цю оцінку",
            StringKeys.INFO_SHEET_BODY to
                "Показники на кшталт готовності, стресу, ВСР, індексу Баєвського, жиру в тілі та " +
                "гідратації — це оцінки самопочуття на основі ваших даних і зразків із пристрою. " +
                "Плани — тренерські пропозиції, а не приписи лікаря.",
            StringKeys.CALC_SETTINGS_TITLE to "Розрахунки",
            StringKeys.CALC_SETTINGS_INTRO to
                "Оберіть, як OmniGart оцінює показники самопочуття: енергію, зони пульсу, " +
                "жир у тілі та силу.",
            StringKeys.CALC_MODE_SIMPLE to "Простий",
            StringKeys.CALC_MODE_ADVANCED to "Розширений",
            StringKeys.CALC_DOMAIN_BMR to "BMR",
            StringKeys.CALC_DOMAIN_HR_MAX to "Макс. пульс",
            StringKeys.CALC_DOMAIN_BODY_FAT to "Жир у тілі",
            StringKeys.CALC_DOMAIN_ONE_RM to "Одноповторений максимум",
            StringKeys.CALC_DOMAIN_HR_ZONES to "Зони пульсу",
            StringKeys.FORMULA_BMR_MIFFLIN_ST_JEOR_TITLE to "Mifflin–St Jeor",
            StringKeys.FORMULA_BMR_MIFFLIN_ST_JEOR_EXPLAIN to
                "Поширена оцінка енергії в спокої з ваги, зросту, віку та статі.",
            StringKeys.FORMULA_BMR_HARRIS_BENEDICT_TITLE to "Harris–Benedict (оновлена)",
            StringKeys.FORMULA_BMR_HARRIS_BENEDICT_EXPLAIN to
                "Класична оцінка енергії в спокої; корисна як альтернативна перевірка.",
            StringKeys.FORMULA_HR_MAX_TANAKA_TITLE to "Tanaka",
            StringKeys.FORMULA_HR_MAX_TANAKA_EXPLAIN to
                "Оцінка макс. пульсу за віком (208 − 0,7×вік).",
            StringKeys.FORMULA_HR_MAX_FOX_TITLE to "Fox (220 − вік)",
            StringKeys.FORMULA_HR_MAX_FOX_EXPLAIN to
                "Традиційне правило макс. пульсу лише за віком.",
            StringKeys.FORMULA_BODY_FAT_CUN_BAE_TITLE to "CUN-BAE",
            StringKeys.FORMULA_BODY_FAT_CUN_BAE_EXPLAIN to
                "Оцінка відсотка жиру в тілі з ваги, зросту, віку та статі.",
            StringKeys.FORMULA_ONE_RM_EPLEY_TITLE to "Epley",
            StringKeys.FORMULA_ONE_RM_EPLEY_EXPLAIN to
                "Оцінює одноповторений максимум із субмаксимальної ваги та кількості повторень.",
            StringKeys.FORMULA_ONE_RM_BRZYCKI_TITLE to "Brzycki",
            StringKeys.FORMULA_ONE_RM_BRZYCKI_EXPLAIN to
                "Альтернативна оцінка одноповтореного максимуму з іншою вагою для багатоповторних сетів.",
            StringKeys.FORMULA_HR_ZONES_KARVONEN_5_TITLE to "Karvonen (5 зон)",
            StringKeys.FORMULA_HR_ZONES_KARVONEN_5_EXPLAIN to
                "П’ять тренувальних зон із резерву пульсу (від спокою до максимуму).",
            StringKeys.FORMULA_HR_ZONES_KARVONEN_3_TITLE to "Karvonen (3 зони)",
            StringKeys.FORMULA_HR_ZONES_KARVONEN_3_EXPLAIN to
                "Простіша розкладка з трьох зон резерву пульсу.",
            StringKeys.FORMULA_HR_ZONES_PERCENT_HRMAX_5_TITLE to "% HRmax (5 зон)",
            StringKeys.FORMULA_HR_ZONES_PERCENT_HRMAX_5_EXPLAIN to
                "П’ять зон як відсотки від оціненого макс. пульсу.",
            StringKeys.WORKOUT_START to "Почати тренування",
            StringKeys.WORKOUT_END to "Завершити тренування",
            StringKeys.WORKOUT_ADD_SET to "Додати сет",
            StringKeys.WORKOUT_ACTIVE to "Тренування триває",
            StringKeys.WORKOUT_IDLE to "Немає активного тренування",
            StringKeys.WORKOUT_EXERCISE to "Вправа",
            StringKeys.WORKOUT_WEIGHT_KG to "Вага (кг)",
            StringKeys.WORKOUT_REPS to "Повторення",
            StringKeys.WORKOUT_RIR to "RIR",
            StringKeys.WORKOUT_WARMUP to "Розминочний сет",
            StringKeys.WORKOUT_INCLUDE_WARMUPS to "Рахувати розминку в обсязі",
            StringKeys.WORKOUT_SUMMARY_TITLE to "Підсумок сесії",
            StringKeys.WORKOUT_VOLUME_LOAD to "Обсяг навантаження",
            StringKeys.WORKOUT_TOTAL_WEIGHT_LIFTED to "Загальна піднята вага",
            StringKeys.WORKOUT_HARD_SETS to "Важкі сети",
            StringKeys.WORKOUT_PER_EXERCISE to "За вправами",
            StringKeys.WORKOUT_INTRO to
                "Записуйте силові сети для оцінок тренувань. Показники — тренерські підказки, " +
                "а не медична консультація.",
            StringKeys.WORKOUT_SUMMARY_DURATION to "Тривалість",
            StringKeys.WORKOUT_SUMMARY_ACTIVE_KCAL to "Активні ккал",
            StringKeys.WORKOUT_SUMMARY_TOTAL_KCAL to "Усього ккал",
            StringKeys.WORKOUT_SUMMARY_AVG_HR to "Середній пульс",
            StringKeys.WORKOUT_SUMMARY_MAX_HR to "Максимальний пульс",
            StringKeys.WORKOUT_SUMMARY_ENERGY_ESTIMATE to "Енергія — оцінка самопочуття (MET × маса тіла).",
            StringKeys.WORKOUT_SUMMARY_EFFORT to "Оцінка зусилля",
            StringKeys.WORKOUT_SUMMARY_HR_RECOVERY to "Відновлення пульсу",
            StringKeys.WORKOUT_START_ON_WATCH to "Почати на Apple Watch",
            StringKeys.WORKOUT_WATCH_STARTED to "Запуск тренування на годиннику…",
            StringKeys.WORKOUT_WATCH_UNAVAILABLE to "Годинник недоступний — використайте оцінку на iPhone.",
            StringKeys.WORKOUT_START_IPHONE_ESTIMATE to "Запис на iPhone (оцінка)",
            StringKeys.WORKOUT_YOGA_TITLE to "Йога",
            StringKeys.WORKOUT_PILATES_TITLE to "Пілатес",
            StringKeys.WORKOUT_MINDFUL_BODY_INTRO to
                "Краще жива сесія на годиннику для пульсу й енергії. Лише на iPhone OmniGart " +
                "зберігає оцінку MET із відповідною позначкою.",
            StringKeys.WORKOUT_MINDFUL_MINUTES_TOGGLE to "Зберігати хвилини усвідомленості для йоги",
            StringKeys.WORKOUT_MINDFUL_MINUTES_BODY to
                "За бажанням. Типово вимкнено. Записує mindful-сесію лише коли ви це вмикаєте.",
            StringKeys.WORKOUT_MINDFUL_SETTINGS_TITLE to "Усвідомленість у йозі",
            StringKeys.DATA_PRIVACY_TITLE to "Дані та приватність",
            StringKeys.DATA_PRIVACY_INTRO to
                "OmniGart зберігає повну локальну копію тренувань і зразків на цьому пристрої. " +
                "Експорт лишається на пристрої. Синхронізація з платформою необов’язкова.",
            StringKeys.DATA_PRIVACY_EXPORT to "Експортувати дані самопочуття",
            StringKeys.DATA_PRIVACY_IMPORT_JSON to "Імпортувати OmniGart JSON"
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
