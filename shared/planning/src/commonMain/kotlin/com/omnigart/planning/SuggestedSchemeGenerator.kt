package com.omnigart.planning

/**
 * Deterministic weekly scheme builder for the Planner tab.
 *
 * Combines strength full-body / split templates with optional runs, treadmill,
 * and Zone 2 easy aerobic days based on goals and equipment. Pure and
 * side-effect free — the UI persists edits in app-local storage only.
 */
class SuggestedSchemeGenerator {
    fun generate(request: SuggestedSchemeRequest): SuggestedWeeklyScheme {
        val days = request.availableDaysPerWeek.coerceIn(2, 6)
        val start = IsoDate.parse(request.weekStartIso)
        val split =
            when {
                request.split != PlannerSchemeSplit.MIXED -> request.split
                enduranceGoal(request.primaryGoal) -> PlannerSchemeSplit.ENDURANCE_BIAS
                days >= 4 && request.hasStrengthEquipment -> PlannerSchemeSplit.UPPER_LOWER
                else -> PlannerSchemeSplit.FULL_BODY
            }

        val blueprints = blueprintsFor(split, days, request)
        val sessions =
            blueprints.mapIndexed { index, bp ->
                SuggestedSchemeSession(
                    dayOffset = index,
                    dateIso = start.plusDays(index).toIso(),
                    kind = bp.kind,
                    intensity = bp.intensity,
                    titleKey = bp.titleKey,
                    muscleGroups = bp.muscles,
                    explanationCode = bp.explanationCode
                )
            }

        val codes =
            buildList {
                add("scheme.split.${split.name.lowercase()}")
                if (request.includeZone2) add("scheme.zone2")
                if (request.includeRuns) add("scheme.runs")
                if (request.includeTreadmill) add("scheme.treadmill")
                add("scheme.editable_local")
            }

        return SuggestedWeeklyScheme(
            weekStartIso = request.weekStartIso,
            split = split,
            sessions = sessions,
            explanationCodes = codes
        )
    }

    private fun enduranceGoal(goal: String): Boolean {
        val g = goal.lowercase()
        return g.contains("endurance") || g.contains("race") || g.contains("run")
    }

    private data class Blueprint(
        val kind: PlannerSessionKind,
        val intensity: PlannerIntensity,
        val titleKey: String,
        val muscles: List<String>,
        val explanationCode: String
    )

    private fun blueprintsFor(split: PlannerSchemeSplit, days: Int, request: SuggestedSchemeRequest): List<Blueprint> {
        val strength =
            when (split) {
                PlannerSchemeSplit.UPPER_LOWER ->
                    listOf(
                        Blueprint(
                            PlannerSessionKind.STRENGTH_SPLIT,
                            PlannerIntensity.HARD,
                            // Distinct picker label from PR #84 (programme session).
                            "watch_strength_start",
                            listOf("chest", "back", "shoulders", "arms"),
                            "scheme.strength.split"
                        ),
                        Blueprint(
                            PlannerSessionKind.STRENGTH_SPLIT,
                            PlannerIntensity.HARD,
                            "watch_strength_start",
                            listOf("quads", "hamstrings", "glutes", "calves"),
                            "scheme.strength.split"
                        ),
                        Blueprint(
                            PlannerSessionKind.STRENGTH_SPLIT,
                            PlannerIntensity.MODERATE,
                            "watch_strength_start",
                            listOf("chest", "back", "shoulders", "arms"),
                            "scheme.strength.split"
                        ),
                        Blueprint(
                            PlannerSessionKind.STRENGTH_SPLIT,
                            PlannerIntensity.MODERATE,
                            "watch_strength_start",
                            listOf("quads", "hamstrings", "glutes"),
                            "scheme.strength.split"
                        )
                    )
                PlannerSchemeSplit.FULL_BODY, PlannerSchemeSplit.MIXED ->
                    listOf(
                        Blueprint(
                            PlannerSessionKind.STRENGTH_FULL_BODY,
                            PlannerIntensity.HARD,
                            "watch_strength_start",
                            listOf("quads", "chest", "back", "hamstrings", "core"),
                            "scheme.strength.full_body"
                        ),
                        Blueprint(
                            PlannerSessionKind.STRENGTH_FULL_BODY,
                            PlannerIntensity.MODERATE,
                            "watch_strength_start",
                            listOf("glutes", "shoulders", "back", "core"),
                            "scheme.strength.full_body"
                        ),
                        Blueprint(
                            PlannerSessionKind.STRENGTH_FULL_BODY,
                            PlannerIntensity.MODERATE,
                            "watch_strength_start",
                            listOf("quads", "chest", "hamstrings", "arms"),
                            "scheme.strength.full_body"
                        )
                    )
                PlannerSchemeSplit.ENDURANCE_BIAS ->
                    listOf(
                        Blueprint(
                            PlannerSessionKind.STRENGTH_FULL_BODY,
                            PlannerIntensity.MODERATE,
                            "watch_strength_start",
                            listOf("quads", "core", "back"),
                            "scheme.strength.support"
                        )
                    )
            }

        val cardio = mutableListOf<Blueprint>()
        if (request.includeZone2) {
            cardio +=
                Blueprint(
                    PlannerSessionKind.ZONE2,
                    PlannerIntensity.EASY,
                    "planner_kind_zone2",
                    emptyList(),
                    "scheme.zone2"
                )
        }
        if (request.includeRuns) {
            cardio +=
                Blueprint(
                    PlannerSessionKind.RUN,
                    PlannerIntensity.MODERATE,
                    "planner_kind_run",
                    listOf("quads", "calves"),
                    "scheme.runs"
                )
        }
        if (request.includeTreadmill) {
            cardio +=
                Blueprint(
                    PlannerSessionKind.TREADMILL,
                    PlannerIntensity.MODERATE,
                    "planner_kind_treadmill",
                    listOf("quads", "calves"),
                    "scheme.treadmill"
                )
        }

        val combined =
            when (split) {
                PlannerSchemeSplit.ENDURANCE_BIAS ->
                    (cardio + strength).ifEmpty { strength }
                else ->
                    interleave(strength, cardio)
            }

        return combined.take(days).ifEmpty {
            listOf(
                Blueprint(
                    PlannerSessionKind.STRENGTH_FULL_BODY,
                    PlannerIntensity.MODERATE,
                    "watch_strength_start",
                    listOf("quads", "chest", "back"),
                    "scheme.strength.full_body"
                )
            )
        }
    }

    private fun interleave(a: List<Blueprint>, b: List<Blueprint>): List<Blueprint> {
        val out = mutableListOf<Blueprint>()
        val max = maxOf(a.size, b.size)
        for (i in 0 until max) {
            if (i < a.size) out += a[i]
            if (i < b.size) out += b[i]
        }
        return out
    }
}
