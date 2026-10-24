package com.omnigart.shared.strength

import com.omnigart.shared.data.createInMemoryDatabase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EquipmentProfileServiceTest {
    @Test
    fun persistsEquipmentAndProgramPrefs() {
        val db = createInMemoryDatabase()
        val service = EquipmentProfileService(db)
        val saved =
            service.saveEquipment(
                UserEquipmentProfile.fromPreset("local-profile", EquipmentPresetKind.TRAVEL)
            )
        assertEquals(EquipmentPresetKind.TRAVEL, saved.activePreset)
        assertTrue(EquipmentItem.BANDS.id in saved.ownedIds)

        val toggled = service.toggleEquipment("local-profile", EquipmentItem.DUMBBELL.id, true)
        assertTrue(EquipmentItem.DUMBBELL.id in toggled.ownedIds)
        assertEquals(EquipmentPresetKind.CUSTOM, toggled.activePreset)

        val ranged = service.setDumbbellRange("local-profile", 5.0, 32.0)
        assertEquals(5.0, ranged.dumbbellMinKg)
        assertEquals(32.0, ranged.dumbbellMaxKg)

        val prefs =
            service.saveProgramPrefs(
                StrengthProgramPreferences(
                    profileId = "local-profile",
                    split = ProgramSplit.PUSH_PULL_LEGS,
                    daysPerWeek = 6,
                    weekIndex = 2
                )
            )
        assertEquals(ProgramSplit.PUSH_PULL_LEGS, prefs.split)
        assertEquals(6, prefs.daysPerWeek)
        assertEquals(2, prefs.weekIndex)
    }

    @Test
    fun namedPresetRoundTrip() {
        val db = createInMemoryDatabase()
        val service = EquipmentProfileService(db)
        service.applyPreset("local-profile", EquipmentPresetKind.HOME)
        service.toggleEquipment("local-profile", EquipmentItem.CABLE.id, true)
        val withPreset = service.saveNamedPreset("local-profile", "Home", EquipmentPresetKind.HOME)
        assertTrue(withPreset.savedPresets.any { it.kind == EquipmentPresetKind.HOME && EquipmentItem.CABLE.id in it.ownedIds })
    }
}
