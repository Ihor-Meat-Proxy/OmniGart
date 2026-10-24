package com.omnigart.shared.strength

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RoutineLibraryCatalogTest {
    @Test
    fun equipmentCatalogueHasAtLeast150SelectableItems() {
        assertTrue(EquipmentDefinitions.selectable().size >= 150)
        assertTrue(EquipmentDefinitions.all.all { it.illustrationKey == "equip_${it.id}" })
    }

    @Test
    fun programmesCoverAllSplitsLevelsAndVenues() {
        val plans = RoutineLibraryCatalog.programmes
        assertEquals(18, plans.size)
        for (split in ProgrammeSplitKind.entries) {
            for (level in ProgrammeLevel.entries) {
                for (venue in ProgrammeVenue.entries) {
                    assertNotNull(
                        plans.find { it.split == split && it.level == level && it.venue == venue },
                        "Missing $split / $level / $venue"
                    )
                }
            }
        }
    }

    @Test
    fun everyRoutineCategoryHasMultipleRoutines() {
        for (category in RoutineCategory.entries) {
            val routines = RoutineLibraryCatalog.routinesIn(category)
            assertTrue(routines.size >= 2, "$category has ${routines.size}")
        }
    }

    @Test
    fun routineExercisesExistInBundledLibrary() {
        val ids = BundledStrengthLibrary.exercises.map { it.id }.toSet()
        for (routine in RoutineLibraryCatalog.routines) {
            for (ex in routine.exercises) {
                assertTrue(ex.exerciseId in ids, "${ex.exerciseId} missing for ${routine.id}")
                assertTrue(ex.illustrationKey.startsWith("ex_"))
                assertTrue(ex.primaryMuscles.isNotEmpty())
            }
        }
    }

    @Test
    fun browsePresetsAreWellnessNamed() {
        val names = EquipmentBrowsePreset.entries.map { it.name.lowercase() }
        assertTrue(names.none { it.contains("trx") })
        assertEquals(7, EquipmentBrowsePreset.entries.size)
    }
}
