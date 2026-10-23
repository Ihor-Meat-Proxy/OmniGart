package com.omnigart.shared.calc

import com.omnigart.shared.domain.ExerciseLoadSemantics
import kotlin.math.max

/**
 * 1RM helpers for bodyweight-plus-external movements.
 * e1RM is computed on **total system load**, then optionally shown as an
 * **added-weight equivalent** (system e1RM − bodyweight × fraction).
 */
data class SystemOneRepMax(
    /** Epley (or chosen formula) on effective system load. */
    val systemE1rmKg: Double,
    /** max(0, systemE1rm − bodyweightShare). */
    val addedWeightEquivalentKg: Double,
    val bodyweightShareKg: Double,
    val effectiveLoadKg: Double,
    val estimated: Boolean
)

object BodyweightOneRepMax {
    fun fromSet(effective: EffectiveLoad, reps: Int, bodyweightKg: Double, semantics: ExerciseLoadSemantics): SystemOneRepMax {
        require(reps >= 1)
        require(bodyweightKg > 0.0)
        val system =
            if (effective.loadKg <= 0.0) {
                0.0
            } else {
                OneRepMax.epley(effective.loadKg, reps)
            }
        val bwShare =
            if (semantics.usesBodyweight) {
                bodyweightKg * semantics.bodyweightFraction.proportion
            } else {
                0.0
            }
        return SystemOneRepMax(
            systemE1rmKg = system,
            addedWeightEquivalentKg = max(0.0, system - bwShare),
            bodyweightShareKg = bwShare,
            effectiveLoadKg = effective.loadKg,
            estimated = effective.estimated
        )
    }
}
