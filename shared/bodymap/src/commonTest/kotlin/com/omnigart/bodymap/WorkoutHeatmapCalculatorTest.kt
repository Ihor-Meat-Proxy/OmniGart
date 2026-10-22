package com.omnigart.bodymap

import com.omnigart.content.Level
import com.omnigart.content.Muscle
import com.omnigart.content.TrainingGoal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WorkoutHeatmapCalculatorTest {

    private fun benchSets(count: Int) = CompletedSet(
        exerciseId = "barbell-bench-press",
        primaryMuscles = listOf(
            Muscle.PECTORALIS_MAJOR,
            Muscle.TRICEPS,
            Muscle.FRONT_DELTOID,
        ),
        secondaryMuscles = listOf(Muscle.SERRATUS_ANTERIOR),
        hardSet = true,
        setCount = count,
    )

    private fun squatSets(count: Int) = CompletedSet(
        exerciseId = "back-squat",
        primaryMuscles = listOf(Muscle.QUADRICEPS, Muscle.GLUTEUS_MAXIMUS),
        secondaryMuscles = listOf(Muscle.HAMSTRINGS, Muscle.ERECTOR_SPINAE, Muscle.CALVES),
        hardSet = true,
        setCount = count,
    )

    @Test
    fun fractionalWeightsMatchPellandDefaults() {
        val workout = CompletedWorkout(
            id = "w1",
            sets = listOf(benchSets(4)),
        )
        val heat = WorkoutHeatmapCalculator.workoutHeatmap(
            workout,
            BiologicalSex.MALE,
            InvolvementWeights.FractionalPelland2025,
        )
        val byMuscle = heat.loads.associate { it.muscle to it.effectiveSets }
        assertEquals(4.0, byMuscle[Muscle.PECTORALIS_MAJOR])
        assertEquals(4.0, byMuscle[Muscle.TRICEPS])
        assertEquals(4.0, byMuscle[Muscle.FRONT_DELTOID])
        assertEquals(2.0, byMuscle[Muscle.SERRATUS_ANTERIOR]) // 4 * 0.5
        assertEquals(4.0, heat.maxEffectiveSets)
        assertTrue(heat.loads.first { it.muscle == Muscle.PECTORALIS_MAJOR }.intensity == IntensityLevel.L5)
        assertTrue(heat.loads.first { it.muscle == Muscle.SERRATUS_ANTERIOR }.intensity.level in 1..5)
    }

    @Test
    fun nonHardSetsAreIgnored() {
        val workout = CompletedWorkout(
            id = "warmup",
            sets = listOf(benchSets(3).copy(hardSet = false)),
        )
        val heat = WorkoutHeatmapCalculator.workoutHeatmap(workout, BiologicalSex.FEMALE)
        assertTrue(heat.loads.isEmpty())
        assertEquals(0.0, heat.maxEffectiveSets)
    }

    @Test
    fun customWeightsAreHonoured() {
        val workout = CompletedWorkout(id = "w2", sets = listOf(benchSets(2)))
        val heat = WorkoutHeatmapCalculator.workoutHeatmap(
            workout,
            BiologicalSex.MALE,
            InvolvementWeights(primary = 1.0, secondary = 0.0),
        )
        val byMuscle = heat.loads.associate { it.muscle to it.effectiveSets }
        assertEquals(null, byMuscle[Muscle.SERRATUS_ANTERIOR])
        assertEquals(2.0, byMuscle[Muscle.PECTORALIS_MAJOR])
    }

    @Test
    fun weeklyHeatmapUsesVolumeLandmarks() {
        val week = listOf(
            CompletedWorkout("d1", listOf(squatSets(5))),
            CompletedWorkout("d2", listOf(squatSets(5))),
        )
        val heat = WorkoutHeatmapCalculator.weeklyVolumeHeatmap(
            workouts = week,
            sex = BiologicalSex.FEMALE,
            level = Level.INTERMEDIATE,
            goal = TrainingGoal.HYPERTROPHY,
        )
        val quads = heat.muscles.first { it.muscle == Muscle.QUADRICEPS }
        assertEquals(10.0, quads.effectiveSets)
        assertTrue(quads.landmarkTarget != null && quads.landmarkTarget > 0)
        assertTrue(quads.ratioToLandmark != null)
        assertTrue(quads.intensity != IntensityLevel.IDLE)
    }

    @Test
    fun intensityBinsCoverFiveLevels() {
        assertEquals(IntensityLevel.IDLE, IntensityNormalizer.levelFor(0.0, 10.0))
        assertEquals(IntensityLevel.L1, IntensityNormalizer.levelFor(1.0, 10.0))
        assertEquals(IntensityLevel.L3, IntensityNormalizer.levelFor(5.0, 10.0))
        assertEquals(IntensityLevel.L5, IntensityNormalizer.levelFor(10.0, 10.0))
        assertEquals(IntensityLevel.L5, IntensityNormalizer.levelForLandmarkRatio(1.2))
        assertEquals(IntensityLevel.IDLE, IntensityNormalizer.levelForLandmarkRatio(null))
    }
}
