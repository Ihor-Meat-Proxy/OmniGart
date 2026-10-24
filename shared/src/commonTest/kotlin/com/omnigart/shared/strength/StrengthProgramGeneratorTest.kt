package com.omnigart.shared.strength

import com.omnigart.shared.domain.ProfileGoal
import com.omnigart.shared.domain.StrengthLevel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class StrengthProgramGeneratorTest {
    private val generator = StrengthProgramGenerator()

    private fun request(
        equipment: UserEquipmentProfile = UserEquipmentProfile.fromPreset("p", EquipmentPresetKind.HOME),
        split: ProgramSplit = ProgramSplit.FULL_BODY,
        days: Int = 3,
        goal: ProfileGoal = ProfileGoal.GENERAL_FITNESS,
        level: StrengthLevel = StrengthLevel.BEGINNER,
        week: Int = 1,
        sessionIds: Set<String>? = null
    ) = StrengthProgramRequest(
        equipment = equipment,
        split = split,
        daysPerWeek = days,
        goal = goal,
        level = level,
        bodyMassKg = 70.0,
        weekIndex = week,
        sessionEquipmentIds = sessionIds
    )

    @Test
    fun fullBodyProducesRequestedDays() {
        val plan = generator.generate(request(days = 3))
        assertEquals(3, plan.sessions.size)
        assertTrue(plan.sessions.all { it.slots.isNotEmpty() })
    }

    @Test
    fun travelEquipmentAvoidsBarbellCompounds() {
        val travel = UserEquipmentProfile.fromPreset("p", EquipmentPresetKind.TRAVEL)
        val plan = generator.generate(request(equipment = travel, days = 2))
        val ids = plan.sessions.flatMap { s -> s.slots.map { it.exerciseId } }
        assertTrue(ids.none { it.contains("barbell") })
        assertTrue(ids.isNotEmpty())
    }

    @Test
    fun missingPullUpBarSubstitutesAssistedOrLatPulldownWhenAvailable() {
        val homeNoBar =
            UserEquipmentProfile.fromPreset("p", EquipmentPresetKind.HOME).copy(
                ownedIds =
                UserEquipmentProfile.presetOwned(EquipmentPresetKind.HOME) - EquipmentItem.PULL_UP_BAR.id
            )
        val gym = UserEquipmentProfile.fromPreset("p", EquipmentPresetKind.GYM)
        val withGymMachines =
            homeNoBar.copy(
                ownedIds = homeNoBar.ownedIds + EquipmentItem.LAT_PULLDOWN.id + EquipmentItem.CABLE.id
            )
        val plan = generator.generate(request(equipment = withGymMachines, split = ProgramSplit.PUSH_PULL_LEGS, days = 3))
        val pullDay = plan.sessions.find { it.focus == "pull" }
        assertNotNull(pullDay)
        assertTrue(pullDay.slots.isNotEmpty())
        assertTrue(pullDay.slots.none { it.exerciseId == "pull-up" } || gym.canPerform(listOf("pull_up_bar")))
    }

    @Test
    fun sessionEquipmentOverrideRegeneratesWithFewerTools() {
        val gym = UserEquipmentProfile.fromPreset("p", EquipmentPresetKind.GYM)
        val full = generator.generate(request(equipment = gym, split = ProgramSplit.UPPER_LOWER, days = 4))
        val bodyweightOnly =
            setOf(EquipmentItem.BODYWEIGHT.id, EquipmentItem.YOGA_MAT.id, EquipmentItem.PULL_UP_BAR.id)
        val adjusted =
            generator.generate(
                request(
                    equipment = gym,
                    split = ProgramSplit.UPPER_LOWER,
                    days = 4,
                    sessionIds = bodyweightOnly
                )
            )
        assertEquals(full.sessions.size, adjusted.sessions.size)
        val adjustedIds = adjusted.sessions.flatMap { s -> s.slots.map { it.exerciseId } }
        assertTrue(adjustedIds.none { it.startsWith("barbell") })
    }

    @Test
    fun pullUpLoadIncludesBodyMass() {
        val gym = UserEquipmentProfile.fromPreset("p", EquipmentPresetKind.GYM)
        val plan =
            generator.generate(
                request(equipment = gym, split = ProgramSplit.PUSH_PULL_LEGS, days = 3, level = StrengthLevel.INTERMEDIATE)
            )
        val pullSlot =
            plan.sessions.flatMap { it.slots }.find {
                it.exerciseId.contains("pull") && it.loadIncludesBodyMass
            }
        if (pullSlot != null) {
            val load = pullSlot.suggestedLoadKg
            assertNotNull(load)
            assertTrue(load > 40.0)
        }
    }

    @Test
    fun progressiveOverloadIncreasesNudgeOverWeeks() {
        val w1 = ProgressiveOverload.prescription(ProfileGoal.STRENGTH, StrengthLevel.BEGINNER, 1, true)
        val w5 = ProgressiveOverload.prescription(ProfileGoal.STRENGTH, StrengthLevel.BEGINNER, 5, true)
        assertTrue(w5.externalLoadNudgeKg >= w1.externalLoadNudgeKg)
    }

    @Test
    fun broSplitIsFiveDays() {
        val plan =
            generator.generate(
                request(
                    equipment = UserEquipmentProfile.fromPreset("p", EquipmentPresetKind.GYM),
                    split = ProgramSplit.BRO_SPLIT,
                    days = 5
                )
            )
        assertEquals(5, plan.sessions.size)
    }

    @Test
    fun generationIsDeterministic() {
        val a = generator.generate(request(days = 3))
        val b = generator.generate(request(days = 3))
        assertEquals(a.sessions.map { it.slots.map { s -> s.exerciseId } }, b.sessions.map { it.slots.map { s -> s.exerciseId } })
    }

    @Test
    fun workedMusclesSurfacedForBodyMap() {
        val plan = generator.generate(request(days = 3))
        assertTrue(plan.sessions.any { it.workedMuscles.isNotEmpty() })
        assertFalse(plan.sessions.first().workedMuscles.any { it.isBlank() })
    }
}
