package com.omnigart.shared.strength

/**
 * Bundled Explore catalogue: multi-week programmes + categorised ready routines.
 * Generated offline; substitutions use [ExerciseSubstitution] against [BundledStrengthLibrary].
 */
object RoutineLibraryCatalog {
    private fun ex(
        id: String,
        sets: Int,
        low: Int,
        high: Int,
        rest: Int = 60,
        primary: List<String>,
        secondary: List<String> = emptyList()
    ): RoutineExerciseRef {
        val snake = id.replace('-', '_')
        return RoutineExerciseRef(
            exerciseId = id,
            illustrationKey = "ex_$snake",
            nameKey = "exercise.$snake.name",
            sets = sets,
            repsLow = low,
            repsHigh = high,
            restSeconds = rest,
            primaryMuscles = primary,
            secondaryMuscles = secondary
        )
    }

    val routines: List<ReadyRoutine> =
        listOf(
            ReadyRoutine(
                id = "routine-full-body-muscle-builder",
                nameKey = "routine.full_body_muscle_builder.name",
                descriptionKey = "routine.full_body_muscle_builder.description",
                category = RoutineCategory.GYM,
                level = ProgrammeLevel.INTERMEDIATE,
                estimatedMinutes = 45,
                equipmentRequired = listOf("barbell", "bench_flat", "plates"),
                equipmentSummaryKey = "routine.need.barbell_bench",
                exercises =
                listOf(
                    ex("back-squat", 4, 6, 8, 150, listOf("quadriceps", "gluteus_maximus")),
                    ex("barbell-bench-press", 4, 6, 8, 120, listOf("pectoralis_major", "triceps")),
                    ex("barbell-row", 3, 8, 10, 90, listOf("latissimus_dorsi", "rhomboids")),
                    ex("overhead-press-barbell", 3, 6, 8, 90, listOf("front_deltoid")),
                    ex("romanian-deadlift-barbell", 3, 8, 10, 120, listOf("hamstrings", "gluteus_maximus")),
                    ex("plank", 3, 30, 45, 45, listOf("rectus_abdominis", "transverse_abdominis"))
                )
            ),
            ReadyRoutine(
                id = "routine-push-up",
                nameKey = "routine.push_up.name",
                descriptionKey = "routine.push_up.description",
                category = RoutineCategory.BODYWEIGHT,
                level = ProgrammeLevel.BEGINNER,
                estimatedMinutes = 20,
                equipmentRequired = listOf("bodyweight"),
                equipmentSummaryKey = "routine.need.bodyweight_only",
                exercises =
                listOf(
                    ex("incline-push-up", 3, 8, 12, 60, listOf("pectoralis_major")),
                    ex("push-up", 3, 6, 10, 60, listOf("pectoralis_major", "triceps")),
                    ex("wide-push-up", 2, 6, 10, 60, listOf("pectoralis_major")),
                    ex("diamond-push-up", 2, 5, 8, 60, listOf("triceps")),
                    ex("plank", 3, 20, 40, 45, listOf("rectus_abdominis"))
                )
            ),
            ReadyRoutine(
                id = "routine-upper-body-beginner",
                nameKey = "routine.upper_body_beginner.name",
                descriptionKey = "routine.upper_body_beginner.description",
                category = RoutineCategory.AT_HOME,
                level = ProgrammeLevel.BEGINNER,
                estimatedMinutes = 30,
                equipmentRequired = listOf("bodyweight"),
                equipmentSummaryKey = "routine.need.bodyweight_only",
                exercises =
                listOf(
                    ex("knee-push-up", 3, 8, 12, 60, listOf("pectoralis_major")),
                    ex("inverted-row", 3, 6, 10, 60, listOf("latissimus_dorsi", "rhomboids")),
                    ex("pike-push-up", 2, 5, 8, 60, listOf("front_deltoid")),
                    ex("bench-dip", 2, 6, 10, 60, listOf("triceps")),
                    ex("side-plank", 2, 20, 30, 45, listOf("obliques"))
                )
            ),
            ReadyRoutine(
                id = "routine-home-pull",
                nameKey = "routine.home_pull.name",
                descriptionKey = "routine.home_pull.description",
                category = RoutineCategory.AT_HOME,
                level = ProgrammeLevel.INTERMEDIATE,
                estimatedMinutes = 25,
                equipmentRequired = listOf("bodyweight", "pull_up_bar"),
                equipmentSummaryKey = "routine.need.pull_up_bar",
                exercises =
                listOf(
                    ex("pull-up", 4, 3, 8, 90, listOf("latissimus_dorsi")),
                    ex("chin-up", 3, 3, 8, 90, listOf("biceps", "latissimus_dorsi")),
                    ex("inverted-row", 3, 8, 12, 60, listOf("rhomboids")),
                    ex("hollow-hold", 3, 20, 40, 45, listOf("rectus_abdominis"))
                )
            ),
            ReadyRoutine(
                id = "routine-no-equipment-lower",
                nameKey = "routine.no_equipment_lower.name",
                descriptionKey = "routine.no_equipment_lower.description",
                category = RoutineCategory.BODYWEIGHT,
                level = ProgrammeLevel.BEGINNER,
                estimatedMinutes = 25,
                equipmentRequired = listOf("bodyweight"),
                equipmentSummaryKey = "routine.need.bodyweight_only",
                exercises =
                listOf(
                    ex("bodyweight-squat", 3, 10, 15, 60, listOf("quadriceps", "gluteus_maximus")),
                    ex("reverse-lunge-bodyweight", 3, 8, 10, 60, listOf("quadriceps", "gluteus_maximus")),
                    ex("glute-bridge", 3, 12, 15, 45, listOf("gluteus_maximus")),
                    ex("wall-sit", 3, 20, 40, 45, listOf("quadriceps")),
                    ex("calf-raise-bodyweight", 3, 12, 20, 45, listOf("calves"))
                )
            ),
            ReadyRoutine(
                id = "routine-dumbbell-full",
                nameKey = "routine.dumbbell_full.name",
                descriptionKey = "routine.dumbbell_full.description",
                category = RoutineCategory.DUMBBELLS_ONLY,
                level = ProgrammeLevel.BEGINNER,
                estimatedMinutes = 35,
                equipmentRequired = listOf("dumbbell"),
                equipmentSummaryKey = "routine.need.dumbbells",
                exercises =
                listOf(
                    ex("goblet-squat", 3, 8, 12, 75, listOf("quadriceps", "gluteus_maximus")),
                    ex("dumbbell-bench-press", 3, 8, 12, 75, listOf("pectoralis_major")),
                    ex("one-arm-dumbbell-row", 3, 8, 12, 60, listOf("latissimus_dorsi")),
                    ex("overhead-press-dumbbell", 3, 8, 10, 75, listOf("front_deltoid")),
                    ex("romanian-deadlift-dumbbell", 3, 8, 12, 75, listOf("hamstrings"))
                )
            ),
            ReadyRoutine(
                id = "routine-bands-travel",
                nameKey = "routine.bands_travel.name",
                descriptionKey = "routine.bands_travel.description",
                category = RoutineCategory.TRAVEL,
                level = ProgrammeLevel.BEGINNER,
                estimatedMinutes = 25,
                equipmentRequired = listOf("bands", "bodyweight"),
                equipmentSummaryKey = "routine.need.bands",
                exercises =
                listOf(
                    ex("band-pull-apart", 3, 12, 15, 45, listOf("rear_deltoid", "rhomboids")),
                    ex("push-up", 3, 8, 12, 60, listOf("pectoralis_major")),
                    ex("bodyweight-squat", 3, 12, 15, 45, listOf("quadriceps")),
                    ex("glute-bridge", 3, 12, 15, 45, listOf("gluteus_maximus")),
                    ex("plank", 3, 20, 40, 45, listOf("rectus_abdominis"))
                ),
                tags = listOf("travel", "band")
            ),
            ReadyRoutine(
                id = "routine-band-upper",
                nameKey = "routine.band_upper.name",
                descriptionKey = "routine.band_upper.description",
                category = RoutineCategory.BAND,
                level = ProgrammeLevel.INTERMEDIATE,
                estimatedMinutes = 30,
                equipmentRequired = listOf("bands"),
                equipmentSummaryKey = "routine.need.bands",
                exercises =
                listOf(
                    ex("band-pull-apart", 3, 12, 20, 45, listOf("rear_deltoid")),
                    ex("face-pull", 3, 12, 15, 45, listOf("rear_deltoid", "rhomboids")),
                    ex("push-up", 3, 8, 12, 60, listOf("pectoralis_major")),
                    ex("pike-push-up", 2, 6, 10, 60, listOf("front_deltoid"))
                )
            ),
            ReadyRoutine(
                id = "routine-cardio-hiit-15",
                nameKey = "routine.cardio_hiit_15.name",
                descriptionKey = "routine.cardio_hiit_15.description",
                category = RoutineCategory.CARDIO_HIIT,
                level = ProgrammeLevel.INTERMEDIATE,
                estimatedMinutes = 15,
                equipmentRequired = listOf("bodyweight"),
                equipmentSummaryKey = "routine.need.bodyweight_only",
                exercises =
                listOf(
                    ex("jump-squat", 4, 8, 12, 30, listOf("quadriceps", "gluteus_maximus")),
                    ex("burpee", 4, 6, 10, 30, listOf("quadriceps", "pectoralis_major")),
                    ex("mountain-climber", 4, 20, 30, 30, listOf("rectus_abdominis")),
                    ex("plank", 3, 20, 40, 30, listOf("rectus_abdominis"))
                )
            ),
            ReadyRoutine(
                id = "routine-suspension-full",
                nameKey = "routine.suspension_full.name",
                descriptionKey = "routine.suspension_full.description",
                category = RoutineCategory.SUSPENSION,
                level = ProgrammeLevel.INTERMEDIATE,
                estimatedMinutes = 30,
                equipmentRequired = listOf("suspension"),
                equipmentSummaryKey = "routine.need.suspension",
                exercises =
                listOf(
                    ex("inverted-row", 3, 8, 12, 60, listOf("latissimus_dorsi", "rhomboids")),
                    ex("push-up", 3, 8, 12, 60, listOf("pectoralis_major")),
                    ex("split-squat-bodyweight", 3, 8, 10, 60, listOf("quadriceps")),
                    ex("plank", 3, 30, 45, 45, listOf("rectus_abdominis"))
                )
            ),
            ReadyRoutine(
                id = "routine-gym-ppl-push",
                nameKey = "routine.gym_ppl_push.name",
                descriptionKey = "routine.gym_ppl_push.description",
                category = RoutineCategory.GYM,
                level = ProgrammeLevel.INTERMEDIATE,
                estimatedMinutes = 50,
                equipmentRequired = listOf("barbell", "dumbbell", "bench_flat"),
                equipmentSummaryKey = "routine.need.gym_upper",
                exercises =
                listOf(
                    ex("barbell-bench-press", 4, 5, 8, 150, listOf("pectoralis_major")),
                    ex("overhead-press-dumbbell", 3, 8, 10, 90, listOf("front_deltoid")),
                    ex("dumbbell-bench-press", 3, 8, 10, 90, listOf("pectoralis_major")),
                    ex("lateral-raise", 3, 12, 15, 60, listOf("lateral_deltoid")),
                    ex("bench-dip", 3, 10, 12, 60, listOf("triceps"))
                )
            ),
            ReadyRoutine(
                id = "routine-gym-ppl-pull",
                nameKey = "routine.gym_ppl_pull.name",
                descriptionKey = "routine.gym_ppl_pull.description",
                category = RoutineCategory.GYM,
                level = ProgrammeLevel.INTERMEDIATE,
                estimatedMinutes = 50,
                equipmentRequired = listOf("barbell", "cable", "pull_up_bar"),
                equipmentSummaryKey = "routine.need.gym_pull",
                exercises =
                listOf(
                    ex("pull-up", 4, 4, 8, 120, listOf("latissimus_dorsi")),
                    ex("barbell-row", 4, 6, 8, 120, listOf("latissimus_dorsi", "rhomboids")),
                    ex("seated-cable-row", 3, 8, 12, 75, listOf("latissimus_dorsi")),
                    ex("face-pull", 3, 12, 15, 60, listOf("rear_deltoid")),
                    ex("barbell-curl", 3, 8, 12, 60, listOf("biceps"))
                )
            ),
            ReadyRoutine(
                id = "routine-gym-lower",
                nameKey = "routine.gym_lower.name",
                descriptionKey = "routine.gym_lower.description",
                category = RoutineCategory.GYM,
                level = ProgrammeLevel.INTERMEDIATE,
                estimatedMinutes = 50,
                equipmentRequired = listOf("barbell", "squat_rack", "plates"),
                equipmentSummaryKey = "routine.need.squat_rack",
                exercises =
                listOf(
                    ex("back-squat", 4, 5, 8, 180, listOf("quadriceps", "gluteus_maximus")),
                    ex("romanian-deadlift-barbell", 3, 6, 10, 150, listOf("hamstrings")),
                    ex("walking-lunge", 3, 8, 10, 75, listOf("quadriceps")),
                    ex("leg-press", 3, 8, 12, 90, listOf("quadriceps")),
                    ex("calf-raise-standing", 3, 12, 15, 60, listOf("calves"))
                )
            ),
            ReadyRoutine(
                id = "routine-home-advanced-bw",
                nameKey = "routine.home_advanced_bw.name",
                descriptionKey = "routine.home_advanced_bw.description",
                category = RoutineCategory.AT_HOME,
                level = ProgrammeLevel.ADVANCED,
                estimatedMinutes = 40,
                equipmentRequired = listOf("bodyweight"),
                equipmentSummaryKey = "routine.need.bodyweight_only",
                exercises =
                listOf(
                    ex("pike-push-up", 4, 6, 10, 75, listOf("front_deltoid")),
                    ex("decline-push-up", 3, 6, 10, 75, listOf("pectoralis_major")),
                    ex("nordic-curl", 3, 4, 6, 90, listOf("hamstrings")),
                    ex("split-squat-bodyweight", 3, 5, 8, 75, listOf("quadriceps")),
                    ex("hollow-hold", 3, 30, 45, 45, listOf("rectus_abdominis")),
                    ex("burpee", 3, 8, 12, 45, listOf("quadriceps", "pectoralis_major"))
                )
            ),
            ReadyRoutine(
                id = "routine-travel-hotel",
                nameKey = "routine.travel_hotel.name",
                descriptionKey = "routine.travel_hotel.description",
                category = RoutineCategory.TRAVEL,
                level = ProgrammeLevel.BEGINNER,
                estimatedMinutes = 20,
                equipmentRequired = listOf("bodyweight"),
                equipmentSummaryKey = "routine.need.bodyweight_only",
                exercises =
                listOf(
                    ex("bodyweight-squat", 3, 12, 15, 45, listOf("quadriceps")),
                    ex("incline-push-up", 3, 8, 12, 45, listOf("pectoralis_major")),
                    ex("glute-bridge", 3, 12, 15, 45, listOf("gluteus_maximus")),
                    ex("plank", 3, 20, 40, 30, listOf("rectus_abdominis"))
                )
            ),
            ReadyRoutine(
                id = "routine-dumbbell-upper",
                nameKey = "routine.dumbbell_upper.name",
                descriptionKey = "routine.dumbbell_upper.description",
                category = RoutineCategory.DUMBBELLS_ONLY,
                level = ProgrammeLevel.INTERMEDIATE,
                estimatedMinutes = 35,
                equipmentRequired = listOf("dumbbell"),
                equipmentSummaryKey = "routine.need.dumbbells",
                exercises =
                listOf(
                    ex("dumbbell-bench-press", 3, 8, 12, 75, listOf("pectoralis_major")),
                    ex("one-arm-dumbbell-row", 3, 8, 12, 60, listOf("latissimus_dorsi")),
                    ex("overhead-press-dumbbell", 3, 8, 10, 75, listOf("front_deltoid")),
                    ex("lateral-raise", 3, 12, 15, 45, listOf("lateral_deltoid")),
                    ex("dumbbell-curl", 2, 10, 12, 45, listOf("biceps"))
                )
            ),
            ReadyRoutine(
                id = "routine-band-lower",
                nameKey = "routine.band_lower.name",
                descriptionKey = "routine.band_lower.description",
                category = RoutineCategory.BAND,
                level = ProgrammeLevel.BEGINNER,
                estimatedMinutes = 25,
                equipmentRequired = listOf("bands"),
                equipmentSummaryKey = "routine.need.bands",
                exercises =
                listOf(
                    ex("bodyweight-squat", 3, 12, 15, 45, listOf("quadriceps")),
                    ex("glute-bridge", 3, 12, 15, 45, listOf("gluteus_maximus")),
                    ex("reverse-lunge-bodyweight", 3, 8, 10, 45, listOf("quadriceps")),
                    ex("calf-raise-bodyweight", 3, 12, 20, 30, listOf("calves"))
                )
            ),
            ReadyRoutine(
                id = "routine-cardio-hiit-20",
                nameKey = "routine.cardio_hiit_20.name",
                descriptionKey = "routine.cardio_hiit_20.description",
                category = RoutineCategory.CARDIO_HIIT,
                level = ProgrammeLevel.BEGINNER,
                estimatedMinutes = 20,
                equipmentRequired = listOf("bodyweight"),
                equipmentSummaryKey = "routine.need.bodyweight_only",
                exercises =
                listOf(
                    ex("jump-squat", 3, 8, 10, 40, listOf("quadriceps")),
                    ex("mountain-climber", 3, 20, 30, 40, listOf("rectus_abdominis")),
                    ex("burpee", 3, 6, 8, 40, listOf("quadriceps", "pectoralis_major")),
                    ex("plank", 3, 20, 30, 30, listOf("rectus_abdominis"))
                )
            ),
            ReadyRoutine(
                id = "routine-suspension-push",
                nameKey = "routine.suspension_push.name",
                descriptionKey = "routine.suspension_push.description",
                category = RoutineCategory.SUSPENSION,
                level = ProgrammeLevel.BEGINNER,
                estimatedMinutes = 25,
                equipmentRequired = listOf("suspension"),
                equipmentSummaryKey = "routine.need.suspension",
                exercises =
                listOf(
                    ex("push-up", 3, 8, 12, 60, listOf("pectoralis_major")),
                    ex("inverted-row", 3, 8, 12, 60, listOf("latissimus_dorsi")),
                    ex("split-squat-bodyweight", 3, 8, 10, 60, listOf("quadriceps")),
                    ex("plank", 3, 20, 40, 45, listOf("rectus_abdominis"))
                )
            ),
            ReadyRoutine(
                id = "routine-bw-core",
                nameKey = "routine.bw_core.name",
                descriptionKey = "routine.bw_core.description",
                category = RoutineCategory.BODYWEIGHT,
                level = ProgrammeLevel.INTERMEDIATE,
                estimatedMinutes = 15,
                equipmentRequired = listOf("bodyweight"),
                equipmentSummaryKey = "routine.need.bodyweight_only",
                exercises =
                listOf(
                    ex("plank", 3, 30, 45, 30, listOf("rectus_abdominis")),
                    ex("side-plank", 3, 20, 30, 30, listOf("obliques")),
                    ex("hollow-hold", 3, 20, 40, 30, listOf("rectus_abdominis")),
                    ex("mountain-climber", 3, 20, 30, 30, listOf("rectus_abdominis"))
                )
            )
        )

    private fun plan(
        id: String,
        split: ProgrammeSplitKind,
        level: ProgrammeLevel,
        venue: ProgrammeVenue,
        days: Int,
        sessions: List<String>
    ): ProgrammePlan {
        val key =
            when (split) {
                ProgrammeSplitKind.FULL_BODY -> "fb"
                ProgrammeSplitKind.PUSH_PULL_LEGS -> "ppl"
                ProgrammeSplitKind.UPPER_LOWER -> "ul"
            }
        val lvl =
            when (level) {
                ProgrammeLevel.BEGINNER -> "beg"
                ProgrammeLevel.INTERMEDIATE -> "int"
                ProgrammeLevel.ADVANCED -> "adv"
            }
        val ven = if (venue == ProgrammeVenue.HOME) "home" else "gym"
        return ProgrammePlan(
            id = id,
            nameKey = "programme.${key}_${lvl}_$ven.name",
            descriptionKey = "programme.${key}_${lvl}_$ven.description",
            split = split,
            level = level,
            venue = venue,
            daysPerWeek = days,
            sessionRoutineIds = sessions
        )
    }

    val programmes: List<ProgrammePlan> =
        listOf(
            // Full body × 3 levels × 2 venues
            plan(
                "prog-fb-beg-home",
                ProgrammeSplitKind.FULL_BODY,
                ProgrammeLevel.BEGINNER,
                ProgrammeVenue.HOME,
                3,
                listOf("routine-upper-body-beginner", "routine-no-equipment-lower", "routine-push-up")
            ),
            plan(
                "prog-fb-int-home",
                ProgrammeSplitKind.FULL_BODY,
                ProgrammeLevel.INTERMEDIATE,
                ProgrammeVenue.HOME,
                3,
                listOf("routine-home-pull", "routine-dumbbell-full", "routine-home-advanced-bw")
            ),
            plan(
                "prog-fb-adv-home",
                ProgrammeSplitKind.FULL_BODY,
                ProgrammeLevel.ADVANCED,
                ProgrammeVenue.HOME,
                4,
                listOf("routine-home-advanced-bw", "routine-home-pull", "routine-push-up", "routine-no-equipment-lower")
            ),
            plan(
                "prog-fb-beg-gym",
                ProgrammeSplitKind.FULL_BODY,
                ProgrammeLevel.BEGINNER,
                ProgrammeVenue.GYM,
                3,
                listOf("routine-dumbbell-full", "routine-gym-lower", "routine-full-body-muscle-builder")
            ),
            plan(
                "prog-fb-int-gym",
                ProgrammeSplitKind.FULL_BODY,
                ProgrammeLevel.INTERMEDIATE,
                ProgrammeVenue.GYM,
                3,
                listOf("routine-full-body-muscle-builder", "routine-gym-ppl-push", "routine-gym-lower")
            ),
            plan(
                "prog-fb-adv-gym",
                ProgrammeSplitKind.FULL_BODY,
                ProgrammeLevel.ADVANCED,
                ProgrammeVenue.GYM,
                4,
                listOf("routine-full-body-muscle-builder", "routine-gym-ppl-pull", "routine-gym-lower", "routine-gym-ppl-push")
            ),
            // Upper/lower
            plan(
                "prog-ul-beg-home",
                ProgrammeSplitKind.UPPER_LOWER,
                ProgrammeLevel.BEGINNER,
                ProgrammeVenue.HOME,
                4,
                listOf("routine-upper-body-beginner", "routine-no-equipment-lower", "routine-push-up", "routine-no-equipment-lower")
            ),
            plan(
                "prog-ul-int-home",
                ProgrammeSplitKind.UPPER_LOWER,
                ProgrammeLevel.INTERMEDIATE,
                ProgrammeVenue.HOME,
                4,
                listOf("routine-home-pull", "routine-no-equipment-lower", "routine-dumbbell-upper", "routine-band-lower")
            ),
            plan(
                "prog-ul-adv-home",
                ProgrammeSplitKind.UPPER_LOWER,
                ProgrammeLevel.ADVANCED,
                ProgrammeVenue.HOME,
                4,
                listOf("routine-home-advanced-bw", "routine-no-equipment-lower", "routine-home-pull", "routine-home-advanced-bw")
            ),
            plan(
                "prog-ul-beg-gym",
                ProgrammeSplitKind.UPPER_LOWER,
                ProgrammeLevel.BEGINNER,
                ProgrammeVenue.GYM,
                4,
                listOf("routine-dumbbell-upper", "routine-gym-lower", "routine-dumbbell-full", "routine-gym-lower")
            ),
            plan(
                "prog-ul-int-gym",
                ProgrammeSplitKind.UPPER_LOWER,
                ProgrammeLevel.INTERMEDIATE,
                ProgrammeVenue.GYM,
                4,
                listOf("routine-gym-ppl-push", "routine-gym-lower", "routine-gym-ppl-pull", "routine-gym-lower")
            ),
            plan(
                "prog-ul-adv-gym",
                ProgrammeSplitKind.UPPER_LOWER,
                ProgrammeLevel.ADVANCED,
                ProgrammeVenue.GYM,
                4,
                listOf("routine-full-body-muscle-builder", "routine-gym-lower", "routine-gym-ppl-pull", "routine-gym-lower")
            ),
            // Push/pull/legs
            plan(
                "prog-ppl-beg-home",
                ProgrammeSplitKind.PUSH_PULL_LEGS,
                ProgrammeLevel.BEGINNER,
                ProgrammeVenue.HOME,
                3,
                listOf("routine-push-up", "routine-home-pull", "routine-no-equipment-lower")
            ),
            plan(
                "prog-ppl-int-home",
                ProgrammeSplitKind.PUSH_PULL_LEGS,
                ProgrammeLevel.INTERMEDIATE,
                ProgrammeVenue.HOME,
                6,
                listOf(
                    "routine-push-up",
                    "routine-home-pull",
                    "routine-no-equipment-lower",
                    "routine-dumbbell-upper",
                    "routine-band-upper",
                    "routine-band-lower"
                )
            ),
            plan(
                "prog-ppl-adv-home",
                ProgrammeSplitKind.PUSH_PULL_LEGS,
                ProgrammeLevel.ADVANCED,
                ProgrammeVenue.HOME,
                6,
                listOf(
                    "routine-home-advanced-bw",
                    "routine-home-pull",
                    "routine-no-equipment-lower",
                    "routine-push-up",
                    "routine-suspension-full",
                    "routine-home-advanced-bw"
                )
            ),
            plan(
                "prog-ppl-beg-gym",
                ProgrammeSplitKind.PUSH_PULL_LEGS,
                ProgrammeLevel.BEGINNER,
                ProgrammeVenue.GYM,
                3,
                listOf("routine-gym-ppl-push", "routine-gym-ppl-pull", "routine-gym-lower")
            ),
            plan(
                "prog-ppl-int-gym",
                ProgrammeSplitKind.PUSH_PULL_LEGS,
                ProgrammeLevel.INTERMEDIATE,
                ProgrammeVenue.GYM,
                6,
                listOf(
                    "routine-gym-ppl-push",
                    "routine-gym-ppl-pull",
                    "routine-gym-lower",
                    "routine-gym-ppl-push",
                    "routine-gym-ppl-pull",
                    "routine-gym-lower"
                )
            ),
            plan(
                "prog-ppl-adv-gym",
                ProgrammeSplitKind.PUSH_PULL_LEGS,
                ProgrammeLevel.ADVANCED,
                ProgrammeVenue.GYM,
                6,
                listOf(
                    "routine-gym-ppl-push",
                    "routine-gym-ppl-pull",
                    "routine-gym-lower",
                    "routine-full-body-muscle-builder",
                    "routine-gym-ppl-pull",
                    "routine-gym-lower"
                )
            )
        )

    fun routineById(id: String): ReadyRoutine? = routines.firstOrNull { it.id == id }

    fun programmeById(id: String): ProgrammePlan? = programmes.firstOrNull { it.id == id }

    fun routinesIn(category: RoutineCategory): List<ReadyRoutine> = routines.filter { it.category == category }

    fun programmes(split: ProgrammeSplitKind? = null, level: ProgrammeLevel? = null, venue: ProgrammeVenue? = null): List<ProgrammePlan> =
        programmes.filter {
            (split == null || it.split == split) &&
                (level == null || it.level == level) &&
                (venue == null || it.venue == venue)
        }
}
