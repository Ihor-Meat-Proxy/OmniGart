package com.omnigart.insights

import com.omnigart.insights.fitness.FitnessAge
import com.omnigart.insights.fitness.FitnessAgeInput
import com.omnigart.insights.fitness.FitnessSex
import com.omnigart.insights.habits.HabitEffectAnalysis
import com.omnigart.insights.habits.HabitEffectInput
import com.omnigart.insights.habits.HabitPair
import com.omnigart.insights.habits.HabitConfidence
import com.omnigart.insights.load.LoadTarget
import com.omnigart.insights.load.LoadTargetInput
import com.omnigart.insights.notes.DeviationDirection
import com.omnigart.insights.notes.MetricPoint
import com.omnigart.insights.notes.OutOfRangeNotes
import com.omnigart.insights.readiness.DailyReadiness
import com.omnigart.insights.readiness.DailyReadinessInput
import com.omnigart.insights.readiness.ReadinessBand
import com.omnigart.insights.reserve.DayReserve
import com.omnigart.insights.reserve.DayReserveInput
import com.omnigart.insights.sleep.SleepBalance
import com.omnigart.insights.sleep.SleepBalanceInput
import com.omnigart.insights.weekly.DailySnapshot
import com.omnigart.insights.weekly.WeeklyOverview
import com.omnigart.insights.weekly.WeeklyOverviewInput
import kotlin.math.abs
import kotlin.math.ln
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class InsightsFormulasTest {

    @Test
    fun dailyReadiness_usesLnRmssdAndEvidenceClaim() {
        val history = List(20) { 60.0 + (it % 5) }
        val rhrHist = List(20) { 52.0 + (it % 3) }
        val result = DailyReadiness.compute(
            DailyReadinessInput(
                rmssdMs = 70.0,
                rmssdHistoryNewestFirst = history,
                restingHrBpm = 50.0,
                restingHrHistoryNewestFirst = rhrHist,
                sleepHours = 8.0,
                sleepNeedHours = 8.0,
            ),
        )
        assertEquals(InsightEvidence.DAILY_READINESS, result.claimId)
        assertEquals(ln(70.0), result.value.lnRmssd, 1e-9)
        assertNotNull(result.value.baseline7)
        assertNotNull(result.value.baseline60)
        assertTrue(result.value.score in 0..100)
        assertTrue(result.value.band == ReadinessBand.HIGH || result.value.band == ReadinessBand.MODERATE)
    }

    @Test
    fun loadTarget_computesAcuteChronicAndRange() {
        val days = List(28) { if (it < 7) 400.0 else 300.0 }
        val result = LoadTarget.compute(LoadTargetInput(dailyLoadsNewestFirst = days))
        assertEquals(InsightEvidence.LOAD_TARGET, result.claimId)
        assertEquals(400.0, result.value.acuteMean, 0.01)
        assertTrue(result.value.chronicMean < result.value.acuteMean)
        assertNotNull(result.value.acuteChronicRatio)
        assertTrue(result.value.recommendedDailyMax >= result.value.recommendedDailyMin)
        assertTrue(result.value.cautionNote.contains("not a universal injury predictor"))
    }

    @Test
    fun sleepBalance_needAndDebt() {
        val result = SleepBalance.compute(
            SleepBalanceInput(
                ageYears = 30,
                lastNightHours = 6.0,
                recentNightsNewestFirst = listOf(6.0, 6.5, 7.0, 5.5),
            ),
        )
        assertEquals(8.0, result.value.needHoursUsed, 0.01)
        assertTrue(result.value.debtHours > 0.0)
        assertTrue(result.value.claimIds.contains(InsightEvidence.SLEEP_BALANCE_NEED))
        assertTrue(result.value.claimIds.contains(InsightEvidence.SLEEP_BALANCE_DEBT))
    }

    @Test
    fun habitEffect_flagsLowConfidenceForSmallN() {
        val pairs = listOf(
            HabitPair(70.0, 60.0),
            HabitPair(68.0, 62.0),
            HabitPair(72.0, 65.0),
        )
        val result = HabitEffectAnalysis.compute(
            HabitEffectInput(pairs, habitLabel = "evening walk", metricLabel = "readiness"),
        )
        assertEquals(InsightEvidence.HABIT_EFFECT, result.claimId)
        assertEquals(HabitConfidence.VERY_LOW, result.value.confidence)
        assertTrue(result.value.meanDifference > 0)
        assertTrue(result.value.caveat.contains("Exploratory"))
    }

    @Test
    fun outOfRange_detectsDeviation() {
        val constant = List(14) { 50.0 }
        val skipped = OutOfRangeNotes.compute(
            listOf(MetricPoint("rhr", "Resting HR", 62.0, constant, zThreshold = 1.5)),
        )
        assertEquals(0, skipped.value.notes.size)

        // Clear elevation vs a tight personal baseline.
        val varying = List(14) { i -> 50.0 + (i % 3) * 0.2 }
        val result = OutOfRangeNotes.compute(
            listOf(MetricPoint("rhr", "Resting HR", 56.0, varying, zThreshold = 1.5)),
        )
        assertEquals(InsightEvidence.OUT_OF_RANGE, result.claimId)
        assertEquals(1, result.value.evaluatedCount)
        assertEquals(1, result.value.notes.size)
        assertEquals(DeviationDirection.ABOVE, result.value.notes.first().direction)
    }

    @Test
    fun weeklyOverview_aggregates() {
        val days = List(7) { i ->
            DailySnapshot(
                readinessScore = 60 + i,
                trainingLoad = if (i % 2 == 0) 300.0 else 0.0,
                sleepHours = 7.0 + (i % 3) * 0.2,
            )
        }
        val result = WeeklyOverview.compute(WeeklyOverviewInput(days))
        assertEquals(InsightEvidence.WEEKLY_OVERVIEW, result.claimId)
        assertEquals(7, result.value.dayCount)
        assertEquals(4, result.value.trainingDays)
        assertNotNull(result.value.meanReadiness)
    }

    @Test
    fun dayReserve_scalesWithReadiness() {
        val ready = DailyReadiness.compute(
            DailyReadinessInput(
                rmssdMs = 80.0,
                rmssdHistoryNewestFirst = List(10) { 55.0 },
                restingHrBpm = 48.0,
                restingHrHistoryNewestFirst = List(10) { 55.0 },
                sleepHours = 8.5,
                sleepNeedHours = 8.0,
            ),
        ).value
        val load = LoadTarget.compute(
            LoadTargetInput(dailyLoadsNewestFirst = List(28) { 300.0 }),
        ).value
        val reserve = DayReserve.compute(
            DayReserveInput(readiness = ready, loadTarget = load, loadSoFarToday = 0.0),
        )
        assertEquals(InsightEvidence.DAY_RESERVE, reserve.claimId)
        assertTrue(reserve.value.reserveScore in 0..100)
        assertTrue(reserve.value.suggestedRemainingLoadMax >= reserve.value.suggestedRemainingLoadMin)
    }

    @Test
    fun fitnessAge_mapsVo2ToNormBand() {
        val youngVo2 = FitnessAge.medianVo2ForAge(FitnessSex.MALE, 20)
        val result = FitnessAge.compute(
            FitnessAgeInput(chronologicalAgeYears = 45, sex = FitnessSex.MALE, vo2maxMlKgMin = youngVo2),
        )
        assertEquals(InsightEvidence.FITNESS_AGE, result.claimId)
        assertEquals(20, result.value.fitnessAgeYears)
        assertTrue(result.value.note.contains("not a medical"))
        assertTrue(abs(result.value.vo2maxMlKgMin - youngVo2) < 0.2)
    }

    @Test
    fun allInsightClaimIds_areUniqueAndStable() {
        assertEquals(9, InsightEvidence.ALL.size)
        assertTrue(InsightEvidence.ALL.all { it.startsWith("insight-") })
    }
}
