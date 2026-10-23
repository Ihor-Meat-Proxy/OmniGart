package com.omnigart.planning

import com.omnigart.content.Discipline
import com.omnigart.content.Level
import com.omnigart.content.TrainingGoal
import com.omnigart.filter.EquipmentProfile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RuleBasedPlanGeneratorTest {
    private fun profile(
        days: Int = 3,
        goal: TrainingGoal = TrainingGoal.HYPERTROPHY,
        readiness: ReadinessBand = ReadinessBand.NORMAL,
        hardWeeks: Int = 1
    ) = UserTrainingProfile(
        ageYears = 30,
        levels = listOf(DisciplineLevel(Discipline.STRENGTH, Level.INTERMEDIATE)),
        goals = setOf(goal),
        availableDaysPerWeek = days,
        sessionMinutes = 60,
        equipment = EquipmentProfile.HOME_GYM,
        readiness = readiness,
        hardWeeksSinceDeload = hardWeeks
    )

    @Test
    fun generatesSessionsMatchingAvailableDays() {
        val plan = RuleBasedPlanGenerator().generate(PlanGenerationInput(profile(days = 4)))
        assertEquals(4, plan.sessions.size)
        assertTrue(plan.explanations.any { it.evidence.refId == "schoenfeld_volume_2016" })
        assertTrue(plan.volumeTargets.isNotEmpty())
    }

    @Test
    fun deloadTriggersAfterHardWeeks() {
        val plan = RuleBasedPlanGenerator().generate(PlanGenerationInput(profile(hardWeeks = 6)))
        assertTrue(plan.isDeload)
        assertTrue(plan.explanations.any { it.code == "deload.active" })
    }

    @Test
    fun readinessLowRaisesRir() {
        val plan = RuleBasedPlanGenerator().generate(
            PlanGenerationInput(profile(readiness = ReadinessBand.LOW, hardWeeks = 1))
        )
        assertTrue(plan.sessions.first().prescriptions.first().rir.contains("3-5"))
    }

    @Test
    fun endurancePlanIncludesPolarizedCaveat() {
        val plan = RuleBasedPlanGenerator().generate(
            PlanGenerationInput(profile(goal = TrainingGoal.ENDURANCE, days = 3))
        )
        assertTrue(plan.explanations.any { it.code == "endurance.polarized" })
        assertTrue(plan.explanations.any { it.code == "endurance.mileage_caveat" })
    }

    @Test
    fun volumeLandmarksScaleByLevel() {
        val beginner = VolumeLandmarks.weeklyHardSetTarget(Level.BEGINNER, TrainingGoal.HYPERTROPHY)
        val advanced = VolumeLandmarks.weeklyHardSetTarget(Level.ADVANCED, TrainingGoal.HYPERTROPHY)
        assertTrue(beginner.last <= advanced.last)
        assertEquals(8..12, beginner)
    }

    @Test
    fun openRouterStubFallsBackWithoutNetwork() {
        val gen = OpenRouterPlanGenerator(networkEnabled = false)
        val plan = gen.generate(PlanGenerationInput(profile()))
        assertEquals("openrouter-stub-v1", plan.generatorId)
        assertTrue(plan.explanations.any { it.code == "ai.disabled" })
        assertFalse(plan.sessions.isEmpty())
    }

    @Test
    fun safetyValidatorClampsExcessSets() {
        val profile = profile()
        val proposal = AiPlanProposal(
            sessions = listOf(
                PlannedSession(
                    dayIndex = 0,
                    title = "Test",
                    focus = "test",
                    prescriptions = listOf(
                        PlannedSetPrescription(
                            "goblet-squat",
                            sets = 20,
                            reps = "10",
                            rir = "1",
                            restSeconds = 60
                        )
                    ),
                    explanations = emptyList()
                )
            ),
            claimedVolume = emptyList()
        )
        val (clamped, notes) = PlanSafetyValidator().validateAndClamp(proposal, profile)
        assertEquals(6, clamped.sessions.first().prescriptions.first().sets)
        assertTrue(notes.any { it.code == "clamp.sets_per_exercise" })
    }
}
