package com.omnigart.shared.strength

/**
 * Expanded offline equipment catalogue (150+). Illustration keys are `equip_<id>`;
 * art lands in a parallel PR — UI shows a neutral stand-in when the asset is missing.
 */
data class EquipmentDefinition(
    val id: String,
    val group: EquipmentGroup,
    val nameKey: String,
    val illustrationKey: String,
    /** Content-tag aliases this item satisfies (in addition to [id]). */
    val aliases: List<String> = emptyList(),
    val searchable: Boolean = true
) {
    companion object {
        fun of(id: String, group: EquipmentGroup, aliases: List<String> = emptyList()): EquipmentDefinition = EquipmentDefinition(
            id = id,
            group = group,
            nameKey = "equipment_item_$id",
            illustrationKey = "equip_$id",
            aliases = aliases
        )
    }
}

enum class EquipmentGroup {
    HOME,
    FREE_WEIGHTS,
    BENCHES_RACKS,
    MACHINES,
    CARDIO,
    BANDS_SUSPENSION,
    ACCESSORIES,
    ALWAYS
}

/**
 * Full catalogue used by the searchable multi-select equipment picker.
 * Legacy [EquipmentItem] ids remain first-class for programme generation.
 */
object EquipmentDefinitions {
    val all: List<EquipmentDefinition> =
        listOf(
            // Always
            EquipmentDefinition.of("bodyweight", EquipmentGroup.ALWAYS),
            // Home improvisation
            EquipmentDefinition.of("chair", EquipmentGroup.HOME),
            EquipmentDefinition.of("sturdy_table", EquipmentGroup.HOME, aliases = listOf("bench")),
            EquipmentDefinition.of("stool", EquipmentGroup.HOME),
            EquipmentDefinition.of("doorframe_pull_up_bar", EquipmentGroup.HOME, aliases = listOf("pull_up_bar")),
            EquipmentDefinition.of("towel", EquipmentGroup.HOME),
            EquipmentDefinition.of("backpack", EquipmentGroup.HOME),
            EquipmentDefinition.of("water_bottles", EquipmentGroup.HOME),
            EquipmentDefinition.of("stairs", EquipmentGroup.HOME),
            EquipmentDefinition.of("wall", EquipmentGroup.HOME),
            EquipmentDefinition.of("couch", EquipmentGroup.HOME),
            EquipmentDefinition.of("broomstick", EquipmentGroup.HOME),
            EquipmentDefinition.of("books", EquipmentGroup.HOME),
            EquipmentDefinition.of("suitcase", EquipmentGroup.HOME),
            EquipmentDefinition.of("filled_backpack", EquipmentGroup.HOME, aliases = listOf("backpack")),
            EquipmentDefinition.of("resistance_door_anchor", EquipmentGroup.HOME),
            EquipmentDefinition.of("slider_discs", EquipmentGroup.HOME),
            EquipmentDefinition.of("furniture_slider", EquipmentGroup.HOME, aliases = listOf("slider_discs")),
            EquipmentDefinition.of("pillow", EquipmentGroup.HOME),
            EquipmentDefinition.of("bed_edge", EquipmentGroup.HOME),
            EquipmentDefinition.of("countertop", EquipmentGroup.HOME),
            EquipmentDefinition.of("park_bench", EquipmentGroup.HOME, aliases = listOf("bench")),
            EquipmentDefinition.of("playground_bar", EquipmentGroup.HOME, aliases = listOf("pull_up_bar")),
            EquipmentDefinition.of("tree_branch", EquipmentGroup.HOME, aliases = listOf("pull_up_bar")),
            // Free weights
            EquipmentDefinition.of("dumbbell", EquipmentGroup.FREE_WEIGHTS),
            EquipmentDefinition.of("adjustable_dumbbell", EquipmentGroup.FREE_WEIGHTS, aliases = listOf("dumbbell")),
            EquipmentDefinition.of("kettlebell", EquipmentGroup.FREE_WEIGHTS),
            EquipmentDefinition.of("barbell", EquipmentGroup.FREE_WEIGHTS),
            EquipmentDefinition.of("olympic_barbell", EquipmentGroup.FREE_WEIGHTS, aliases = listOf("barbell")),
            EquipmentDefinition.of("ez_bar", EquipmentGroup.FREE_WEIGHTS),
            EquipmentDefinition.of("trap_bar", EquipmentGroup.FREE_WEIGHTS),
            EquipmentDefinition.of("safety_squat_bar", EquipmentGroup.FREE_WEIGHTS, aliases = listOf("barbell")),
            EquipmentDefinition.of("swiss_bar", EquipmentGroup.FREE_WEIGHTS, aliases = listOf("barbell")),
            EquipmentDefinition.of("plates", EquipmentGroup.FREE_WEIGHTS),
            EquipmentDefinition.of("bumper_plates", EquipmentGroup.FREE_WEIGHTS, aliases = listOf("plates")),
            EquipmentDefinition.of("change_plates", EquipmentGroup.FREE_WEIGHTS, aliases = listOf("plates")),
            EquipmentDefinition.of("weight_vest", EquipmentGroup.FREE_WEIGHTS),
            EquipmentDefinition.of("ankle_weights", EquipmentGroup.FREE_WEIGHTS),
            EquipmentDefinition.of("wrist_weights", EquipmentGroup.FREE_WEIGHTS),
            EquipmentDefinition.of("sandbag", EquipmentGroup.FREE_WEIGHTS),
            EquipmentDefinition.of("medicine_ball", EquipmentGroup.FREE_WEIGHTS),
            EquipmentDefinition.of("slam_ball", EquipmentGroup.FREE_WEIGHTS, aliases = listOf("medicine_ball")),
            EquipmentDefinition.of("wall_ball", EquipmentGroup.FREE_WEIGHTS, aliases = listOf("medicine_ball")),
            EquipmentDefinition.of("clubbell", EquipmentGroup.FREE_WEIGHTS),
            EquipmentDefinition.of("macebell", EquipmentGroup.FREE_WEIGHTS),
            EquipmentDefinition.of("indian_club", EquipmentGroup.FREE_WEIGHTS),
            EquipmentDefinition.of("steel_mace", EquipmentGroup.FREE_WEIGHTS, aliases = listOf("macebell")),
            EquipmentDefinition.of("log_bar", EquipmentGroup.FREE_WEIGHTS),
            EquipmentDefinition.of("axle_bar", EquipmentGroup.FREE_WEIGHTS, aliases = listOf("barbell")),
            EquipmentDefinition.of("farmers_walk_handles", EquipmentGroup.FREE_WEIGHTS),
            EquipmentDefinition.of("hex_dumbbell", EquipmentGroup.FREE_WEIGHTS, aliases = listOf("dumbbell")),
            EquipmentDefinition.of("fixed_barbell", EquipmentGroup.FREE_WEIGHTS, aliases = listOf("barbell")),
            // Benches & racks
            EquipmentDefinition.of("bench_flat", EquipmentGroup.BENCHES_RACKS, aliases = listOf("bench")),
            EquipmentDefinition.of("bench_adjustable", EquipmentGroup.BENCHES_RACKS, aliases = listOf("bench", "bench_flat")),
            EquipmentDefinition.of("bench_decline", EquipmentGroup.BENCHES_RACKS, aliases = listOf("bench")),
            EquipmentDefinition.of("incline_bench", EquipmentGroup.BENCHES_RACKS, aliases = listOf("bench", "bench_adjustable")),
            EquipmentDefinition.of("preacher_bench", EquipmentGroup.BENCHES_RACKS),
            EquipmentDefinition.of("utility_bench", EquipmentGroup.BENCHES_RACKS, aliases = listOf("bench")),
            EquipmentDefinition.of("squat_rack", EquipmentGroup.BENCHES_RACKS),
            EquipmentDefinition.of("power_rack", EquipmentGroup.BENCHES_RACKS, aliases = listOf("squat_rack")),
            EquipmentDefinition.of("half_rack", EquipmentGroup.BENCHES_RACKS, aliases = listOf("squat_rack")),
            EquipmentDefinition.of("squat_stand", EquipmentGroup.BENCHES_RACKS, aliases = listOf("squat_rack")),
            EquipmentDefinition.of("cage", EquipmentGroup.BENCHES_RACKS, aliases = listOf("squat_rack", "power_rack")),
            EquipmentDefinition.of("wall_mounted_rack", EquipmentGroup.BENCHES_RACKS, aliases = listOf("squat_rack")),
            EquipmentDefinition.of("folding_rack", EquipmentGroup.BENCHES_RACKS, aliases = listOf("squat_rack")),
            EquipmentDefinition.of("dip_station", EquipmentGroup.BENCHES_RACKS, aliases = listOf("dip_bars")),
            EquipmentDefinition.of("pull_up_station", EquipmentGroup.BENCHES_RACKS, aliases = listOf("pull_up_bar")),
            EquipmentDefinition.of("power_tower", EquipmentGroup.BENCHES_RACKS, aliases = listOf("pull_up_bar", "dip_bars")),
            EquipmentDefinition.of("landmine", EquipmentGroup.BENCHES_RACKS),
            EquipmentDefinition.of("j_hooks", EquipmentGroup.BENCHES_RACKS),
            EquipmentDefinition.of("safety_arms", EquipmentGroup.BENCHES_RACKS),
            EquipmentDefinition.of("spotter_arms", EquipmentGroup.BENCHES_RACKS, aliases = listOf("safety_arms")),
            // Machines
            EquipmentDefinition.of("cable", EquipmentGroup.MACHINES),
            EquipmentDefinition.of("cable_stack", EquipmentGroup.MACHINES, aliases = listOf("cable")),
            EquipmentDefinition.of("functional_trainer", EquipmentGroup.MACHINES, aliases = listOf("cable")),
            EquipmentDefinition.of("lat_pulldown", EquipmentGroup.MACHINES, aliases = listOf("cable", "machine")),
            EquipmentDefinition.of("seated_row_machine", EquipmentGroup.MACHINES, aliases = listOf("cable", "machine")),
            EquipmentDefinition.of("chest_press_machine", EquipmentGroup.MACHINES, aliases = listOf("machine")),
            EquipmentDefinition.of("pec_deck", EquipmentGroup.MACHINES, aliases = listOf("machine")),
            EquipmentDefinition.of("shoulder_press_machine", EquipmentGroup.MACHINES, aliases = listOf("machine")),
            EquipmentDefinition.of("smith_machine", EquipmentGroup.MACHINES, aliases = listOf("machine")),
            EquipmentDefinition.of("hack_squat", EquipmentGroup.MACHINES, aliases = listOf("machine")),
            EquipmentDefinition.of("leg_press", EquipmentGroup.MACHINES, aliases = listOf("machine")),
            EquipmentDefinition.of("leg_curl", EquipmentGroup.MACHINES, aliases = listOf("machine")),
            EquipmentDefinition.of("leg_extension", EquipmentGroup.MACHINES, aliases = listOf("machine")),
            EquipmentDefinition.of("calf_raise_machine", EquipmentGroup.MACHINES, aliases = listOf("machine")),
            EquipmentDefinition.of("hip_abductor", EquipmentGroup.MACHINES, aliases = listOf("machine")),
            EquipmentDefinition.of("hip_adductor", EquipmentGroup.MACHINES, aliases = listOf("machine")),
            EquipmentDefinition.of("glute_drive", EquipmentGroup.MACHINES, aliases = listOf("machine")),
            EquipmentDefinition.of("assisted_pullup_dip_machine", EquipmentGroup.MACHINES, aliases = listOf("machine")),
            EquipmentDefinition.of("torso_rotation", EquipmentGroup.MACHINES, aliases = listOf("machine")),
            EquipmentDefinition.of("back_extension_machine", EquipmentGroup.MACHINES, aliases = listOf("machine")),
            EquipmentDefinition.of("ab_crunch_machine", EquipmentGroup.MACHINES, aliases = listOf("machine")),
            EquipmentDefinition.of("preacher_curl_machine", EquipmentGroup.MACHINES, aliases = listOf("machine")),
            EquipmentDefinition.of("triceps_extension_machine", EquipmentGroup.MACHINES, aliases = listOf("machine")),
            EquipmentDefinition.of("lateral_raise_machine", EquipmentGroup.MACHINES, aliases = listOf("machine")),
            EquipmentDefinition.of("rear_delt_machine", EquipmentGroup.MACHINES, aliases = listOf("machine")),
            EquipmentDefinition.of("chest_supported_row_machine", EquipmentGroup.MACHINES, aliases = listOf("machine")),
            EquipmentDefinition.of("pendulum_squat", EquipmentGroup.MACHINES, aliases = listOf("machine")),
            EquipmentDefinition.of("belt_squat", EquipmentGroup.MACHINES, aliases = listOf("machine")),
            EquipmentDefinition.of("v_squat", EquipmentGroup.MACHINES, aliases = listOf("machine", "hack_squat")),
            EquipmentDefinition.of("machine", EquipmentGroup.MACHINES),
            // Cardio
            EquipmentDefinition.of("treadmill", EquipmentGroup.CARDIO),
            EquipmentDefinition.of("exercise_bike", EquipmentGroup.CARDIO),
            EquipmentDefinition.of("spin_bike", EquipmentGroup.CARDIO, aliases = listOf("exercise_bike")),
            EquipmentDefinition.of("air_bike", EquipmentGroup.CARDIO),
            EquipmentDefinition.of("rower", EquipmentGroup.CARDIO),
            EquipmentDefinition.of("elliptical", EquipmentGroup.CARDIO),
            EquipmentDefinition.of("stair_climber", EquipmentGroup.CARDIO),
            EquipmentDefinition.of("stepmill", EquipmentGroup.CARDIO, aliases = listOf("stair_climber")),
            EquipmentDefinition.of("ski_ergometer", EquipmentGroup.CARDIO),
            EquipmentDefinition.of("curved_treadmill", EquipmentGroup.CARDIO),
            EquipmentDefinition.of("jump_rope", EquipmentGroup.CARDIO),
            EquipmentDefinition.of("battle_ropes", EquipmentGroup.CARDIO),
            EquipmentDefinition.of("sled", EquipmentGroup.CARDIO),
            EquipmentDefinition.of("push_sled", EquipmentGroup.CARDIO, aliases = listOf("sled")),
            EquipmentDefinition.of("agility_ladder", EquipmentGroup.CARDIO),
            EquipmentDefinition.of("cones", EquipmentGroup.CARDIO),
            EquipmentDefinition.of("track", EquipmentGroup.CARDIO),
            EquipmentDefinition.of("pool", EquipmentGroup.CARDIO),
            // Bands & suspension
            EquipmentDefinition.of("bands", EquipmentGroup.BANDS_SUSPENSION),
            EquipmentDefinition.of("loop_bands", EquipmentGroup.BANDS_SUSPENSION, aliases = listOf("bands")),
            EquipmentDefinition.of("tube_bands", EquipmentGroup.BANDS_SUSPENSION, aliases = listOf("bands")),
            EquipmentDefinition.of("mini_bands", EquipmentGroup.BANDS_SUSPENSION, aliases = listOf("bands")),
            EquipmentDefinition.of("fabric_hip_bands", EquipmentGroup.BANDS_SUSPENSION, aliases = listOf("bands")),
            EquipmentDefinition.of("suspension", EquipmentGroup.BANDS_SUSPENSION),
            EquipmentDefinition.of("suspension_straps", EquipmentGroup.BANDS_SUSPENSION, aliases = listOf("suspension")),
            EquipmentDefinition.of("rings", EquipmentGroup.BANDS_SUSPENSION, aliases = listOf("suspension")),
            EquipmentDefinition.of("gymnastic_rings", EquipmentGroup.BANDS_SUSPENSION, aliases = listOf("rings", "suspension")),
            EquipmentDefinition.of("pull_up_bar", EquipmentGroup.BANDS_SUSPENSION),
            EquipmentDefinition.of("dip_bars", EquipmentGroup.BANDS_SUSPENSION),
            EquipmentDefinition.of("parallettes", EquipmentGroup.BANDS_SUSPENSION),
            EquipmentDefinition.of("push_up_handles", EquipmentGroup.BANDS_SUSPENSION),
            EquipmentDefinition.of("ab_wheel", EquipmentGroup.BANDS_SUSPENSION),
            EquipmentDefinition.of("resistance_handles", EquipmentGroup.BANDS_SUSPENSION, aliases = listOf("bands")),
            // Accessories
            EquipmentDefinition.of("yoga_mat", EquipmentGroup.ACCESSORIES),
            EquipmentDefinition.of("box", EquipmentGroup.ACCESSORIES),
            EquipmentDefinition.of("plyo_box", EquipmentGroup.ACCESSORIES, aliases = listOf("box")),
            EquipmentDefinition.of("step_platform", EquipmentGroup.ACCESSORIES, aliases = listOf("box")),
            EquipmentDefinition.of("foam_roller", EquipmentGroup.ACCESSORIES),
            EquipmentDefinition.of("swiss_ball", EquipmentGroup.ACCESSORIES),
            EquipmentDefinition.of("balance_trainer", EquipmentGroup.ACCESSORIES),
            EquipmentDefinition.of("balance_pad", EquipmentGroup.ACCESSORIES),
            EquipmentDefinition.of("yoga_block", EquipmentGroup.ACCESSORIES),
            EquipmentDefinition.of("yoga_strap", EquipmentGroup.ACCESSORIES),
            EquipmentDefinition.of("massage_gun", EquipmentGroup.ACCESSORIES),
            EquipmentDefinition.of("lacrosse_ball", EquipmentGroup.ACCESSORIES),
            EquipmentDefinition.of("lifting_straps", EquipmentGroup.ACCESSORIES),
            EquipmentDefinition.of("lifting_belt", EquipmentGroup.ACCESSORIES),
            EquipmentDefinition.of("knee_sleeves", EquipmentGroup.ACCESSORIES),
            EquipmentDefinition.of("wrist_wraps", EquipmentGroup.ACCESSORIES),
            EquipmentDefinition.of("chalk", EquipmentGroup.ACCESSORIES),
            EquipmentDefinition.of("grip_strengthener", EquipmentGroup.ACCESSORIES),
            EquipmentDefinition.of("fat_grips", EquipmentGroup.ACCESSORIES),
            EquipmentDefinition.of("ab_mat", EquipmentGroup.ACCESSORIES),
            EquipmentDefinition.of("hip_thrust_pad", EquipmentGroup.ACCESSORIES),
            EquipmentDefinition.of("barbell_pad", EquipmentGroup.ACCESSORIES),
            EquipmentDefinition.of("weight_tree", EquipmentGroup.ACCESSORIES),
            EquipmentDefinition.of("storage_rack", EquipmentGroup.ACCESSORIES),
            EquipmentDefinition.of("mirror", EquipmentGroup.ACCESSORIES),
            EquipmentDefinition.of("timer", EquipmentGroup.ACCESSORIES),
            EquipmentDefinition.of("heart_rate_monitor", EquipmentGroup.ACCESSORIES),
            EquipmentDefinition.of("lifting_shoes", EquipmentGroup.ACCESSORIES),
            EquipmentDefinition.of("glute_band", EquipmentGroup.ACCESSORIES, aliases = listOf("bands", "mini_bands")),
            EquipmentDefinition.of("door_anchor", EquipmentGroup.ACCESSORIES, aliases = listOf("resistance_door_anchor")),
            EquipmentDefinition.of("carabiner", EquipmentGroup.ACCESSORIES),
            EquipmentDefinition.of("gym_towel", EquipmentGroup.ACCESSORIES, aliases = listOf("towel")),
            EquipmentDefinition.of("water_bottle", EquipmentGroup.ACCESSORIES),
            EquipmentDefinition.of("none", EquipmentGroup.ALWAYS, aliases = emptyList())
        )

    private val byId: Map<String, EquipmentDefinition> = all.associateBy { it.id }

    fun fromId(id: String): EquipmentDefinition? = byId[id]

    fun selectable(): List<EquipmentDefinition> = all.filter { it.id != "bodyweight" && it.id != "none" && it.searchable }

    fun grouped(): Map<EquipmentGroup, List<EquipmentDefinition>> = selectable().groupBy { it.group }

    fun search(query: String): List<EquipmentDefinition> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return selectable()
        return selectable().filter { def ->
            def.id.contains(q) ||
                def.nameKey.contains(q) ||
                def.aliases.any { it.contains(q) }
        }
    }

    fun illustrationKey(id: String): String = byId[id]?.illustrationKey ?: "equip_$id"

    /** Expand owned ids with aliases so content tags resolve. */
    fun expandOwned(owned: Set<String>): Set<String> {
        val out = owned.toMutableSet()
        for (id in owned) {
            val def = byId[id] ?: continue
            out += def.aliases
        }
        // Legacy bench / machine expansions
        if ("bench_flat" in out || "bench_adjustable" in out || "sturdy_table" in out) {
            out += "bench"
        }
        if ("machine" in out) {
            out +=
                listOf(
                    "smith_machine",
                    "leg_press",
                    "lat_pulldown",
                    "assisted_pullup_dip_machine",
                    "hack_squat",
                    "pec_deck",
                    "chest_press_machine"
                )
        }
        if (out.any { it.endsWith("_machine") || it in setOf("hack_squat", "pec_deck", "leg_curl", "leg_extension") }) {
            out += "machine"
        }
        if ("cable" in out || "cable_stack" in out || "functional_trainer" in out) {
            out += "lat_pulldown"
            out += "cable"
        }
        if ("doorframe_pull_up_bar" in out || "playground_bar" in out || "power_tower" in out) {
            out += "pull_up_bar"
        }
        if ("dip_station" in out || "power_tower" in out) {
            out += "dip_bars"
        }
        if ("suspension_straps" in out || "rings" in out || "gymnastic_rings" in out) {
            out += "suspension"
        }
        out += "bodyweight"
        out += "none"
        return out
    }
}
