package com.omnigart.planning

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SuggestedSchemeGeneratorTest {
    private val generator = SuggestedSchemeGenerator()

    @Test
    fun fullBodySchemeUsesAvailableDays() {
        val scheme =
            generator.generate(
                SuggestedSchemeRequest(
                    weekStartIso = "2026-10-19",
                    availableDaysPerWeek = 3,
                    primaryGoal = "hypertrophy",
                    split = PlannerSchemeSplit.FULL_BODY,
                    includeRuns = false,
                    includeTreadmill = false,
                    includeZone2 = false
                )
            )
        assertEquals(3, scheme.sessions.size)
        assertTrue(scheme.sessions.all { it.kind == PlannerSessionKind.STRENGTH_FULL_BODY })
        assertEquals("2026-10-19", scheme.sessions.first().dateIso)
        assertEquals("2026-10-21", scheme.sessions.last().dateIso)
    }

    @Test
    fun enduranceBiasPrefersCardio() {
        val scheme =
            generator.generate(
                SuggestedSchemeRequest(
                    weekStartIso = "2026-10-19",
                    availableDaysPerWeek = 4,
                    primaryGoal = "endurance",
                    split = PlannerSchemeSplit.ENDURANCE_BIAS,
                    includeRuns = true,
                    includeZone2 = true
                )
            )
        assertTrue(scheme.sessions.any { it.kind == PlannerSessionKind.ZONE2 || it.kind == PlannerSessionKind.RUN })
        assertTrue(scheme.explanationCodes.contains("scheme.zone2"))
    }

    @Test
    fun upperLowerUsesSplitKinds() {
        val scheme =
            generator.generate(
                SuggestedSchemeRequest(
                    weekStartIso = "2026-10-19",
                    availableDaysPerWeek = 4,
                    primaryGoal = "strength",
                    split = PlannerSchemeSplit.UPPER_LOWER,
                    includeRuns = false,
                    includeZone2 = false
                )
            )
        assertTrue(scheme.sessions.all { it.kind == PlannerSessionKind.STRENGTH_SPLIT })
    }
}
