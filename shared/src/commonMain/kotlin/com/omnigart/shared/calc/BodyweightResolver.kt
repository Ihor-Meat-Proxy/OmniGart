package com.omnigart.shared.calc

import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

enum class BodyweightSource {
    HEALTH,
    PROFILE
}

data class BodyweightSample(val weightKg: Double, val recordedAt: Instant, val source: BodyweightSource) {
    init {
        require(weightKg > 0.0) { "weightKg must be positive" }
    }
}

/**
 * Result of resolving body mass for a workout date.
 * [promptInline] is true when the reading is missing or older than [STALE_AFTER_DAYS].
 */
data class BodyweightResolution(
    val weightKg: Double?,
    val source: BodyweightSource?,
    val recordedAt: Instant?,
    val stale: Boolean,
    val promptInline: Boolean,
    val missing: Boolean
)

/**
 * Prefer the latest health / body measurement on or before the workout date;
 * fall back to profile mass. Prompt inline when missing or stale (&gt; 14 days).
 */
object BodyweightResolver {
    const val STALE_AFTER_DAYS: Int = 14

    fun resolve(
        workoutAt: Instant,
        healthOrBodySamples: List<BodyweightSample>,
        profileWeightKg: Double?,
        profileUpdatedAt: Instant? = null
    ): BodyweightResolution {
        val eligibleHealth =
            healthOrBodySamples
                .filter { it.source == BodyweightSource.HEALTH && it.recordedAt <= workoutAt }
                .maxByOrNull { it.recordedAt }

        val chosen =
            eligibleHealth
                ?: profileWeightKg?.let { kg ->
                    if (kg > 0.0) {
                        BodyweightSample(
                            weightKg = kg,
                            recordedAt = profileUpdatedAt ?: workoutAt,
                            source = BodyweightSource.PROFILE
                        )
                    } else {
                        null
                    }
                }

        if (chosen == null) {
            return BodyweightResolution(
                weightKg = null,
                source = null,
                recordedAt = null,
                stale = true,
                promptInline = true,
                missing = true
            )
        }

        val age = workoutAt - chosen.recordedAt
        val stale = age > STALE_AFTER_DAYS.days
        return BodyweightResolution(
            weightKg = chosen.weightKg,
            source = chosen.source,
            recordedAt = chosen.recordedAt,
            stale = stale,
            promptInline = stale,
            missing = false
        )
    }
}
