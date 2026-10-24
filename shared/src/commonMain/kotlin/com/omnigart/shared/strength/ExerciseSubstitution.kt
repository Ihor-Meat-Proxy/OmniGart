package com.omnigart.shared.strength

/**
 * Picks an equipment-legal exercise for a slot, walking regressions / pattern matches.
 * Selection is deterministic: candidates are sorted by id.
 */
object ExerciseSubstitution {
    data class Selection(val exercise: StrengthLibraryExercise, val substitutedFromId: String?)

    fun select(
        preferredIds: List<String>,
        pattern: String,
        targetMuscles: Set<String>,
        equipment: UserEquipmentProfile,
        library: List<StrengthLibraryExercise>,
        excludeIds: Set<String>,
        maxLevelOrdinal: Int
    ): Selection? {
        val byId = library.associateBy { it.id }
        for (preferred in preferredIds) {
            val hit = resolveChain(preferred, byId, equipment, excludeIds, maxLevelOrdinal)
            if (hit != null) return hit
        }
        val patternMatches =
            library
                .asSequence()
                .filter { it.id !in excludeIds }
                .filter { levelOrdinal(it.level) <= maxLevelOrdinal }
                .filter { equipment.canPerform(it.equipment) }
                .filter { it.movementPattern == pattern }
                .filter { ex ->
                    targetMuscles.isEmpty() ||
                        ex.primaryMuscles.any { it in targetMuscles } ||
                        ex.secondaryMuscles.any { it in targetMuscles }
                }
                .sortedBy { it.id }
                .toList()
        val best = patternMatches.firstOrNull() ?: return null
        return Selection(best, substitutedFromId = null)
    }

    private fun resolveChain(
        startId: String,
        byId: Map<String, StrengthLibraryExercise>,
        equipment: UserEquipmentProfile,
        excludeIds: Set<String>,
        maxLevelOrdinal: Int
    ): Selection? {
        val start = byId[startId] ?: return null
        if (start.id !in excludeIds &&
            levelOrdinal(start.level) <= maxLevelOrdinal &&
            equipment.canPerform(start.equipment)
        ) {
            return Selection(start, substitutedFromId = null)
        }
        val queue = ArrayDeque<Pair<String, String>>()
        start.regressions.forEach { queue.add(it to startId) }
        val seen = mutableSetOf(startId)
        while (queue.isNotEmpty()) {
            val (candidateId, fromId) = queue.removeFirst()
            if (!seen.add(candidateId)) continue
            val candidate = byId[candidateId] ?: continue
            if (candidate.id in excludeIds) continue
            if (levelOrdinal(candidate.level) > maxLevelOrdinal) {
                candidate.regressions.forEach { queue.add(it to fromId) }
                continue
            }
            if (equipment.canPerform(candidate.equipment)) {
                return Selection(candidate, substitutedFromId = fromId)
            }
            candidate.regressions.forEach { queue.add(it to fromId) }
        }
        return null
    }

    fun levelOrdinal(level: String): Int = when (level) {
        "beginner" -> 0
        "intermediate" -> 1
        "advanced" -> 2
        else -> 1
    }
}
