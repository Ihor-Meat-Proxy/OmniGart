package com.omnigart.planning

/**
 * Rule-based, fully on-device adaptive planner.
 *
 * Suggests moving, swapping (hard → easy / Zone 2 / rest), or skipping planned
 * sessions using recent workouts, acute:chronic load, readiness (HRV / RHR / sleep),
 * and muscle-group recovery. Wellness wording only — never deletes Health data.
 */
class AdaptiveSuggestionEngine(private val elevatedLoadThreshold: Double = 1.3, private val maxSuggestions: Int = 5) {
    fun suggest(context: AdaptiveSuggestionContext): AdaptiveSuggestionResult {
        val upcoming =
            context.planned
                .filter { it.dateIso >= context.todayIso }
                .sortedBy { it.dateIso }
        if (upcoming.isEmpty()) {
            return AdaptiveSuggestionResult(emptyList())
        }

        val unrecovered = MuscleRecoveryHeuristic.unrecoveredMuscles(context.todayIso, context.completedRecent)
        val out = mutableListOf<AdaptiveSuggestion>()
        val seenTargets = mutableSetOf<String>()

        fun emit(suggestion: AdaptiveSuggestion) {
            if (suggestion.id in context.dismissedSuggestionIds) return
            if (suggestion.targetSessionId in seenTargets) return
            if (out.size >= maxSuggestions) return
            seenTargets += suggestion.targetSessionId
            out += suggestion
        }

        // 1) Readiness REST / LOW → ease or skip nearest hard session.
        if (context.readiness == PlannerReadinessBand.REST ||
            context.readiness == PlannerReadinessBand.LOW
        ) {
            upcoming.firstOrNull { it.intensity == PlannerIntensity.HARD }?.let { hard ->
                val reason =
                    when (context.readiness) {
                        PlannerReadinessBand.REST -> "planner_suggest_reason_readiness_rest"
                        else -> "planner_suggest_reason_readiness_low"
                    }
                if (context.readiness == PlannerReadinessBand.REST) {
                    emit(
                        AdaptiveSuggestion(
                            id = "skip-readiness-${hard.id}",
                            action = AdaptiveSuggestionAction.SKIP,
                            targetSessionId = hard.id,
                            proposedDateIso = null,
                            proposedKind = PlannerSessionKind.REST,
                            proposedIntensity = PlannerIntensity.EASY,
                            reasonKey = reason
                        )
                    )
                } else {
                    emit(
                        AdaptiveSuggestion(
                            id = "swap-readiness-${hard.id}",
                            action = AdaptiveSuggestionAction.SWAP,
                            targetSessionId = hard.id,
                            proposedDateIso = null,
                            proposedKind = PlannerSessionKind.ZONE2,
                            proposedIntensity = PlannerIntensity.EASY,
                            reasonKey = reason
                        )
                    )
                }
            }
        }

        // 2) Elevated acute:chronic → move or ease a hard day.
        val ratio = context.acuteChronicRatio
        if (ratio != null && ratio > elevatedLoadThreshold) {
            upcoming.firstOrNull { it.intensity == PlannerIntensity.HARD }?.let { hard ->
                val tomorrow = IsoDate.parse(hard.dateIso).plusDays(1).toIso()
                emit(
                    AdaptiveSuggestion(
                        id = "move-load-${hard.id}",
                        action = AdaptiveSuggestionAction.MOVE,
                        targetSessionId = hard.id,
                        proposedDateIso = tomorrow,
                        proposedKind = null,
                        proposedIntensity = null,
                        reasonKey = "planner_suggest_reason_load_elevated"
                    )
                )
            }
            upcoming
                .firstOrNull {
                    it.intensity != PlannerIntensity.EASY &&
                        it.kind != PlannerSessionKind.ZONE2 &&
                        it.kind != PlannerSessionKind.REST
                }?.let { session ->
                    emit(
                        AdaptiveSuggestion(
                            id = "swap-load-${session.id}",
                            action = AdaptiveSuggestionAction.SWAP,
                            targetSessionId = session.id,
                            proposedDateIso = null,
                            proposedKind = PlannerSessionKind.ZONE2,
                            proposedIntensity = PlannerIntensity.EASY,
                            reasonKey = "planner_suggest_reason_load_elevated"
                        )
                    )
                }
        }

        // 3) HRV / RHR / sleep signals reinforce an easy swap when readiness is unknown.
        if (context.readiness == PlannerReadinessBand.UNKNOWN ||
            context.readiness == PlannerReadinessBand.NORMAL
        ) {
            val stressed =
                (context.hrvZScore != null && context.hrvZScore < -0.5) ||
                    (context.restingHrDeltaBpm != null && context.restingHrDeltaBpm >= 5.0) ||
                    (context.sleepRatio != null && context.sleepRatio < 0.85)
            if (stressed) {
                upcoming.firstOrNull { it.intensity == PlannerIntensity.HARD }?.let { hard ->
                    emit(
                        AdaptiveSuggestion(
                            id = "swap-signals-${hard.id}",
                            action = AdaptiveSuggestionAction.SWAP,
                            targetSessionId = hard.id,
                            proposedDateIso = null,
                            proposedKind = PlannerSessionKind.ZONE2,
                            proposedIntensity = PlannerIntensity.EASY,
                            reasonKey = "planner_suggest_reason_recovery_signals"
                        )
                    )
                }
            }
        }

        // 4) Hard session completed yesterday → ease today's hard plan (before broader muscle moves).
        val yesterday = IsoDate.parse(context.todayIso).plusDays(-1).toIso()
        val hardYesterday =
            context.completedRecent.any {
                it.dateIso == yesterday && it.intensity == PlannerIntensity.HARD
            }
        if (hardYesterday) {
            upcoming
                .firstOrNull { it.dateIso == context.todayIso && it.intensity == PlannerIntensity.HARD }
                ?.let { todayHard ->
                    emit(
                        AdaptiveSuggestion(
                            id = "swap-backtoback-${todayHard.id}",
                            action = AdaptiveSuggestionAction.SWAP,
                            targetSessionId = todayHard.id,
                            proposedDateIso = null,
                            proposedKind = PlannerSessionKind.ZONE2,
                            proposedIntensity = PlannerIntensity.EASY,
                            reasonKey = "planner_suggest_reason_back_to_back"
                        )
                    )
                }
        }

        // 5) Muscle-group recovery: move strength that hits unrecovered muscles.
        if (unrecovered.isNotEmpty()) {
            for (session in upcoming) {
                if (session.kind != PlannerSessionKind.STRENGTH_FULL_BODY &&
                    session.kind != PlannerSessionKind.STRENGTH_SPLIT &&
                    session.kind != PlannerSessionKind.GUIDED_STRENGTH &&
                    session.kind != PlannerSessionKind.FREE_STRENGTH &&
                    session.kind != PlannerSessionKind.FUNCTIONAL
                ) {
                    continue
                }
                val overlap =
                    session.muscleGroups.map { it.lowercase() }.intersect(unrecovered)
                if (overlap.isEmpty()) continue
                val later = IsoDate.parse(session.dateIso).plusDays(1).toIso()
                emit(
                    AdaptiveSuggestion(
                        id = "move-muscle-${session.id}",
                        action = AdaptiveSuggestionAction.MOVE,
                        targetSessionId = session.id,
                        proposedDateIso = later,
                        proposedKind = null,
                        proposedIntensity = null,
                        reasonKey = "planner_suggest_reason_muscle_recovery",
                        reasonDetail = overlap.first()
                    )
                )
            }
        }

        return AdaptiveSuggestionResult(out)
    }
}
