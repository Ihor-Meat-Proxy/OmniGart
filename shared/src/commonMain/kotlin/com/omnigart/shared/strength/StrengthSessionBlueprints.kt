package com.omnigart.shared.strength

internal data class SlotBlueprint(val preferredIds: List<String>, val pattern: String, val muscles: Set<String>)

internal data class SessionBlueprint(val titleKey: String, val focus: String, val slots: List<SlotBlueprint>)

internal object StrengthSessionBlueprints {
    fun forSplit(split: ProgramSplit, days: Int): List<SessionBlueprint> {
        val pool =
            when (split) {
                ProgramSplit.FULL_BODY -> fullBody()
                ProgramSplit.UPPER_LOWER -> upperLower()
                ProgramSplit.PUSH_PULL_LEGS -> {
                    val ppl = pushPullLegs()
                    if (days >= 6) ppl + ppl else ppl
                }
                ProgramSplit.BRO_SPLIT -> broSplit()
            }
        return pool.take(days)
    }

    private fun slot(preferred: List<String>, pattern: String, vararg muscles: String) = SlotBlueprint(preferred, pattern, muscles.toSet())

    private fun fullBody(): List<SessionBlueprint> = listOf(
        SessionBlueprint(
            "program_session_full_a",
            "squat_push_hinge",
            listOf(
                slot(
                    listOf("bodyweight-squat", "goblet-squat", "back-squat", "leg-press"),
                    "squat",
                    "quadriceps",
                    "gluteus_maximus"
                ),
                slot(listOf("push-up", "incline-push-up", "dumbbell-bench-press", "barbell-bench-press"), "push", "pectoralis_major"),
                slot(
                    listOf("glute-bridge", "romanian-deadlift-dumbbell", "romanian-deadlift-barbell", "kettlebell-swing"),
                    "hinge",
                    "hamstrings",
                    "gluteus_maximus"
                ),
                slot(
                    listOf("inverted-row", "one-arm-dumbbell-row", "barbell-row", "seated-cable-row"),
                    "pull",
                    "latissimus_dorsi"
                ),
                slot(listOf("plank", "hollow-hold", "dead-bug"), "core_brace", "rectus_abdominis", "transverse_abdominis")
            )
        ),
        SessionBlueprint(
            "program_session_full_b",
            "lunge_pull_push",
            listOf(
                slot(
                    listOf(
                        "reverse-lunge-bodyweight",
                        "walking-lunge",
                        "split-squat-bodyweight",
                        "reverse-lunge-dumbbell",
                        "bulgarian-split-squat"
                    ),
                    "lunge",
                    "quadriceps",
                    "gluteus_maximus"
                ),
                slot(
                    listOf("pull-up", "inverted-row", "lat-pulldown", "assisted-pull-up-band"),
                    "pull",
                    "latissimus_dorsi"
                ),
                slot(
                    listOf("pike-push-up", "overhead-press-dumbbell", "overhead-press-barbell", "machine-shoulder-press"),
                    "push",
                    "front_deltoid"
                ),
                slot(listOf("hip-thrust-bodyweight", "glute-bridge", "hip-thrust"), "hinge", "gluteus_maximus"),
                slot(listOf("band-pull-apart", "face-pull", "inverted-row"), "pull", "rear_deltoid", "rhomboids")
            )
        ),
        SessionBlueprint(
            "program_session_full_c",
            "hinge_carry_balance",
            listOf(
                slot(
                    listOf("nordic-curl", "glute-bridge", "kettlebell-swing", "conventional-deadlift", "trap-bar-deadlift"),
                    "hinge",
                    "hamstrings",
                    "gluteus_maximus"
                ),
                slot(
                    listOf("dip-parallel-bar", "bench-dip", "decline-push-up", "machine-assisted-dip"),
                    "push",
                    "triceps",
                    "pectoralis_major"
                ),
                slot(listOf("wall-sit", "farmers-carry"), "squat", "quadriceps", "forearms"),
                slot(listOf("calf-raise-bodyweight", "calf-raise-standing", "lateral-raise"), "other", "calves", "lateral_deltoid"),
                slot(listOf("bird-dog", "side-plank", "dead-bug"), "core_brace", "erector_spinae", "transverse_abdominis")
            )
        )
    )

    private fun upperLower(): List<SessionBlueprint> = listOf(
        SessionBlueprint(
            "program_session_upper_a",
            "push_pull",
            listOf(
                slot(listOf("barbell-bench-press", "dumbbell-bench-press", "push-up"), "push", "pectoralis_major"),
                slot(listOf("chest-supported-row", "one-arm-dumbbell-row", "seated-cable-row"), "pull", "latissimus_dorsi"),
                slot(listOf("overhead-press-barbell", "overhead-press-dumbbell"), "push", "front_deltoid"),
                slot(listOf("lat-pulldown", "pull-up", "assisted-pull-up-band"), "pull", "latissimus_dorsi"),
                slot(listOf("tricep-pushdown", "diamond-push-up"), "push", "triceps")
            )
        ),
        SessionBlueprint(
            "program_session_lower_a",
            "squat_hinge",
            listOf(
                slot(listOf("back-squat", "goblet-squat", "leg-press", "smith-machine-squat"), "squat", "quadriceps"),
                slot(listOf("romanian-deadlift-barbell", "romanian-deadlift-dumbbell"), "hinge", "hamstrings"),
                slot(listOf("walking-lunge", "bulgarian-split-squat"), "lunge", "quadriceps", "gluteus_maximus"),
                slot(listOf("leg-curl-machine", "nordic-curl-assisted"), "hinge", "hamstrings"),
                slot(listOf("calf-raise-standing"), "other", "calves")
            )
        ),
        SessionBlueprint(
            "program_session_upper_b",
            "pull_push",
            listOf(
                slot(listOf("pull-up", "chin-up", "lat-pulldown", "assisted-pull-up-band"), "pull", "latissimus_dorsi"),
                slot(listOf("dumbbell-incline-press", "dumbbell-bench-press"), "push", "pectoralis_major"),
                slot(listOf("seated-cable-row", "barbell-row"), "pull", "rhomboids", "latissimus_dorsi"),
                slot(listOf("lateral-raise"), "push", "lateral_deltoid"),
                slot(listOf("barbell-curl", "dumbbell-curl"), "pull", "biceps")
            )
        ),
        SessionBlueprint(
            "program_session_lower_b",
            "hinge_single_leg",
            listOf(
                slot(
                    listOf("trap-bar-deadlift", "conventional-deadlift", "kettlebell-swing"),
                    "hinge",
                    "hamstrings",
                    "gluteus_maximus"
                ),
                slot(listOf("bulgarian-split-squat", "reverse-lunge-dumbbell"), "lunge", "quadriceps"),
                slot(listOf("leg-press", "goblet-squat"), "squat", "quadriceps"),
                slot(listOf("hip-thrust", "glute-bridge"), "hinge", "gluteus_maximus"),
                slot(listOf("cable-woodchop"), "rotation", "obliques")
            )
        )
    )

    private fun pushPullLegs(): List<SessionBlueprint> = listOf(
        SessionBlueprint(
            "program_session_push",
            "push",
            listOf(
                slot(listOf("barbell-bench-press", "dumbbell-bench-press", "push-up"), "push", "pectoralis_major"),
                slot(listOf("overhead-press-dumbbell", "overhead-press-barbell"), "push", "front_deltoid"),
                slot(listOf("dip-parallel-bar", "machine-assisted-dip", "bench-dip"), "push", "triceps"),
                slot(listOf("lateral-raise"), "push", "lateral_deltoid"),
                slot(listOf("tricep-pushdown", "diamond-push-up"), "push", "triceps")
            )
        ),
        SessionBlueprint(
            "program_session_pull",
            "pull",
            listOf(
                slot(listOf("pull-up", "lat-pulldown", "assisted-pull-up-band"), "pull", "latissimus_dorsi"),
                slot(listOf("barbell-row", "one-arm-dumbbell-row"), "pull", "latissimus_dorsi"),
                slot(listOf("seated-cable-row", "chest-supported-row"), "pull", "rhomboids"),
                slot(listOf("face-pull", "band-pull-apart"), "pull", "rear_deltoid"),
                slot(listOf("dumbbell-curl", "barbell-curl"), "pull", "biceps")
            )
        ),
        SessionBlueprint(
            "program_session_legs",
            "legs",
            listOf(
                slot(listOf("back-squat", "goblet-squat", "leg-press"), "squat", "quadriceps"),
                slot(listOf("romanian-deadlift-barbell", "romanian-deadlift-dumbbell"), "hinge", "hamstrings"),
                slot(listOf("bulgarian-split-squat", "walking-lunge"), "lunge", "gluteus_maximus"),
                slot(listOf("leg-curl-machine", "nordic-curl-assisted"), "hinge", "hamstrings"),
                slot(listOf("calf-raise-standing"), "other", "calves")
            )
        )
    )

    private fun broSplit(): List<SessionBlueprint> = listOf(
        SessionBlueprint(
            "program_session_chest",
            "chest",
            listOf(
                slot(listOf("barbell-bench-press", "dumbbell-bench-press"), "push", "pectoralis_major"),
                slot(listOf("dumbbell-incline-press", "push-up"), "push", "pectoralis_major"),
                slot(listOf("cable-chest-fly", "push-up"), "push", "pectoralis_major"),
                slot(listOf("dip-parallel-bar", "bench-dip"), "push", "pectoralis_major", "triceps")
            )
        ),
        SessionBlueprint(
            "program_session_back",
            "back",
            listOf(
                slot(listOf("pull-up", "lat-pulldown", "assisted-pull-up-band"), "pull", "latissimus_dorsi"),
                slot(listOf("barbell-row", "one-arm-dumbbell-row"), "pull", "latissimus_dorsi"),
                slot(listOf("seated-cable-row", "chest-supported-row"), "pull", "rhomboids"),
                slot(listOf("face-pull"), "pull", "rear_deltoid")
            )
        ),
        SessionBlueprint(
            "program_session_shoulders",
            "shoulders",
            listOf(
                slot(listOf("overhead-press-barbell", "overhead-press-dumbbell"), "push", "front_deltoid"),
                slot(listOf("lateral-raise"), "push", "lateral_deltoid"),
                slot(listOf("face-pull", "band-pull-apart"), "pull", "rear_deltoid"),
                slot(listOf("machine-shoulder-press", "push-up"), "push", "front_deltoid")
            )
        ),
        SessionBlueprint(
            "program_session_arms",
            "arms",
            listOf(
                slot(listOf("barbell-curl", "dumbbell-curl"), "pull", "biceps"),
                slot(listOf("tricep-pushdown", "diamond-push-up", "close-grip-bench-press"), "push", "triceps"),
                slot(listOf("chin-up", "assisted-pull-up-band"), "pull", "biceps"),
                slot(listOf("bench-dip", "dip-parallel-bar"), "push", "triceps")
            )
        ),
        SessionBlueprint(
            "program_session_legs",
            "legs",
            listOf(
                slot(listOf("back-squat", "goblet-squat", "leg-press"), "squat", "quadriceps"),
                slot(listOf("romanian-deadlift-barbell", "romanian-deadlift-dumbbell"), "hinge", "hamstrings"),
                slot(listOf("walking-lunge", "bulgarian-split-squat"), "lunge", "gluteus_maximus"),
                slot(listOf("calf-raise-standing"), "other", "calves"),
                slot(listOf("hip-thrust", "glute-bridge"), "hinge", "gluteus_maximus")
            )
        )
    )
}
