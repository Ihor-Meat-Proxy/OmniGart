package com.omnigart.hydration.caffeine

/**
 * Sleep-timing insight for late caffeine.
 *
 * Drake et al. (2013) J Clin Sleep Med — 400 mg caffeine at bedtime, 3 h, or 6 h before bed
 * all reduced total sleep time; even the 6-hour condition was disruptive.
 * DOI 10.5664/jcsm.3170
 */
object LateCaffeineInsight {

    /** Hours before bedtime still considered “late” per Drake 2013’s 6 h window. */
    const val LATE_WINDOW_HOURS = 6

    data class CaffeineEvent(
        /** Minutes from local midnight when the drink was logged. */
        val loggedAtMinutesFromMidnight: Int,
        val caffeineMg: Double,
    )

    data class Insight(
        val triggered: Boolean,
        val eventsInWindow: List<CaffeineEvent>,
        val totalMgInWindow: Double,
        val messageEnGb: String?,
        val citation: String,
    )

    /**
     * @param bedtimeMinutesFromMidnight local bedtime from sleep data (0..1439).
     * @param events caffeine-containing logs for the calendar day (and prior evening if needed).
     */
    fun evaluate(
        bedtimeMinutesFromMidnight: Int,
        events: List<CaffeineEvent>,
        windowHours: Int = LATE_WINDOW_HOURS,
    ): Insight {
        val bed = ((bedtimeMinutesFromMidnight % 1440) + 1440) % 1440
        val windowMinutes = windowHours.coerceAtLeast(0) * 60
        val inWindow =
            events.filter { event ->
                if (event.caffeineMg <= 0.0) return@filter false
                minutesBeforeBed(event.loggedAtMinutesFromMidnight, bed) in 0 until windowMinutes
            }
        val total = inWindow.sumOf { it.caffeineMg }
        val triggered = inWindow.isNotEmpty()
        val message =
            if (triggered) {
                "Caffeine within $windowHours h of bedtime can shorten sleep — Drake et al. (2013) found " +
                    "400 mg disrupted sleep even when taken 6 h before bed " +
                    "(DOI 10.5664/jcsm.3170). You logged ${total.toInt()} mg in that window."
            } else {
                null
            }
        return Insight(
            triggered = triggered,
            eventsInWindow = inWindow,
            totalMgInWindow = total,
            messageEnGb = message,
            citation = "Drake et al. (2013) J Clin Sleep Med. DOI 10.5664/jcsm.3170",
        )
    }

    /** Minutes between [logged] and [bed], counting backwards across midnight if needed. */
    fun minutesBeforeBed(loggedMinutesFromMidnight: Int, bedMinutesFromMidnight: Int): Int {
        val logged = ((loggedMinutesFromMidnight % 1440) + 1440) % 1440
        val bed = ((bedMinutesFromMidnight % 1440) + 1440) % 1440
        return if (bed >= logged) bed - logged else bed + 1440 - logged
    }
}
