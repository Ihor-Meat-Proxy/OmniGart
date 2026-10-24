package com.omnigart.shared.strength

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ReadyMadeWorkoutCatalogTest {
    @Test
    fun catalogBundlesExpectedReadyWorkouts() {
        val ids = ReadyMadeWorkoutCatalog.workouts.map { it.id }.toSet()
        assertTrue("ready-bw-full-body-beginner" in ids)
        assertTrue("ready-bw-quick-15" in ids)
        assertTrue("ready-bw-core" in ids)
        assertTrue("ready-home-dumbbell-full" in ids)
        assertTrue("ready-gym-upper-strength" in ids)
        assertEquals(13, ReadyMadeWorkoutCatalog.workouts.size)
    }

    @Test
    fun bodyweightOnlyFiltersToBwAndOptionalBar() {
        val bw =
            UserEquipmentProfile(
                profileId = "p",
                ownedIds = setOf(EquipmentItem.BODYWEIGHT.id),
                activePreset = EquipmentPresetKind.CUSTOM
            )
        val available = ReadyMadeWorkoutCatalog.availableFor(bw)
        assertTrue(available.isNotEmpty())
        assertTrue(available.all { bw.canPerform(it.equipmentRequired) })
        assertTrue(available.none { it.id.startsWith("ready-gym") })
        assertTrue(available.none { it.id == "ready-home-dumbbell-full" })
        assertTrue(available.any { it.id == "ready-bw-full-body-beginner" })
    }

    @Test
    fun materializeUsesBodyMassForBwLoadsAndCanSubstitute() {
        val bw =
            UserEquipmentProfile(
                profileId = "p",
                ownedIds = setOf(EquipmentItem.BODYWEIGHT.id),
                activePreset = EquipmentPresetKind.CUSTOM
            )
        val workout = ReadyMadeWorkoutCatalog.byId("ready-bw-full-body-beginner")
        assertNotNull(workout)
        val session =
            ReadyMadeWorkoutCatalog.materialize(
                workout = workout,
                equipment = bw,
                bodyMassKg = 80.0,
                maxLevel = "beginner"
            )
        assertTrue(session.slots.isNotEmpty())
        assertTrue(session.workedMuscles.isNotEmpty())
        val loaded = session.slots.filter { it.loadIncludesBodyMass }
        assertTrue(loaded.isNotEmpty())
        assertTrue(loaded.all { (it.suggestedLoadKg ?: 0.0) > 0.0 })
    }

    @Test
    fun adjustWithoutBarSwapsPullUpFocusedWorkout() {
        val withBar =
            UserEquipmentProfile(
                profileId = "p",
                ownedIds = setOf(EquipmentItem.BODYWEIGHT.id, EquipmentItem.PULL_UP_BAR.id),
                activePreset = EquipmentPresetKind.CUSTOM
            )
        val withoutBar =
            UserEquipmentProfile(
                profileId = "p",
                ownedIds = setOf(EquipmentItem.BODYWEIGHT.id),
                activePreset = EquipmentPresetKind.CUSTOM
            )
        val workout = ReadyMadeWorkoutCatalog.byId("ready-bw-bar-pull-dip")
        assertNotNull(workout)
        assertTrue(withBar.canPerform(workout.equipmentRequired))
        assertTrue(!withoutBar.canPerform(workout.equipmentRequired))
        // When listed as available under with-bar kit, materialising under bodyweight-only still
        // substitutes preferred pull-up/dip slots via regressions / pattern match.
        val session =
            ReadyMadeWorkoutCatalog.materialize(
                workout = workout,
                equipment = withoutBar,
                bodyMassKg = 70.0,
                maxLevel = "intermediate"
            )
        assertTrue(session.slots.isNotEmpty())
        assertTrue(session.slots.none { it.exerciseId == "pull-up" })
    }
}
