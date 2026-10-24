package com.omnigart.planning

/**
 * Pure calendar helpers for the Planner tab (week strip + month grid).
 * Dates are ISO `yyyy-MM-dd` so the module stays free of platform calendar APIs.
 */
object PlannerCalendar {
    data class DayCell(
        /** ISO local date. */
        val dateIso: String,
        /** Day of month 1–31. */
        val dayOfMonth: Int,
        /** True when this cell belongs to the displayed month (month grid only). */
        val inMonth: Boolean,
        val hasPlanned: Boolean,
        val hasCompleted: Boolean
    )

    /**
     * Seven consecutive ISO dates starting at [weekStartIso] (inclusive).
     */
    fun weekStrip(weekStartIso: String, plannedDates: Set<String>, completedDates: Set<String>): List<DayCell> {
        val start = IsoDate.parse(weekStartIso)
        return (0 until 7).map { offset ->
            val d = start.plusDays(offset)
            DayCell(
                dateIso = d.toIso(),
                dayOfMonth = d.day,
                inMonth = true,
                hasPlanned = d.toIso() in plannedDates,
                hasCompleted = d.toIso() in completedDates
            )
        }
    }

    /**
     * Month grid: weeks of 7 cells from Monday of the week containing the 1st
     * through Sunday of the week containing the last day of [year]/[month].
     */
    fun monthGrid(year: Int, month: Int, plannedDates: Set<String>, completedDates: Set<String>): List<DayCell> {
        require(month in 1..12)
        val first = IsoDate(year, month, 1)
        val daysInMonth = IsoDate.daysInMonth(year, month)
        val startOffset = (first.dayOfWeekMonday0())
        val gridStart = first.plusDays(-startOffset)
        val totalCells = ((startOffset + daysInMonth + 6) / 7) * 7
        return (0 until totalCells).map { offset ->
            val d = gridStart.plusDays(offset)
            DayCell(
                dateIso = d.toIso(),
                dayOfMonth = d.day,
                inMonth = d.year == year && d.month == month,
                hasPlanned = d.toIso() in plannedDates,
                hasCompleted = d.toIso() in completedDates
            )
        }
    }
}

/** Minimal Gregorian date maths (no kotlinx-datetime dependency in planning). */
internal data class IsoDate(val year: Int, val month: Int, val day: Int) {
    fun toIso(): String = "${year.toString().padStart(4, '0')}-" +
        "${month.toString().padStart(2, '0')}-" +
        day.toString().padStart(2, '0')

    fun plusDays(delta: Int): IsoDate {
        var y = year
        var m = month
        var d = day + delta
        while (d > daysInMonth(y, m)) {
            d -= daysInMonth(y, m)
            m += 1
            if (m > 12) {
                m = 1
                y += 1
            }
        }
        while (d < 1) {
            m -= 1
            if (m < 1) {
                m = 12
                y -= 1
            }
            d += daysInMonth(y, m)
        }
        return IsoDate(y, m, d)
    }

    /** Monday = 0 … Sunday = 6 (ISO-8601). */
    fun dayOfWeekMonday0(): Int {
        // Sakamoto's methods
        val t = intArrayOf(0, 3, 2, 5, 0, 3, 5, 1, 4, 6, 2, 4)
        var y = year
        if (month < 3) y -= 1
        val dow = (y + y / 4 - y / 100 + y / 400 + t[month - 1] + day) % 7
        // Sakamoto: 0=Sun … 6=Sat → convert to Mon=0
        return (dow + 6) % 7
    }

    companion object {
        fun parse(iso: String): IsoDate {
            val parts = iso.split('-')
            require(parts.size == 3) { "Expected yyyy-MM-dd, got $iso" }
            return IsoDate(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
        }

        fun daysInMonth(year: Int, month: Int): Int = when (month) {
            1, 3, 5, 7, 8, 10, 12 -> 31
            4, 6, 9, 11 -> 30
            2 -> if (isLeap(year)) 29 else 28
            else -> error("Invalid month $month")
        }

        private fun isLeap(year: Int): Boolean = (year % 4 == 0 && year % 100 != 0) || year % 400 == 0
    }
}
