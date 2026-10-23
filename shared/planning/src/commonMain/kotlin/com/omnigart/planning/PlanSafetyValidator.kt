package com.omnigart.planning

import com.omnigart.content.Level
import com.omnigart.content.TrainingGoal
import kotlin.math.min

/**
 * Clamps AI (or any) proposals to rule-engine safety bounds.
 */
class PlanSafetyValidator(private val maxSetsPerSessionExercise: Int = 6, private val maxSessionsPerWeek: Int = 6) {
    fun validateAndClamp(proposal: AiPlanProposal, profile: UserTrainingProfile): Pair<AiPlanProposal, List<PlanExplanation>> {
        val explanations = mutableListOf<PlanExplanation>()
        val level = profile.levels.firstOrNull()?.level ?: Level.BEGINNER
        val goal = profile.goals.firstOrNull() ?: TrainingGoal.GENERAL_FITNESS
        val band = VolumeLandmarks.weeklyHardSetTarget(level, goal)
        val ceiling = band.last + 4

        val sessions = proposal.sessions
            .take(min(profile.availableDaysPerWeek, maxSessionsPerWeek))
            .map { session ->
                session.copy(
                    prescriptions = session.prescriptions.map { rx ->
                        val sets = rx.sets.coerceIn(1, maxSetsPerSessionExercise)
                        if (sets != rx.sets) {
                            explanations += PlanExplanation(
                                code = "clamp.sets_per_exercise",
                                message = "Clamped sets for ${rx.exerciseId} to $sets for safety.",
                                evidence = EvidenceCatalog.ACSM_RT_2026
                            )
                        }
                        rx.copy(sets = sets, rir = normalizeRir(rx.rir, profile))
                    }
                )
            }

        val volume = proposal.claimedVolume.map { target ->
            val capped = target.hardSets.coerceAtMost(ceiling)
            if (capped != target.hardSets) {
                explanations += PlanExplanation(
                    code = "clamp.weekly_volume",
                    message = "Capped ${target.muscle} weekly hard sets to $capped (goal band ${band.first}-${band.last}).",
                    evidence = EvidenceCatalog.PELLAND_DOSE_2025
                )
            }
            target.copy(hardSets = capped)
        }

        return AiPlanProposal(sessions = sessions, claimedVolume = volume) to explanations
    }

    private fun normalizeRir(rir: String, profile: UserTrainingProfile): String {
        if (profile.readiness == ReadinessBand.LOW || profile.readiness == ReadinessBand.VERY_LOW) {
            return "3-5"
        }
        return rir.ifBlank { "2-3" }
    }
}
