package com.omnigart.shared.strength

/**
 * Selectable equipment for the local profile and session override.
 * Ids align with content exercise equipment tags (plus catalogue-only aliases).
 */
enum class EquipmentItem(val id: String, val category: EquipmentCategory) {
    BODYWEIGHT("bodyweight", EquipmentCategory.ALWAYS),
    PULL_UP_BAR("pull_up_bar", EquipmentCategory.CALISTHENICS),
    DIP_BARS("dip_bars", EquipmentCategory.CALISTHENICS),
    BANDS("bands", EquipmentCategory.PORTABLE),
    DUMBBELL("dumbbell", EquipmentCategory.FREE_WEIGHTS),
    KETTLEBELL("kettlebell", EquipmentCategory.FREE_WEIGHTS),
    BARBELL("barbell", EquipmentCategory.FREE_WEIGHTS),
    PLATES("plates", EquipmentCategory.FREE_WEIGHTS),
    BENCH_FLAT("bench_flat", EquipmentCategory.BENCH),
    BENCH_ADJUSTABLE("bench_adjustable", EquipmentCategory.BENCH),
    SQUAT_RACK("squat_rack", EquipmentCategory.RACK),
    CABLE("cable", EquipmentCategory.MACHINES),
    SMITH_MACHINE("smith_machine", EquipmentCategory.MACHINES),
    LEG_PRESS("leg_press", EquipmentCategory.MACHINES),
    LAT_PULLDOWN("lat_pulldown", EquipmentCategory.MACHINES),
    ASSISTED_PULLUP_DIP_MACHINE("assisted_pullup_dip_machine", EquipmentCategory.MACHINES),
    MACHINE("machine", EquipmentCategory.MACHINES),
    TRAP_BAR("trap_bar", EquipmentCategory.FREE_WEIGHTS),
    EZ_BAR("ez_bar", EquipmentCategory.FREE_WEIGHTS),
    SUSPENSION("suspension", EquipmentCategory.PORTABLE),
    BOX("box", EquipmentCategory.ACCESSORIES),
    LANDMINE("landmine", EquipmentCategory.ACCESSORIES),
    YOGA_MAT("yoga_mat", EquipmentCategory.ACCESSORIES);

    companion object {
        private val byId = entries.associateBy { it.id }

        fun fromId(id: String): EquipmentItem? = byId[id]

        /** Catalogue shown in equipment pickers (excludes always-on bodyweight). */
        fun selectable(): List<EquipmentItem> = entries.filter { it != BODYWEIGHT }
    }
}

enum class EquipmentCategory {
    ALWAYS,
    CALISTHENICS,
    PORTABLE,
    FREE_WEIGHTS,
    BENCH,
    RACK,
    MACHINES,
    ACCESSORIES
}

enum class EquipmentPresetKind {
    HOME,
    GYM,
    TRAVEL,
    CUSTOM
}

/**
 * Persisted equipment profile for a local user.
 *
 * [dumbbellMinKg] / [dumbbellMaxKg] describe an adjustable dumbbell range when
 * [EquipmentItem.DUMBBELL] is owned; unused otherwise.
 */
data class UserEquipmentProfile(
    val profileId: String,
    val ownedIds: Set<String>,
    val activePreset: EquipmentPresetKind = EquipmentPresetKind.CUSTOM,
    val dumbbellMinKg: Double? = null,
    val dumbbellMaxKg: Double? = null,
    /** Named presets the user saved (Home / Gym / Travel / custom labels). */
    val savedPresets: List<SavedEquipmentPreset> = emptyList()
) {
    fun withSessionOverride(sessionOwnedIds: Set<String>): UserEquipmentProfile =
        copy(ownedIds = sessionOwnedIds, activePreset = EquipmentPresetKind.CUSTOM)

    fun effectiveOwned(): Set<String> = ownedIds + EquipmentItem.BODYWEIGHT.id + "none"

    fun canPerform(required: Collection<String>): Boolean {
        if (required.isEmpty()) return true
        val owned = effectiveOwned()
        val expanded = expandCapabilities(owned)
        return required.all { need ->
            need in expanded || need == EquipmentItem.BODYWEIGHT.id || need == "none"
        }
    }

    companion object {
        fun defaultFor(profileId: String): UserEquipmentProfile = fromPreset(profileId, EquipmentPresetKind.HOME)

        fun fromPreset(profileId: String, kind: EquipmentPresetKind): UserEquipmentProfile {
            val owned = presetOwned(kind)
            return UserEquipmentProfile(
                profileId = profileId,
                ownedIds = owned,
                activePreset = kind,
                dumbbellMinKg = if (EquipmentItem.DUMBBELL.id in owned) 2.0 else null,
                dumbbellMaxKg = if (EquipmentItem.DUMBBELL.id in owned) 24.0 else null,
                savedPresets =
                listOf(
                    SavedEquipmentPreset("Home", EquipmentPresetKind.HOME, presetOwned(EquipmentPresetKind.HOME)),
                    SavedEquipmentPreset("Gym", EquipmentPresetKind.GYM, presetOwned(EquipmentPresetKind.GYM)),
                    SavedEquipmentPreset("Travel", EquipmentPresetKind.TRAVEL, presetOwned(EquipmentPresetKind.TRAVEL))
                )
            )
        }

        fun presetOwned(kind: EquipmentPresetKind): Set<String> = when (kind) {
            EquipmentPresetKind.HOME ->
                setOf(
                    EquipmentItem.DUMBBELL.id,
                    EquipmentItem.KETTLEBELL.id,
                    EquipmentItem.BARBELL.id,
                    EquipmentItem.PLATES.id,
                    EquipmentItem.BENCH_FLAT.id,
                    EquipmentItem.BENCH_ADJUSTABLE.id,
                    EquipmentItem.BANDS.id,
                    EquipmentItem.PULL_UP_BAR.id,
                    EquipmentItem.DIP_BARS.id,
                    EquipmentItem.SQUAT_RACK.id,
                    EquipmentItem.YOGA_MAT.id,
                    EquipmentItem.BODYWEIGHT.id
                )
            EquipmentPresetKind.GYM -> EquipmentItem.entries.map { it.id }.toSet()
            EquipmentPresetKind.TRAVEL ->
                setOf(
                    EquipmentItem.BANDS.id,
                    EquipmentItem.BODYWEIGHT.id,
                    EquipmentItem.YOGA_MAT.id,
                    EquipmentItem.SUSPENSION.id
                )
            EquipmentPresetKind.CUSTOM -> presetOwned(EquipmentPresetKind.HOME)
        }

        /**
         * Catalogue aliases expand to content tags so older library rows stay reachable.
         * Example: owning [bench_flat] also satisfies exercises tagged `bench`.
         */
        fun expandCapabilities(owned: Set<String>): Set<String> {
            val out = owned.toMutableSet()
            if (EquipmentItem.BENCH_FLAT.id in owned || EquipmentItem.BENCH_ADJUSTABLE.id in owned) {
                out += "bench"
            }
            if (EquipmentItem.PLATES.id in owned && EquipmentItem.BARBELL.id in owned) {
                out += EquipmentItem.BARBELL.id
            }
            // Generic machine unlock covers unspecified machine-tagged rows.
            if (EquipmentItem.MACHINE.id in owned) {
                out += EquipmentItem.SMITH_MACHINE.id
                out += EquipmentItem.LEG_PRESS.id
                out += EquipmentItem.LAT_PULLDOWN.id
                out += EquipmentItem.ASSISTED_PULLUP_DIP_MACHINE.id
            }
            // Specific machines also count as "machine" for loosely tagged content.
            val specificMachines =
                setOf(
                    EquipmentItem.SMITH_MACHINE.id,
                    EquipmentItem.LEG_PRESS.id,
                    EquipmentItem.LAT_PULLDOWN.id,
                    EquipmentItem.ASSISTED_PULLUP_DIP_MACHINE.id
                )
            if (owned.any { it in specificMachines }) {
                out += EquipmentItem.MACHINE.id
            }
            if (EquipmentItem.CABLE.id in owned) {
                out += EquipmentItem.LAT_PULLDOWN.id
            }
            return out
        }
    }
}

data class SavedEquipmentPreset(val label: String, val kind: EquipmentPresetKind, val ownedIds: Set<String>)
