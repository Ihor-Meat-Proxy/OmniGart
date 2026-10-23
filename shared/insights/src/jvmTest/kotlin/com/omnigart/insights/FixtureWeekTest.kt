package com.omnigart.insights

import com.omnigart.insights.load.LoadTarget
import com.omnigart.insights.load.LoadTargetInput
import com.omnigart.insights.readiness.DailyReadiness
import com.omnigart.insights.readiness.DailyReadinessInput
import com.omnigart.insights.weekly.DailySnapshot
import com.omnigart.insights.weekly.WeeklyOverview
import com.omnigart.insights.weekly.WeeklyOverviewInput
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FixtureWeekTest {

    @Serializable
    private data class DayFx(
        val rmssdMs: Double,
        val restingHrBpm: Double,
        val sleepHours: Double,
        val sessionRpe: Double,
        val durationMinutes: Double,
    )

    @Serializable
    private data class WeekFx(
        val sleepNeedHours: Double,
        val daysNewestFirst: List<DayFx>,
    )

    @Test
    fun sampleWeekFixture_producesCoherentInsights() {
        val text = javaClass.getResource("/fixtures/sample-week.json")!!.readText()
        val json = Json { ignoreUnknownKeys = true }
        val fx = json.decodeFromString(WeekFx.serializer(), text)
        val rmssdHist = fx.daysNewestFirst.map { it.rmssdMs }
        val rhrHist = fx.daysNewestFirst.map { it.restingHrBpm }
        val today = fx.daysNewestFirst.first()

        val readiness = DailyReadiness.compute(
            DailyReadinessInput(
                rmssdMs = today.rmssdMs,
                rmssdHistoryNewestFirst = rmssdHist.drop(1),
                restingHrBpm = today.restingHrBpm,
                restingHrHistoryNewestFirst = rhrHist.drop(1),
                sleepHours = today.sleepHours,
                sleepNeedHours = fx.sleepNeedHours,
            ),
        )
        assertEquals(InsightEvidence.DAILY_READINESS, readiness.claimId)

        val dailyLoads = fx.daysNewestFirst.map { it.sessionRpe * it.durationMinutes }
        // Pad chronic window with mild prior load so ratio is defined.
        val padded = dailyLoads + List(21) { 250.0 }
        val load = LoadTarget.compute(LoadTargetInput(dailyLoadsNewestFirst = padded))
        assertEquals(InsightEvidence.LOAD_TARGET, load.claimId)

        val week = WeeklyOverview.compute(
            WeeklyOverviewInput(
                daysNewestFirst = fx.daysNewestFirst.mapIndexed { idx, d ->
                    DailySnapshot(
                        readinessScore = readiness.value.score - idx,
                        trainingLoad = d.sessionRpe * d.durationMinutes,
                        sleepHours = d.sleepHours,
                    )
                },
            ),
        )
        assertEquals(7, week.value.dayCount)
        assertTrue(week.value.totalLoad > 0.0)
    }
}
