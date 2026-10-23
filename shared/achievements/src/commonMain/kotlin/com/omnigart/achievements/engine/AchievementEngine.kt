package com.omnigart.achievements.engine

import com.omnigart.achievements.events.DomainEvent
import com.omnigart.achievements.events.FoodLogged
import com.omnigart.achievements.events.FreezeTokenUsed
import com.omnigart.achievements.events.MeasurementAdded
import com.omnigart.achievements.events.PlannedRestHonoured
import com.omnigart.achievements.events.SetLogged
import com.omnigart.achievements.events.SleepImported
import com.omnigart.achievements.events.WaterLogged
import com.omnigart.achievements.events.WorkoutCompleted
import com.omnigart.achievements.model.AchievementDefinition
import com.omnigart.achievements.model.Aggregation
import com.omnigart.achievements.model.CalendarUnit
import com.omnigart.achievements.model.ChallengePeriod
import com.omnigart.achievements.model.CompareOp
import com.omnigart.achievements.model.MetricId
import com.omnigart.achievements.model.RuleFilters
import com.omnigart.achievements.model.StreakQualify
import com.omnigart.achievements.model.TierId
import com.omnigart.achievements.model.WindowKind
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlin.time.Instant

/**
 * Incremental, idempotent achievement evaluator.
 *
 * - Day boundaries use each event's [DomainEvent.timeZoneId] (DST-safe via kotlinx-datetime).
 * - Replaying the same [DomainEvent.eventId] is a no-op.
 * - Deleted events revoke their contribution and may drop tiers.
 * - [backfill] suppresses per-unlock notifications and returns a summary instead.
 */
class AchievementEngine(
    private val catalogue: List<AchievementDefinition>,
) {
    data class Result(
        val state: EvaluationState,
        val summary: EvaluationSummary,
    )

    fun evaluate(
        state: EvaluationState,
        events: List<DomainEvent>,
        now: Instant,
        evaluationTimeZoneId: String,
        backfill: Boolean = false,
    ): Result {
        var processed = state.processedEventIds.toMutableSet()
        var revoked = state.revokedEventIds.toMutableSet()
        val progress = state.progressByAchievement.toMutableMap()
        var skipped = 0
        val unlocks = mutableListOf<UnlockNotification>()

        for (event in events) {
            if (event.eventId in processed && !event.deleted) {
                skipped++
                continue
            }
            if (event.deleted) {
                revoked.add(event.eventId)
                processed.add(event.eventId)
                recomputeAll(progress, now, evaluationTimeZoneId, processed, revoked, events)
                continue
            }
            processed.add(event.eventId)
            for (def in catalogue) {
                val before = progress[def.id] ?: AchievementProgress(def.id)
                val after = applyEvent(def, before, event, now, evaluationTimeZoneId)
                if (after != before) {
                    val newUnlocks = detectUnlocks(def, before, after)
                    if (!backfill) unlocks += newUnlocks
                    progress[def.id] = after.copy(progress = tierProgress(def, after.value))
                }
            }
        }

        // Ensure tier progress is fresh
        for (def in catalogue) {
            val p = progress[def.id] ?: continue
            progress[def.id] = p.copy(progress = tierProgress(def, p.value), unlockedTier = tiersReached(def, p.value))
        }

        val summaryUnlocks = if (backfill) {
            progress.values.flatMap { p ->
                val def = catalogue.first { it.id == p.achievementId }
                p.unlockedTier.map { UnlockNotification(p.achievementId, it, p.value) }
            }
        } else {
            unlocks
        }

        val summary = EvaluationSummary(
            processedEvents = events.size - skipped,
            skippedDuplicates = skipped,
            unlocks = if (backfill) emptyList() else unlocks,
            backfill = backfill,
            summaryMessageKey = if (backfill && summaryUnlocks.isNotEmpty()) {
                "achievement.backfill.summary"
            } else {
                null
            },
            summaryUnlockCount = if (backfill) summaryUnlocks.size else unlocks.size,
        )

        return Result(
            state = EvaluationState(
                processedEventIds = processed,
                progressByAchievement = progress,
                revokedEventIds = revoked,
            ),
            summary = summary,
        )
    }

    private fun recomputeAll(
        progress: MutableMap<String, AchievementProgress>,
        now: Instant,
        evaluationTimeZoneId: String,
        processed: Set<String>,
        revoked: Set<String>,
        allEvents: List<DomainEvent>,
    ) {
        // Full recompute from non-deleted, processed events (revocation path).
        progress.clear()
        val active = allEvents.filter { it.eventId in processed && it.eventId !in revoked && !it.deleted }
        for (event in active) {
            for (def in catalogue) {
                val before = progress[def.id] ?: AchievementProgress(def.id)
                progress[def.id] = applyEvent(def, before, event, now, evaluationTimeZoneId)
            }
        }
        for (def in catalogue) {
            val p = progress[def.id] ?: continue
            progress[def.id] = p.copy(
                progress = tierProgress(def, p.value),
                unlockedTier = tiersReached(def, p.value),
            )
        }
    }

    private fun applyEvent(
        def: AchievementDefinition,
        current: AchievementProgress,
        event: DomainEvent,
        now: Instant,
        evaluationTimeZoneId: String,
    ): AchievementProgress {
        if (!matchesFilters(def.rule.filters, event)) return current

        val contribution = metricContribution(def.rule.metric, event) ?: return current
        val localDay = when (event) {
            is FoodLogged -> event.localDay
            is WaterLogged -> event.localDay
            is SleepImported -> event.localDay
            is MeasurementAdded -> event.localDay
            is PlannedRestHonoured -> event.localDay
            is FreezeTokenUsed -> event.localDay
            else -> DayBoundaries.localDayKey(event.occurredAt, event.timeZoneId)
        }

        var qualifying = current.qualifyingDays
        var freezeDays = current.freezeDays
        var plannedRest = current.plannedRestDays
        var distinct = current.distinctKeys

        when (event) {
            is PlannedRestHonoured -> plannedRest = plannedRest + localDay
            is FreezeTokenUsed -> freezeDays = freezeDays + localDay
            else -> Unit
        }

        when (def.rule.window.qualify) {
            StreakQualify.ACTIVITY,
            StreakQualify.LOG,
            StreakQualify.GOAL_MET,
            -> if (contribution.qualifiesDay) qualifying = qualifying + localDay
            StreakQualify.ACTIVITY_OR_PLANNED_REST -> {
                if (contribution.qualifiesDay || event is PlannedRestHonoured) {
                    qualifying = qualifying + localDay
                }
            }
            null -> if (contribution.qualifiesDay) qualifying = qualifying + localDay
        }

        if (contribution.distinctKey != null) {
            distinct = distinct + contribution.distinctKey
        }
        if (def.rule.metric == MetricId.SWIM_STROKES_DISTINCT && event is WorkoutCompleted) {
            distinct = distinct + event.swimStrokes.toSet()
        }

        val value = computeValue(
            def = def,
            current = current.copy(
                qualifyingDays = qualifying,
                freezeDays = freezeDays,
                plannedRestDays = plannedRest,
                distinctKeys = distinct,
            ),
            event = event,
            contribution = contribution,
            now = now,
            evaluationTimeZoneId = evaluationTimeZoneId,
        )

        return current.copy(
            value = value,
            qualifyingDays = qualifying,
            freezeDays = freezeDays,
            plannedRestDays = plannedRest,
            distinctKeys = distinct,
            unlockedTier = tiersReached(def, value),
            progress = tierProgress(def, value),
        )
    }

    private data class Contribution(
        val amount: Double,
        val qualifiesDay: Boolean,
        val distinctKey: String? = null,
    )

    private fun metricContribution(metric: MetricId, event: DomainEvent): Contribution? =
        when (metric) {
            MetricId.WORKOUTS_COMPLETED -> when (event) {
                is WorkoutCompleted -> if (!event.plannedRest) Contribution(1.0, true) else null
                else -> null
            }
            MetricId.SETS_LOGGED -> when (event) {
                is WorkoutCompleted -> Contribution(event.setsCount.toDouble(), event.setsCount > 0)
                is SetLogged -> Contribution(1.0, true)
                else -> null
            }
            MetricId.VOLUME_KG -> when (event) {
                is WorkoutCompleted -> event.volumeKg?.let { Contribution(it, it > 0) }
                is SetLogged -> Contribution(event.volumeKg, event.volumeKg > 0)
                else -> null
            }
            MetricId.SESSION_DURATION_MIN -> when (event) {
                is WorkoutCompleted -> Contribution(event.durationMin, event.durationMin > 0)
                else -> null
            }
            MetricId.DISTANCE_M -> when (event) {
                is WorkoutCompleted -> event.distanceM?.let { Contribution(it, it > 0) }
                else -> null
            }
            MetricId.PACE_SEC_PER_KM -> null // derived PRs via PERSONAL_RECORD_EVENTS
            MetricId.SWIM_STROKES_DISTINCT -> when (event) {
                is WorkoutCompleted -> if (event.swimStrokes.isNotEmpty()) {
                    Contribution(event.swimStrokes.size.toDouble(), true, null)
                } else {
                    null
                }
                else -> null
            }
            MetricId.YOGA_MINUTES -> when (event) {
                is WorkoutCompleted -> if (event.discipline == "yoga") Contribution(event.durationMin, true) else null
                else -> null
            }
            MetricId.PILATES_MINUTES -> when (event) {
                is WorkoutCompleted -> if (event.discipline == "pilates") Contribution(event.durationMin, true) else null
                else -> null
            }
            MetricId.FOOD_DAYS_LOGGED -> when (event) {
                is FoodLogged -> Contribution(1.0, true, event.localDay)
                else -> null
            }
            MetricId.WATER_GOAL_DAYS -> when (event) {
                is WaterLogged -> if (event.goalMet) Contribution(1.0, true, event.localDay) else null
                else -> null
            }
            MetricId.SLEEP_CONSISTENT_DAYS -> when (event) {
                is SleepImported -> if (event.consistent) Contribution(1.0, true, event.localDay) else null
                else -> null
            }
            MetricId.MEASUREMENTS_LOGGED -> when (event) {
                is MeasurementAdded -> Contribution(1.0, true, event.kind)
                else -> null
            }
            MetricId.MEASUREMENT_DAYS -> when (event) {
                is MeasurementAdded -> Contribution(1.0, true, event.localDay)
                else -> null
            }
            MetricId.PERSONAL_RECORD_EVENTS -> when (event) {
                is WorkoutCompleted -> if (event.isPersonalRecord) Contribution(1.0, true) else null
                else -> null
            }
            MetricId.DISCIPLINES_TRIED -> when (event) {
                is WorkoutCompleted -> Contribution(1.0, true, event.discipline)
                else -> null
            }
            MetricId.PLANNED_REST_HONOURED -> when (event) {
                is PlannedRestHonoured -> Contribution(1.0, true, event.localDay)
                is WorkoutCompleted -> if (event.plannedRest) Contribution(1.0, true) else null
                else -> null
            }
            MetricId.FREEZE_TOKENS_USED -> when (event) {
                is FreezeTokenUsed -> Contribution(1.0, true, event.localDay)
                else -> null
            }
            MetricId.CHALLENGE_POINTS -> when (event) {
                is WorkoutCompleted -> Contribution(1.0, true)
                else -> null
            }
        }

    private fun computeValue(
        def: AchievementDefinition,
        current: AchievementProgress,
        event: DomainEvent,
        contribution: Contribution,
        now: Instant,
        evaluationTimeZoneId: String,
    ): Double {
        val window = def.rule.window
        val agg = def.rule.aggregation

        // Streak computation is special
        if (agg == Aggregation.STREAK || window.kind == WindowKind.STREAK_DAYS) {
            return computeStreak(
                qualifyingDays = current.qualifyingDays,
                freezeDays = if (window.freezeTokensAllowed == true) current.freezeDays else emptySet(),
                plannedRestDays = current.plannedRestDays,
                restTolerancePerWeek = window.restDayTolerancePerWeek ?: 0,
                maxFreeze = window.maxFreezePerStreak ?: 0,
                now = now,
                timeZoneId = evaluationTimeZoneId,
            ).toDouble()
        }

        // Window gating for single_event / calendar / rolling: incremental approximations
        return when (agg) {
            Aggregation.EXISTS -> if (contribution.amount > 0) 1.0 else current.value
            Aggregation.DISTINCT -> current.distinctKeys.size.toDouble()
            Aggregation.MAX -> {
                if (!inWindow(window, event, now, evaluationTimeZoneId)) current.value
                else maxOf(current.value, contribution.amount)
            }
            Aggregation.MIN -> {
                if (!inWindow(window, event, now, evaluationTimeZoneId)) current.value
                else if (current.value == 0.0) contribution.amount else minOf(current.value, contribution.amount)
            }
            Aggregation.SUM, Aggregation.COUNT -> {
                if (window.kind == WindowKind.SINGLE_EVENT) {
                    contribution.amount
                } else if (
                    contribution.distinctKey != null &&
                    def.rule.metric in setOf(
                        MetricId.FOOD_DAYS_LOGGED,
                        MetricId.WATER_GOAL_DAYS,
                        MetricId.SLEEP_CONSISTENT_DAYS,
                        MetricId.MEASUREMENT_DAYS,
                        MetricId.DISCIPLINES_TRIED,
                        MetricId.SWIM_STROKES_DISTINCT,
                        MetricId.MEASUREMENTS_LOGGED,
                        MetricId.PLANNED_REST_HONOURED,
                        MetricId.FREEZE_TOKENS_USED,
                    )
                ) {
                    if (def.rule.metric == MetricId.SWIM_STROKES_DISTINCT && event is WorkoutCompleted) {
                        (current.distinctKeys + event.swimStrokes).size.toDouble()
                    } else {
                        current.distinctKeys.size.toDouble()
                    }
                } else if (
                    !inWindow(window, event, now, evaluationTimeZoneId) &&
                    window.kind != WindowKind.LIFETIME &&
                    (
                        window.kind == WindowKind.CHALLENGE_PERIOD ||
                            window.kind == WindowKind.CALENDAR ||
                            window.kind == WindowKind.ROLLING_DAYS
                        )
                ) {
                    current.value
                } else if (agg == Aggregation.COUNT) {
                    current.value + (if (contribution.amount > 0) 1.0 else 0.0)
                } else {
                    // Aggregation.SUM (COUNT handled above)
                    current.value + contribution.amount
                }
            }
            Aggregation.STREAK -> current.value
        }
    }

    private fun inWindow(
        window: com.omnigart.achievements.model.RuleWindow,
        event: DomainEvent,
        now: Instant,
        evaluationTimeZoneId: String,
    ): Boolean {
        val eventDay = DayBoundaries.localDate(event.occurredAt, event.timeZoneId)
        val today = DayBoundaries.localDate(now, evaluationTimeZoneId)
        return when (window.kind) {
            WindowKind.LIFETIME -> true
            WindowKind.SINGLE_EVENT -> true
            WindowKind.STREAK_DAYS -> true
            WindowKind.ROLLING_DAYS -> {
                val days = window.days ?: return false
                val start = DayBoundaries.minusDays(today, days - 1)
                eventDay >= start && eventDay <= today
            }
            WindowKind.CALENDAR -> {
                when (window.unit) {
                    CalendarUnit.DAY -> eventDay == today
                    CalendarUnit.WEEK -> sameWeek(eventDay, today)
                    CalendarUnit.MONTH -> eventDay.year == today.year && eventDay.month == today.month
                    null -> false
                }
            }
            WindowKind.CHALLENGE_PERIOD -> when (window.period) {
                ChallengePeriod.CURRENT_MONTH -> eventDay.year == today.year && eventDay.month == today.month
                ChallengePeriod.CURRENT_WEEK -> sameWeek(eventDay, today)
                ChallengePeriod.EXPLICIT -> {
                    val start = window.startDate?.let { LocalDate.parse(it) } ?: return false
                    val end = window.endDate?.let { LocalDate.parse(it) } ?: return false
                    eventDay in start..end
                }
                null -> false
            }
        }
    }

    private fun sameWeek(a: LocalDate, b: LocalDate): Boolean =
        weekStartMonday(a) == weekStartMonday(b)

    private fun isoDayNumber(day: DayOfWeek): Int = when (day) {
        DayOfWeek.MONDAY -> 1
        DayOfWeek.TUESDAY -> 2
        DayOfWeek.WEDNESDAY -> 3
        DayOfWeek.THURSDAY -> 4
        DayOfWeek.FRIDAY -> 5
        DayOfWeek.SATURDAY -> 6
        DayOfWeek.SUNDAY -> 7
    }

    /**
     * Current streak length ending today (or yesterday if today not yet qualified),
     * honouring planned rest (already in qualifyingDays), weekly rest tolerance, and freeze tokens.
     */
    internal fun computeStreak(
        qualifyingDays: Set<String>,
        freezeDays: Set<String>,
        plannedRestDays: Set<String>,
        restTolerancePerWeek: Int,
        maxFreeze: Int,
        now: Instant,
        timeZoneId: String,
    ): Int {
        if (qualifyingDays.isEmpty() && freezeDays.isEmpty()) return 0
        val today = DayBoundaries.localDate(now, timeZoneId)
        var cursor = today
        // If today isn't qualified, start from yesterday (allow incomplete today)
        val todayKey = cursor.toString()
        if (todayKey !in qualifyingDays && todayKey !in freezeDays && todayKey !in plannedRestDays) {
            cursor = DayBoundaries.minusDays(cursor, 1)
        }

        var streak = 0
        var freezesUsed = 0
        var weekStart = weekStartMonday(cursor)
        var toleranceUsedThisWeek = 0

        // Walk backwards up to 2 years
        repeat(366 * 2) {
            val key = cursor.toString()
            val ws = weekStartMonday(cursor)
            if (ws != weekStart) {
                weekStart = ws
                toleranceUsedThisWeek = 0
            }
            val ok = key in qualifyingDays || key in plannedRestDays
            if (ok) {
                streak++
            } else if (key in freezeDays && freezesUsed < maxFreeze) {
                streak++
                freezesUsed++
            } else if (toleranceUsedThisWeek < restTolerancePerWeek) {
                streak++
                toleranceUsedThisWeek++
            } else {
                return streak
            }
            cursor = DayBoundaries.minusDays(cursor, 1)
        }
        return streak
    }

    private fun weekStartMonday(d: LocalDate): LocalDate {
        val offset = (isoDayNumber(d.dayOfWeek) - 1 + 7) % 7
        return d.minus(DatePeriod(days = offset))
    }

    private fun matchesFilters(filters: RuleFilters?, event: DomainEvent): Boolean {
        if (filters == null) return true
        val discipline = when (event) {
            is WorkoutCompleted -> event.discipline
            is SetLogged -> event.discipline
            else -> null
        }
        if (filters.discipline != null && discipline != null && discipline !in filters.discipline) return false
        if (event is WorkoutCompleted) {
            filters.minDurationMin?.let { if (event.durationMin < it) return false }
            filters.minDistanceM?.let { dist -> event.distanceM?.let { if (it < dist) return false } ?: return false }
            filters.maxDistanceM?.let { dist -> event.distanceM?.let { if (it > dist) return false } }
            filters.stroke?.let { allowed ->
                if (event.swimStrokes.isEmpty() || event.swimStrokes.none { it in allowed }) return false
            }
        }
        if (event is MeasurementAdded) {
            filters.measurementKind?.let { if (event.kind !in it) return false }
        }
        if (event is SleepImported) {
            filters.sleepMidpointVarianceMin?.let { if (event.midpointVarianceMin > it) return false }
        }
        return true
    }

    private fun tiersReached(def: AchievementDefinition, value: Double): List<TierId> =
        def.tiers.filter { compare(value, it.threshold, def.rule.compare) }.map { it.id }

    private fun detectUnlocks(
        def: AchievementDefinition,
        before: AchievementProgress,
        after: AchievementProgress,
    ): List<UnlockNotification> {
        val newly = after.unlockedTier.toSet() - before.unlockedTier.toSet()
        return newly.map { UnlockNotification(def.id, it, after.value) }
    }

    private fun tierProgress(def: AchievementDefinition, value: Double): TierProgress {
        val sorted = def.tiers.sortedBy { it.threshold }
        val reached = sorted.filter { compare(value, it.threshold, def.rule.compare) }
        val current = reached.lastOrNull()
        val next = sorted.firstOrNull { !compare(value, it.threshold, def.rule.compare) }
        val prevThreshold = current?.threshold ?: 0.0
        val ratio = when {
            next == null -> 1.0
            next.threshold == prevThreshold -> 1.0
            else -> ((value - prevThreshold) / (next.threshold - prevThreshold)).coerceIn(0.0, 1.0)
        }
        return TierProgress(
            currentTier = current?.id,
            nextTier = next?.id,
            currentValue = value,
            nextThreshold = next?.threshold,
            progressRatio = ratio,
        )
    }

    private fun compare(value: Double, threshold: Double, op: CompareOp): Boolean =
        when (op) {
            CompareOp.GTE -> value >= threshold
            CompareOp.LTE -> value <= threshold
            CompareOp.EQ -> value == threshold
        }
}
