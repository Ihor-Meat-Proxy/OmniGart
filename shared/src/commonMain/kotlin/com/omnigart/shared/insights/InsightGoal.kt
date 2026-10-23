package com.omnigart.shared.insights

import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Serializable
enum class InsightGoalKind {
    WEEKLY_LOAD,
    WEEKLY_WORKOUTS,
    MONTHLY_ACTIVE_KCAL,
    CUSTOM
}

@Serializable
data class InsightGoal(
    val id: String,
    val kind: InsightGoalKind,
    val title: String,
    val targetValue: Double,
    val unitLabel: String,
    val createdAt: Instant = Clock.System.now()
) {
    /** Swift-friendly constructor without createdAt default. */
    constructor(
        id: String,
        kind: InsightGoalKind,
        title: String,
        targetValue: Double,
        unitLabel: String
    ) : this(
        id = id,
        kind = kind,
        title = title,
        targetValue = targetValue,
        unitLabel = unitLabel,
        createdAt = Clock.System.now()
    )
}

interface InsightGoalStore {
    fun list(): List<InsightGoal>

    fun add(goal: InsightGoal)

    fun remove(id: String)
}

/** Process-local store; platforms may wrap with persistence later. */
class InMemoryInsightGoalStore : InsightGoalStore {
    private val goals = mutableListOf<InsightGoal>()

    override fun list(): List<InsightGoal> = goals.toList()

    override fun add(goal: InsightGoal) {
        goals.removeAll { it.id == goal.id }
        goals += goal
    }

    override fun remove(id: String) {
        goals.removeAll { it.id == id }
    }
}
