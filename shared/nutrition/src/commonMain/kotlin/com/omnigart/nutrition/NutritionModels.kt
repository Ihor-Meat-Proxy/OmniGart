package com.omnigart.nutrition

/**
 * Shared nutrition target models (wellness coaching aids, not medical advice).
 * Diary storage lives in a separate food-logging surface; this package only computes targets.
 */

enum class NutritionSex {
    FEMALE,
    MALE,
    UNSPECIFIED,
}

enum class NutritionGoalKind {
    LOSE,
    MAINTAIN,
    GAIN,
}

enum class TargetMode {
    /** Automatic adaptive target with guardrails. */
    GUIDED,

    /** User splits a weekly energy budget across days; guardrails stay on. */
    FLEXIBLE_WEEKLY,

    /** User sets daily energy directly; floor check still warns. */
    MANUAL,
}

enum class ConfidenceLevel {
    ROUGH,
    GOOD,
    PERSONALISED,
}

enum class DayLogStatus {
    EMPTY,
    INCOMPLETE,
    COMPLETE,
    FASTING,
}

enum class BmrMethod {
    MIFFLIN_ST_JEOR,
    KATCH_MCARDLE,
    CUNNINGHAM,
    HENRY_OXFORD_WEIGHT_ONLY,
}

enum class ActivitySource {
    MEASURED_ACTIVE_ENERGY,
    PAL_STEPS,
    NONE,
}

data class NutritionProfileInput(
    val sex: NutritionSex,
    val ageYears: Int?,
    val heightCm: Double?,
    val weightKg: Double?,
    val bodyFatPercent: Double? = null,
    val isAthleticBodyFat: Boolean = false,
    val pregnantOrBreastfeeding: Boolean = false,
)

data class WeightPoint(
    val epochMs: Long,
    val massKg: Double,
)

data class IntakeDay(
    /** Local calendar day key as yyyy-MM-dd in the user's day-boundary timezone. */
    val dayKey: String,
    val energyKcal: Double?,
    val status: DayLogStatus,
)

data class ActivityDay(
    val dayKey: String,
    val activeEnergyKcal: Double? = null,
    val steps: Int? = null,
)

data class NutritionGoalSettings(
    val kind: NutritionGoalKind = NutritionGoalKind.MAINTAIN,
    /** Fraction of body weight per week, e.g. 0.005 = 0.5%/wk. */
    val rateFractionPerWeek: Double = 0.0,
    val targetWeightKg: Double? = null,
    val mode: TargetMode = TargetMode.GUIDED,
    /** Manual mode absolute daily energy, if set. */
    val manualDailyKcal: Double? = null,
    /** Flexible weekly budget total kcal for 7 days. */
    val flexibleWeeklyBudgetKcal: Double? = null,
    /** When true, add discounted exercise kcal on top of the daily target. */
    val addExerciseCalories: Boolean = false,
    val exerciseDiscountBase: Double = 0.72,
    /**
     * Minutes from local midnight that start a nutrition day (configurable start-of-day).
     * Default 04:00 so late-night logging stays on the previous calendar day.
     */
    val dayStartMinutes: Int = 4 * 60,
    /** 0 = Sunday … 6 = Saturday (ISO-style local weekday index used by hosts). */
    val weeklyCheckInWeekday: Int = 0,
    val remindersPaused: Boolean = false,
    val weighInNudgeEnabled: Boolean = true,
    val measurementReminderWeeks: Int = 3,
)

data class WeekdayPattern(
    /** True when that weekday is a training day (index 0 = Sunday … 6 = Saturday). */
    val trainingDays: BooleanArray = BooleanArray(7) { false },
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is WeekdayPattern) return false
        return trainingDays.contentEquals(other.trainingDays)
    }

    override fun hashCode(): Int = trainingDays.contentHashCode()
}

data class ChangeReason(
    val key: String,
    val detail: String? = null,
)

/** Per-weekday energy row — List (not Map) so the type exports cleanly to Swift/ObjC. */
data class WeekdayEnergyTarget(
    val weekday: Int,
    val kcal: Double,
)

data class NutritionTargetResult(
    val estimatedTdeeKcal: Double?,
    val suggestedDailyKcal: Double?,
    val weekdayTargets: List<WeekdayEnergyTarget> = emptyList(),
    val proteinG: Double? = null,
    val fatG: Double? = null,
    val carbsG: Double? = null,
    val fibreG: Double? = null,
    val waterLitres: Double? = null,
    val bmrKcal: Double? = null,
    val bmrMethod: BmrMethod? = null,
    val activitySource: ActivitySource = ActivitySource.NONE,
    val confidence: ConfidenceLevel = ConfidenceLevel.ROUGH,
    val changeReasons: List<ChangeReason> = emptyList(),
    val floorWarning: FloorWarning? = null,
    val lowIntakeSupportive: Boolean = false,
    val dataInsufficient: Boolean = false,
    val loggingBreak: Boolean = false,
    val pausedKeepingLastEstimate: Boolean = false,
    val labelRoughEstimate: Boolean = false,
) {
    /** Kotlin hosts that prefer a map can rebuild it from [weekdayTargets]. */
    fun weekdayTargetsKcalMap(): Map<Int, Double> = weekdayTargets.associate { it.weekday to it.kcal }
}

data class FloorWarning(
    val belowFloor: Boolean,
    val floorKcal: Double,
    val proposedKcal: Double,
    val messageKey: String = "nutrition_floor_warning",
)
