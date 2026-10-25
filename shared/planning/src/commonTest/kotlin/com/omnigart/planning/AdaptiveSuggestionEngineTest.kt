package com.omnigart.planning

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AdaptiveSuggestionEngineTest {
    private val engine = AdaptiveSuggestionEngine()

    private fun planned(
        id: String = "p1",
        date: String = "2026-10-24",
        kind: PlannerSessionKind = PlannerSessionKind.STRENGTH_FULL_BODY,
        intensity: PlannerIntensity = PlannerIntensity.HARD,
        muscles: List<String> = listOf("quads", "chest")
    ) = PlannedSessionSnapshot(id, date, kind, intensity, muscles, "watch_strength_start")

    private fun completed(
        date: String,
        intensity: PlannerIntensity = PlannerIntensity.HARD,
        muscles: List<String> = listOf("quads"),
        load: Double = 80.0
    ) = CompletedSessionSnapshot(date, PlannerSessionKind.STRENGTH_FULL_BODY, intensity, muscles, load)

    @Test
    fun readinessRestSuggestsSkipHard() {
        val result =
            engine.suggest(
                AdaptiveSuggestionContext(
                    todayIso = "2026-10-24",
                    planned = listOf(planned()),
                    completedRecent = emptyList(),
                    acuteChronicRatio = 1.0,
                    readiness = PlannerReadinessBand.REST
                )
            )
        assertTrue(result.suggestions.any { it.action == AdaptiveSuggestionAction.SKIP })
        assertEquals("planner_suggest_reason_readiness_rest", result.suggestions.first().reasonKey)
    }

    @Test
    fun readinessLowSuggestsSwapToZone2() {
        val result =
            engine.suggest(
                AdaptiveSuggestionContext(
                    todayIso = "2026-10-24",
                    planned = listOf(planned()),
                    completedRecent = emptyList(),
                    acuteChronicRatio = 1.0,
                    readiness = PlannerReadinessBand.LOW
                )
            )
        val swap = result.suggestions.first { it.action == AdaptiveSuggestionAction.SWAP }
        assertEquals(PlannerSessionKind.ZONE2, swap.proposedKind)
        assertEquals(PlannerIntensity.EASY, swap.proposedIntensity)
    }

    @Test
    fun elevatedAcuteChronicSuggestsMove() {
        val result =
            engine.suggest(
                AdaptiveSuggestionContext(
                    todayIso = "2026-10-24",
                    planned = listOf(planned()),
                    completedRecent = emptyList(),
                    acuteChronicRatio = 1.5,
                    readiness = PlannerReadinessBand.NORMAL
                )
            )
        assertTrue(result.suggestions.any { it.action == AdaptiveSuggestionAction.MOVE })
        assertTrue(result.suggestions.any { it.reasonKey == "planner_suggest_reason_load_elevated" })
    }

    @Test
    fun muscleRecoverySuggestsMove() {
        val result =
            engine.suggest(
                AdaptiveSuggestionContext(
                    todayIso = "2026-10-24",
                    // Day after tomorrow so back-to-back (today) does not claim the target first.
                    planned = listOf(planned(date = "2026-10-25", muscles = listOf("quads", "chest"))),
                    completedRecent = listOf(completed(date = "2026-10-23", muscles = listOf("quads"))),
                    acuteChronicRatio = 1.0,
                    readiness = PlannerReadinessBand.HIGH
                )
            )
        assertTrue(
            result.suggestions.any {
                it.action == AdaptiveSuggestionAction.MOVE &&
                    it.reasonKey == "planner_suggest_reason_muscle_recovery"
            }
        )
    }

    @Test
    fun recoverySignalsSuggestEasySwap() {
        val result =
            engine.suggest(
                AdaptiveSuggestionContext(
                    todayIso = "2026-10-24",
                    planned = listOf(planned()),
                    completedRecent = emptyList(),
                    acuteChronicRatio = 1.0,
                    readiness = PlannerReadinessBand.NORMAL,
                    hrvZScore = -1.0,
                    restingHrDeltaBpm = 6.0,
                    sleepRatio = 0.7
                )
            )
        assertTrue(
            result.suggestions.any {
                it.action == AdaptiveSuggestionAction.SWAP &&
                    it.reasonKey == "planner_suggest_reason_recovery_signals"
            }
        )
    }

    @Test
    fun backToBackHardSuggestsEaseToday() {
        val result =
            engine.suggest(
                AdaptiveSuggestionContext(
                    todayIso = "2026-10-24",
                    planned = listOf(planned(date = "2026-10-24")),
                    completedRecent = listOf(completed(date = "2026-10-23")),
                    acuteChronicRatio = 1.0,
                    readiness = PlannerReadinessBand.HIGH
                )
            )
        assertTrue(
            result.suggestions.any {
                it.action == AdaptiveSuggestionAction.SWAP &&
                    it.reasonKey == "planner_suggest_reason_back_to_back"
            }
        )
    }

    @Test
    fun dismissedIdsAreNotReEmitted() {
        val hard = planned()
        val result =
            engine.suggest(
                AdaptiveSuggestionContext(
                    todayIso = "2026-10-24",
                    planned = listOf(hard),
                    completedRecent = emptyList(),
                    acuteChronicRatio = 1.0,
                    readiness = PlannerReadinessBand.REST,
                    dismissedSuggestionIds = setOf("skip-readiness-${hard.id}")
                )
            )
        assertTrue(result.suggestions.none { it.id == "skip-readiness-${hard.id}" })
    }

    @Test
    fun easySessionsAreNotPrimaryTargetsForLoadMove() {
        val result =
            engine.suggest(
                AdaptiveSuggestionContext(
                    todayIso = "2026-10-24",
                    planned =
                    listOf(
                        planned(
                            id = "easy",
                            kind = PlannerSessionKind.ZONE2,
                            intensity = PlannerIntensity.EASY,
                            muscles = emptyList()
                        )
                    ),
                    completedRecent = emptyList(),
                    acuteChronicRatio = 1.6,
                    readiness = PlannerReadinessBand.NORMAL
                )
            )
        assertTrue(result.suggestions.none { it.action == AdaptiveSuggestionAction.MOVE })
    }
}
