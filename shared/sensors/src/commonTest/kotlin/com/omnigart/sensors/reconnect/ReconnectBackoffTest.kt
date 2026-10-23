package com.omnigart.sensors.reconnect

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReconnectBackoffTest {

    @Test
    fun respectsCapAndJitterBounds() {
        val backoff = ReconnectBackoff(
            initialDelayMs = 1_000,
            maxDelayMs = 8_000,
            multiplier = 2.0,
            random = Random(42),
        )
        repeat(10) { attempt ->
            val d = backoff.delayMsForAttempt(attempt)
            assertTrue(d in 0L..8_000L, "attempt=$attempt delay=$d")
        }
    }

    @Test
    fun exhaustedAttemptsReturnNegative() {
        val backoff = ReconnectBackoff(maxAttempts = 3, random = Random(0))
        assertTrue(backoff.delayMsForAttempt(0) >= 0)
        assertEquals(-1L, backoff.delayMsForAttempt(3))
    }
}
