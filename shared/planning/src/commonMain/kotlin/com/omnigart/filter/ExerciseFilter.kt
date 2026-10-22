package com.omnigart.filter

import com.omnigart.content.Discipline
import com.omnigart.content.Equipment
import com.omnigart.content.Exercise
import com.omnigart.content.Level
import com.omnigart.content.MovementPattern
import com.omnigart.content.Muscle

data class ExerciseQuery(
    val equipmentProfile: EquipmentProfile? = null,
    val requiredEquipmentAny: Set<Equipment> = emptySet(),
    val muscles: Set<Muscle> = emptySet(),
    val matchSecondaryMuscles: Boolean = true,
    val patterns: Set<MovementPattern> = emptySet(),
    val levels: Set<Level> = emptySet(),
    val disciplines: Set<Discipline> = emptySet(),
    val text: String? = null,
    val excludeIds: Set<String> = emptySet()
)

object ExerciseSearch {
    fun filter(exercises: Collection<Exercise>, query: ExerciseQuery): List<Exercise> {
        val needle = query.text?.trim()?.lowercase().orEmpty()
        return exercises.asSequence()
            .filter { it.id !in query.excludeIds }
            .filter { ex ->
                query.disciplines.isEmpty() || ex.discipline in query.disciplines
            }
            .filter { ex ->
                query.levels.isEmpty() || ex.level in query.levels
            }
            .filter { ex ->
                query.patterns.isEmpty() || ex.movementPattern in query.patterns
            }
            .filter { ex ->
                if (query.muscles.isEmpty()) {
                    true
                } else {
                    val hitPrimary = ex.primaryMuscles.any { it in query.muscles }
                    val hitSecondary =
                        query.matchSecondaryMuscles &&
                            ex.secondaryMuscles.any { it in query.muscles }
                    hitPrimary || hitSecondary
                }
            }
            .filter { ex ->
                val profile = query.equipmentProfile
                if (profile != null) {
                    profile.canPerform(ex.equipment)
                } else if (query.requiredEquipmentAny.isNotEmpty()) {
                    ex.equipment.any { it in query.requiredEquipmentAny }
                } else {
                    true
                }
            }
            .filter { ex ->
                if (needle.isEmpty()) {
                    true
                } else {
                    ex.id.contains(needle) ||
                        ex.aliases.any { it.lowercase().contains(needle) } ||
                        ex.tags.any { it.lowercase().contains(needle) } ||
                        ex.nameKeys.enGB.lowercase().contains(needle) ||
                        ex.nameKeys.uk.lowercase().contains(needle)
                }
            }
            .sortedBy { it.id }
            .toList()
    }
}
