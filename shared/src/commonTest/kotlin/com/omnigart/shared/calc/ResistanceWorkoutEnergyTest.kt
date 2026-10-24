package com.omnigart.shared.calc

import com.omnigart.shared.domain.BiologicalSex
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ResistanceWorkoutEnergyTest {
    @Test
    fun keytel_male_oneHourModerateHr_inSanityRange() {
        // ~140 bpm, 80 kg, 30 y male, 40 min active → roughly 300–700 kcal
        val kcal =
            ResistanceWorkoutEnergy.keytelActiveKcal(
                averageHrBpm = 140.0,
                weightKg = 80.0,
                ageYears = 30,
                sex = BiologicalSex.MALE,
                activeSeconds = 40.0 * 60.0
            )
        assertTrue(kcal in 280.0..750.0, "got $kcal")
    }

    @Test
    fun keytel_female_scalesBelowMaleAtSameInputs_whenWeightLower() {
        val male =
            ResistanceWorkoutEnergy.keytelActiveKcal(140.0, 80.0, 30, BiologicalSex.MALE, 1800.0)
        val female =
            ResistanceWorkoutEnergy.keytelActiveKcal(140.0, 62.0, 30, BiologicalSex.FEMALE, 1800.0)
        assertTrue(female > 100.0 && female < male)
    }

    @Test
    fun keytel_zeroActive_isZero() {
        assertEquals(
            0.0,
            ResistanceWorkoutEnergy.keytelActiveKcal(140.0, 70.0, 25, BiologicalSex.MALE, 0.0)
        )
    }

    @Test
    fun workBased_compoundHeavySession_inSanityRange() {
        val sets =
            listOf(
                ResistanceEnergySetInput(
                    loadKg = 100.0,
                    reps = 5,
                    estimated1RmKg = 120.0,
                    rpe = 8.0,
                    muscleMassKind = MuscleMassKind.LOWER_COMPOUND
                ),
                ResistanceEnergySetInput(
                    loadKg = 100.0,
                    reps = 5,
                    estimated1RmKg = 120.0,
                    rpe = 8.5,
                    muscleMassKind = MuscleMassKind.LOWER_COMPOUND
                ),
                ResistanceEnergySetInput(
                    loadKg = 100.0,
                    reps = 5,
                    estimated1RmKg = 120.0,
                    rpe = 9.0,
                    muscleMassKind = MuscleMassKind.LOWER_COMPOUND
                ),
                ResistanceEnergySetInput(
                    loadKg = 60.0,
                    reps = 8,
                    estimated1RmKg = 80.0,
                    rpe = 7.0,
                    muscleMassKind = MuscleMassKind.UPPER_COMPOUND
                ),
                ResistanceEnergySetInput(
                    loadKg = 60.0,
                    reps = 8,
                    estimated1RmKg = 80.0,
                    rpe = 7.5,
                    muscleMassKind = MuscleMassKind.UPPER_COMPOUND
                )
            )
        val kcal =
            ResistanceWorkoutEnergy.workBasedActiveKcal(
                sets = sets,
                bodyWeightKg = 80.0,
                restSecondsTotal = 600.0
            )
        // ~45–50 min gym work for a trained adult — expect roughly 80–450 active kcal
        assertTrue(kcal in 40.0..500.0, "got $kcal")
    }

    @Test
    fun workBased_warmupsContributeLessThanWorking() {
        val working =
            ResistanceEnergySetInput(
                loadKg = 80.0,
                reps = 8,
                estimated1RmKg = 100.0,
                rpe = 7.0,
                isWarmup = false,
                muscleMassKind = MuscleMassKind.UPPER_COMPOUND
            )
        val warmup = working.copy(isWarmup = true, loadKg = 40.0, estimated1RmKg = 100.0, rpe = 4.0)
        val w =
            ResistanceWorkoutEnergy.workBasedActiveKcal(listOf(working), bodyWeightKg = 75.0)
        val wu =
            ResistanceWorkoutEnergy.workBasedActiveKcal(listOf(warmup), bodyWeightKg = 75.0)
        assertTrue(wu < w)
    }

    @Test
    fun estimate_prefersKeytelWhenHrPresent() {
        val sets =
            listOf(
                ResistanceEnergySetInput(
                    loadKg = 60.0,
                    reps = 10,
                    estimated1RmKg = 80.0,
                    rpe = 7.0,
                    muscleMassKind = MuscleMassKind.UPPER_COMPOUND
                )
            )
        val withHr =
            ResistanceWorkoutEnergy.estimate(
                ResistanceEnergyInput(
                    weightKg = 70.0,
                    heightCm = 175.0,
                    ageYears = 28,
                    sex = BiologicalSex.MALE,
                    sessionDurationSeconds = 3600.0,
                    sets = sets,
                    restSecondsTotal = 900.0,
                    averageHrBpm = 135.0,
                    activeWorkSeconds = 1200.0
                )
            )
        val withoutHr =
            ResistanceWorkoutEnergy.estimate(
                ResistanceEnergyInput(
                    weightKg = 70.0,
                    heightCm = 175.0,
                    ageYears = 28,
                    sex = BiologicalSex.MALE,
                    sessionDurationSeconds = 3600.0,
                    sets = sets,
                    restSecondsTotal = 900.0,
                    averageHrBpm = null
                )
            )
        assertTrue(withHr.activeKcal > 0 && withoutHr.activeKcal > 0)
        assertTrue(withHr.basalKcal > 0)
        assertEquals(
            withHr.activeKcal + withHr.basalKcal,
            withHr.totalKcal,
            absoluteTolerance = 0.01
        )
        // Keytel over 20 min work at 135 bpm should exceed a single isolation-ish set proxy
        assertTrue(withHr.activeKcal > withoutHr.activeKcal * 0.5)
    }

    @Test
    fun estimate_splitsActiveAndBasal() {
        val breakdown =
            ResistanceWorkoutEnergy.estimate(
                ResistanceEnergyInput(
                    weightKg = 62.0,
                    heightCm = 168.0,
                    ageYears = 32,
                    sex = BiologicalSex.FEMALE,
                    sessionDurationSeconds = 1800.0,
                    sets =
                    listOf(
                        ResistanceEnergySetInput(
                            loadKg = 40.0,
                            reps = 10,
                            estimated1RmKg = 55.0,
                            rpe = 7.0,
                            muscleMassKind = MuscleMassKind.UPPER_COMPOUND
                        ),
                        ResistanceEnergySetInput(
                            loadKg = 40.0,
                            reps = 10,
                            estimated1RmKg = 55.0,
                            rpe = 7.5,
                            muscleMassKind = MuscleMassKind.UPPER_COMPOUND
                        )
                    ),
                    restSecondsTotal = 180.0
                )
            )
        val expectedBasal = Bmr.mifflinStJeor(62.0, 168.0, 32, BiologicalSex.FEMALE) * (0.5 / 24.0)
        assertEquals(expectedBasal, breakdown.basalKcal, absoluteTolerance = 0.05)
        assertTrue(breakdown.activeKcal in 5.0..250.0, "got ${breakdown.activeKcal}")
        assertTrue(breakdown.totalKcal > breakdown.activeKcal)
    }

    @Test
    fun rpeFromRir_mapsExpected() {
        assertEquals(8.0, ResistanceWorkoutEnergy.rpeFromRir(2.0))
        assertEquals(10.0, ResistanceWorkoutEnergy.rpeFromRir(0.0))
        assertEquals(null, ResistanceWorkoutEnergy.rpeFromRir(null))
    }

    @Test
    fun muscleMassInfer_lowerCompound() {
        assertEquals(
            MuscleMassKind.LOWER_COMPOUND,
            MuscleMassKindInfer.infer(listOf("quadriceps", "gluteus_maximus"), compound = true)
        )
        assertEquals(
            MuscleMassKind.UPPER_ISOLATION,
            MuscleMassKindInfer.infer(listOf("biceps"), compound = false)
        )
    }

    @Test
    fun rejectsInvalidKeytelInputs() {
        assertFailsWith<IllegalArgumentException> {
            ResistanceWorkoutEnergy.keytelActiveKcal(0.0, 70.0, 25, BiologicalSex.MALE, 60.0)
        }
    }
}
