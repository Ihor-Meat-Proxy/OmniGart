package com.omnigart.planning

/**
 * Simple on-device muscle-group recovery windows (wellness heuristic, not medical recovery times).
 * Hours until a group is considered ready for another hard session.
 */
object MuscleRecoveryHeuristic {
    private val hoursByGroup: Map<String, Int> =
        mapOf(
            "quads" to 48,
            "quadriceps" to 48,
            "hamstrings" to 48,
            "glutes" to 48,
            "calves" to 36,
            "chest" to 48,
            "back" to 48,
            "shoulders" to 36,
            "arms" to 36,
            "biceps" to 36,
            "triceps" to 36,
            "core" to 24
        )

    fun recoveryHours(muscleGroup: String): Int = hoursByGroup[muscleGroup.lowercase()] ?: 36

    /**
     * Returns muscle groups from [completed] that still fall inside their recovery window
     * relative to [todayIso].
     */
    fun unrecoveredMuscles(todayIso: String, completed: List<CompletedSessionSnapshot>): Set<String> {
        val todayOrdinal = IsoDate.parse(todayIso).toEpochDay()
        val out = mutableSetOf<String>()
        for (session in completed) {
            if (session.intensity == PlannerIntensity.EASY) continue
            val hoursAgo = (todayOrdinal - IsoDate.parse(session.dateIso).toEpochDay()) * 24
            if (hoursAgo < 0) continue
            for (muscle in session.muscleGroups) {
                if (hoursAgo < recoveryHours(muscle)) {
                    out += muscle.lowercase()
                }
            }
        }
        return out
    }
}

/** Days since civil epoch (proleptic Gregorian); adequate for short deltas. */
internal fun IsoDate.toEpochDay(): Long {
    val y = year.toLong()
    val m = month.toLong()
    val d = day.toLong()
    val y0 = if (m <= 2) y - 1 else y
    val era = (if (y0 >= 0) y0 else y0 - 399) / 400
    val yoe = y0 - era * 400
    val mp = if (m > 2) m - 3 else m + 9
    val doy = (153 * mp + 2) / 5 + d - 1
    val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
    return era * 146097 + doe - 719468
}
