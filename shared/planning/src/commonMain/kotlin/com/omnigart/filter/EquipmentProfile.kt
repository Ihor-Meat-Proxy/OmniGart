package com.omnigart.filter

import com.omnigart.content.Equipment
import com.omnigart.content.EquipmentPresetId

/**
 * Durable "my equipment" profile used to filter the library and templates.
 */
data class EquipmentProfile(val preset: EquipmentPresetId, val owned: Set<Equipment>, val label: String) {
    fun canPerform(required: Collection<Equipment>): Boolean {
        if (required.isEmpty()) return true
        val effective = expand(owned) + Equipment.NONE
        // bodyweight and none are always implicitly available
        val always = setOf(Equipment.BODYWEIGHT, Equipment.NONE)
        return required.all { it in effective || it in always }
    }

    private fun expand(owned: Set<Equipment>): Set<Equipment> {
        val out = owned.toMutableSet()
        if (Equipment.BENCH_FLAT in owned || Equipment.BENCH_ADJUSTABLE in owned) {
            out += Equipment.BENCH
        }
        if (Equipment.MACHINE in owned) {
            out +=
                setOf(
                    Equipment.SMITH_MACHINE,
                    Equipment.LEG_PRESS,
                    Equipment.LAT_PULLDOWN,
                    Equipment.ASSISTED_PULLUP_DIP_MACHINE
                )
        }
        if (
            Equipment.SMITH_MACHINE in owned ||
            Equipment.LEG_PRESS in owned ||
            Equipment.LAT_PULLDOWN in owned ||
            Equipment.ASSISTED_PULLUP_DIP_MACHINE in owned
        ) {
            out += Equipment.MACHINE
        }
        if (Equipment.CABLE in owned) {
            out += Equipment.LAT_PULLDOWN
        }
        return out
    }

    companion object {
        val HOME_GYM = EquipmentProfile(
            preset = EquipmentPresetId.HOME_GYM,
            label = "Home",
            owned = setOf(
                Equipment.DUMBBELL,
                Equipment.KETTLEBELL,
                Equipment.BARBELL,
                Equipment.PLATES,
                Equipment.BENCH,
                Equipment.BENCH_FLAT,
                Equipment.BENCH_ADJUSTABLE,
                Equipment.BANDS,
                Equipment.PULL_UP_BAR,
                Equipment.DIP_BARS,
                Equipment.SQUAT_RACK,
                Equipment.YOGA_MAT,
                Equipment.BODYWEIGHT,
                Equipment.NONE
            )
        )

        val COMMERCIAL_GYM = EquipmentProfile(
            preset = EquipmentPresetId.COMMERCIAL_GYM,
            label = "Gym",
            owned = Equipment.entries.toSet()
        )

        val TRAVEL = EquipmentProfile(
            preset = EquipmentPresetId.TRAVEL,
            label = "Travel",
            owned = setOf(
                Equipment.BANDS,
                Equipment.BODYWEIGHT,
                Equipment.YOGA_MAT,
                Equipment.SUSPENSION,
                Equipment.NONE
            )
        )

        val BODYWEIGHT = EquipmentProfile(
            preset = EquipmentPresetId.BODYWEIGHT,
            label = "Bodyweight only",
            owned = setOf(
                Equipment.BODYWEIGHT,
                Equipment.NONE,
                Equipment.YOGA_MAT,
                Equipment.PULL_UP_BAR
            )
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
                Equipment.NONE
            )
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
