package com.omnigart.shared.logging

import com.omnigart.shared.calc.MassUnit
import com.omnigart.shared.domain.Session
import com.omnigart.shared.domain.WorkoutSet
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class InWorkoutLoggingTest {
    private fun session(vararg sets: WorkoutSet) = Session(
        id = "s1",
        startedAt = Instant.parse("2026-10-23T10:00:00Z"),
        sets = sets.toList()
    )

    @Test
    fun setLabel_warmupAndWorking() {
        val sets =
            listOf(
                WorkoutSet(id = "w", exerciseId = "bench", setIndex = 0, reps = 10, isWarmup = true),
                WorkoutSet(id = "a", exerciseId = "bench", setIndex = 1, reps = 8),
                WorkoutSet(id = "b", exerciseId = "bench", setIndex = 2, reps = 8)
            )
        assertEquals("W", InWorkoutLogging.setLabel(sets, "w"))
        assertEquals("1", InWorkoutLogging.setLabel(sets, "a"))
        assertEquals("2", InWorkoutLogging.setLabel(sets, "b"))
    }

    @Test
    fun previousDisplay_formatsKg() {
        val prev =
            WorkoutSet(
                id = "p",
                exerciseId = "bench",
                setIndex = 0,
                reps = 12,
                weightKg = 15.0,
                completed = true
            )
        assertEquals("15kg × 12", InWorkoutLogging.previousDisplay(prev, MassUnit.KG))
        assertNull(InWorkoutLogging.previousDisplay(null, MassUnit.KG))
    }

    @Test
    fun restTimer_ticksToFinished() {
        var state = RestTimerState().start(2)
        assertTrue(state.running)
        state = state.tick()
        assertEquals(1, state.remainingSeconds)
        state = state.tick()
        assertFalse(state.running)
        assertTrue(state.finished)
    }

    @Test
    fun headerStats_sumsCompletedVolume() {
        val s =
            session(
                WorkoutSet(
                    id = "1",
                    exerciseId = "bench",
                    setIndex = 0,
                    reps = 10,
                    weightKg = 50.0,
                    completed = true
                ),
                WorkoutSet(
                    id = "2",
                    exerciseId = "bench",
                    setIndex = 1,
                    reps = 10,
                    weightKg = 50.0,
                    completed = false
                )
            )
        val stats = InWorkoutLogging.headerStats(s)
        assertEquals(500.0, stats.totalVolumeKg, absoluteTolerance = 0.01)
        assertEquals(1, stats.completedSetCount)
    }

    @Test
    fun progressionChip_formatsTarget() {
        assertEquals("60kg × 8", ProgressionChip.label(60.0, 8, MassUnit.KG))
        assertNull(ProgressionChip.label(null, null, MassUnit.KG))
    }
}
