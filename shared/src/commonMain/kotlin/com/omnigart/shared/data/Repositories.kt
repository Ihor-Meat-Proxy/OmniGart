package com.omnigart.shared.data

import com.omnigart.shared.db.OmniGartDatabase
import com.omnigart.shared.domain.ActivityLevel
import com.omnigart.shared.domain.BiologicalSex
import com.omnigart.shared.domain.Exercise
import com.omnigart.shared.domain.ExerciseModality
import com.omnigart.shared.domain.ProfileGoal
import com.omnigart.shared.domain.Session
import com.omnigart.shared.domain.StrengthLevel
import com.omnigart.shared.domain.UnitSystem
import com.omnigart.shared.domain.UserProfile
import com.omnigart.shared.domain.Workout
import com.omnigart.shared.domain.WorkoutSet
import kotlin.time.Instant
import kotlinx.datetime.LocalDate

class UserProfileRepository(private val db: OmniGartDatabase) {
    fun upsert(profile: UserProfile) {
        db.omniGartQueries.upsertProfile(
            id = profile.id,
            biological_sex = profile.biologicalSex.name,
            height_cm = profile.heightCm,
            weight_kg = profile.weightKg,
            date_of_birth = profile.dateOfBirth.toString(),
            preferred_units = profile.preferredUnits.name,
            waist_cm = profile.waistCm,
            hip_cm = profile.hipCm,
            primary_goal = profile.primaryGoal.name,
            strength_level = profile.strengthLevel.name,
            activity_level = profile.activityLevel.name,
            onboarding_completed = if (profile.onboardingCompleted) 1L else 0L
        )
    }

    fun get(id: String): UserProfile? = db.omniGartQueries.selectProfile(id).executeAsOneOrNull()?.toDomain()

    fun getAll(): List<UserProfile> = db.omniGartQueries.selectAllProfiles().executeAsList().map { it.toDomain() }

    fun delete(id: String) {
        db.omniGartQueries.deleteProfile(id)
    }
}

class ExerciseRepository(private val db: OmniGartDatabase) {
    fun upsert(exercise: Exercise) {
        db.omniGartQueries.upsertExercise(
            id = exercise.id,
            content_key = exercise.contentKey,
            modality = exercise.modality.name,
            primary_muscles = exercise.primaryMuscles.joinToString(",")
        )
    }

    fun get(id: String): Exercise? = db.omniGartQueries.selectExercise(id).executeAsOneOrNull()?.toDomain()

    fun getAll(): List<Exercise> = db.omniGartQueries.selectAllExercises().executeAsList().map { it.toDomain() }
}

class WorkoutRepository(private val db: OmniGartDatabase) {
    fun upsert(workout: Workout) {
        db.omniGartQueries.upsertWorkout(
            id = workout.id,
            name = workout.name,
            notes = workout.notes,
            exercise_ids = workout.exerciseIds.joinToString(",")
        )
    }

    fun get(id: String): Workout? = db.omniGartQueries.selectWorkout(id).executeAsOneOrNull()?.toDomain()

    fun getAll(): List<Workout> = db.omniGartQueries.selectAllWorkouts().executeAsList().map { it.toDomain() }
}

class SessionRepository(private val db: OmniGartDatabase) {
    fun upsert(session: Session) {
        db.transaction {
            db.omniGartQueries.upsertSession(
                id = session.id,
                workout_id = session.workoutId,
                started_at = session.startedAt.toString(),
                ended_at = session.endedAt?.toString(),
                perceived_exertion = session.perceivedExertion?.toLong(),
                notes = session.notes
            )
            db.omniGartQueries.deleteSetsForSession(session.id)
            session.sets.forEach { set ->
                db.omniGartQueries.upsertSet(
                    id = set.id,
                    session_id = session.id,
                    exercise_id = set.exerciseId,
                    set_index = set.setIndex.toLong(),
                    reps = set.reps?.toLong(),
                    weight_kg = set.weightKg,
                    duration_seconds = set.durationSeconds?.toLong(),
                    distance_metres = set.distanceMetres,
                    rir = set.rir,
                    completed = if (set.completed) 1L else 0L,
                    is_warmup = if (set.isWarmup) 1L else 0L,
                    assistance_kg = set.assistanceKg,
                    logged_per_hand = if (set.loggedPerHand) 1L else 0L
                )
            }
        }
    }

    fun get(id: String): Session? {
        val row = db.omniGartQueries.selectSession(id).executeAsOneOrNull() ?: return null
        val sets =
            db.omniGartQueries.selectSetsForSession(id).executeAsList().map { it.toDomain() }
        return row.toDomain(sets)
    }

    fun getAll(): List<Session> = db.omniGartQueries.selectAllSessions().executeAsList().map { row ->
        val sets =
            db.omniGartQueries.selectSetsForSession(row.id).executeAsList().map {
                it.toDomain()
            }
        row.toDomain(sets)
    }

    fun deleteSet(setId: String) {
        db.omniGartQueries.deleteSet(setId)
    }
}

fun createDatabase(driverFactory: DatabaseDriverFactory): OmniGartDatabase = OmniGartDatabase(driverFactory.createDriver())

private fun com.omnigart.shared.db.User_profile.toDomain(): UserProfile = UserProfile(
    id = id,
    biologicalSex = BiologicalSex.valueOf(biological_sex),
    heightCm = height_cm,
    weightKg = weight_kg,
    dateOfBirth = LocalDate.parse(date_of_birth),
    preferredUnits = UnitSystem.valueOf(preferred_units),
    waistCm = waist_cm,
    hipCm = hip_cm,
    primaryGoal = runCatching { ProfileGoal.valueOf(primary_goal) }.getOrDefault(ProfileGoal.GENERAL_FITNESS),
    strengthLevel = runCatching { StrengthLevel.valueOf(strength_level) }.getOrDefault(StrengthLevel.BEGINNER),
    activityLevel = runCatching { ActivityLevel.valueOf(activity_level) }.getOrDefault(ActivityLevel.MODERATE),
    onboardingCompleted = onboarding_completed != 0L
)

private fun com.omnigart.shared.db.Exercise.toDomain(): Exercise = Exercise(
    id = id,
    contentKey = content_key,
    modality = ExerciseModality.valueOf(modality),
    primaryMuscles = if (primary_muscles.isBlank()) emptyList() else primary_muscles.split(",")
)

private fun com.omnigart.shared.db.Workout.toDomain(): Workout = Workout(
    id = id,
    name = name,
    notes = notes,
    exerciseIds = if (exercise_ids.isBlank()) emptyList() else exercise_ids.split(",")
)

private fun com.omnigart.shared.db.Session.toDomain(sets: List<WorkoutSet>): Session = Session(
    id = id,
    workoutId = workout_id,
    startedAt = Instant.parse(started_at),
    endedAt = ended_at?.let { Instant.parse(it) },
    sets = sets,
    perceivedExertion = perceived_exertion?.toInt(),
    notes = notes
)

private fun com.omnigart.shared.db.Workout_set.toDomain(): WorkoutSet = WorkoutSet(
    id = id,
    exerciseId = exercise_id,
    setIndex = set_index.toInt(),
    reps = reps?.toInt(),
    weightKg = weight_kg,
    durationSeconds = duration_seconds?.toInt(),
    distanceMetres = distance_metres,
    rir = rir,
    completed = completed != 0L,
    isWarmup = is_warmup != 0L,
    assistanceKg = assistance_kg,
    loggedPerHand = logged_per_hand != 0L
)
