package com.omnigart.shared.planner

import com.omnigart.planning.AdaptiveSuggestion
import com.omnigart.planning.AdaptiveSuggestionAction
import com.omnigart.planning.AdaptiveSuggestionContext
import com.omnigart.planning.AdaptiveSuggestionEngine
import com.omnigart.planning.CompletedSessionSnapshot
import com.omnigart.planning.PlannedSessionSnapshot
import com.omnigart.planning.PlannerCalendar
import com.omnigart.planning.PlannerIntensity
import com.omnigart.planning.PlannerReadinessBand
import com.omnigart.planning.PlannerSessionKind
import com.omnigart.planning.SuggestedSchemeGenerator
import com.omnigart.planning.SuggestedSchemeRequest
import com.omnigart.shared.calc.Readiness
import com.omnigart.shared.calc.ReadinessBand
import com.omnigart.shared.calc.ReadinessInput
import com.omnigart.shared.calc.ReadinessResult
import com.omnigart.shared.health.HealthWorkout
import com.omnigart.shared.health.WorkoutActivityType
import com.omnigart.shared.health.store.LocalHealthStore
import com.omnigart.shared.health.store.LocalHealthWorkout
import com.omnigart.shared.insights.FitnessBanister
import com.omnigart.shared.insights.TrainingLoadRatio
import com.omnigart.shared.workoutkit.CompletedWorkoutLoad
import kotlin.time.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn

/**
 * On-device Planner facade: calendar, suggested weekly scheme, adaptive suggestions.
 * Planned sessions live in app storage only — never writes or deletes Health / Health Connect.
 */
class PlannerService(
    private val planned: PlannedWorkoutRepository,
    private val localHealth: LocalHealthStore,
    private val schemeGenerator: SuggestedSchemeGenerator = SuggestedSchemeGenerator(),
    private val suggestionEngine: AdaptiveSuggestionEngine = AdaptiveSuggestionEngine(),
    private val clock: Clock = Clock.System
) {
    fun listPlanned(profileId: String): List<PlannedWorkout> = planned.forProfile(profileId)

    fun upsertPlanned(workout: PlannedWorkout): PlannedWorkout {
        planned.upsert(workout.copy(updatedAt = clock.now()))
        return planned.get(workout.id) ?: workout
    }

    fun movePlanned(id: String, newDateIso: String): PlannedWorkout? {
        val existing = planned.get(id) ?: return null
        val moved = existing.copy(scheduledDateIso = newDateIso, updatedAt = clock.now())
        planned.upsert(moved)
        return moved
    }

    fun deletePlanned(id: String) {
        planned.delete(id)
    }

    fun addSession(
        profileId: String,
        dateIso: String,
        kind: PlannerSessionKind,
        intensity: PlannerIntensity = PlannerIntensity.MODERATE,
        titleKey: String = titleKeyFor(kind),
        muscleGroups: List<String> = defaultMuscles(kind),
        notes: String? = null
    ): PlannedWorkout {
        val workout =
            PlannedWorkout(
                id = "plan-${clock.now().toEpochMilliseconds()}-${kind.name.lowercase()}",
                profileId = profileId,
                scheduledDateIso = dateIso,
                titleKey = titleKey,
                kind = kind,
                intensity = intensity,
                muscleGroups = muscleGroups,
                notes = notes,
                source = PlannedWorkoutSource.USER,
                updatedAt = clock.now()
            )
        planned.upsert(workout)
        return workout
    }

    /** Swift-friendly overload (Kotlin defaults are not exported to Apple). */
    fun addSession(profileId: String, dateIso: String, kind: PlannerSessionKind, intensity: PlannerIntensity): PlannedWorkout = addSession(
        profileId = profileId,
        dateIso = dateIso,
        kind = kind,
        intensity = intensity,
        titleKey = titleKeyFor(kind),
        muscleGroups = defaultMuscles(kind),
        notes = null
    )

    /**
     * Generate a weekly scheme from goals / equipment / programmes and persist
     * as app-local planned sessions (editable afterwards).
     */
    fun generateAndPersistScheme(profileId: String, options: GenerateSchemeOptions): List<PlannedWorkout> {
        val scheme =
            schemeGenerator.generate(
                SuggestedSchemeRequest(
                    weekStartIso = options.weekStartIso,
                    availableDaysPerWeek = options.availableDaysPerWeek,
                    primaryGoal = options.primaryGoal,
                    split = options.split,
                    includeRuns = options.includeRuns,
                    includeTreadmill = options.includeTreadmill,
                    includeZone2 = options.includeZone2,
                    hasStrengthEquipment = options.hasStrengthEquipment
                )
            )
        val weekEnd = LocalDate.parse(options.weekStartIso).plus(6, DateTimeUnit.DAY).toString()
        if (options.replaceWeek) {
            planned.inRange(profileId, options.weekStartIso, weekEnd).forEach { planned.delete(it.id) }
        }
        val now = clock.now()
        return scheme.sessions.map { session ->
            val workout =
                PlannedWorkout(
                    id = "scheme-$profileId-${session.dateIso}-${session.kind.name}-${session.dayOffset}",
                    profileId = profileId,
                    scheduledDateIso = session.dateIso,
                    titleKey = session.titleKey,
                    kind = session.kind,
                    intensity = session.intensity,
                    muscleGroups = session.muscleGroups,
                    notes = session.explanationCode,
                    source = PlannedWorkoutSource.SUGGESTED_SCHEME,
                    updatedAt = now
                )
            planned.upsert(workout)
            workout
        }
    }

    fun calendar(
        profileId: String,
        selectedDateIso: String,
        timeZone: TimeZone
    ): PlannerCalendarSnapshot {
        val selected = LocalDate.parse(selectedDateIso)
        val weekStart = selected.minus((selected.dayOfWeek.ordinal).toLong(), DateTimeUnit.DAY)
        val weekStartIso = weekStart.toString()
        val monthStart = LocalDate(selected.year, selected.month, 1)
        val monthEnd = monthStart.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY)
        val rangeStart = weekStart.minus(7, DateTimeUnit.DAY)
        val rangeEnd = monthEnd.plus(7, DateTimeUnit.DAY)

        val plannedInRange = planned.inRange(profileId, rangeStart.toString(), rangeEnd.toString())
        val plannedDates = plannedInRange.map { it.scheduledDateIso }.toSet()
        val completed = completedInRange(rangeStart, rangeEnd, timeZone)
        val completedDates = completed.map { it.dateIso }.toSet()

        val week =
            PlannerCalendar.weekStrip(
                weekStartIso = weekStartIso,
                plannedDates = plannedDates,
                completedDates = completedDates
            )
        val month =
            PlannerCalendar.monthGrid(
                year = selected.year,
                month = selected.month.ordinal + 1,
                plannedDates = plannedDates,
                completedDates = completedDates
            )
        return PlannerCalendarSnapshot(
            week = week,
            month = month,
            year = selected.year,
            monthNumber = selected.month.ordinal + 1,
            weekStartIso = weekStartIso,
            selectedDateIso = selectedDateIso,
            plannedForSelected = plannedInRange.filter { it.scheduledDateIso == selectedDateIso },
            completedCountForSelected = completed.count { it.dateIso == selectedDateIso }
        )
    }

    /** Swift-friendly overload. */
    fun calendar(profileId: String, selectedDateIso: String): PlannerCalendarSnapshot =
        calendar(profileId, selectedDateIso, TimeZone.currentSystemDefault())

    fun suggestions(
        profileId: String,
        todayIso: String,
        timeZone: TimeZone,
        readinessInput: ReadinessInput? = null,
        hrvZScore: Double? = null,
        restingHrDeltaBpm: Double? = null,
        sleepRatio: Double? = null
    ): PlannerSuggestionsSnapshot {
        val today = LocalDate.parse(todayIso)
        val lookbackStart = today.minus(28, DateTimeUnit.DAY)
        val lookAheadEnd = today.plus(14, DateTimeUnit.DAY)
        val plannedSessions =
            planned.inRange(profileId, todayIso, lookAheadEnd.toString()).map {
                PlannedSessionSnapshot(
                    id = it.id,
                    dateIso = it.scheduledDateIso,
                    kind = it.kind,
                    intensity = it.intensity,
                    muscleGroups = it.muscleGroups,
                    titleKey = it.titleKey
                )
            }
        val completedDays = completedInRange(lookbackStart, today, timeZone)
        val completedSnapshots =
            completedDays.map {
                CompletedSessionSnapshot(
                    dateIso = it.dateIso,
                    kind = it.kind,
                    intensity = it.intensity,
                    muscleGroups = it.muscleGroups,
                    load = it.load
                )
            }
        val ratio = acuteChronicRatio(completedDays)
        val readinessBand = mapReadiness(readinessInput)
        val result =
            suggestionEngine.suggest(
                AdaptiveSuggestionContext(
                    todayIso = todayIso,
                    planned = plannedSessions,
                    completedRecent = completedSnapshots,
                    acuteChronicRatio = ratio,
                    readiness = readinessBand,
                    hrvZScore = hrvZScore,
                    restingHrDeltaBpm = restingHrDeltaBpm,
                    sleepRatio = sleepRatio,
                    dismissedSuggestionIds = planned.dismissedSuggestionIds()
                )
            )
        return PlannerSuggestionsSnapshot(result.suggestions)
    }

    /** Swift-friendly overload (system time zone, today). */
    fun suggestions(profileId: String): PlannerSuggestionsSnapshot {
        val zone = TimeZone.currentSystemDefault()
        return suggestions(
            profileId = profileId,
            todayIso = clock.todayIn(zone).toString(),
            timeZone = zone
        )
    }

    fun acceptSuggestion(profileId: String, suggestion: AdaptiveSuggestion): PlannedWorkout? {
        planned.recordDecision(
            id = "decision-${suggestion.id}-accepted",
            suggestionId = suggestion.id,
            decision = PlannerSuggestionDecisionKind.ACCEPTED,
            at = clock.now()
        )
        val target = planned.get(suggestion.targetSessionId) ?: return null
        return when (suggestion.action) {
            AdaptiveSuggestionAction.SKIP -> {
                planned.delete(target.id)
                null
            }
            AdaptiveSuggestionAction.MOVE -> {
                val date = suggestion.proposedDateIso ?: return target
                movePlanned(target.id, date)
            }
            AdaptiveSuggestionAction.SWAP -> {
                val kind = suggestion.proposedKind ?: PlannerSessionKind.ZONE2
                val intensity = suggestion.proposedIntensity ?: PlannerIntensity.EASY
                val swapped =
                    target.copy(
                        kind = kind,
                        intensity = intensity,
                        titleKey = titleKeyFor(kind),
                        muscleGroups = if (kind == PlannerSessionKind.ZONE2 ||
                            kind == PlannerSessionKind.REST
                        ) {
                            emptyList()
                        } else {
                            target.muscleGroups
                        },
                        source = PlannedWorkoutSource.ADAPTIVE,
                        updatedAt = clock.now()
                    )
                planned.upsert(swapped)
                swapped
            }
        }
    }

    fun dismissSuggestion(suggestion: AdaptiveSuggestion) {
        planned.recordDecision(
            id = "decision-${suggestion.id}-dismissed",
            suggestionId = suggestion.id,
            decision = PlannerSuggestionDecisionKind.DISMISSED,
            at = clock.now()
        )
    }

    fun todayIso(timeZone: TimeZone): String = clock.todayIn(timeZone).toString()

    /** Swift-friendly overload. */
    fun todayIso(): String = todayIso(TimeZone.currentSystemDefault())

    fun mondayOfWeekContaining(dateIso: String): String {
        val date = LocalDate.parse(dateIso)
        return date.minus(date.dayOfWeek.ordinal.toLong(), DateTimeUnit.DAY).toString()
    }

    companion object {
        /**
         * Title keys aligned with PR #84 distinct strength labels:
         * Guided strength / Free strength / Functional.
         */
        fun titleKeyFor(kind: PlannerSessionKind): String = when (kind) {
            PlannerSessionKind.GUIDED_STRENGTH,
            PlannerSessionKind.STRENGTH_FULL_BODY,
            PlannerSessionKind.STRENGTH_SPLIT -> "watch_strength_start"
            PlannerSessionKind.FREE_STRENGTH -> "wear_strength_start"
            // Same en-GB label as watch.workout.strength_functional (PR #84).
            PlannerSessionKind.FUNCTIONAL -> "planner_kind_functional"
            PlannerSessionKind.RUN -> "planner_kind_run"
            PlannerSessionKind.TREADMILL -> "planner_kind_treadmill"
            PlannerSessionKind.ZONE2 -> "planner_kind_zone2"
            PlannerSessionKind.REST -> "planner_kind_rest"
        }

        fun defaultMuscles(kind: PlannerSessionKind): List<String> = when (kind) {
            PlannerSessionKind.GUIDED_STRENGTH,
            PlannerSessionKind.STRENGTH_FULL_BODY ->
                listOf("quads", "chest", "back", "hamstrings", "core")
            PlannerSessionKind.STRENGTH_SPLIT -> listOf("chest", "back", "shoulders")
            PlannerSessionKind.FREE_STRENGTH -> listOf("quads", "chest", "back")
            PlannerSessionKind.FUNCTIONAL -> listOf("core", "glutes", "shoulders")
            PlannerSessionKind.RUN, PlannerSessionKind.TREADMILL -> listOf("quads", "calves")
            PlannerSessionKind.ZONE2, PlannerSessionKind.REST -> emptyList()
        }
    }

    private data class CompletedDay(
        val dateIso: String,
        val kind: PlannerSessionKind,
        val intensity: PlannerIntensity,
        val muscleGroups: List<String>,
        val load: Double
    )

    private fun completedInRange(start: LocalDate, end: LocalDate, timeZone: TimeZone): List<CompletedDay> {
        val startInstant = start.atStartOfDayIn(timeZone)
        val endInstant = end.plus(1, DateTimeUnit.DAY).atStartOfDayIn(timeZone)
        return localHealth
            .listWorkouts()
            .filter { !it.deleted && it.startAt >= startInstant && it.startAt < endInstant }
            .map { workout ->
                val dateIso = workout.startAt.toLocalDateTime(timeZone).date.toString()
                val legacy =
                    HealthWorkout(
                        id = workout.id,
                        activityType = workout.activityType,
                        start = workout.startAt,
                        end = workout.endAt,
                        energyKcal = workout.activeEnergyKcal,
                        distanceMetres = workout.distanceMetres,
                        averageHeartRateBpm = workout.averageHeartRateBpm
                    )
                val load =
                    CompletedWorkoutLoad
                        .fromHealthWorkout(legacy, restingHrBpm = 60.0, maxHrBpm = 190.0)
                        .loadScore
                CompletedDay(
                    dateIso = dateIso,
                    kind = mapActivity(workout.activityType),
                    intensity = intensityFor(workout, load),
                    muscleGroups = musclesFromSets(workout),
                    load = load
                )
            }
    }

    private fun mapActivity(type: WorkoutActivityType): PlannerSessionKind = when (type) {
        WorkoutActivityType.STRENGTH -> PlannerSessionKind.FREE_STRENGTH
        WorkoutActivityType.RUNNING -> PlannerSessionKind.RUN
        WorkoutActivityType.HIIT -> PlannerSessionKind.FUNCTIONAL
        else -> PlannerSessionKind.ZONE2
    }

    private fun intensityFor(workout: LocalHealthWorkout, load: Double): PlannerIntensity {
        val minutes = workout.durationSeconds() / 60.0
        return when {
            load >= 90 || (workout.averageHeartRateBpm != null && workout.averageHeartRateBpm >= 150) ->
                PlannerIntensity.HARD
            minutes <= 35 && load < 45 -> PlannerIntensity.EASY
            else -> PlannerIntensity.MODERATE
        }
    }

    private fun musclesFromSets(workout: LocalHealthWorkout): List<String> {
        if (workout.sets.isEmpty()) return emptyList()
        // Soft heuristic from exercise keys — planner only.
        val keys = workout.sets.map { it.exerciseKey.lowercase() }
        val out = mutableSetOf<String>()
        if (keys.any { it.contains("squat") || it.contains("leg") }) out += "quads"
        if (keys.any { it.contains("dead") || it.contains("hinge") || it.contains("rdl") }) out += "hamstrings"
        if (keys.any { it.contains("bench") || it.contains("press") || it.contains("push") }) out += "chest"
        if (keys.any { it.contains("row") || it.contains("pull") || it.contains("lat") }) out += "back"
        if (keys.any { it.contains("shoulder") || it.contains("overhead") }) out += "shoulders"
        if (keys.any { it.contains("curl") || it.contains("tricep") || it.contains("arm") }) out += "arms"
        if (keys.any { it.contains("core") || it.contains("plank") || it.contains("ab") }) out += "core"
        return out.toList()
    }

    private fun acuteChronicRatio(completed: List<CompletedDay>): Double? {
        if (completed.isEmpty()) return null
        val byDay =
            completed
                .groupBy { it.dateIso }
                .mapValues { (_, rows) -> rows.sumOf { it.load } }
        val dayIndexes =
            byDay.keys.map { LocalDate.parse(it).toEpochDays() to (byDay[it] ?: 0.0) }.toMap()
        if (dayIndexes.isEmpty()) return null
        val series = FitnessBanister.compute(dayIndexes, projectDays = 0)
        val result = TrainingLoadRatio.compute(series.currentAtl, series.currentCtl)
        return result.ratio
    }

    private fun mapReadiness(input: ReadinessInput?): PlannerReadinessBand {
        if (input == null) return PlannerReadinessBand.UNKNOWN
        return when (val result = Readiness.evaluate(input)) {
            is ReadinessResult.InsufficientData -> PlannerReadinessBand.UNKNOWN
            is ReadinessResult.Scored ->
                when (result.band) {
                    ReadinessBand.GO -> PlannerReadinessBand.HIGH
                    ReadinessBand.MODERATE -> PlannerReadinessBand.NORMAL
                    ReadinessBand.EASY -> PlannerReadinessBand.LOW
                    ReadinessBand.REST -> PlannerReadinessBand.REST
                    ReadinessBand.INSUFFICIENT_DATA -> PlannerReadinessBand.UNKNOWN
                }
        }
    }
}
