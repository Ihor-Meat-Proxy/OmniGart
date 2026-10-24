package com.omnigart.shared.planner

import com.omnigart.planning.PlannerIntensity
import com.omnigart.planning.PlannerSchemeSplit
import com.omnigart.planning.PlannerSessionKind
import com.omnigart.shared.data.createInMemoryDatabase
import com.omnigart.shared.health.store.InMemoryLocalHealthStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlannerServiceTest {
    private fun service(): PlannerService {
        val db = createInMemoryDatabase()
        return PlannerService(
            planned = PlannedWorkoutRepository(db),
            localHealth = InMemoryLocalHealthStore()
        )
    }

    @Test
    fun generateSchemePersistsGuidedStrengthLabels() {
        val planner = service()
        val week =
            planner.generateAndPersistScheme(
                "local-profile",
                GenerateSchemeOptions(
                    weekStartIso = "2026-10-19",
                    availableDaysPerWeek = 3,
                    primaryGoal = "HYPERTROPHY",
                    split = PlannerSchemeSplit.FULL_BODY,
                    includeRuns = false,
                    includeZone2 = false,
                    replaceWeek = true
                )
            )
        assertEquals(3, week.size)
        assertTrue(week.all { it.titleKey == "watch_strength_start" })
        assertTrue(week.all { it.kind == PlannerSessionKind.STRENGTH_FULL_BODY })
    }

    @Test
    fun addMoveDeleteStayAppLocal() {
        val planner = service()
        val added =
            planner.addSession(
                profileId = "local-profile",
                dateIso = "2026-10-24",
                kind = PlannerSessionKind.FREE_STRENGTH,
                intensity = PlannerIntensity.MODERATE
            )
        assertEquals("wear_strength_start", added.titleKey)
        val moved = planner.movePlanned(added.id, "2026-10-25")
        assertEquals("2026-10-25", moved?.scheduledDateIso)
        planner.deletePlanned(added.id)
        assertTrue(planner.listPlanned("local-profile").none { it.id == added.id })
    }

    @Test
    fun calendarMarksPlannedDay() {
        val planner = service()
        planner.addSession(
            profileId = "local-profile",
            dateIso = "2026-10-24",
            kind = PlannerSessionKind.FUNCTIONAL,
            intensity = PlannerIntensity.MODERATE
        )
        val snap = planner.calendar("local-profile", "2026-10-24")
        assertTrue(snap.week.any { it.dateIso == "2026-10-24" && it.hasPlanned })
        assertEquals(1, snap.plannedForSelected.size)
        assertEquals("planner_kind_functional", snap.plannedForSelected.first().titleKey)
    }
}
