package com.omnigart.filter

import com.omnigart.content.Equipment
import com.omnigart.content.EquipmentPresetId

/**
 * Durable "my equipment" profile used to filter the library and templates.
 */
data class EquipmentProfile(
    val preset: EquipmentPresetId,
    val owned: Set<Equipment>,
    val label: String,
) {
    fun canPerform(required: Collection<Equipment>): Boolean {
        if (required.isEmpty()) return true
        val effective = owned + Equipment.NONE
        // bodyweight and none are always implicitly available
        val always = setOf(Equipment.BODYWEIGHT, Equipment.NONE)
        return required.all { it in effective || it in always }
    }

    companion object {
        val HOME_GYM = EquipmentProfile(
            preset = EquipmentPresetId.HOME_GYM,
            label = "Home gym",
            owned = setOf(
                Equipment.DUMBBELL,
                Equipment.KETTLEBELL,
                Equipment.BARBELL,
                Equipment.BENCH,
                Equipment.BANDS,
                Equipment.PULL_UP_BAR,
                Equipment.YOGA_MAT,
                Equipment.BODYWEIGHT,
                Equipment.NONE,
            ),
        )

        val COMMERCIAL_GYM = EquipmentProfile(
            preset = EquipmentPresetId.COMMERCIAL_GYM,
            label = "Commercial gym",
            owned = Equipment.entries.toSet(),
        )

        val TRAVEL = EquipmentProfile(
            preset = EquipmentPresetId.TRAVEL,
            label = "Travel",
            owned = setOf(
                Equipment.BANDS,
                Equipment.BODYWEIGHT,
                Equipment.YOGA_MAT,
                Equipment.NONE,
            ),
        )

        val BODYWEIGHT = EquipmentProfile(
            preset = EquipmentPresetId.BODYWEIGHT,
            label = "Bodyweight only",
            owned = setOf(Equipment.BODYWEIGHT, Equipment.NONE, Equipment.YOGA_MAT, Equipment.PULL_UP_BAR),
        )

        val POOL = EquipmentProfile(
            preset = EquipmentPresetId.POOL,
            label = "Pool focus",
            owned = setOf(
                Equipment.POOL,
                Equipment.KICKBOARD,
                Equipment.PULL_BUOY,
                Equipment.FINS,
                Equipment.PADDLE,
                Equipment.BODYWEIGHT,
                Equipment.NONE,
            ),
        )

        fun preset(id: EquipmentPresetId): EquipmentProfile = when (id) {
            EquipmentPresetId.HOME_GYM -> HOME_GYM
            EquipmentPresetId.COMMERCIAL_GYM -> COMMERCIAL_GYM
            EquipmentPresetId.TRAVEL -> TRAVEL
            EquipmentPresetId.BODYWEIGHT -> BODYWEIGHT
            EquipmentPresetId.POOL -> POOL
            EquipmentPresetId.CUSTOM -> HOME_GYM
        }
    }
}
