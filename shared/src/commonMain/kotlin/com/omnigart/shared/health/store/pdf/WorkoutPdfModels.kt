package com.omnigart.shared.health.store.pdf

import kotlinx.serialization.Serializable

/**
 * Platform-agnostic workout PDF layout. Hosts render with UIGraphicsPDFRenderer /
 * PdfDocument using [PdfFontSpec] for locale script coverage.
 */
@Serializable
data class WorkoutPdfDocument(
    val locale: String,
    val brandTitle: String,
    val reportTitle: String,
    val generatedAtIso: String,
    val disclaimer: String,
    val workouts: List<WorkoutPdfPage>,
    val fontSpec: PdfFontSpec = PdfFontSpec.forLocale(locale)
)

@Serializable
data class PdfFontSpec(
    /** Primary text face (Latin + Cyrillic). */
    val bodyFamily: String,
    val boldFamily: String,
    /** Georgian (ka) face when distinct. */
    val georgianFamily: String? = null,
    /** zh-Hans uses platform CJK cascade; hint for hosts. */
    val cjkFamilyHint: String? = null,
    val usePlatformCjkCascade: Boolean = false
) {
    companion object {
        fun forLocale(locale: String): PdfFontSpec {
            val normalized = locale.replace('_', '-')
            return when {
                normalized.startsWith("ka") ->
                    PdfFontSpec(
                        bodyFamily = "NotoSans",
                        boldFamily = "NotoSans-Bold",
                        georgianFamily = "NotoSansGeorgian"
                    )
                normalized.startsWith("zh") ->
                    PdfFontSpec(
                        bodyFamily = "NotoSans",
                        boldFamily = "NotoSans-Bold",
                        cjkFamilyHint = "PingFang SC",
                        usePlatformCjkCascade = true
                    )
                normalized in setOf("uk", "be", "kk") ||
                    normalized.startsWith("uk") ||
                    normalized.startsWith("be") ||
                    normalized.startsWith("kk") ->
                    PdfFontSpec(bodyFamily = "NotoSans", boldFamily = "NotoSans-Bold")
                else ->
                    PdfFontSpec(bodyFamily = "NotoSans", boldFamily = "NotoSans-Bold")
            }
        }
    }
}

@Serializable
data class WorkoutPdfPage(
    val workoutId: String,
    val header: WorkoutPdfHeader,
    val metrics: WorkoutPdfMetrics,
    val heartRate: WorkoutPdfHeartRate?,
    val cardio: WorkoutPdfCardio?,
    val strength: WorkoutPdfStrength?,
    val segments: List<WorkoutPdfSegment>,
    val muscleMap: List<WorkoutPdfMuscleBar>
)

@Serializable
data class WorkoutPdfHeader(val dateLabel: String, val activityLabel: String, val durationLabel: String)

@Serializable
data class WorkoutPdfMetrics(val activeKcalLabel: String, val totalKcalLabel: String, val avgHrLabel: String?, val maxHrLabel: String?)

@Serializable
data class WorkoutPdfHeartRate(
    /** Normalised 0–1 samples for chart polyline (time, bpmFraction). */
    val chartPoints: List<PdfChartPoint>,
    val zoneBands: List<PdfZoneBand>,
    val timeInZoneLines: List<String>
)

@Serializable
data class PdfChartPoint(val x: Float, val y: Float)

@Serializable
data class PdfZoneBand(
    val name: String,
    /** 0–1 of chart height (lower). */
    val lowerY: Float,
    /** 0–1 of chart height (upper). */
    val upperY: Float,
    val colorArgb: Long
)

@Serializable
data class WorkoutPdfCardio(val distanceLabel: String?, val paceLabel: String?, val cadenceLabel: String?)

@Serializable
data class WorkoutPdfStrength(val totalWeightLabel: String, val exercises: List<WorkoutPdfExercise>)

@Serializable
data class WorkoutPdfExercise(val title: String, val setLines: List<String>)

@Serializable
data class WorkoutPdfSegment(val label: String, val detail: String)

@Serializable
data class WorkoutPdfMuscleBar(
    val muscleKey: String,
    val displayName: String,
    /** 0–1 relative intensity. */
    val intensity: Float
)

/** Translated labels passed into [WorkoutPdfBuilder] (host resolves i18n). */
data class WorkoutPdfLabels(
    val brandTitle: String = "OmniGart",
    val singleReportTitle: String,
    val rangeReportTitle: String,
    val disclaimer: String,
    val date: String,
    val activity: String,
    val duration: String,
    val activeKcal: String,
    val totalKcal: String,
    val avgHr: String,
    val maxHr: String,
    val heartRateChart: String,
    val timeInZone: String,
    val distance: String,
    val pace: String,
    val cadence: String,
    val totalWeightLifted: String,
    val setsRepsLoad: String,
    val segments: String,
    val muscleMap: String,
    val zonePrefix: String = "Z",
    val minutesSuffix: String = "min",
    val perKg: String = "kg",
    val activityNames: Map<String, String> = emptyMap()
)
