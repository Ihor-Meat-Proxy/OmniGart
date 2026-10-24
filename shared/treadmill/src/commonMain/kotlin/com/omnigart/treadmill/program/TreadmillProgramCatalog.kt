package com.omnigart.treadmill.program

/** Built-in library of treadmill run and walk programs (i18n keys only). */
object TreadmillProgramCatalog {
    val all: List<TreadmillProgram> by lazy {
        listOf(
            easyRun,
            intervals,
            hillIncline,
            tempo,
            fartlek,
            progression,
            walkRunBeginner,
            recoveryWalk
        )
    }

    fun byId(id: String): TreadmillProgram? = all.firstOrNull { it.id == id }

    fun forMode(mode: TreadmillActivityMode): List<TreadmillProgram> =
        all.filter { it.mode == mode }

    private val easyRun =
        TreadmillProgram(
            id = "easy_run",
            titleKey = "treadmill_program_easy_run_title",
            bodyKey = "treadmill_program_easy_run_body",
            mode = TreadmillActivityMode.RUN,
            steps =
                listOf(
                    step("wu", "treadmill_step_warmup", 300, speed = 7.0, incline = 0.0, zone = 2),
                    step("work", "treadmill_step_easy", 1200, speed = 9.0, incline = 0.0, zone = 2),
                    step("cd", "treadmill_step_cooldown", 300, speed = 6.0, incline = 0.0, zone = 1)
                )
        )

    private val intervals =
        TreadmillProgram(
            id = "intervals",
            titleKey = "treadmill_program_intervals_title",
            bodyKey = "treadmill_program_intervals_body",
            mode = TreadmillActivityMode.RUN,
            steps =
                listOf(
                    step("wu", "treadmill_step_warmup", 300, speed = 7.0, incline = 0.0, zone = 2),
                    step("on1", "treadmill_step_hard", 90, speed = 12.0, incline = 0.0, zone = 4),
                    step("off1", "treadmill_step_recover", 90, speed = 7.0, incline = 0.0, zone = 2),
                    step("on2", "treadmill_step_hard", 90, speed = 12.0, incline = 0.0, zone = 4),
                    step("off2", "treadmill_step_recover", 90, speed = 7.0, incline = 0.0, zone = 2),
                    step("on3", "treadmill_step_hard", 90, speed = 12.0, incline = 0.0, zone = 4),
                    step("off3", "treadmill_step_recover", 90, speed = 7.0, incline = 0.0, zone = 2),
                    step("on4", "treadmill_step_hard", 90, speed = 12.0, incline = 0.0, zone = 4),
                    step("off4", "treadmill_step_recover", 90, speed = 7.0, incline = 0.0, zone = 2),
                    step("cd", "treadmill_step_cooldown", 300, speed = 6.0, incline = 0.0, zone = 1)
                )
        )

    private val hillIncline =
        TreadmillProgram(
            id = "hill_incline",
            titleKey = "treadmill_program_hill_title",
            bodyKey = "treadmill_program_hill_body",
            mode = TreadmillActivityMode.RUN,
            steps =
                listOf(
                    step("wu", "treadmill_step_warmup", 240, speed = 7.0, incline = 0.0, zone = 2),
                    step("h1", "treadmill_step_hill", 120, speed = 8.0, incline = 4.0, zone = 3),
                    step("r1", "treadmill_step_recover", 90, speed = 7.0, incline = 1.0, zone = 2),
                    step("h2", "treadmill_step_hill", 120, speed = 8.0, incline = 6.0, zone = 3),
                    step("r2", "treadmill_step_recover", 90, speed = 7.0, incline = 1.0, zone = 2),
                    step("h3", "treadmill_step_hill", 120, speed = 8.0, incline = 8.0, zone = 4),
                    step("cd", "treadmill_step_cooldown", 300, speed = 6.0, incline = 0.0, zone = 1)
                )
        )

    private val tempo =
        TreadmillProgram(
            id = "tempo",
            titleKey = "treadmill_program_tempo_title",
            bodyKey = "treadmill_program_tempo_body",
            mode = TreadmillActivityMode.RUN,
            steps =
                listOf(
                    step("wu", "treadmill_step_warmup", 300, speed = 7.5, incline = 0.0, zone = 2),
                    step("tempo", "treadmill_step_tempo", 1200, speed = 11.0, incline = 0.0, zone = 3),
                    step("cd", "treadmill_step_cooldown", 300, speed = 6.5, incline = 0.0, zone = 1)
                )
        )

    private val fartlek =
        TreadmillProgram(
            id = "fartlek",
            titleKey = "treadmill_program_fartlek_title",
            bodyKey = "treadmill_program_fartlek_body",
            mode = TreadmillActivityMode.RUN,
            steps =
                listOf(
                    step("wu", "treadmill_step_warmup", 300, speed = 7.0, incline = 0.0, zone = 2),
                    step("f1", "treadmill_step_surge", 60, speed = 13.0, incline = 0.0, zone = 4),
                    step("e1", "treadmill_step_easy", 120, speed = 8.5, incline = 0.0, zone = 2),
                    step("f2", "treadmill_step_surge", 45, speed = 14.0, incline = 0.0, zone = 5),
                    step("e2", "treadmill_step_easy", 150, speed = 8.0, incline = 0.0, zone = 2),
                    step("f3", "treadmill_step_surge", 90, speed = 12.5, incline = 1.0, zone = 4),
                    step("e3", "treadmill_step_easy", 180, speed = 8.0, incline = 0.0, zone = 2),
                    step("cd", "treadmill_step_cooldown", 300, speed = 6.0, incline = 0.0, zone = 1)
                )
        )

    private val progression =
        TreadmillProgram(
            id = "progression",
            titleKey = "treadmill_program_progression_title",
            bodyKey = "treadmill_program_progression_body",
            mode = TreadmillActivityMode.RUN,
            steps =
                listOf(
                    step("wu", "treadmill_step_warmup", 300, speed = 7.0, incline = 0.0, zone = 2),
                    step("p1", "treadmill_step_easy", 600, speed = 9.0, incline = 0.0, zone = 2),
                    step("p2", "treadmill_step_build", 600, speed = 10.5, incline = 0.0, zone = 3),
                    step("p3", "treadmill_step_tempo", 600, speed = 11.5, incline = 0.0, zone = 3),
                    step("cd", "treadmill_step_cooldown", 300, speed = 6.5, incline = 0.0, zone = 1)
                )
        )

    private val walkRunBeginner =
        TreadmillProgram(
            id = "walk_run_beginner",
            titleKey = "treadmill_program_walk_run_title",
            bodyKey = "treadmill_program_walk_run_body",
            mode = TreadmillActivityMode.RUN,
            steps =
                listOf(
                    step("wu", "treadmill_step_warmup_walk", 300, speed = 4.5, incline = 0.0, zone = 1),
                    step("r1", "treadmill_step_jog", 60, speed = 7.5, incline = 0.0, zone = 2),
                    step("w1", "treadmill_step_walk", 90, speed = 4.5, incline = 0.0, zone = 1),
                    step("r2", "treadmill_step_jog", 60, speed = 7.5, incline = 0.0, zone = 2),
                    step("w2", "treadmill_step_walk", 90, speed = 4.5, incline = 0.0, zone = 1),
                    step("r3", "treadmill_step_jog", 60, speed = 7.5, incline = 0.0, zone = 2),
                    step("w3", "treadmill_step_walk", 90, speed = 4.5, incline = 0.0, zone = 1),
                    step("r4", "treadmill_step_jog", 60, speed = 7.5, incline = 0.0, zone = 2),
                    step("w4", "treadmill_step_walk", 90, speed = 4.5, incline = 0.0, zone = 1),
                    step("cd", "treadmill_step_cooldown_walk", 300, speed = 4.0, incline = 0.0, zone = 1)
                )
        )

    private val recoveryWalk =
        TreadmillProgram(
            id = "recovery_walk",
            titleKey = "treadmill_program_recovery_walk_title",
            bodyKey = "treadmill_program_recovery_walk_body",
            mode = TreadmillActivityMode.WALK,
            steps =
                listOf(
                    step("wu", "treadmill_step_warmup_walk", 180, speed = 4.0, incline = 0.0, zone = 1),
                    step("work", "treadmill_step_easy_walk", 1200, speed = 5.0, incline = 1.0, zone = 1),
                    step("cd", "treadmill_step_cooldown_walk", 180, speed = 3.5, incline = 0.0, zone = 1)
                )
        )

    private fun step(
        id: String,
        titleKey: String,
        seconds: Int,
        speed: Double?,
        incline: Double?,
        zone: Int?
    ): TreadmillProgramStep =
        TreadmillProgramStep(
            id = id,
            titleKey = titleKey,
            goalKind = TreadmillStepGoalKind.DURATION_SECONDS,
            goalValue = seconds.toDouble(),
            targetSpeedKmh = speed,
            targetInclinePercent = incline,
            targetHrZone = zone
        )
}
