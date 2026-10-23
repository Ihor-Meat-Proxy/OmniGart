package com.omnigart.sensors.reconnect

import kotlin.math.min
import kotlin.math.pow
import kotlin.random.Random

/**
 * Battery-friendly reconnect schedule: exponential backoff with full jitter.
 *
 * Defaults: 1s → 2s → 4s … capped at 60s. Jitter avoids reconnect stampedes when
 * multiple sensors drop simultaneously.
 */
data class ReconnectBackoff(
    val initialDelayMs: Long = 1_000L,
    val maxDelayMs: Long = 60_000L,
    val multiplier: Double = 2.0,
    val maxAttempts: Int = Int.MAX_VALUE,
    private val random: Random = Random.Default,
) {
    fun delayMsForAttempt(attempt: Int): Long {
        require(attempt >= 0)
        if (attempt >= maxAttempts) return -1L
        val exp = initialDelayMs * multiplier.pow(attempt.toDouble())
        val capped = min(maxDelayMs.toDouble(), exp).toLong()
        // Full jitter: uniform in [0, capped]
        return if (capped <= 0L) 0L else random.nextLong(capped + 1)
    }

    fun sequence(limit: Int = 8): List<Long> =
        (0 until limit).map { delayMsForAttempt(it).coerceAtLeast(0L) }
}
