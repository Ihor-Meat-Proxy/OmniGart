package com.omnigart.shared.data

import com.omnigart.shared.domain.BiologicalSex
import com.omnigart.shared.domain.Exercise
import com.omnigart.shared.domain.ExerciseModality
import com.omnigart.shared.domain.Session
import com.omnigart.shared.domain.UnitSystem
import com.omnigart.shared.domain.UserProfile
import com.omnigart.shared.domain.Workout
import com.omnigart.shared.domain.WorkoutSet
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Instant
import kotlinx.datetime.LocalDate

class RepositoryTest {
    @Test
    fun profileRoundTrip() {
        val db = createInMemoryDatabase()
        val repo = UserProfileRepository(db)
        val profile =
            UserProfile(
                id = "p1",
                biologicalSex = BiologicalSex.FEMALE,
                heightCm = 165.0,
                weightKg = 58.0,
                dateOfBirth = LocalDate(1995, 3, 12),
                preferredUnits = UnitSystem.METRIC,
                waistCm = 70.0,
                hipCm = 95.0
            )
        repo.upsert(profile)
        assertEquals(profile, repo.get("p1"))
        repo.delete("p1")
        assertNull(repo.get("p1"))
    }

    @Test
    fun exerciseWorkoutAndSessionRoundTrip() {
        val db = createInMemoryDatabase()
        val exercises = ExerciseRepository(db)
        val workouts = WorkoutRepository(db)
        val sessions = SessionRepository(db)

        val squat =
            Exercise(
                id = "ex-squat",
                contentKey = "ex.barbell_back_squat",
                modality = ExerciseModality.STRENGTH,
                primaryMuscles = listOf("quads", "glutes")
            )
        exercises.upsert(squat)
        assertEquals(squat, exercises.get("ex-squat"))

        val workout = Workout(id = "w1", name = "Lower A", exerciseIds = listOf("ex-squat"))
        workouts.upsert(workout)
        assertEquals(workout, workouts.get("w1"))

        val session =
            Session(
                id = "s1",
                workoutId = "w1",
                startedAt = Instant.parse("2026-06-01T08:00:00Z"),
                endedAt = Instant.parse("2026-06-01T09:00:00Z"),
                sets =
                listOf(
                    WorkoutSet(
                        id = "set1",
                        exerciseId = "ex-squat",
                        setIndex = 0,
                        reps = 5,
                        weightKg = 80.0,
                        rir = 2.0
                    )
                ),
                perceivedExertion = 7
            )
        sessions.upsert(session)
        val loaded = sessions.get("s1")
        assertNotNull(loaded)
        assertEquals(session, loaded)
    }
}
