package com.omnigart.planning

import com.omnigart.content.Level
import com.omnigart.content.TrainingGoal

/**
 * Deterministic rule-based weekly planner. No network I/O.
 */
class RuleBasedPlanGenerator : PlanGenerator {
    override val id: String = "rule-based-v1"

    override fun generate(input: PlanGenerationInput): PlanRecommendation {
        val profile = input.profile
        val level = profile.levels.firstOrNull()?.level ?: Level.BEGINNER
        val primaryGoal = profile.goals.firstOrNull() ?: TrainingGoal.GENERAL_FITNESS
        val deload = profile.hardWeeksSinceDeload >= 6 ||
            profile.readiness == ReadinessBand.VERY_LOW

        val rawTargets = VolumeLandmarks.defaultTargets(level, primaryGoal)
        val volumeTargets = rawTargets.map {
            val adjusted = VolumeLandmarks.applyDeload(
                VolumeLandmarks.applyReadiness(it.hardSets, profile.readiness),
                deload,
            )
            it.copy(hardSets = adjusted)
        }

        val explanations = mutableListOf<PlanExplanation>()
        explanations += PlanExplanation(
            code = "volume.landmarks",
            message = "Weekly hard-set targets follow graded volume–hypertrophy evidence with diminishing returns.",
            evidence = EvidenceCatalog.SCHOENFELD_VOLUME_2016,
        )
        explanations += PlanExplanation(
            code = "volume.fractional",
            message = "Targets interpreted with awareness of fractional set counting and diminishing returns.",
            evidence = EvidenceCatalog.PELLAND_DOSE_2025,
        )
        explanations += PlanExplanation(
            code = "frequency.spread",
            message = "Sessions spread so major muscles are trained about twice weekly when days allow; frequency mainly spreads volume.",
            evidence = EvidenceCatalog.SCHOENFELD_FREQ_2019,
        )
        if (profile.availableDaysPerWeek >= 2) {
            explanations += PlanExplanation(
                code = "frequency.minimum",
                message = "At least two weekly exposures preferred when comparing unequal volumes historically.",
                evidence = EvidenceCatalog.SCHOENFELD_FREQ_2016,
            )
        }

        val rir = rirBand(primaryGoal, deload, profile.readiness)
        explanations += PlanExplanation(
            code = "intensity.rir",
            message = "Working-set RIR band set to $rir for goal=$primaryGoal (deload=$deload).",
            evidence = EvidenceCatalog.ACSM_RT_2026,
        )

        if (primaryGoal == TrainingGoal.ENDURANCE || primaryGoal == TrainingGoal.RACE_PREP) {
            explanations += PlanExplanation(
                code = "endurance.polarized",
                message = "Endurance days bias easy volume with limited high-intensity; 80/20 is descriptive, not universal law.",
                evidence = EvidenceCatalog.SEILER_2010,
            )
            explanations += PlanExplanation(
                code = "endurance.mileage_caveat",
                message = "Weekly running progression uses a soft ~10% ceiling only as a caution flag — evidence is weak; readiness wins.",
                evidence = EvidenceCatalog.TEN_PERCENT_CAVEAT,
            )
        }

        if (deload) {
            explanations += PlanExplanation(
                code = "deload.active",
                message = "Deload active: volume cut ~45% and RIR raised after accumulated hard weeks or very low readiness.",
                evidence = EvidenceCatalog.ACSM_GARBER_2011,
            )
        }

        val sessions = buildSessions(profile, primaryGoal, level, rir, deload, input.allowedExerciseIds)
        return PlanRecommendation(
            generatorId = id,
            weekNumber = profile.hardWeeksSinceDeload + 1,
            isDeload = deload,
            volumeTargets = volumeTargets,
            sessions = sessions,
            explanations = explanations,
            intensityNotes = explanations.filter { it.code.startsWith("intensity") || it.code.startsWith("endurance") },
        )
    }

    private fun rirBand(goal: TrainingGoal, deload: Boolean, readiness: ReadinessBand): String {
        if (deload || readiness == ReadinessBand.LOW || readiness == ReadinessBand.VERY_LOW) return "3-5"
        return when (goal) {
            TrainingGoal.HYPERTROPHY, TrainingGoal.FAT_LOSS_SUPPORT -> "1-3"
            TrainingGoal.STRENGTH -> "2-4"
            else -> "2-4"
        }
    }

    private fun buildSessions(
        profile: UserTrainingProfile,
        goal: TrainingGoal,
        level: Level,
        rir: String,
        deload: Boolean,
        allowed: Set<String>?,
    ): List<PlannedSession> {
        val days = profile.availableDaysPerWeek.coerceIn(1, 6)
        val setsMain = if (deload) 2 else if (level == Level.BEGINNER) 3 else 3
        val templates = sessionBlueprints(goal, days)
        return templates.mapIndexed { index, blueprint ->
            val prescriptions = blueprint.exercises
                .filter { allowed == null || it in allowed }
                .map { id ->
                    PlannedSetPrescription(
                        exerciseId = id,
                        sets = setsMain,
                        reps = blueprint.reps,
                        rir = rir,
                        restSeconds = blueprint.rest,
                    )
                }
            PlannedSession(
                dayIndex = index,
                title = blueprint.title,
                focus = blueprint.focus,
                prescriptions = prescriptions,
                explanations = listOf(
                    PlanExplanation(
                        code = "session.pattern",
                        message = blueprint.why,
                        evidence = blueprint.evidence,
                    ),
                ),
            )
        }
    }

    private data class SessionBlueprint(
        val title: String,
        val focus: String,
        val exercises: List<String>,
        val reps: String,
        val rest: Int,
        val why: String,
        val evidence: EvidencePointer,
    )

    private fun sessionBlueprints(goal: TrainingGoal, days: Int): List<SessionBlueprint> {
        val fullBody = listOf(
            SessionBlueprint(
                title = "Full body A",
                focus = "squat_push_hinge",
                exercises = listOf(
                    "goblet-squat",
                    "push-up",
                    "romanian-deadlift-dumbbell",
                    "one-arm-dumbbell-row",
                    "plank",
                ),
                reps = "8-12",
                rest = 90,
                why = "Full-body frequency spreads weekly volume across ≥2 exposures when training 3 days.",
                evidence = EvidenceCatalog.SCHOENFELD_FREQ_2016,
            ),
            SessionBlueprint(
                title = "Full body B",
                focus = "lunge_pull_push",
                exercises = listOf(
                    "reverse-lunge-dumbbell",
                    "lat-pulldown",
                    "dumbbell-bench-press",
                    "hip-thrust",
                    "dead-bug",
                ),
                reps = "8-12",
                rest = 90,
                why = "Alternating full-body days keep pattern coverage without specializing early.",
                evidence = EvidenceCatalog.ACSM_RT_2026,
            ),
            SessionBlueprint(
                title = "Full body C",
                focus = "hinge_carry_mobility",
                exercises = listOf(
                    "kettlebell-swing",
                    "overhead-press-dumbbell",
                    "farmers-carry",
                    "face-pull",
                    "bird-dog",
                ),
                reps = "8-12",
                rest = 90,
                why = "Third exposure adds carry/rotation balance and posterior chain volume.",
                evidence = EvidenceCatalog.SCHOENFELD_VOLUME_2016,
            ),
        )

        val upperLower = listOf(
            SessionBlueprint(
                title = "Upper A",
                focus = "push_pull",
                exercises = listOf("barbell-bench-press", "chest-supported-row", "overhead-press-barbell", "lat-pulldown", "tricep-pushdown"),
                reps = "5-8",
                rest = 120,
                why = "Upper/lower split concentrates volume while retaining ~2×/week muscle frequency on 4 days.",
                evidence = EvidenceCatalog.SCHOENFELD_FREQ_2016,
            ),
            SessionBlueprint(
                title = "Lower A",
                focus = "squat_hinge",
                exercises = listOf("back-squat", "romanian-deadlift-barbell", "walking-lunge", "leg-curl-machine", "calf-raise-standing"),
                reps = "5-8",
                rest = 150,
                why = "Lower day prioritises squat/hinge compounds per ACSM RT guidance.",
                evidence = EvidenceCatalog.ACSM_RT_2026,
            ),
            SessionBlueprint(
                title = "Upper B",
                focus = "pull_push",
                exercises = listOf("pull-up", "dumbbell-incline-press", "seated-cable-row", "lateral-raise", "barbell-curl"),
                reps = "8-12",
                rest = 90,
                why = "Second upper day spreads pulling/pushing volume.",
                evidence = EvidenceCatalog.PELLAND_DOSE_2025,
            ),
            SessionBlueprint(
                title = "Lower B",
                focus = "hinge_single_leg",
                exercises = listOf("trap-bar-deadlift", "bulgarian-split-squat", "leg-press", "nordic-curl-assisted", "cable-woodchop"),
                reps = "6-10",
                rest = 120,
                why = "Single-leg and hinge variety for robust lower-limb volume.",
                evidence = EvidenceCatalog.SCHOENFELD_VOLUME_2016,
            ),
        )

        val endurance = listOf(
            SessionBlueprint(
                title = "Easy run",
                focus = "zone2",
                exercises = listOf("easy-run", "worlds-greatest-stretch"),
                reps = "time",
                rest = 0,
                why = "Majority easy aerobic volume aligns with polarised descriptive distributions.",
                evidence = EvidenceCatalog.SEILER_2010,
            ),
            SessionBlueprint(
                title = "Quality run",
                focus = "intervals",
                exercises = listOf("interval-run", "a-skip", "walking-lunge"),
                reps = "intervals",
                rest = 60,
                why = "Limited high-intensity stimulus; keep most weekly time easy.",
                evidence = EvidenceCatalog.MUNOZ_POLARIZED,
            ),
            SessionBlueprint(
                title = "Technique / mobility",
                focus = "drills",
                exercises = listOf("high-knees-drill", "butt-kicks", "downward-dog", "cat-cow"),
                reps = "drill",
                rest = 30,
                why = "Easy day substitutes drills/mobility when readiness is managed.",
                evidence = EvidenceCatalog.ACSM_GARBER_2011,
            ),
        )

        val pool = listOf(
            SessionBlueprint(
                title = "Swim technique",
                focus = "freestyle_drills",
                exercises = listOf("freestyle-stroke", "catch-up-drill", "kickboard-kick", "pull-buoy-pull"),
                reps = "distance",
                rest = 30,
                why = "Technique block before adding yardage aggressively.",
                evidence = EvidenceCatalog.ACSM_GARBER_2011,
            ),
        )

        return when {
            goal == TrainingGoal.ENDURANCE || goal == TrainingGoal.RACE_PREP -> endurance.take(days.coerceAtMost(3))
            goal == TrainingGoal.TECHNIQUE && days <= 3 -> pool.take(days)
            days <= 3 || goal == TrainingGoal.GENERAL_FITNESS -> fullBody.take(days)
            else -> upperLower.take(days)
        }.let { list ->
            if (list.size >= days) list.take(days)
            else (list + fullBody).take(days)
        }
    }
}
