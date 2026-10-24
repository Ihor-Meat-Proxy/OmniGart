package com.omnigart.nutrition

/**
 * Configurable start-of-day for nutrition logging windows.
 * Default 04:00 keeps late-night meals on the previous day.
 */
object DayBoundary {

    const val DEFAULT_START_MINUTES = 4 * 60

    /**
     * Map an instant to a day key using [dayStartMinutes] after local midnight.
     * [localMinutesFromMidnight] and [localDateKeyAtMidnight] are supplied by the host
     * (timezone-aware); this keeps the module free of kotlinx-datetime coupling.
     *
     * @param localMinutesFromMidnight minutes since local midnight of the wall-clock instant
     * @param todayDateKey yyyy-MM-dd for the wall-clock local date
     * @param yesterdayDateKey yyyy-MM-dd for the previous local date
     */
    fun dayKeyForInstant(
        localMinutesFromMidnight: Int,
        todayDateKey: String,
        yesterdayDateKey: String,
        dayStartMinutes: Int = DEFAULT_START_MINUTES,
    ): String {
        val start = ((dayStartMinutes % 1440) + 1440) % 1440
        val minutes = ((localMinutesFromMidnight % 1440) + 1440) % 1440
        return if (minutes < start) yesterdayDateKey else todayDateKey
    }
}
