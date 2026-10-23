package com.omnigart.achievements

import kotlinx.datetime.DatePeriod
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

/**
 * Calendar-day helpers that respect the event's IANA time zone, including DST transitions.
 * Day identity is always [LocalDate] in [timeZoneId], never a fixed UTC offset.
 */
object DayBoundary {
    fun localDate(instant: Instant, timeZoneId: String): LocalDate {
        val zone = TimeZone.of(timeZoneId)
        return instant.toLocalDateTime(zone).date
    }

    fun dayKey(instant: Instant, timeZoneId: String): String =
        localDate(instant, timeZoneId).toString()

    /** Monday-start week key: ISO date of that week's Monday in the given zone. */
    fun weekKey(instant: Instant, timeZoneId: String): String {
        val date = localDate(instant, timeZoneId)
        // kotlinx.datetime.DayOfWeek is Monday-first; ordinal 0 == Monday.
        val mondayOffset = date.dayOfWeek.ordinal
        val monday = date.minus(DatePeriod(days = mondayOffset))
        return monday.toString()
    }

    fun monthKey(instant: Instant, timeZoneId: String): String {
        val date = localDate(instant, timeZoneId)
        return "${date.year}-${((date.month.ordinal + 1).toString()).padStart(2, '0')}"
    }

    fun daysBetween(earlier: LocalDate, later: LocalDate): Int =
        (later.toEpochDays() - earlier.toEpochDays()).toInt()
}
