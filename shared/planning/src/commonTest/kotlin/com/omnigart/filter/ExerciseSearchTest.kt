package com.omnigart.filter

import com.omnigart.content.Discipline
import com.omnigart.content.Equipment
import com.omnigart.content.Exercise
import com.omnigart.content.Level
import com.omnigart.content.LocalizedNameKeys
import com.omnigart.content.Mechanics
import com.omnigart.content.MediaPlaceholders
import com.omnigart.content.MovementPattern
import com.omnigart.content.Muscle
import com.omnigart.content.TrackingType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExerciseSearchTest {
    private fun sample(
        id: String,
        discipline: Discipline = Discipline.STRENGTH,
        pattern: MovementPattern = MovementPattern.SQUAT,
        muscles: List<Muscle> = listOf(Muscle.QUADRICEPS),
        equipment: List<Equipment> = listOf(Equipment.BARBELL),
        level: Level = Level.BEGINNER
    ) = Exercise(
        id = id,
        nameKeys = LocalizedNameKeys("exercise.$id.name", "exercise.$id.name"),
        discipline = discipline,
        movementPattern = pattern,
        primaryMuscles = muscles,
        equipment = equipment,
        level = level,
        unilateral = false,
        mechanics = Mechanics.COMPOUND,
        trackingType = TrackingType.WEIGHT_REPS,
        technique = listOf("Step one of technique text"),
        setup = listOf("Setup step text here"),
        breathing = "Breathe steadily throughout.",
        commonMistakes = listOf("Common mistake example"),
        safety = listOf("Safety note example here"),
        media = MediaPlaceholders()
    )

    private val library = listOf(
        sample(
            "back-squat",
            equipment = listOf(Equipment.BARBELL),
            muscles = listOf(Muscle.QUADRICEPS)
        ),
        sample(
            "goblet-squat",
            equipment = listOf(Equipment.DUMBBELL),
            muscles = listOf(Muscle.QUADRICEPS)
        ),
        sample(
            "push-up",
            pattern = MovementPattern.PUSH,
            equipment = listOf(Equipment.BODYWEIGHT),
            muscles = listOf(Muscle.PECTORALIS_MAJOR)
        ),
        sample(
            "freestyle-stroke",
            Discipline.SWIMMING,
            MovementPattern.SWIM_STROKE,
            listOf(Muscle.LATISSIMUS_DORSI),
            listOf(Equipment.POOL)
        ),
        sample(
            "easy-run",
            Discipline.RUNNING,
            MovementPattern.GAIT,
            listOf(Muscle.QUADRICEPS),
            listOf(Equipment.ROAD),
            Level.BEGINNER
        )
    )

    @Test
    fun filtersByEquipmentProfile() {
        val travel = ExerciseSearch.filter(
            library,
            ExerciseQuery(equipmentProfile = EquipmentProfile.TRAVEL)
        )
        assertEquals(listOf("push-up"), travel.map { it.id })
    }

    @Test
    fun filtersByMusclePatternLevelDiscipline() {
        val result = ExerciseSearch.filter(
            library,
            ExerciseQuery(
                muscles = setOf(Muscle.QUADRICEPS),
                patterns = setOf(MovementPattern.SQUAT),
                levels = setOf(Level.BEGINNER),
                disciplines = setOf(Discipline.STRENGTH)
            )
        )
        assertTrue(result.all { it.id.contains("squat") })
        assertEquals(2, result.size)
    }

    @Test
    fun homeGymIncludesDumbbellNotPool() {
        val result = ExerciseSearch.filter(
            library,
            ExerciseQuery(equipmentProfile = EquipmentProfile.HOME_GYM)
        )
        assertTrue(result.any { it.id == "goblet-squat" })
        assertTrue(result.none { it.id == "freestyle-stroke" })
    }

    @Test
    fun commercialGymIncludesEverything() {
        val result = ExerciseSearch.filter(
            library,
            ExerciseQuery(equipmentProfile = EquipmentProfile.COMMERCIAL_GYM)
        )
        assertEquals(library.size, result.size)
    }
}
