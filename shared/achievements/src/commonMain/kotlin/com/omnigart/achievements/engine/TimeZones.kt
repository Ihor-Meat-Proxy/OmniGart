package com.omnigart.achievements.engine

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/**
 * Day-boundary helpers that are DST-safe via kotlinx-datetime TimeZone conversion.
 * Travel: each event carries its own [timeZoneId]; local calendar days follow that zone.
 */
object DayBoundaries {
    fun timeZone(id: String): TimeZone = TimeZone.of(id)

    fun localDate(instant: Instant, timeZoneId: String): LocalDate =
        instant.toLocalDateTime(timeZone(timeZoneId)).date

    fun localDayKey(instant: Instant, timeZoneId: String): String =
        localDate(instant, timeZoneId).toString()

    /** Inclusive chronological list of local dates for a rolling window ending on [end]. */
    fun rollingDates(end: LocalDate, days: Int): List<LocalDate> {
        require(days >= 1)
        return (days - 1 downTo 0).map { offset -> end.minus(DatePeriod(days = offset)) }
    }

    fun plusDays(date: LocalDate, days: Int): LocalDate = date.plus(DatePeriod(days = days))

    fun minusDays(date: LocalDate, days: Int): LocalDate = date.minus(DatePeriod(days = days))
}
