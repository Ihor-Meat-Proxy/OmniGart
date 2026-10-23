package com.omnigart.achievements

import kotlin.time.Instant
import kotlinx.datetime.LocalDate

/**
 * Incremental, idempotent achievement evaluator.
 *
 * - Day boundaries use the event's [DomainEvent.timeZoneId] (DST-correct).
 * - Duplicate [DomainEvent.eventId]s are ignored.
 * - [backfill] suppresses per-unlock notifications and returns a batch summary count.
 */
class AchievementEngine(
    private val catalog: AchievementCatalog,
    private val store: AchievementStore = InMemoryAchievementStore(),
) {
    fun ingest(event: DomainEvent, notify: Boolean = true): EvaluationResult {
        if (store.hasProcessedEvent(event.eventId)) {
            return EvaluationResult(ignoredDuplicate = true)
        }
        store.markProcessed(event.eventId)

        val unlocks = mutableListOf<UnlockNotification>()
        val contributions = MetricExtractor.extract(event)
        for (def in catalog.achievements) {
            val before = store.getProgress(def.id) ?: initialProgress(def)
            if (before.unlocked && !def.repeatable) continue

            val after = applyContributions(def, before, contributions) ?: continue
            store.putProgress(after)
            if (after.unlocked && (!before.unlocked || def.repeatable && after.unlockCount > before.unlockCount)) {
                unlocks += UnlockNotification(
                    achievementId = def.id,
                    unlockedAt = after.unlockedAt ?: event.occurredAt,
                    hidden = def.hidden,
                )
            }
        }

        return if (notify) {
            EvaluationResult(unlocks = unlocks)
        } else {
            EvaluationResult(unlocks = unlocks, batchSummaryCount = unlocks.size)
        }
    }

    /**
     * Replay historical events (e.g. after Health import). Unlocks are collected but
     * [BackfillResult.batchSummaryCount] is the signal for UI — do not spam toasts.
     */
    fun backfill(events: List<DomainEvent>): BackfillResult {
        var processed = 0
        var skipped = 0
        val allUnlocks = mutableListOf<UnlockNotification>()
        val ordered = events.sortedBy { it.occurredAt }
        for (event in ordered) {
            val result = ingest(event, notify = false)
            if (result.ignoredDuplicate) {
                skipped++
            } else {
                processed++
                allUnlocks += result.unlocks
            }
        }
        return BackfillResult(
            processedEvents = processed,
            skippedDuplicates = skipped,
            unlocks = allUnlocks,
            batchSummaryCount = allUnlocks.size,
        )
    }

    fun progress(achievementId: String): ProgressSnapshot? {
        val def = catalog.byId(achievementId) ?: return null
        val p = store.getProgress(achievementId) ?: initialProgress(def)
        val ratio = if (p.targetValue <= 0) 0.0 else (p.currentValue / p.targetValue).coerceIn(0.0, 1.0)
        return ProgressSnapshot(
            achievementId = achievementId,
            currentValue = p.currentValue,
            targetValue = p.targetValue,
            ratio = ratio,
            unlocked = p.unlocked,
            unlockedAt = p.unlockedAt,
            nextInFamily = nextTierProgress(def, p),
        )
    }

    fun nextTierProgress(def: AchievementDefinition, current: AchievementProgress): NextTierProgress? {
        val family = catalog.familyMembers(def.familyId)
        val idx = family.indexOfFirst { it.id == def.id }
        if (idx < 0 || idx >= family.lastIndex) return null
        val next = family[idx + 1]
        val nextProgress = store.getProgress(next.id) ?: initialProgress(next)
        val value = when (next.rule.kind) {
            RuleKind.STREAK -> nextProgress.currentStreak.toDouble()
            else -> nextProgress.currentValue
        }
        val target = next.rule.value
        return NextTierProgress(
            achievementId = next.id,
            currentValue = value,
            targetValue = target,
            ratio = if (target <= 0) 0.0 else (value / target).coerceIn(0.0, 1.0),
        )
    }

    private fun initialProgress(def: AchievementDefinition) =
        AchievementProgress(
            achievementId = def.id,
            targetValue = def.rule.value,
        )

    private fun applyContributions(
        def: AchievementDefinition,
        state: AchievementProgress,
        contributions: List<MetricContribution>,
    ): AchievementProgress? {
        val matching = contributions.filter { matches(def.rule, it) }
        if (matching.isEmpty()) return null

        return when (def.rule.kind) {
            RuleKind.COUNTER -> applyCounter(def, state, matching)
            RuleKind.STREAK -> applyStreak(def, state, matching)
            RuleKind.THRESHOLD -> applyThreshold(def, state, matching)
            RuleKind.PERSONAL_RECORD -> applyPersonalRecord(def, state, matching)
            RuleKind.CHALLENGE -> applyCounter(def, state, matching)
        }
    }

    private fun matches(rule: AchievementRule, c: MetricContribution): Boolean {
        val metricOk = when {
            c.metric == rule.metric -> true
            rule.kind == RuleKind.STREAK && c.isStreakFreeze &&
                rule.streakPolicy?.allowStreakFreeze == true &&
                (c.metric == rule.metric || c.metric == "active_or_rest_day") -> true
            rule.kind == RuleKind.STREAK && c.isPlannedRest &&
                rule.streakPolicy?.allowPlannedRest == true &&
                c.metric == rule.metric -> true
            else -> false
        }
        if (!metricOk) return false

        for ((key, expected) in rule.filters) {
            if (key == "challengeType" && expected == "any") continue
            val actual = c.attributes[key] ?: return false
            if (actual != expected) return false
        }
        return true
    }

    private fun applyCounter(
        def: AchievementDefinition,
        state: AchievementProgress,
        matching: List<MetricContribution>,
    ): AchievementProgress {
        var current = state.currentValue
        var days = state.contributingDays
        var weeks = state.contributingWeeks
        var months = state.contributingMonths
        var sites = state.contributingSites
        var disciplinesByWeek = state.contributingDisciplinesByWeek
        var unlockedAt = state.unlockedAt
        var unlocked = state.unlocked
        var unlockCount = state.unlockCount

        for (c in matching) {
            val day = DayBoundary.dayKey(c.occurredAt, c.timeZoneId)
            val week = DayBoundary.weekKey(c.occurredAt, c.timeZoneId)
            val month = DayBoundary.monthKey(c.occurredAt, c.timeZoneId)

            when (def.rule.metric) {
                "water_goal_day", "nutrition_day", "sleep_logged_day", "active_day",
                "measurement_week_day", "protein_target_day",
                -> {
                    if (day in days) continue
                    days = days + day
                    current = days.size.toDouble()
                }
                "measurement_week_logged" -> {
                    if (week in weeks) continue
                    weeks = weeks + week
                    current = weeks.size.toDouble()
                }
                "distinct_measurement_sites" -> {
                    val site = c.attributes["site"] ?: continue
                    if (site in sites) continue
                    sites = sites + site
                    current = sites.size.toDouble()
                }
                "distinct_disciplines_week" -> {
                    val discipline = c.attributes["discipline"] ?: continue
                    val set = disciplinesByWeek[week].orEmpty() + discipline
                    disciplinesByWeek = disciplinesByWeek + (week to set)
                    // Windowed: value is max distinct disciplines in any single week in scope
                    current = if (def.rule.window?.unit == WindowUnit.WEEK) {
                        set.size.toDouble()
                    } else {
                        disciplinesByWeek.values.maxOfOrNull { it.size }?.toDouble() ?: 0.0
                    }
                }
                "distance_m" -> {
                    when (def.rule.window?.unit) {
                        WindowUnit.MONTH -> {
                            val next = (months[month] ?: 0.0) + c.amount
                            months = months + (month to next)
                            current = months.values.maxOrNull() ?: 0.0
                        }
                        WindowUnit.WEEK -> {
                            val next = (months[week] ?: 0.0) + c.amount
                            months = months + (week to next)
                            current = months.values.maxOrNull() ?: 0.0
                        }
                        else -> current += c.amount
                    }
                }
                "planned_session_completed" -> {
                    when (def.rule.window?.unit) {
                        WindowUnit.WEEK -> {
                            val next = (months[week] ?: 0.0) + c.amount
                            months = months + (week to next)
                            current = months.values.maxOrNull() ?: 0.0
                        }
                        else -> current += c.amount
                    }
                }
                "weekly_goal_met" -> {
                    val key = c.attributes["weekStartIso"] ?: week
                    if (key in weeks) continue
                    weeks = weeks + key
                    current = weeks.size.toDouble()
                }
                else -> {
                    if (isDayScopedMetric(def.rule.metric)) {
                        if (day in days) continue
                        days = days + day
                        current = days.size.toDouble()
                    } else {
                        current += c.amount
                    }
                }
            }
        }

        if (!unlocked && compare(current, def.rule.comparator, def.rule.value)) {
            unlocked = true
            unlockedAt = matching.last().occurredAt
            unlockCount += 1
        } else if (unlocked && def.repeatable && compare(current, def.rule.comparator, def.rule.value)) {
            // Repeatable counters only re-fire when explicitly designed; keep simple for v1.
        }

        return state.copy(
            currentValue = current,
            targetValue = def.rule.value,
            unlocked = unlocked,
            unlockedAt = unlockedAt,
            unlockCount = unlockCount,
            contributingDays = days,
            contributingWeeks = weeks,
            contributingMonths = months,
            contributingSites = sites,
            contributingDisciplinesByWeek = disciplinesByWeek,
        )
    }

    private fun isDayScopedMetric(metric: String): Boolean =
        metric.endsWith("_day") || metric == "rest_taken_on_low_readiness"

    private fun applyStreak(
        def: AchievementDefinition,
        state: AchievementProgress,
        matching: List<MetricContribution>,
    ): AchievementProgress {
        val policy = def.rule.streakPolicy ?: StreakPolicy()
        var streak = state.currentStreak
        var lastDay = state.lastStreakDay?.let { LocalDate.parse(it) }
        var freezes = state.freezeDaysUsed
        var unlocked = state.unlocked
        var unlockedAt = state.unlockedAt
        var unlockCount = state.unlockCount
        var currentValue = state.currentValue

        val ordered = matching.sortedBy { it.occurredAt }
        for (c in ordered) {
            val day = DayBoundary.localDate(c.occurredAt, c.timeZoneId)
            val dayStr = day.toString()
            if (lastDay != null && day == lastDay) continue

            val isFreeze = c.isStreakFreeze
            val isRest = c.isPlannedRest

            if (isFreeze && !policy.allowStreakFreeze) continue
            if (isRest && !policy.allowPlannedRest) continue

            if (isFreeze) {
                val recentFreezes = freezes.count { freezeDay ->
                    val fd = LocalDate.parse(freezeDay)
                    kotlin.math.abs(DayBoundary.daysBetween(fd, day)) <= 30
                }
                if (recentFreezes >= policy.maxFreezeDaysPer30Days) continue
                freezes = freezes + dayStr
            }

            if (lastDay == null) {
                streak = 1
                lastDay = day
            } else {
                val gap = DayBoundary.daysBetween(lastDay, day)
                when {
                    gap == 1 -> {
                        streak += 1
                        lastDay = day
                    }
                    gap == 0 -> {
                        // same day already handled
                    }
                    gap > 1 -> {
                        // Broken streak — restart (out-of-order history should be sorted by backfill)
                        streak = 1
                        lastDay = day
                    }
                    gap < 0 -> {
                        // Out-of-order older event: ignore for live streak tip; backfill sorts ascending.
                        continue
                    }
                }
            }

            currentValue = streak.toDouble()
            if (!unlocked && compare(streak.toDouble(), def.rule.comparator, def.rule.value)) {
                unlocked = true
                unlockedAt = c.occurredAt
                unlockCount += 1
            }
        }

        return state.copy(
            currentValue = currentValue,
            targetValue = def.rule.value,
            unlocked = unlocked,
            unlockedAt = unlockedAt,
            unlockCount = unlockCount,
            lastStreakDay = lastDay?.toString(),
            currentStreak = streak,
            freezeDaysUsed = freezes,
        )
    }

    private fun applyThreshold(
        def: AchievementDefinition,
        state: AchievementProgress,
        matching: List<MetricContribution>,
    ): AchievementProgress {
        var best = state.currentValue
        var unlocked = state.unlocked
        var unlockedAt = state.unlockedAt
        var unlockCount = state.unlockCount
        for (c in matching) {
            if (c.amount > best) best = c.amount
            if (!unlocked && compare(c.amount, def.rule.comparator, def.rule.value)) {
                unlocked = true
                unlockedAt = c.occurredAt
                unlockCount += 1
            }
        }
        return state.copy(
            currentValue = best,
            targetValue = def.rule.value,
            unlocked = unlocked,
            unlockedAt = unlockedAt,
            unlockCount = unlockCount,
        )
    }

    private fun applyPersonalRecord(
        def: AchievementDefinition,
        state: AchievementProgress,
        matching: List<MetricContribution>,
    ): AchievementProgress {
        val prs = matching.filter { it.isPersonalRecord || it.metric == "personal_record" }
        if (prs.isEmpty()) return state
        // First PR unlocks personal_record metric rules; count rules use counter kind.
        if (def.rule.metric == "personal_record") {
            return applyCounter(def, state, prs.filter { it.metric == "personal_record" })
        }
        var unlocked = state.unlocked
        var unlockedAt = state.unlockedAt
        var unlockCount = state.unlockCount
        var best = state.currentValue
        for (c in prs) {
            if (c.amount > best) best = c.amount
            if (!unlocked) {
                unlocked = true
                unlockedAt = c.occurredAt
                unlockCount += 1
            }
        }
        return state.copy(
            currentValue = best,
            targetValue = def.rule.value,
            unlocked = unlocked,
            unlockedAt = unlockedAt,
            unlockCount = unlockCount,
        )
    }

    private fun compare(actual: Double, op: ComparatorOp, expected: Double): Boolean =
        when (op) {
            ComparatorOp.GTE -> actual >= expected
            ComparatorOp.GT -> actual > expected
            ComparatorOp.LTE -> actual <= expected
            ComparatorOp.LT -> actual < expected
            ComparatorOp.EQ -> actual == expected
        }
}
