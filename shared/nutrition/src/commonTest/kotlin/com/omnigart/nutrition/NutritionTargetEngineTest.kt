package com.omnigart.nutrition

import com.omnigart.nutrition.activity.ActivityEnergy
import com.omnigart.nutrition.bmr.BmrSelector
import com.omnigart.nutrition.goal.ExerciseCalorieDiscount
import com.omnigart.nutrition.goal.GoalTargets
import com.omnigart.nutrition.guardrails.Guardrails
import com.omnigart.nutrition.tdee.AdaptiveTdee
import com.omnigart.nutrition.tdee.DataSufficiency
import com.omnigart.nutrition.targets.WeekdayTargets
import com.omnigart.nutrition.water.WaterGoal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NutritionTargetEngineTest {

    @Test
    fun noWeight_returnsNoTarget() {
        val result =
            NutritionTargetEngine.compute(
                NutritionTargetEngine.Input(
                    profile =
                        NutritionProfileInput(
                            sex = NutritionSex.FEMALE,
                            ageYears = 30,
                            heightCm = 168.0,
                            weightKg = null,
                        ),
                    goal = NutritionGoalSettings(),
                    intakeDays = emptyList(),
                    weights = emptyList(),
                    activityDays = emptyList(),
                ),
            )
        assertNull(result.suggestedDailyKcal)
        assertTrue(result.changeReasons.any { it.key == "nutrition_reason_no_weight" })
    }

    @Test
    fun missingHeight_usesHenryOxfordRough() {
        val bmr =
            BmrSelector.select(
                weightKg = 70.0,
                heightCm = null,
                ageYears = 30,
                sex = NutritionSex.MALE,
                bodyFatPercent = null,
                trustedBodyFat = false,
                athletic = false,
            )
        assertNotNull(bmr)
        assertEquals(BmrMethod.HENRY_OXFORD_WEIGHT_ONLY, bmr.method)
        assertTrue(bmr.roughEstimate)
    }

    @Test
    fun unspecifiedSex_averagesMaleFemale() {
        val male = BmrSelector.mifflinStJeor(70.0, 175.0, 30, NutritionSex.MALE)
        val female = BmrSelector.mifflinStJeor(70.0, 175.0, 30, NutritionSex.FEMALE)
        val avg = BmrSelector.mifflinStJeor(70.0, 175.0, 30, NutritionSex.UNSPECIFIED)
        assertEquals((male + female) / 2.0, avg, absoluteTolerance = 0.01)
    }

    @Test
    fun trustedBodyFat_usesKatch() {
        val bmr =
            BmrSelector.select(
                weightKg = 80.0,
                heightCm = 180.0,
                ageYears = 28,
                sex = NutritionSex.MALE,
                bodyFatPercent = 15.0,
                trustedBodyFat = true,
                athletic = false,
            )
        assertNotNull(bmr)
        assertEquals(BmrMethod.KATCH_MCARDLE, bmr.method)
    }

    @Test
    fun athleticBodyFat_usesCunningham() {
        val bmr =
            BmrSelector.select(
                weightKg = 80.0,
                heightCm = 180.0,
                ageYears = 28,
                sex = NutritionSex.MALE,
                bodyFatPercent = 10.0,
                trustedBodyFat = true,
                athletic = true,
            )
        assertNotNull(bmr)
        assertEquals(BmrMethod.CUNNINGHAM, bmr.method)
    }

    @Test
    fun activity_neverCombinesMeasuredAndPal() {
        val days =
            listOf(
                ActivityDay("2026-01-01", activeEnergyKcal = 500.0, steps = 12_000),
                ActivityDay("2026-01-02", activeEnergyKcal = 400.0, steps = 11_000),
            )
        val result = ActivityEnergy.resolve(days, bmrKcal = 1600.0, meanIntakeKcal = 2000.0)
        assertEquals(ActivitySource.MEASURED_ACTIVE_ENERGY, result.source)
        assertNull(result.pal)
    }

    @Test
    fun activity_fallsBackToPalWhenNoActiveEnergy() {
        val days = listOf(ActivityDay("2026-01-01", steps = 6_000))
        val result = ActivityEnergy.resolve(days, bmrKcal = 1600.0, meanIntakeKcal = null)
        assertEquals(ActivitySource.PAL_STEPS, result.source)
        assertEquals(1.375, result.pal)
    }

    @Test
    fun weeklyRate_1100KcalPerKgPerWeek() {
        // 0.5%/wk of 80 kg = 0.4 kg/wk → 440 kcal/day deficit
        val delta = GoalTargets.dailyDeltaKcal(80.0, NutritionGoalKind.LOSE, 0.005)
        assertEquals(-440.0, delta, absoluteTolerance = 0.01)
    }

    @Test
    fun taper_fullAt5kg_zeroAt1kg() {
        assertEquals(1.0, GoalTargets.taperFactor(80.0, 74.0), absoluteTolerance = 0.01)
        assertEquals(0.0, GoalTargets.taperFactor(80.0, 79.5), absoluteTolerance = 0.01)
        assertEquals(0.5, GoalTargets.taperFactor(80.0, 77.0), absoluteTolerance = 0.01)
    }

    @Test
    fun exerciseDiscount_scalesWithBodyFat() {
        val lean = ExerciseCalorieDiscount.factor(12.0)
        val higher = ExerciseCalorieDiscount.factor(35.0)
        assertEquals(0.72, lean, absoluteTolerance = 0.01)
        assertTrue(higher < lean)
    }

    @Test
    fun floorCheck_warnsBelow() {
        val warning = Guardrails.checkFloor(1100.0, 1200.0)
        assertTrue(warning.belowFloor)
    }

    @Test
    fun noDeficit_under18_orLowBmi_orPregnancy() {
        assertFalse(Guardrails.deficitAllowed(17, false, 60.0, 170.0))
        assertFalse(Guardrails.deficitAllowed(30, true, 60.0, 170.0))
        assertFalse(Guardrails.deficitAllowed(30, false, 50.0, 170.0))
        assertTrue(Guardrails.deficitAllowed(30, false, 70.0, 170.0))
    }

    @Test
    fun fatShare_slowerLoss_moreFat() {
        assertTrue(AdaptiveTdee.fatShareForRate(-0.3) > AdaptiveTdee.fatShareForRate(-1.2))
        assertTrue(AdaptiveTdee.fatShareForRate(0.6) > AdaptiveTdee.fatShareForRate(0.1))
    }

    @Test
    fun weeklyCap_limitsChange() {
        val capped = AdaptiveTdee.applyWeeklyCap(2000.0, 2300.0)
        assertEquals(2100.0, capped, absoluteTolerance = 0.01)
    }

    @Test
    fun dataSufficiency_requires4FoodAnd1WeighIn() {
        val intake =
            (1..7).map { i ->
                IntakeDay(
                    dayKey = "2026-01-0$i",
                    energyKcal = if (i <= 3) 2000.0 else null,
                    status = if (i <= 3) DayLogStatus.COMPLETE else DayLogStatus.EMPTY,
                )
            }
        val week =
            DataSufficiency.assessWeek(
                intakeDays = intake,
                weights = listOf(WeightPoint(1_000L, 70.0)),
                weekStartEpochMs = 0L,
                weekEndEpochMs = 10_000L,
            )
        assertFalse(week.sufficient)
        assertEquals(3, week.foodDaysLogged)
    }

    @Test
    fun insufficientData_keepsLastEstimate() {
        val result =
            NutritionTargetEngine.compute(
                NutritionTargetEngine.Input(
                    profile =
                        NutritionProfileInput(
                            sex = NutritionSex.FEMALE,
                            ageYears = 32,
                            heightCm = 168.0,
                            weightKg = 62.0,
                        ),
                    goal = NutritionGoalSettings(),
                    intakeDays = emptyList(),
                    weights = emptyList(),
                    activityDays = emptyList(),
                    previousPublishedTargetKcal = 1900.0,
                    weekStartEpochMs = 0L,
                    weekEndEpochMs = 7L * 86_400_000L,
                ),
            )
        assertEquals(1900.0, result.suggestedDailyKcal)
        assertTrue(result.pausedKeepingLastEstimate)
        assertTrue(result.loggingBreak)
    }

    @Test
    fun baselineTarget_withWeightOnly() {
        val result =
            NutritionTargetEngine.compute(
                NutritionTargetEngine.Input(
                    profile =
                        NutritionProfileInput(
                            sex = NutritionSex.MALE,
                            ageYears = 30,
                            heightCm = 180.0,
                            weightKg = 80.0,
                        ),
                    goal =
                        NutritionGoalSettings(
                            kind = NutritionGoalKind.MAINTAIN,
                        ),
                    intakeDays =
                        (1..7).map {
                            IntakeDay("d$it", 2500.0, DayLogStatus.COMPLETE)
                        },
                    weights = listOf(WeightPoint(1L, 80.0)),
                    activityDays = listOf(ActivityDay("d1", steps = 8_000)),
                    weekStartEpochMs = 0L,
                    weekEndEpochMs = Long.MAX_VALUE,
                ),
            )
        val suggested = result.suggestedDailyKcal
        assertNotNull(suggested)
        assertTrue(suggested > 1500.0)
        assertNotNull(result.proteinG)
        assertNotNull(result.waterLitres)
    }

    @Test
    fun weekdayTargets_trainingBonusPreservesMean() {
        val pattern =
            WeekdayPattern(
                booleanArrayOf(false, true, false, true, false, true, false),
            )
        val map = WeekdayTargets.fromGuidedMean(2000.0, pattern, trainingBonusKcal = 210.0)
        val mean = map.values.average()
        assertEquals(2000.0, mean, absoluteTolerance = 0.5)
    }

    @Test
    fun waterGoal_increasesWithTraining() {
        val base = WaterGoal.litres(NutritionSex.FEMALE, 62.0, 0.0)
        val trained = WaterGoal.litres(NutritionSex.FEMALE, 62.0, 1.0)
        assertTrue(trained > base)
    }

    @Test
    fun dayBoundary_beforeStart_usesYesterday() {
        val key =
            DayBoundary.dayKeyForInstant(
                localMinutesFromMidnight = 2 * 60,
                todayDateKey = "2026-03-10",
                yesterdayDateKey = "2026-03-09",
                dayStartMinutes = 4 * 60,
            )
        assertEquals("2026-03-09", key)
    }

    @Test
    fun incompleteDays_excludedFromMean() {
        val days =
            listOf(
                IntakeDay("a", 2000.0, DayLogStatus.COMPLETE),
                IntakeDay("b", 500.0, DayLogStatus.INCOMPLETE),
                IntakeDay("c", null, DayLogStatus.EMPTY),
            )
        val usable = DataSufficiency.daysForMeanIntake(days)
        assertEquals(1, usable.size)
    }

    @Test
    fun flexibleMode_guardrailsKeepFloor() {
        val floor = Guardrails.effectiveFloorKcal(bmrKcal = 1400.0, sex = NutritionSex.FEMALE)
        val splits = (0..6).associateWith { 900.0 }
        val days = WeekdayTargets.fromFlexibleBudget(7 * 900.0, splits)
        assertTrue(days.values.all { it == 900.0 })
        val guarded = days.mapValues { (_, v) -> maxOf(v, floor) }
        assertTrue(guarded.values.all { it >= floor })
    }

    @Test
    fun energyBalance21_computesWhenWindowRich() {
        val intake =
            (1..21).map { i ->
                IntakeDay("2026-02-${i.toString().padStart(2, '0')}", 2200.0, DayLogStatus.COMPLETE)
            }
        val start = 1_700_000_000_000L
        val weights =
            listOf(
                WeightPoint(start, 80.0),
                WeightPoint(start + 10L * 86_400_000L, 79.6),
                WeightPoint(start + 21L * 86_400_000L, 79.2),
            )
        val result = AdaptiveTdee.energyBalance21(intake, weights)
        assertNotNull(result)
        assertTrue(result.eligible)
        assertTrue(result.expenditureKcal > 0.0)
        assertTrue(result.fatShare in 0.0..1.0)
    }
}
