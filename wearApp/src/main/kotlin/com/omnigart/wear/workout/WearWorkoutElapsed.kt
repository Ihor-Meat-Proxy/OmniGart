package com.omnigart.wear.workout

/**
 * Active (unpaused) elapsed time for Wear Health Services sessions.
 *
 * Health Services records pause/resume via [androidx.health.services.client.ExerciseClient];
 * the UI timer must still exclude paused wall time when ending while paused or mid-pause.
 */
object WearWorkoutElapsed {
    fun activeElapsedMs(nowMs: Long, startedAtMs: Long, accumulatedPauseMs: Long, pauseBeganAtMs: Long?): Long {
        var pause = accumulatedPauseMs.coerceAtLeast(0L)
        if (pauseBeganAtMs != null) {
            pause += (nowMs - pauseBeganAtMs).coerceAtLeast(0L)
        }
        return (nowMs - startedAtMs - pause).coerceAtLeast(0L)
    }
}
