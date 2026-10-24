package com.omnigart.shared.health.store.pdf

import com.omnigart.shared.i18n.StringCatalog

/**
 * Builds [WorkoutPdfLabels] from [StringCatalog] for the given locale.
 * Platform UIs may instead resolve Android/iOS string resources and pass labels directly.
 */
object WorkoutPdfLabelCatalog {
    fun fromCatalog(locale: String): WorkoutPdfLabels {
        fun t(key: String): String = PdfStrings.t(key, locale)
        return WorkoutPdfLabels(
            brandTitle = StringCatalog.t("app_name", locale),
            singleReportTitle = t("pdf_report_single_title"),
            rangeReportTitle = t("pdf_report_range_title"),
            disclaimer = StringCatalog.t("wellness_disclaimer", locale),
            date = t("pdf_field_date"),
            activity = t("pdf_field_activity"),
            duration = t("pdf_field_duration"),
            activeKcal = t("pdf_field_active_kcal"),
            totalKcal = t("pdf_field_total_kcal"),
            avgHr = t("pdf_field_avg_hr"),
            maxHr = t("pdf_field_max_hr"),
            heartRateChart = t("pdf_field_hr_chart"),
            timeInZone = t("pdf_field_time_in_zone"),
            distance = t("pdf_field_distance"),
            pace = t("pdf_field_pace"),
            cadence = t("pdf_field_cadence"),
            totalWeightLifted = t("pdf_field_total_weight"),
            setsRepsLoad = t("pdf_field_sets"),
            segments = t("pdf_field_segments"),
            muscleMap = t("pdf_field_muscle_map"),
            zonePrefix = t("pdf_zone_prefix"),
            minutesSuffix = t("pdf_minutes_suffix"),
            perKg = t("pdf_unit_kg"),
            activityNames =
            mapOf(
                "STRENGTH" to t("pdf_activity_strength"),
                "RUNNING" to t("pdf_activity_running"),
                "CYCLING" to t("pdf_activity_cycling"),
                "SWIMMING" to t("pdf_activity_swimming"),
                "WALKING" to t("pdf_activity_walking"),
                "HIIT" to t("pdf_activity_hiit"),
                "YOGA" to t("pdf_activity_yoga"),
                "PILATES" to t("pdf_activity_pilates"),
                "OTHER" to t("pdf_activity_other")
            )
        )
    }
}
