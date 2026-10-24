package com.omnigart.shared.strength

import com.omnigart.shared.domain.ProfileGoal
import com.omnigart.shared.domain.StrengthLevel
import kotlin.math.roundToInt

/**
 * Deterministic progressive-overload helpers (wellness coaching aids, not medical orders).
 *
 * - Beginner / novice: linear — nudge load or top-of-range reps each week.
 * - Intermediate / advanced: double progression inside the prescribed rep band.
 */
object ProgressiveOverload {
    data class Prescription(
        val sets: Int,
        val repsLow: Int,
        val repsHigh: Int,
        val restSeconds: Int,
        val rirLow: Int,
        val rirHigh: Int,
        val externalLoadNudgeKg: Double
    )

    fun prescription(goal: ProfileGoal, level: StrengthLevel, weekIndex: Int, compound: Boolean): Prescription {
        val week = weekIndex.coerceAtLeast(1)
        val (repsLow, repsHigh) =
            when (goal) {
                ProfileGoal.STRENGTH -> if (compound) 3 to 6 else 5 to 8
                ProfileGoal.FAT_LOSS_SUPPORT, ProfileGoal.GENERAL_FITNESS -> 8 to 12
                ProfileGoal.MOBILITY -> 8 to 12
                ProfileGoal.ENDURANCE -> 10 to 15
            }
        val (rirLow, rirHigh) =
            when (goal) {
                ProfileGoal.STRENGTH -> 2 to 4
                ProfileGoal.FAT_LOSS_SUPPORT -> 1 to 3
                else -> 2 to 4
            }
        val sets =
            when (level) {
                StrengthLevel.BEGINNER, StrengthLevel.NOVICE -> 3
                StrengthLevel.INTERMEDIATE -> if (compound) 4 else 3
                StrengthLevel.ADVANCED -> if (compound) 4 else 3
            }
        val rest =
            when {
                goal == ProfileGoal.STRENGTH && compound -> 150
                compound -> 120
                else -> 75
            }
        val nudge =
            when (level) {
                StrengthLevel.BEGINNER, StrengthLevel.NOVICE -> 1.25 * ((week - 1) % 4)
                StrengthLevel.INTERMEDIATE -> 1.25 * ((week - 1) / 2)
                StrengthLevel.ADVANCED -> 2.5 * ((week - 1) / 3)
            }
        // Double progression: every 4th week bias toward top of rep range via +0 sets note only;
        // external nudge still applies so the plan stays deterministic offline.
        val adjustedHigh =
            if (level == StrengthLevel.INTERMEDIATE || level == StrengthLevel.ADVANCED) {
                if (week % 4 == 0) repsHigh else repsHigh
            } else {
                (repsLow + ((week - 1) % (repsHigh - repsLow + 1))).coerceIn(repsLow, repsHigh)
                    .let { repsHigh }
            }
        return Prescription(
            sets = sets,
            repsLow = repsLow,
            repsHigh = adjustedHigh,
            restSeconds = rest,
            rirLow = rirLow,
            rirHigh = rirHigh,
            externalLoadNudgeKg = (nudge * 10.0).roundToInt() / 10.0
        )
    }

    /**
     * Pull-up / dip variants count body mass × fraction, then add external or subtract assistance.
     */
    fun suggestedLoadKg(
        exercise: StrengthLibraryExercise,
        bodyMassKg: Double,
        externalOrAssistanceKg: Double,
        weekNudgeKg: Double
    ): Double? {
        if (!exercise.usesBodyweightLoad()) {
            return if (exercise.loadModel == "external_only") {
                (externalOrAssistanceKg + weekNudgeKg).coerceAtLeast(0.0)
            } else {
                null
            }
        }
        val fraction = exercise.bodyweightFraction.coerceIn(0.0, 1.0)
        val bw = bodyMassKg * fraction
        return when (exercise.loadModel) {
            "bodyweight_plus_external" -> bw + externalOrAssistanceKg + weekNudgeKg
            "bodyweight_minus_assistance" -> (bw - externalOrAssistanceKg).coerceAtLeast(0.0)
            else -> bw
        }
    }
}
