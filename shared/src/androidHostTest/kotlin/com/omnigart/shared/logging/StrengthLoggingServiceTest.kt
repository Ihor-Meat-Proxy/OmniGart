package com.omnigart.shared.logging

import com.omnigart.shared.calc.MassUnit
import com.omnigart.shared.calc.strategy.FormulaSettingsService
import com.omnigart.shared.data.ExerciseRepository
import com.omnigart.shared.data.SessionRepository
import com.omnigart.shared.data.UserProfileRepository
import com.omnigart.shared.data.createInMemoryDatabase
import com.omnigart.shared.domain.BiologicalSex
import com.omnigart.shared.domain.UserProfile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.datetime.LocalDate

class StrengthLoggingServiceTest {
    private fun service(): StrengthLoggingService {
        val db = createInMemoryDatabase()
        val profiles = UserProfileRepository(db)
        profiles.upsert(
            UserProfile(
                id = "p1",
                biologicalSex = BiologicalSex.MALE,
                heightCm = 180.0,
                weightKg = 80.0,
                dateOfBirth = LocalDate(1990, 1, 1)
            )
        )
        return StrengthLoggingService(
            sessions = SessionRepository(db),
            exercises = ExerciseRepository(db),
            profiles = profiles,
            formulaSettings = FormulaSettingsService(db)
        )
    }

    @Test
    fun bodyweightLoad_usesCoefficient() {
        val svc = service()
        svc.ensureDemoExercises()
        val session = svc.startSession(startedAt = Instant.parse("2026-06-01T10:00:00Z"))
        svc.addSet(sessionId = session.id, exerciseId = "push-up", reps = 10)
        val summary =
            svc.sessionSummary(
                sessionId = session.id,
                includeWarmups = false,
                bodyweightKg = 80.0,
                unit = MassUnit.KG
            )
        // 80 × 0.64 × 10 = 512
        assertEquals(512.0, summary.volume.totalVolumeKg, absoluteTolerance = 0.001)
        assertEquals(512.0, summary.totalWeightLiftedKg, absoluteTolerance = 0.001)
        assertTrue(summary.volume.perExercise.single().estimated)
    }

    @Test
    fun assisted_floorsAtZero() {
        val svc = service()
        svc.ensureDemoExercises()
        val session = svc.startSession(startedAt = Instant.parse("2026-06-01T10:00:00Z"))
        svc.addSet(
            sessionId = session.id,
            exerciseId = "assisted-pull-up-band",
            reps = 5,
            assistanceKg = 200.0
        )
        val summary =
            svc.sessionSummary(
                sessionId = session.id,
                includeWarmups = false,
                bodyweightKg = 80.0,
                unit = MassUnit.KG
            )
        assertEquals(0.0, summary.volume.totalVolumeKg, absoluteTolerance = 0.001)
    }

    @Test
    fun unilateral_loggedPerHand_doublesVolume() {
        val svc = service()
        svc.ensureDemoExercises()
        val session = svc.startSession(startedAt = Instant.parse("2026-06-01T10:00:00Z"))
        svc.addSet(
            sessionId = session.id,
            exerciseId = "reverse-lunge-dumbbell",
            reps = 10,
            weightKg = 20.0,
            loggedPerHand = true
        )
        val summary =
            svc.sessionSummary(
                sessionId = session.id,
                includeWarmups = false,
                bodyweightKg = 80.0,
                unit = MassUnit.KG
            )
        // 20 × 10 × 2 = 400
        assertEquals(400.0, summary.volume.totalVolumeKg, absoluteTolerance = 0.001)
        assertEquals(2, summary.volume.setContributions.single().sideMultiplier)
    }

    @Test
    fun warmup_excludedByDefault_includedWhenToggled() {
        val svc = service()
        svc.ensureDemoExercises()
        val session = svc.startSession(startedAt = Instant.parse("2026-06-01T10:00:00Z"))
        svc.addSet(
            sessionId = session.id,
            exerciseId = "barbell-bench-press",
            reps = 8,
            weightKg = 60.0,
            isWarmup = true
        )
        svc.addSet(
            sessionId = session.id,
            exerciseId = "barbell-bench-press",
            reps = 5,
            weightKg = 100.0,
            isWarmup = false
        )
        val without =
            svc.sessionSummary(
                sessionId = session.id,
                includeWarmups = false,
                bodyweightKg = 80.0,
                unit = MassUnit.KG
            )
        assertEquals(500.0, without.volume.totalVolumeKg, absoluteTolerance = 0.001)
        assertEquals(1, without.volume.hardSets)

        val withWarmups =
            svc.sessionSummary(
                sessionId = session.id,
                includeWarmups = true,
                bodyweightKg = 80.0,
                unit = MassUnit.KG
            )
        assertEquals(500.0 + 480.0, withWarmups.volume.totalVolumeKg, absoluteTolerance = 0.001)
        assertEquals(1, withWarmups.volume.hardSets)
        assertEquals(980.0, withWarmups.totalWeightLiftedKg, absoluteTolerance = 0.001)
    }

    @Test
    fun addUpdateDeleteSet_andEndSession() {
        val svc = service()
        svc.ensureDemoExercises()
        val session = svc.startSession(startedAt = Instant.parse("2026-06-01T10:00:00Z"))
        var current =
            svc.addSet(
                sessionId = session.id,
                exerciseId = "barbell-bench-press",
                reps = 5,
                weightKg = 100.0,
                rir = 2.0
            )
        assertEquals(1, current.sets.size)
        val set = current.sets.single().copy(reps = 3, weightKg = 110.0)
        current = svc.updateSet(session.id, set)
        assertEquals(3, current.sets.single().reps)
        current = svc.deleteSet(session.id, set.id)
        assertTrue(current.sets.isEmpty())
        current = svc.endSession(session.id, Instant.parse("2026-06-01T11:00:00Z"))
        assertEquals(Instant.parse("2026-06-01T11:00:00Z"), current.endedAt)
    }
}
