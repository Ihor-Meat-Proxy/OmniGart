package com.omnigart.shared.strength

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EquipmentCatalogueTest {
    @Test
    fun homePresetCanPerformDumbbellBench() {
        val home = UserEquipmentProfile.fromPreset("p", EquipmentPresetKind.HOME)
        assertTrue(home.canPerform(listOf("dumbbell", "bench_flat")))
        assertTrue(home.canPerform(listOf("dumbbell", "bench")))
    }

    @Test
    fun travelCannotPerformBarbellSquat() {
        val travel = UserEquipmentProfile.fromPreset("p", EquipmentPresetKind.TRAVEL)
        assertFalse(travel.canPerform(listOf("barbell", "squat_rack", "plates")))
    }

    @Test
    fun specificMachineImpliesGenericMachineTag() {
        val profile =
            UserEquipmentProfile(
                profileId = "p",
                ownedIds = setOf(EquipmentItem.LEG_PRESS.id, EquipmentItem.BODYWEIGHT.id)
            )
        assertTrue(profile.canPerform(listOf("machine")))
        assertTrue(profile.canPerform(listOf("leg_press")))
    }

    @Test
    fun catalogueCoversRequestedStrengthGear() {
        val ids = EquipmentItem.entries.map { it.id }.toSet()
        listOf(
            "bodyweight",
            "pull_up_bar",
            "dip_bars",
            "bands",
            "dumbbell",
            "kettlebell",
            "barbell",
            "plates",
            "bench_flat",
            "bench_adjustable",
            "squat_rack",
            "cable",
            "smith_machine",
            "leg_press",
            "lat_pulldown",
            "assisted_pullup_dip_machine"
        ).forEach { assertTrue(it in ids, "missing $it") }
    }
}
