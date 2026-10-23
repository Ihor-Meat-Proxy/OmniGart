package com.omnigart.hydration.reminders

/**
 * Pure scheduling logic for hydration reminders.
 * Host apps supply clock/sleep windows; this module never touches notification APIs.
 */
data class QuietHours(
    /** Inclusive start, minutes from local midnight. */
    val startMinutes: Int,
    /** Exclusive end, minutes from local midnight (may wrap past midnight). */
    val endMinutes: Int,
) {
    fun contains(minutesFromMidnight: Int): Boolean {
        val t = ((minutesFromMidnight % 1440) + 1440) % 1440
        val start = ((startMinutes % 1440) + 1440) % 1440
        val end = ((endMinutes % 1440) + 1440) % 1440
        return if (start <= end) {
            t in start until end
        } else {
            t >= start || t < end
        }
    }
}

data class SleepWindow(
    val startMinutes: Int,
    val endMinutes: Int,
) {
    fun contains(minutesFromMidnight: Int): Boolean =
        QuietHours(startMinutes, endMinutes).contains(minutesFromMidnight)
}

data class ReminderConfig(
    /** Minimum minutes between reminders. */
    val baseSpacingMinutes: Int = 90,
    val quietHours: QuietHours = QuietHours(22 * 60, 7 * 60),
    val pauseDuringSleep: Boolean = true,
    /** When progress is low mid-day, tighten spacing by this factor (0–1 → shorter gaps). */
    val catchUpSpacingFactor: Double = 0.7,
    /** When progress is ahead, stretch spacing. */
    val aheadSpacingFactor: Double = 1.3,
)

data class ReminderDecision(
    val shouldNotify: Boolean,
    val reason: String,
    /** Suggested minutes until the next eligible reminder attempt. */
    val nextAttemptInMinutes: Int,
)

object SmartReminders {

    /**
     * Decide whether to fire a reminder now.
     *
     * Stops once [countedMl] ≥ [targetMl]. Respects quiet hours and optional sleep pause.
     * Spacing adapts to expected linear progress through the waking day.
     */
    fun decide(
        nowMinutesFromMidnight: Int,
        countedMl: Double,
        targetMl: Double,
        minutesSinceLastReminder: Int?,
        sleepWindow: SleepWindow?,
        config: ReminderConfig = ReminderConfig(),
        wakeMinutes: Int = 7 * 60,
        dayEndMinutes: Int = 22 * 60,
    ): ReminderDecision {
        if (targetMl <= 0.0) {
            return ReminderDecision(false, "no_target", config.baseSpacingMinutes)
        }
        if (countedMl >= targetMl) {
            return ReminderDecision(false, "target_reached", config.baseSpacingMinutes)
        }
        if (config.quietHours.contains(nowMinutesFromMidnight)) {
            return ReminderDecision(false, "quiet_hours", minutesUntil(nowMinutesFromMidnight, config.quietHours.endMinutes))
        }
        if (config.pauseDuringSleep && sleepWindow != null && sleepWindow.contains(nowMinutesFromMidnight)) {
            return ReminderDecision(false, "asleep", minutesUntil(nowMinutesFromMidnight, sleepWindow.endMinutes))
        }

        val spacing = adaptedSpacingMinutes(
            nowMinutesFromMidnight = nowMinutesFromMidnight,
            countedMl = countedMl,
            targetMl = targetMl,
            wakeMinutes = wakeMinutes,
            dayEndMinutes = dayEndMinutes,
            config = config,
        )

        if (minutesSinceLastReminder != null && minutesSinceLastReminder < spacing) {
            return ReminderDecision(
                false,
                "spacing",
                (spacing - minutesSinceLastReminder).coerceAtLeast(1),
            )
        }

        return ReminderDecision(true, "due", spacing)
    }

    fun adaptedSpacingMinutes(
        nowMinutesFromMidnight: Int,
        countedMl: Double,
        targetMl: Double,
        wakeMinutes: Int,
        dayEndMinutes: Int,
        config: ReminderConfig,
    ): Int {
        val dayLength = (dayEndMinutes - wakeMinutes).coerceAtLeast(60)
        val elapsed = (nowMinutesFromMidnight - wakeMinutes).coerceIn(0, dayLength)
        val expectedFraction = elapsed.toDouble() / dayLength.toDouble()
        val actualFraction = (countedMl / targetMl).coerceAtLeast(0.0)
        val base = config.baseSpacingMinutes.coerceAtLeast(15)
        val factor =
            when {
                actualFraction < expectedFraction - 0.15 -> config.catchUpSpacingFactor
                actualFraction > expectedFraction + 0.15 -> config.aheadSpacingFactor
                else -> 1.0
            }
        return (base * factor).toInt().coerceIn(15, base * 2)
    }

    fun minutesUntil(from: Int, to: Int): Int {
        val a = ((from % 1440) + 1440) % 1440
        val b = ((to % 1440) + 1440) % 1440
        return if (b >= a) b - a else b + 1440 - a
    }
}
