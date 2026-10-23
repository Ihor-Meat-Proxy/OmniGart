package com.omnigart.achievements

import kotlinx.datetime.DatePeriod
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlinx.serialization.Serializable

@Serializable
data class ChallengeGoal(
    val metric: String,
    val comparator: ComparatorOp = ComparatorOp.GTE,
    val value: Double,
    val filters: Map<String, String> = emptyMap(),
    val window: RuleWindow? = null,
)

@Serializable
data class ChallengeTemplate(
    val id: String,
    val challengeType: String,
    val durationDays: Int,
    val iconId: String,
    val titleKey: String,
    val descriptionKey: String,
    val goal: ChallengeGoal,
    val localOnly: Boolean = true,
    val social: Boolean = false,
)

@Serializable
data class ChallengeTemplateCatalog(
    val version: Int,
    val templates: List<ChallengeTemplate>,
)

@Serializable
enum class ChallengeStatus {
    ACTIVE,
    COMPLETED,
    EXPIRED,
    ABANDONED,
}

/**
 * Local-only personal challenge instance. No social graph, leaderboards, or sharing hooks.
 */
@Serializable
data class ChallengeInstance(
    val instanceId: String,
    val templateId: String,
    val challengeType: String,
    val startedAt: Instant,
    val startDay: String,
    val endDay: String,
    val timeZoneId: String,
    val goal: ChallengeGoal,
    val progressValue: Double = 0.0,
    val contributingDays: Set<String> = emptySet(),
    val status: ChallengeStatus = ChallengeStatus.ACTIVE,
    val completedAt: Instant? = null,
)

data class ChallengeEvaluation(
    val instance: ChallengeInstance,
    val newlyCompleted: Boolean,
)

class ChallengeEngine(
    private val templates: ChallengeTemplateCatalog,
) {
    private val instances = mutableMapOf<String, ChallengeInstance>()

    fun template(id: String): ChallengeTemplate? = templates.templates.firstOrNull { it.id == id }

    fun start(
        templateId: String,
        instanceId: String,
        startedAt: Instant,
        timeZoneId: String,
    ): ChallengeInstance {
        val template = template(templateId) ?: error("Unknown challenge template: $templateId")
        check(template.localOnly && !template.social) {
            "Challenges are local-only; social features are not supported."
        }
        val start = DayBoundary.localDate(startedAt, timeZoneId)
        val end = start.plusDays(template.durationDays - 1)
        val instance = ChallengeInstance(
            instanceId = instanceId,
            templateId = template.id,
            challengeType = template.challengeType,
            startedAt = startedAt,
            startDay = start.toString(),
            endDay = end.toString(),
            timeZoneId = timeZoneId,
            goal = template.goal,
        )
        instances[instanceId] = instance
        return instance
    }

    fun get(instanceId: String): ChallengeInstance? = instances[instanceId]

    fun all(): List<ChallengeInstance> = instances.values.toList()

    fun ingest(event: DomainEvent): List<ChallengeEvaluation> {
        val contributions = MetricExtractor.extract(event)
        val results = mutableListOf<ChallengeEvaluation>()
        for ((id, instance) in instances.toList()) {
            if (instance.status != ChallengeStatus.ACTIVE) continue
            val day = DayBoundary.localDate(event.occurredAt, event.timeZoneId)
            val start = LocalDate.parse(instance.startDay)
            val end = LocalDate.parse(instance.endDay)
            if (day < start) continue
            if (day > end) {
                val expired = instance.copy(status = ChallengeStatus.EXPIRED)
                instances[id] = expired
                results += ChallengeEvaluation(expired, newlyCompleted = false)
                continue
            }

            var progress = instance.progressValue
            var days = instance.contributingDays
            var changed = false
            for (c in contributions) {
                if (!goalMatches(instance.goal, c)) continue
                when {
                    instance.goal.metric.endsWith("_day") ||
                        instance.goal.metric == "training_or_rest_day" ||
                        instance.goal.metric == "active_or_rest_day" -> {
                        val key = DayBoundary.dayKey(c.occurredAt, c.timeZoneId)
                        if (key !in days) {
                            days = days + key
                            progress = days.size.toDouble()
                            changed = true
                        }
                    }
                    instance.goal.metric == "distance_m" ||
                        instance.goal.metric == "workout_completed" -> {
                        progress += c.amount
                        changed = true
                    }
                    else -> {
                        progress += c.amount
                        changed = true
                    }
                }
            }
            if (!changed) continue

            val completed = compare(progress, instance.goal.comparator, instance.goal.value)
            val updated = instance.copy(
                progressValue = progress,
                contributingDays = days,
                status = if (completed) ChallengeStatus.COMPLETED else ChallengeStatus.ACTIVE,
                completedAt = if (completed) event.occurredAt else null,
            )
            instances[id] = updated
            results += ChallengeEvaluation(updated, newlyCompleted = completed && instance.status != ChallengeStatus.COMPLETED)
        }
        return results
    }

    private fun goalMatches(goal: ChallengeGoal, c: MetricContribution): Boolean {
        if (c.metric != goal.metric) return false
        for ((key, expected) in goal.filters) {
            val actual = c.attributes[key] ?: return false
            if (actual != expected) return false
        }
        return true
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

private fun LocalDate.plusDays(days: Int): LocalDate =
    this + DatePeriod(days = days)
