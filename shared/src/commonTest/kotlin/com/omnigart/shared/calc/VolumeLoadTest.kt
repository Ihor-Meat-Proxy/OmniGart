package com.omnigart.shared.calc

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class VolumeLoadTest {
    private val bench =
        VolumeExerciseMeta(
            exerciseId = "barbell-bench-press",
            unilateral = false,
            primaryMuscles = listOf("pectoralis_major", "triceps", "front_deltoid")
        )

    private val pushUp =
        VolumeExerciseMeta(
            exerciseId = "push-up",
            unilateral = false,
            primaryMuscles = listOf("pectoralis_major", "triceps", "front_deltoid"),
            usesBodyweightLoad = true
        )

    private val assistedPullUp =
        VolumeExerciseMeta(
            exerciseId = "assisted-pull-up-band",
            unilateral = false,
            primaryMuscles = listOf("latissimus_dorsi", "biceps"),
            usesBodyweightLoad = true
        )

    private val weightedPullUp =
        VolumeExerciseMeta(
            exerciseId = "weighted-pull-up",
            unilateral = false,
            primaryMuscles = listOf("latissimus_dorsi", "biceps"),
            usesBodyweightLoad = true
        )

    private val dbLunge =
        VolumeExerciseMeta(
            exerciseId = "reverse-lunge-dumbbell",
            unilateral = true,
            primaryMuscles = listOf("quadriceps", "gluteus_maximus")
        )

    private val exercises =
        mapOf(
            bench.exerciseId to bench,
            pushUp.exerciseId to pushUp,
            assistedPullUp.exerciseId to assistedPullUp,
            weightedPullUp.exerciseId to weightedPullUp,
            dbLunge.exerciseId to dbLunge
        )

    private val opts = VolumeLoadOptions(bodyweightKg = 80.0, includeWarmups = false)

    @Test
    fun externalLoad_volumeIsWeightTimesReps() {
        val sets =
            listOf(
                VolumeSetInput(id = "1", exerciseId = bench.exerciseId, reps = 5, weightKg = 100.0),
                VolumeSetInput(id = "2", exerciseId = bench.exerciseId, reps = 5, weightKg = 100.0)
            )
        val summary = VolumeLoad.summarize(sets, exercises, opts)
        assertEquals(1000.0, summary.totalVolumeKg, absoluteTolerance = 0.001)
        assertEquals(2, summary.hardSets)
        assertEquals(0.0, summary.estimatedVolumeKg, absoluteTolerance = 0.001)
    }

    @Test
    fun warmups_excludedByDefault_includedWhenToggled() {
        val sets =
            listOf(
                VolumeSetInput(
                    id = "w",
                    exerciseId = bench.exerciseId,
                    reps = 8,
                    weightKg = 60.0,
                    isWarmup = true
                ),
                VolumeSetInput(id = "1", exerciseId = bench.exerciseId, reps = 5, weightKg = 100.0)
            )
        val without = VolumeLoad.summarize(sets, exercises, opts)
        assertEquals(500.0, without.totalVolumeKg, absoluteTolerance = 0.001)
        assertEquals(1, without.hardSets)
        assertFalse(without.setContributions.first().included)

        val withWarmups =
            VolumeLoad.summarize(sets, exercises, opts.copy(includeWarmups = true))
        assertEquals(500.0 + 480.0, withWarmups.totalVolumeKg, absoluteTolerance = 0.001)
        assertEquals(1, withWarmups.hardSets) // warm-ups never count as hard sets
    }

    @Test
    fun bodyweight_usesCoefficientAndFlagsEstimated() {
        // 80 kg × 0.64 × 10 reps = 512
        val sets =
            listOf(
                VolumeSetInput(id = "1", exerciseId = pushUp.exerciseId, reps = 10)
            )
        val summary = VolumeLoad.summarize(sets, exercises, opts)
        assertEquals(512.0, summary.totalVolumeKg, absoluteTolerance = 0.001)
        assertTrue(summary.perExercise.single().estimated)
        assertEquals(512.0, summary.estimatedVolumeKg, absoluteTolerance = 0.001)
        assertEquals(
            BodyweightLoadCoefficients.PUSH_UP,
            VolumeLoad.loadCoefficient(pushUp),
            absoluteTolerance = 0.0
        )
    }

    @Test
    fun weightedBodyweight_addsExternalMass() {
        // (80 × 1.0 + 20) × 5 = 500
        val sets =
            listOf(
                VolumeSetInput(
                    id = "1",
                    exerciseId = weightedPullUp.exerciseId,
                    reps = 5,
                    weightKg = 20.0
                )
            )
        val summary = VolumeLoad.summarize(sets, exercises, opts)
        assertEquals(500.0, summary.totalVolumeKg, absoluteTolerance = 0.001)
        assertTrue(summary.perExercise.single().estimated)
    }

    @Test
    fun assisted_subtractsAssistanceAndFloorsAtZero() {
        // (80 × 1.0 − 30) × 8 = 400
        val sets =
            listOf(
                VolumeSetInput(
                    id = "1",
                    exerciseId = assistedPullUp.exerciseId,
                    reps = 8,
                    assistanceKg = 30.0
                )
            )
        val summary = VolumeLoad.summarize(sets, exercises, opts)
        assertEquals(400.0, summary.totalVolumeKg, absoluteTolerance = 0.001)

        val overAssisted =
            listOf(
                VolumeSetInput(
                    id = "2",
                    exerciseId = assistedPullUp.exerciseId,
                    reps = 5,
                    assistanceKg = 200.0
                )
            )
        val floored = VolumeLoad.summarize(overAssisted, exercises, opts)
        assertEquals(0.0, floored.totalVolumeKg, absoluteTolerance = 0.001)
    }

    @Test
    fun unilateral_loggedPerHand_countsBothSides() {
        // 20 kg × 10 reps × 2 sides = 400
        val sets =
            listOf(
                VolumeSetInput(
                    id = "1",
                    exerciseId = dbLunge.exerciseId,
                    reps = 10,
                    weightKg = 20.0,
                    loggedPerHand = true
                )
            )
        val summary = VolumeLoad.summarize(sets, exercises, opts)
        assertEquals(400.0, summary.totalVolumeKg, absoluteTolerance = 0.001)
        assertEquals(2, summary.setContributions.single().sideMultiplier)

        val oneSideLogged =
            listOf(
                VolumeSetInput(
                    id = "2",
                    exerciseId = dbLunge.exerciseId,
                    reps = 10,
                    weightKg = 20.0,
                    loggedPerHand = false
                )
            )
        val single = VolumeLoad.summarize(oneSideLogged, exercises, opts)
        assertEquals(200.0, single.totalVolumeKg, absoluteTolerance = 0.001)
    }

    @Test
    fun perMuscle_attributesFullVolumeToEachPrimary() {
        val sets =
            listOf(
                VolumeSetInput(id = "1", exerciseId = bench.exerciseId, reps = 5, weightKg = 100.0)
            )
        val summary = VolumeLoad.summarize(sets, exercises, opts)
        assertEquals(500.0, summary.perMuscleVolumeKg.getValue("pectoralis_major"), absoluteTolerance = 0.001)
        assertEquals(500.0, summary.perMuscleVolumeKg.getValue("triceps"), absoluteTolerance = 0.001)
        assertEquals(500.0, summary.perMuscleVolumeKg.getValue("front_deltoid"), absoluteTolerance = 0.001)
    }

    @Test
    fun displayUnit_lb() {
        val sets =
            listOf(
                VolumeSetInput(id = "1", exerciseId = bench.exerciseId, reps = 10, weightKg = 100.0)
            )
        val summary =
            VolumeLoad.summarize(sets, exercises, opts.copy(displayUnit = MassUnit.LB))
        assertEquals(1000.0, summary.totalVolumeKg, absoluteTolerance = 0.001)
        assertEquals(
            MassUnits.kgToDisplay(1000.0, MassUnit.LB),
            summary.totalVolumeDisplay,
            absoluteTolerance = 0.001
        )
        assertEquals(MassUnit.LB, summary.displayUnit)
    }

    @Test
    fun incompleteOrZeroReps_excluded() {
        val sets =
            listOf(
                VolumeSetInput(
                    id = "1",
                    exerciseId = bench.exerciseId,
                    reps = 5,
                    weightKg = 100.0,
                    completed = false
                ),
                VolumeSetInput(id = "2", exerciseId = bench.exerciseId, reps = 0, weightKg = 100.0)
            )
        val summary = VolumeLoad.summarize(sets, exercises, opts)
        assertEquals(0.0, summary.totalVolumeKg, absoluteTolerance = 0.001)
        assertEquals(0, summary.hardSets)
    }

    @Test
    fun compareWithPrevious_byWorkoutId() {
        val t0 = Instant.parse("2026-01-01T10:00:00Z")
        val t1 = Instant.parse("2026-01-08T10:00:00Z")
        val previous =
            VolumeSessionInput(
                id = "prev",
                workoutId = "tmpl-a",
                startedAt = t0,
                sets =
                listOf(
                    VolumeSetInput(id = "p1", exerciseId = bench.exerciseId, reps = 5, weightKg = 80.0)
                )
            )
        val current =
            VolumeSessionInput(
                id = "cur",
                workoutId = "tmpl-a",
                startedAt = t1,
                sets =
                listOf(
                    VolumeSetInput(id = "c1", exerciseId = bench.exerciseId, reps = 5, weightKg = 100.0)
                )
            )
        val found = VolumeLoadStats.findPreviousSameWorkout(current, listOf(previous))
        assertEquals("prev", found?.id)

        val curSummary = VolumeLoad.summarize(current.sets, exercises, opts)
        val prevSummary = VolumeLoad.summarize(previous.sets, exercises, opts)
        val cmp = assertNotNull(VolumeLoadStats.compareWithPrevious(curSummary, prevSummary))
        assertEquals(100.0, cmp.volumeDeltaKg, absoluteTolerance = 0.001) // 500 - 400
        assertEquals(0, cmp.hardSetsDelta)
    }

    @Test
    fun findPrevious_fallsBackToExerciseMultiset() {
        val t0 = Instant.parse("2026-01-01T10:00:00Z")
        val t1 = Instant.parse("2026-01-08T10:00:00Z")
        val previous =
            VolumeSessionInput(
                id = "prev",
                workoutId = null,
                startedAt = t0,
                sets =
                listOf(
                    VolumeSetInput(id = "p1", exerciseId = bench.exerciseId, reps = 5, weightKg = 80.0),
                    VolumeSetInput(id = "p2", exerciseId = pushUp.exerciseId, reps = 10)
                )
            )
        val current =
            VolumeSessionInput(
                id = "cur",
                workoutId = null,
                startedAt = t1,
                sets =
                listOf(
                    VolumeSetInput(id = "c1", exerciseId = pushUp.exerciseId, reps = 12),
                    VolumeSetInput(id = "c2", exerciseId = bench.exerciseId, reps = 5, weightKg = 90.0)
                )
            )
        assertEquals("prev", VolumeLoadStats.findPreviousSameWorkout(current, listOf(previous))?.id)

        val unrelated =
            VolumeSessionInput(
                id = "other",
                startedAt = t0,
                sets =
                listOf(
                    VolumeSetInput(id = "o1", exerciseId = dbLunge.exerciseId, reps = 8, weightKg = 20.0)
                )
            )
        assertNull(VolumeLoadStats.findPreviousSameWorkout(current, listOf(unrelated)))
    }

    @Test
    fun detectPersonalRecords_e1rmSetAndSession() {
        val t0 = Instant.parse("2026-01-01T10:00:00Z")
        val t1 = Instant.parse("2026-01-08T10:00:00Z")
        val previous =
            VolumeSessionInput(
                id = "prev",
                startedAt = t0,
                sets =
                listOf(
                    VolumeSetInput(id = "p1", exerciseId = bench.exerciseId, reps = 5, weightKg = 80.0)
                )
            )
        val current =
            VolumeSessionInput(
                id = "cur",
                startedAt = t1,
                sets =
                listOf(
                    VolumeSetInput(id = "c1", exerciseId = bench.exerciseId, reps = 5, weightKg = 100.0),
                    VolumeSetInput(id = "c2", exerciseId = bench.exerciseId, reps = 5, weightKg = 100.0)
                )
            )
        val summary = VolumeLoad.summarize(current.sets, exercises, opts)
        val prs =
            VolumeLoadStats.detectPersonalRecords(current, summary, listOf(previous), exercises, opts)
        assertTrue(prs.any { it.kind == PersonalRecordKind.BEST_E1RM })
        assertTrue(prs.any { it.kind == PersonalRecordKind.BEST_SET_VOLUME })
        assertTrue(prs.any { it.kind == PersonalRecordKind.BEST_SESSION_VOLUME })
    }

    @Test
    fun aggregatePeriod_weeklyStyle() {
        val t0 = Instant.parse("2026-01-01T10:00:00Z")
        val t1 = Instant.parse("2026-01-03T10:00:00Z")
        val s1 =
            VolumeSessionInput(
                id = "s1",
                startedAt = t0,
                sets =
                listOf(
                    VolumeSetInput(id = "a", exerciseId = bench.exerciseId, reps = 5, weightKg = 100.0)
                )
            )
        val s2 =
            VolumeSessionInput(
                id = "s2",
                startedAt = t1,
                sets =
                listOf(
                    VolumeSetInput(id = "b", exerciseId = pushUp.exerciseId, reps = 10)
                )
            )
        val period = VolumeLoadStats.aggregatePeriod(listOf(s1, s2), exercises, opts)
        assertEquals(2, period.sessionCount)
        assertEquals(500.0 + 512.0, period.totalVolumeKg, absoluteTolerance = 0.001)
        assertEquals(2, period.hardSets)
        assertEquals(500.0, period.perExerciseVolumeKg.getValue(bench.exerciseId), absoluteTolerance = 0.001)
        assertEquals(512.0, period.estimatedVolumeKg, absoluteTolerance = 0.001)
        assertTrue(period.perMuscleVolumeKg.containsKey("pectoralis_major"))
    }
}
