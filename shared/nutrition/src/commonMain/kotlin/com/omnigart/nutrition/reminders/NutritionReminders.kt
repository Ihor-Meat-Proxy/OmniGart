package com.omnigart.nutrition.reminders

/**
 * Pure scheduling for local nutrition reminders (weigh-in nudge, measurements, target change).
 * Host apps own UNUserNotificationCenter / NotificationCompat — this module never posts.
 */
object NutritionReminders {

    const val WEIGH_IN_NUDGE_AFTER_DAYS = 3
    const val MEASUREMENT_MIN_WEEKS = 2
    const val MEASUREMENT_MAX_WEEKS = 4
    const val DEFAULT_MEASUREMENT_WEEKS = 3

    data class ReminderConfig(
        val paused: Boolean = false,
        val weighInNudgeEnabled: Boolean = true,
        val measurementEveryWeeks: Int = DEFAULT_MEASUREMENT_WEEKS,
        /** Minutes from midnight for optional morning weigh-in reminder. */
        val morningWeighInMinutes: Int? = 7 * 60,
    )

    enum class ReminderKind {
        MORNING_WEIGH_IN,
        WEIGH_IN_NUDGE,
        MEASUREMENTS_DUE,
        TARGET_CHANGED,
        WEEKLY_CHECK_IN,
        LOGGING_BREAK,
    }

    data class ReminderDecision(
        val kind: ReminderKind,
        val shouldNotify: Boolean,
        val reasonKey: String,
    )

    fun weighInNudge(
        daysSinceLastWeighIn: Int?,
        config: ReminderConfig,
    ): ReminderDecision {
        if (config.paused || !config.weighInNudgeEnabled) {
            return ReminderDecision(ReminderKind.WEIGH_IN_NUDGE, false, "paused_or_disabled")
        }
        if (daysSinceLastWeighIn == null) {
            return ReminderDecision(ReminderKind.WEIGH_IN_NUDGE, true, "no_weigh_in_yet")
        }
        val due = daysSinceLastWeighIn >= WEIGH_IN_NUDGE_AFTER_DAYS
        return ReminderDecision(
            ReminderKind.WEIGH_IN_NUDGE,
            due,
            if (due) "days_without_weigh_in" else "recent_weigh_in",
        )
    }

    fun measurementsDue(
        daysSinceLastMeasurements: Int?,
        config: ReminderConfig,
    ): ReminderDecision {
        if (config.paused) {
            return ReminderDecision(ReminderKind.MEASUREMENTS_DUE, false, "paused")
        }
        val weeks = config.measurementEveryWeeks.coerceIn(MEASUREMENT_MIN_WEEKS, MEASUREMENT_MAX_WEEKS)
        val thresholdDays = weeks * 7
        if (daysSinceLastMeasurements == null) {
            return ReminderDecision(ReminderKind.MEASUREMENTS_DUE, true, "no_measurements_yet")
        }
        val due = daysSinceLastMeasurements >= thresholdDays
        return ReminderDecision(
            ReminderKind.MEASUREMENTS_DUE,
            due,
            if (due) "measurements_interval" else "measurements_recent",
        )
    }

    fun targetChanged(previousKcal: Double?, newKcal: Double?, config: ReminderConfig): ReminderDecision {
        if (config.paused) {
            return ReminderDecision(ReminderKind.TARGET_CHANGED, false, "paused")
        }
        if (previousKcal == null || newKcal == null) {
            return ReminderDecision(ReminderKind.TARGET_CHANGED, false, "no_comparison")
        }
        val changed = kotlin.math.abs(previousKcal - newKcal) >= 1.0
        return ReminderDecision(
            ReminderKind.TARGET_CHANGED,
            changed,
            if (changed) "target_updated" else "unchanged",
        )
    }

    fun recomputeNeeded(
        daysSinceLastRecompute: Int,
        weightChangeKg: Double,
        goalChanged: Boolean,
    ): Boolean =
        goalChanged || daysSinceLastRecompute >= 7 || kotlin.math.abs(weightChangeKg) >= 2.0
}
