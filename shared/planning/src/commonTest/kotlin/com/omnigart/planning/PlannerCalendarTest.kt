package com.omnigart.planning

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlannerCalendarTest {
    @Test
    fun weekStripMarksPlannedAndCompleted() {
        val strip =
            PlannerCalendar.weekStrip(
                weekStartIso = "2026-10-19",
                plannedDates = setOf("2026-10-20"),
                completedDates = setOf("2026-10-21", "2026-10-20")
            )
        assertEquals(7, strip.size)
        assertEquals("2026-10-19", strip.first().dateIso)
        assertEquals("2026-10-25", strip.last().dateIso)
        val tue = strip[1]
        assertTrue(tue.hasPlanned)
        assertTrue(tue.hasCompleted)
        assertFalse(strip[0].hasPlanned)
    }

    @Test
    fun monthGridStartsOnMondayAndFlagsInMonth() {
        // October 2026: 1st is Thursday → grid starts Monday 28 Sep.
        val grid =
            PlannerCalendar.monthGrid(
                year = 2026,
                month = 10,
                plannedDates = setOf("2026-10-15"),
                completedDates = emptySet()
            )
        assertTrue(grid.size >= 28)
        assertEquals(0, grid.size % 7)
        assertEquals("2026-09-28", grid.first().dateIso)
        assertFalse(grid.first().inMonth)
        val fifteenth = grid.first { it.dateIso == "2026-10-15" }
        assertTrue(fifteenth.inMonth)
        assertTrue(fifteenth.hasPlanned)
    }
}
