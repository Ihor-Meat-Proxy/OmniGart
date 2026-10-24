package com.omnigart.wear.workout

import org.junit.Assert.assertEquals
import org.junit.Test

class WearWorkoutElapsedTest {
    @Test
    fun activeElapsedExcludesCompletedPause() {
        val started = 1_000L
        val now = 1_000L + 90_000L
        val elapsed =
            WearWorkoutElapsed.activeElapsedMs(
                nowMs = now,
                startedAtMs = started,
                accumulatedPauseMs = 30_000L,
                pauseBeganAtMs = null
            )
        assertEquals(60_000L, elapsed)
    }

    @Test
    fun activeElapsedExcludesOpenPause() {
        val started = 1_000L
        val pauseBegan = started + 40_000L
        val now = pauseBegan + 25_000L
        val elapsed =
            WearWorkoutElapsed.activeElapsedMs(
                nowMs = now,
                startedAtMs = started,
                accumulatedPauseMs = 0L,
                pauseBeganAtMs = pauseBegan
            )
        assertEquals(40_000L, elapsed)
    }

    @Test
    fun activeElapsedNeverNegative() {
        val elapsed =
            WearWorkoutElapsed.activeElapsedMs(
                nowMs = 100L,
                startedAtMs = 200L,
                accumulatedPauseMs = 0L,
                pauseBeganAtMs = null
            )
        assertEquals(0L, elapsed)
    }
}
