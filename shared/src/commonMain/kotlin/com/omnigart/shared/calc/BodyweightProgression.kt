package com.omnigart.shared.calc

/**
 * Suggested progression stage for vertical-pull / dip style movements:
 * assisted → bodyweight → weighted.
 */
enum class BodyweightProgressionStage {
    ASSISTED,
    BODYWEIGHT,
    WEIGHTED
}

data class BodyweightProgressionSuggestion(
    val currentStage: BodyweightProgressionStage,
    val suggestedStage: BodyweightProgressionStage,
    val rationale: String
)

object BodyweightProgression {
    /**
     * Classify a set from added / assistance masses (kg).
     * Assistance &gt; 0 → assisted; else added &gt; 0 → weighted; else bodyweight.
     */
    fun stageFor(addedKg: Double, assistanceKg: Double): BodyweightProgressionStage =
        when {
            assistanceKg > 0.0 -> BodyweightProgressionStage.ASSISTED
            addedKg > 0.0 -> BodyweightProgressionStage.WEIGHTED
            else -> BodyweightProgressionStage.BODYWEIGHT
        }

    /**
     * Suggest the next stage from recent completed working sets.
     * Heuristic: if the lightest recent assistance is 0 and best BW reps ≥ [bodyweightRepTarget],
     * suggest weighted; if still assisting but assistance trending down, stay assisted with note;
     * if already weighted, stay weighted.
     */
    fun suggest(
        recentAddedKg: List<Double>,
        recentAssistanceKg: List<Double>,
        recentBodyweightReps: List<Int> = emptyList(),
        bodyweightRepTarget: Int = 8
    ): BodyweightProgressionSuggestion {
        val maxAssist = recentAssistanceKg.maxOrNull() ?: 0.0
        val minAssist = recentAssistanceKg.filter { it > 0.0 }.minOrNull()
        val maxAdded = recentAddedKg.maxOrNull() ?: 0.0
        val bestBwReps = recentBodyweightReps.maxOrNull() ?: 0

        val current =
            when {
                maxAssist > 0.0 && maxAdded <= 0.0 -> BodyweightProgressionStage.ASSISTED
                maxAdded > 0.0 -> BodyweightProgressionStage.WEIGHTED
                else -> BodyweightProgressionStage.BODYWEIGHT
            }

        return when (current) {
            BodyweightProgressionStage.ASSISTED -> {
                if (minAssist == null || (recentAssistanceKg.any { it <= 0.0 } && bestBwReps >= bodyweightRepTarget)) {
                    BodyweightProgressionSuggestion(
                        currentStage = current,
                        suggestedStage = BodyweightProgressionStage.BODYWEIGHT,
                        rationale = "Assistance is near zero with solid bodyweight reps; try unassisted sets."
                    )
                } else {
                    BodyweightProgressionSuggestion(
                        currentStage = current,
                        suggestedStage = BodyweightProgressionStage.ASSISTED,
                        rationale = "Keep reducing assistance (lighter band / less machine help)."
                    )
                }
            }
            BodyweightProgressionStage.BODYWEIGHT -> {
                if (bestBwReps >= bodyweightRepTarget) {
                    BodyweightProgressionSuggestion(
                        currentStage = current,
                        suggestedStage = BodyweightProgressionStage.WEIGHTED,
                        rationale = "Hit the bodyweight rep target; add external load."
                    )
                } else {
                    BodyweightProgressionSuggestion(
                        currentStage = current,
                        suggestedStage = BodyweightProgressionStage.BODYWEIGHT,
                        rationale = "Build bodyweight reps before adding load."
                    )
                }
            }
            BodyweightProgressionStage.WEIGHTED ->
                BodyweightProgressionSuggestion(
                    currentStage = current,
                    suggestedStage = BodyweightProgressionStage.WEIGHTED,
                    rationale = "Progress added load while keeping full range of motion."
                )
        }
    }
}
