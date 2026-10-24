package com.omnigart.shared.strength

/**
 * Predefined single-session workouts browsable in Workouts alongside the programme generator.
 * Filtered by [UserEquipmentProfile]; Adjust equipment walks substitution chains per slot.
 *
 * Generated from content/exercises/templates/ready-*.json.
 */
data class ReadyMadeSlot(
    val preferredIds: List<String>,
    val sets: Int,
    val repsLow: Int,
    val repsHigh: Int,
    val restSeconds: Int,
    val rirLow: Int,
    val rirHigh: Int,
    val durationSeconds: Int? = null
)

data class ReadyMadeWorkout(
    val id: String,
    val nameKey: String,
    val level: String,
    val sessionMinutes: Int,
    val equipmentRequired: List<String>,
    val focus: String,
    val slots: List<ReadyMadeSlot>
)

object ReadyMadeWorkoutCatalog {
    val workouts: List<ReadyMadeWorkout> = listOf(
        ReadyMadeWorkout(
            id = "ready-bw-bar-pull-dip",
            nameKey = "template.ready_bw_bar_pull_dip.name",
            level = "intermediate",
            sessionMinutes = 30,
            equipmentRequired = listOf(
                "bodyweight",
                "pull_up_bar"
            ),
            focus = "strength",
            slots = listOf(
                ReadyMadeSlot(
                    preferredIds = listOf("pull-up"),
                    sets = 4,
                    repsLow = 8,
                    repsHigh = 12,
                    restSeconds = 60,
                    rirLow = 2,
                    rirHigh = 3
                ),
                ReadyMadeSlot(
                    preferredIds = listOf("push-up"),
                    sets = 3,
                    repsLow = 10,
                    repsHigh = 15,
                    restSeconds = 60,
                    rirLow = 2,
                    rirHigh = 3
                ),
                ReadyMadeSlot(
                    preferredIds = listOf("inverted-row"),
                    sets = 3,
                    repsLow = 8,
                    repsHigh = 12,
                    restSeconds = 60,
                    rirLow = 2,
                    rirHigh = 3
                ),
                ReadyMadeSlot(
                    preferredIds = listOf("pike-push-up"),
                    sets = 3,
                    repsLow = 6,
                    repsHigh = 10,
                    restSeconds = 60,
                    rirLow = 2,
                    rirHigh = 3
                ),
                ReadyMadeSlot(
                    preferredIds = listOf("hanging-knee-raise"),
                    sets = 3,
                    repsLow = 8,
                    repsHigh = 12,
                    restSeconds = 60,
                    rirLow = 2,
                    rirHigh = 3
                ),
                ReadyMadeSlot(
                    preferredIds = listOf("plank"),
                    sets = 2,
                    repsLow = 8,
                    repsHigh = 12,
                    restSeconds = 60,
                    rirLow = 2,
                    rirHigh = 3,
                    durationSeconds = 30
                )
            )
        ),
        ReadyMadeWorkout(
            id = "ready-bw-core",
            nameKey = "template.ready_bw_core.name",
            level = "beginner",
            sessionMinutes = 20,
            equipmentRequired = listOf("bodyweight"),
            focus = "general_fitness",
            slots = listOf(
                ReadyMadeSlot(
                    preferredIds = listOf("plank"),
                    sets = 3,
                    repsLow = 8,
                    repsHigh = 12,
                    restSeconds = 60,
                    rirLow = 2,
                    rirHigh = 3,
                    durationSeconds = 30
                ),
                ReadyMadeSlot(
                    preferredIds = listOf("side-plank"),
                    sets = 3,
                    repsLow = 8,
                    repsHigh = 12,
                    restSeconds = 60,
                    rirLow = 2,
                    rirHigh = 3,
                    durationSeconds = 25
                ),
                ReadyMadeSlot(
                    preferredIds = listOf("dead-bug"),
                    sets = 3,
                    repsLow = 8,
                    repsHigh = 8,
                    restSeconds = 60,
                    rirLow = 2,
                    rirHigh = 3
                ),
                ReadyMadeSlot(
                    preferredIds = listOf("hollow-hold"),
                    sets = 3,
                    repsLow = 8,
                    repsHigh = 12,
                    restSeconds = 60,
                    rirLow = 2,
                    rirHigh = 3,
                    durationSeconds = 20
                ),
                ReadyMadeSlot(
                    preferredIds = listOf("mountain-climber"),
                    sets = 2,
                    repsLow = 16,
                    repsHigh = 16,
                    restSeconds = 45,
                    rirLow = 2,
                    rirHigh = 3
                ),
                ReadyMadeSlot(
                    preferredIds = listOf("bird-dog"),
                    sets = 2,
                    repsLow = 8,
                    repsHigh = 8,
                    restSeconds = 60,
                    rirLow = 2,
                    rirHigh = 3
                )
            )
        ),
        ReadyMadeWorkout(
            id = "ready-bw-full-body-advanced",
            nameKey = "template.ready_bw_full_body_advanced.name",
            level = "advanced",
            sessionMinutes = 40,
            equipmentRequired = listOf("bodyweight"),
            focus = "strength",
            slots = listOf(
                ReadyMadeSlot(
                    preferredIds = listOf("decline-push-up"),
                    sets = 4,
                    repsLow = 8,
                    repsHigh = 12,
                    restSeconds = 60,
                    rirLow = 2,
                    rirHigh = 3
                ),
                ReadyMadeSlot(
                    preferredIds = listOf("nordic-curl"),
                    sets = 3,
                    repsLow = 5,
                    repsHigh = 8,
                    restSeconds = 60,
                    rirLow = 2,
                    rirHigh = 3
                ),
                ReadyMadeSlot(
                    preferredIds = listOf("burpee"),
                    sets = 3,
                    repsLow = 8,
                    repsHigh = 12,
                    restSeconds = 75,
                    rirLow = 2,
                    rirHigh = 3
                ),
                ReadyMadeSlot(
                    preferredIds = listOf("wide-push-up"),
                    sets = 3,
                    repsLow = 12,
                    repsHigh = 15,
                    restSeconds = 60,
                    rirLow = 2,
                    rirHigh = 3
                ),
                ReadyMadeSlot(
                    preferredIds = listOf("split-squat-bodyweight"),
                    sets = 3,
                    repsLow = 10,
                    repsHigh = 10,
                    restSeconds = 60,
                    rirLow = 2,
                    rirHigh = 3
                ),
                ReadyMadeSlot(
                    preferredIds = listOf("side-plank"),
                    sets = 3,
                    repsLow = 8,
                    repsHigh = 12,
                    restSeconds = 60,
                    rirLow = 2,
                    rirHigh = 3,
                    durationSeconds = 30
                ),
                ReadyMadeSlot(
                    preferredIds = listOf("wall-sit"),
                    sets = 2,
                    repsLow = 8,
                    repsHigh = 12,
                    restSeconds = 60,
                    rirLow = 2,
                    rirHigh = 3,
                    durationSeconds = 40
                )
            )
        ),
        ReadyMadeWorkout(
            id = "ready-bw-full-body-beginner",
            nameKey = "template.ready_bw_full_body_beginner.name",
            level = "beginner",
            sessionMinutes = 25,
            equipmentRequired = listOf("bodyweight"),
            focus = "general_fitness",
            slots = listOf(
                ReadyMadeSlot(
                    preferredIds = listOf("bodyweight-squat"),
                    sets = 3,
                    repsLow = 10,
                    repsHigh = 12,
                    restSeconds = 60,
                    rirLow = 2,
                    rirHigh = 3
                ),
                ReadyMadeSlot(
                    preferredIds = listOf("knee-push-up"),
                    sets = 3,
                    repsLow = 8,
                    repsHigh = 12,
                    restSeconds = 60,
                    rirLow = 2,
                    rirHigh = 3
                ),
                ReadyMadeSlot(
                    preferredIds = listOf("reverse-lunge-bodyweight"),
                    sets = 3,
                    repsLow = 8,
                    repsHigh = 8,
                    restSeconds = 60,
                    rirLow = 2,
                    rirHigh = 3
                ),
                ReadyMadeSlot(
                    preferredIds = listOf("glute-bridge"),
                    sets = 3,
                    repsLow = 12,
                    repsHigh = 12,
                    restSeconds = 60,
                    rirLow = 2,
                    rirHigh = 3
                ),
                ReadyMadeSlot(
                    preferredIds = listOf("plank"),
                    sets = 3,
                    repsLow = 8,
                    repsHigh = 12,
                    restSeconds = 45,
                    rirLow = 2,
                    rirHigh = 3,
                    durationSeconds = 30
                ),
                ReadyMadeSlot(
                    preferredIds = listOf("bird-dog"),
                    sets = 2,
                    repsLow = 8,
                    repsHigh = 8,
                    restSeconds = 60,
                    rirLow = 2,
                    rirHigh = 3
                )
            )
        ),
        ReadyMadeWorkout(
            id = "ready-bw-full-body-intermediate",
            nameKey = "template.ready_bw_full_body_intermediate.name",
            level = "intermediate",
            sessionMinutes = 35,
            equipmentRequired = listOf("bodyweight"),
            focus = "general_fitness",
            slots = listOf(
                ReadyMadeSlot(
                    preferredIds = listOf("jump-squat"),
                    sets = 3,
                    repsLow = 8,
                    repsHigh = 10,
                    restSeconds = 60,
                    rirLow = 2,
                    rirHigh = 3
                ),
                ReadyMadeSlot(
                    preferredIds = listOf("push-up"),
                    sets = 3,
                    repsLow = 10,
                    repsHigh = 15,
                    restSeconds = 60,
                    rirLow = 2,
                    rirHigh = 3
                ),
                ReadyMadeSlot(
                    preferredIds = listOf("walking-lunge"),
                    sets = 3,
                    repsLow = 10,
                    repsHigh = 10,
                    restSeconds = 60,
                    rirLow = 2,
                    rirHigh = 3
                ),
                ReadyMadeSlot(
                    preferredIds = listOf("hip-thrust-bodyweight"),
                    sets = 3,
                    repsLow = 12,
                    repsHigh = 12,
                    restSeconds = 60,
                    rirLow = 2,
                    rirHigh = 3
                ),
                ReadyMadeSlot(
                    preferredIds = listOf("pike-push-up"),
                    sets = 3,
                    repsLow = 6,
                    repsHigh = 10,
                    restSeconds = 60,
                    rirLow = 2,
                    rirHigh = 3
                ),
                ReadyMadeSlot(
                    preferredIds = listOf("hollow-hold"),
                    sets = 3,
                    repsLow = 8,
                    repsHigh = 12,
                    restSeconds = 60,
                    rirLow = 2,
                    rirHigh = 3,
                    durationSeconds = 25
                ),
                ReadyMadeSlot(
                    preferredIds = listOf("mountain-climber"),
                    sets = 2,
                    repsLow = 20,
                    repsHigh = 20,
                    restSeconds = 45,
                    rirLow = 2,
                    rirHigh = 3
                )
            )
        ),
        ReadyMadeWorkout(
            id = "ready-bw-lower",
            nameKey = "template.ready_bw_lower.name",
            level = "intermediate",
            sessionMinutes = 30,
            equipmentRequired = listOf("bodyweight"),
            focus = "hypertrophy",
            slots = listOf(
                ReadyMadeSlot(
                    preferredIds = listOf("bodyweight-squat"),
                    sets = 4,
                    repsLow = 12,
                    repsHigh = 15,
                    restSeconds = 60,
                    rirLow = 2,
                    rirHigh = 3
                ),
                ReadyMadeSlot(
                    preferredIds = listOf("reverse-lunge-bodyweight"),
                    sets = 3,
                    repsLow = 10,
                    repsHigh = 10,
                    restSeconds = 60,
                    rirLow = 2,
                    rirHigh = 3
                ),
                ReadyMadeSlot(
                    preferredIds = listOf("hip-thrust-bodyweight"),
                    sets = 4,
                    repsLow = 12,
                    repsHigh = 12,
                    restSeconds = 60,
                    rirLow = 2,
                    rirHigh = 3
                ),
                ReadyMadeSlot(
                    preferredIds = listOf("nordic-curl-assisted"),
                    sets = 3,
                    repsLow = 6,
                    repsHigh = 10,
                    restSeconds = 60,
                    rirLow = 2,
                    rirHigh = 3
                ),
                ReadyMadeSlot(
                    preferredIds = listOf("calf-raise-bodyweight"),
                    sets = 3,
                    repsLow = 15,
                    repsHigh = 20,
                    restSeconds = 60,
                    rirLow = 2,
                    rirHigh = 3
                ),
                ReadyMadeSlot(
                    preferredIds = listOf("wall-sit"),
                    sets = 2,
                    repsLow = 8,
                    repsHigh = 12,
                    restSeconds = 60,
                    rirLow = 2,
                    rirHigh = 3,
                    durationSeconds = 45
                )
            )
        ),
        ReadyMadeWorkout(
            id = "ready-bw-quick-15",
            nameKey = "template.ready_bw_quick_15.name",
            level = "beginner",
            sessionMinutes = 15,
            equipmentRequired = listOf("bodyweight"),
            focus = "fat_loss_support",
            slots = listOf(
                ReadyMadeSlot(
                    preferredIds = listOf("bodyweight-squat"),
                    sets = 2,
                    repsLow = 12,
                    repsHigh = 12,
                    restSeconds = 60,
                    rirLow = 2,
                    rirHigh = 3
                ),
                ReadyMadeSlot(
                    preferredIds = listOf("push-up"),
                    sets = 2,
                    repsLow = 8,
                    repsHigh = 12,
                    restSeconds = 60,
                    rirLow = 2,
                    rirHigh = 3
                ),
                ReadyMadeSlot(
                    preferredIds = listOf("reverse-lunge-bodyweight"),
                    sets = 2,
                    repsLow = 8,
                    repsHigh = 8,
                    restSeconds = 60,
                    rirLow = 2,
                    rirHigh = 3
                ),
                ReadyMadeSlot(
                    preferredIds = listOf("mountain-climber"),
                    sets = 2,
                    repsLow = 16,
                    repsHigh = 16,
                    restSeconds = 30,
                    rirLow = 2,
                    rirHigh = 3
                ),
                ReadyMadeSlot(
                    preferredIds = listOf("plank"),
                    sets = 2,
                    repsLow = 8,
                    repsHigh = 12,
                    restSeconds = 60,
                    rirLow = 2,
                    rirHigh = 3,
                    durationSeconds = 25
                )
            )
        ),
        ReadyMadeWorkout(
            id = "ready-bw-quick-20",
            nameKey = "template.ready_bw_quick_20.name",
            level = "intermediate",
            sessionMinutes = 20,
            equipmentRequired = listOf("bodyweight"),
            focus = "fat_loss_support",
            slots = listOf(
                ReadyMadeSlot(
                    preferredIds = listOf("jump-squat"),
                    sets = 3,
                    repsLow = 8,
                    repsHigh = 8,
                    restSeconds = 60,
                    rirLow = 2,
                    rirHigh = 3
                ),
                ReadyMadeSlot(
                    preferredIds = listOf("burpee"),
                    sets = 3,
                    repsLow = 6,
                    repsHigh = 10,
                    restSeconds = 45,
                    rirLow = 2,
                    rirHigh = 3
                ),
                ReadyMadeSlot(
                    preferredIds = listOf("push-up"),
                    sets = 3,
                    repsLow = 10,
                    repsHigh = 10,
                    restSeconds = 60,
                    rirLow = 2,
                    rirHigh = 3
                ),
                ReadyMadeSlot(
                    preferredIds = listOf("walking-lunge"),
                    sets = 2,
                    repsLow = 10,
                    repsHigh = 10,
                    restSeconds = 60,
                    rirLow = 2,
                    rirHigh = 3
                ),
                ReadyMadeSlot(
                    preferredIds = listOf("hollow-hold"),
                    sets = 2,
                    repsLow = 8,
                    repsHigh = 12,
                    restSeconds = 60,
                    rirLow = 2,
                    rirHigh = 3,
                    durationSeconds = 20
                )
            )
        ),
        ReadyMadeWorkout(
            id = "ready-bw-upper",
            nameKey = "template.ready_bw_upper.name",
            level = "intermediate",
            sessionMinutes = 30,
            equipmentRequired = listOf("bodyweight"),
            focus = "hypertrophy",
            slots = listOf(
                ReadyMadeSlot(
                    preferredIds = listOf("push-up"),
                    sets = 4,
                    repsLow = 8,
                    repsHigh = 15,
                    restSeconds = 60,
                    rirLow = 2,
                    rirHigh = 3
                ),
                ReadyMadeSlot(
                    preferredIds = listOf("inverted-row"),
                    sets = 4,
                    repsLow = 8,
                    repsHigh = 12,
                    restSeconds = 60,
                    rirLow = 2,
                    rirHigh = 3
                ),
                ReadyMadeSlot(
                    preferredIds = listOf("pike-push-up"),
                    sets = 3,
                    repsLow = 6,
                    repsHigh = 10,
                    restSeconds = 60,
                    rirLow = 2,
                    rirHigh = 3
                ),
                ReadyMadeSlot(
                    preferredIds = listOf("diamond-push-up"),
                    sets = 3,
                    repsLow = 8,
                    repsHigh = 12,
                    restSeconds = 60,
                    rirLow = 2,
                    rirHigh = 3
                ),
                ReadyMadeSlot(
                    preferredIds = listOf("wide-push-up"),
                    sets = 2,
                    repsLow = 10,
                    repsHigh = 15,
                    restSeconds = 60,
                    rirLow = 2,
                    rirHigh = 3
                ),
                ReadyMadeSlot(
                    preferredIds = listOf("plank"),
                    sets = 3,
                    repsLow = 8,
                    repsHigh = 12,
                    restSeconds = 60,
                    rirLow = 2,
                    rirHigh = 3,
                    durationSeconds = 35
                )
            )
        ),
        ReadyMadeWorkout(
            id = "ready-gym-lower-strength",
            nameKey = "template.ready_gym_lower_strength.name",
            level = "intermediate",
            sessionMinutes = 45,
            equipmentRequired = listOf(
                "barbell",
                "squat_rack",
                "plates"
            ),
            focus = "strength",
            slots = listOf(
                ReadyMadeSlot(
                    preferredIds = listOf("back-squat"),
                    sets = 4,
                    repsLow = 5,
                    repsHigh = 8,
                    restSeconds = 180,
                    rirLow = 2,
                    rirHigh = 3
                ),
                ReadyMadeSlot(
                    preferredIds = listOf("romanian-deadlift-barbell"),
                    sets = 3,
                    repsLow = 6,
                    repsHigh = 10,
                    restSeconds = 150,
                    rirLow = 2,
                    rirHigh = 3
                ),
                ReadyMadeSlot(
                    preferredIds = listOf("leg-press"),
                    sets = 3,
                    repsLow = 8,
                    repsHigh = 12,
                    restSeconds = 60,
                    rirLow = 2,
                    rirHigh = 3
                ),
                ReadyMadeSlot(
                    preferredIds = listOf("walking-lunge"),
                    sets = 2,
                    repsLow = 8,
                    repsHigh = 8,
                    restSeconds = 60,
                    rirLow = 2,
                    rirHigh = 3
                ),
                ReadyMadeSlot(
                    preferredIds = listOf("calf-raise-standing"),
                    sets = 3,
                    repsLow = 12,
                    repsHigh = 15,
                    restSeconds = 60,
                    rirLow = 2,
                    rirHigh = 3
                )
            )
        ),
        ReadyMadeWorkout(
            id = "ready-gym-upper-strength",
            nameKey = "template.ready_gym_upper_strength.name",
            level = "intermediate",
            sessionMinutes = 45,
            equipmentRequired = listOf(
                "barbell",
                "bench_flat",
                "plates"
            ),
            focus = "strength",
            slots = listOf(
                ReadyMadeSlot(
                    preferredIds = listOf("barbell-bench-press"),
                    sets = 4,
                    repsLow = 5,
                    repsHigh = 8,
                    restSeconds = 150,
                    rirLow = 2,
                    rirHigh = 3
                ),
                ReadyMadeSlot(
                    preferredIds = listOf("pull-up"),
                    sets = 4,
                    repsLow = 8,
                    repsHigh = 12,
                    restSeconds = 120,
                    rirLow = 2,
                    rirHigh = 3
                ),
                ReadyMadeSlot(
                    preferredIds = listOf("overhead-press-barbell"),
                    sets = 3,
                    repsLow = 5,
                    repsHigh = 8,
                    restSeconds = 120,
                    rirLow = 2,
                    rirHigh = 3
                ),
                ReadyMadeSlot(
                    preferredIds = listOf("seated-cable-row"),
                    sets = 3,
                    repsLow = 8,
                    repsHigh = 12,
                    restSeconds = 60,
                    rirLow = 2,
                    rirHigh = 3
                ),
                ReadyMadeSlot(
                    preferredIds = listOf("face-pull"),
                    sets = 3,
                    repsLow = 12,
                    repsHigh = 15,
                    restSeconds = 60,
                    rirLow = 2,
                    rirHigh = 3
                )
            )
        ),
        ReadyMadeWorkout(
            id = "ready-home-bands-travel",
            nameKey = "template.ready_home_bands_travel.name",
            level = "beginner",
            sessionMinutes = 25,
            equipmentRequired = listOf(
                "bodyweight",
                "bands"
            ),
            focus = "general_fitness",
            slots = listOf(
                ReadyMadeSlot(
                    preferredIds = listOf("bodyweight-squat"),
                    sets = 3,
                    repsLow = 12,
                    repsHigh = 12,
                    restSeconds = 60,
                    rirLow = 2,
                    rirHigh = 3
                ),
                ReadyMadeSlot(
                    preferredIds = listOf("push-up"),
                    sets = 3,
                    repsLow = 8,
                    repsHigh = 12,
                    restSeconds = 60,
                    rirLow = 2,
                    rirHigh = 3
                ),
                ReadyMadeSlot(
                    preferredIds = listOf("band-pull-apart"),
                    sets = 3,
                    repsLow = 15,
                    repsHigh = 15,
                    restSeconds = 60,
                    rirLow = 2,
                    rirHigh = 3
                ),
                ReadyMadeSlot(
                    preferredIds = listOf("glute-bridge"),
                    sets = 3,
                    repsLow = 12,
                    repsHigh = 12,
                    restSeconds = 60,
                    rirLow = 2,
                    rirHigh = 3
                ),
                ReadyMadeSlot(
                    preferredIds = listOf("nordic-curl-assisted"),
                    sets = 2,
                    repsLow = 6,
                    repsHigh = 8,
                    restSeconds = 60,
                    rirLow = 2,
                    rirHigh = 3
                ),
                ReadyMadeSlot(
                    preferredIds = listOf("plank"),
                    sets = 2,
                    repsLow = 8,
                    repsHigh = 12,
                    restSeconds = 60,
                    rirLow = 2,
                    rirHigh = 3,
                    durationSeconds = 30
                )
            )
        ),
        ReadyMadeWorkout(
            id = "ready-home-dumbbell-full",
            nameKey = "template.ready_home_dumbbell_full.name",
            level = "beginner",
            sessionMinutes = 35,
            equipmentRequired = listOf(
                "bodyweight",
                "dumbbell"
            ),
            focus = "general_fitness",
            slots = listOf(
                ReadyMadeSlot(
                    preferredIds = listOf("goblet-squat"),
                    sets = 3,
                    repsLow = 10,
                    repsHigh = 12,
                    restSeconds = 60,
                    rirLow = 2,
                    rirHigh = 3
                ),
                ReadyMadeSlot(
                    preferredIds = listOf("dumbbell-bench-press"),
                    sets = 3,
                    repsLow = 8,
                    repsHigh = 12,
                    restSeconds = 60,
                    rirLow = 2,
                    rirHigh = 3
                ),
                ReadyMadeSlot(
                    preferredIds = listOf("reverse-lunge-dumbbell"),
                    sets = 3,
                    repsLow = 8,
                    repsHigh = 8,
                    restSeconds = 60,
                    rirLow = 2,
                    rirHigh = 3
                ),
                ReadyMadeSlot(
                    preferredIds = listOf("one-arm-dumbbell-row"),
                    sets = 3,
                    repsLow = 10,
                    repsHigh = 10,
                    restSeconds = 60,
                    rirLow = 2,
                    rirHigh = 3
                ),
                ReadyMadeSlot(
                    preferredIds = listOf("overhead-press-dumbbell"),
                    sets = 3,
                    repsLow = 8,
                    repsHigh = 12,
                    restSeconds = 60,
                    rirLow = 2,
                    rirHigh = 3
                ),
                ReadyMadeSlot(
                    preferredIds = listOf("plank"),
                    sets = 2,
                    repsLow = 8,
                    repsHigh = 12,
                    restSeconds = 60,
                    rirLow = 2,
                    rirHigh = 3,
                    durationSeconds = 30
                )
            )
        )
    )

    fun availableFor(equipment: UserEquipmentProfile): List<ReadyMadeWorkout> =
        workouts.filter { equipment.canPerform(it.equipmentRequired) }

    fun byId(id: String): ReadyMadeWorkout? = workouts.firstOrNull { it.id == id }

    /**
     * Materialise a ready workout into a [GeneratedStrengthSession], substituting when
     * session equipment cannot cover a preferred exercise.
     */
    fun materialize(
        workout: ReadyMadeWorkout,
        equipment: UserEquipmentProfile,
        bodyMassKg: Double,
        library: List<StrengthLibraryExercise> = BundledStrengthLibrary.exercises,
        maxLevel: String = "advanced"
    ): GeneratedStrengthSession {
        val maxOrdinal = ExerciseSubstitution.levelOrdinal(maxLevel)
        val used = mutableSetOf<String>()
        val slots = mutableListOf<GeneratedExerciseSlot>()
        val muscles = linkedSetOf<String>()
        for (spec in workout.slots) {
            val preferred = spec.preferredIds.firstOrNull()
            val seed = library.firstOrNull { it.id == preferred }
            val pattern = seed?.movementPattern ?: "other"
            val targets = seed?.primaryMuscles?.toSet().orEmpty()
            val selection =
                ExerciseSubstitution.select(
                    preferredIds = spec.preferredIds,
                    pattern = pattern,
                    targetMuscles = targets,
                    equipment = equipment,
                    library = library,
                    excludeIds = used,
                    maxLevelOrdinal = maxOrdinal
                ) ?: continue
            used += selection.exercise.id
            muscles.addAll(selection.exercise.primaryMuscles)
            val load =
                ProgressiveOverload.suggestedLoadKg(
                    exercise = selection.exercise,
                    bodyMassKg = bodyMassKg,
                    externalOrAssistanceKg = 0.0,
                    weekNudgeKg = 0.0
                )
            slots +=
                GeneratedExerciseSlot(
                    exerciseId = selection.exercise.id,
                    sets = spec.sets,
                    repsLow = spec.repsLow,
                    repsHigh = spec.repsHigh,
                    restSeconds = spec.restSeconds,
                    rirLow = spec.rirLow,
                    rirHigh = spec.rirHigh,
                    suggestedLoadKg = load,
                    loadIncludesBodyMass = selection.exercise.usesBodyweightLoad(),
                    substitutedFromId = selection.substitutedFromId,
                    primaryMuscles = selection.exercise.primaryMuscles,
                    movementPattern = selection.exercise.movementPattern
                )
        }
        return GeneratedStrengthSession(
            dayIndex = 0,
            titleKey = workout.nameKey,
            focus = workout.focus,
            workedMuscles = muscles.toList(),
            slots = slots
        )
    }
}
