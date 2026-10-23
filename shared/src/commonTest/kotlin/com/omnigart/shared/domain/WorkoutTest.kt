package com.omnigart.shared.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Instant

class WorkoutTest {
    @Test
    fun sessionDurationHours() {
        val session =
            Session(
                id = "s1",
                startedAt = Instant.parse("2026-01-01T10:00:00Z"),
                endedAt = Instant.parse("2026-01-01T11:30:00Z")
            )
        assertEquals(1.5, session.durationHours(), absoluteTolerance = 0.001)
    }

    @Test
    fun setValidation() {
        assertFailsWith<IllegalArgumentException> {
            WorkoutSet(id = "set1", exerciseId = "ex1", setIndex = 0, rir = 11.0)
        }
    }
}
