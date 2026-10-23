package com.omnigart.shared.insights

import com.omnigart.shared.health.WorkoutActivityType
import com.omnigart.shared.health.store.HealthRecordOrigin
import com.omnigart.shared.health.store.HealthSyncIdentity
import com.omnigart.shared.health.store.InMemoryLocalHealthStore
import com.omnigart.shared.health.store.LocalHealthWorkout
import com.omnigart.shared.health.store.LocalHeartRatePoint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class InsightsMetricsTest {
    @Test
    fun fitnessEwmaProducesCtlAtlTsb() {
        val loads = (0L..40L).associateWith { if (it % 2L == 0L) 50.0 else 0.0 }
        val series = FitnessBanister.compute(loads, projectDays = 7)
        assertTrue(series.currentCtl > 0)
        assertTrue(series.currentAtl > 0)
        assertEquals(series.currentCtl - series.currentAtl, series.currentTsb, absoluteTolerance = 1e-6)
        assertTrue(series.points.any { it.projected })
    }

    @Test
    fun trainingLoadPrefersHeartRate() {
        val start = Instant.fromEpochMilliseconds(1_700_000_000_000)
        val series =
            listOf(
                LocalHeartRatePoint(start, 90.0),
                LocalHeartRatePoint(start + 10.minutes, 140.0),
                LocalHeartRatePoint(start + 20.minutes, 160.0)
            )
        val workout =
            LocalHealthWorkout(
                id = "w1",
                identity = HealthSyncIdentity("sync-w1"),
                activityType = WorkoutActivityType.RUNNING,
                startAt = start,
                endAt = start + 30.minutes,
                origin = HealthRecordOrigin.OMNIGART,
                updatedAt = start,
                heartRateSeries = series
            )
        val result =
            TrainingLoadCalculator.fromWorkout(
                workout = workout,
                anchors = TrainingLoadCalculator.HrAnchors(60.0, 190.0),
                calorieProvider = StubCalorieEnergyProvider(),
                weightKg = 70.0
            )
        assertEquals(TrainingLoadCalculator.LoadSource.HEART_RATE_TRIMP, result.source)
        assertTrue(result.load > 0)
    }

    @Test
    fun loadRatioSweetSpot() {
        val sweet = TrainingLoadRatio.compute(atl = 100.0, ctl = 100.0)
        assertEquals(TrainingLoadRatio.Band.SWEET, sweet.band)
        val elevated = TrainingLoadRatio.compute(atl = 160.0, ctl = 100.0)
        assertEquals(TrainingLoadRatio.Band.ELEVATED, elevated.band)
    }

    @Test
    fun hrvStressFromMagnitudeIsEstimate() {
        val r = HrvStressIndex.fromHrvMagnitude(40.0)
        assertNotNull(r.index)
        assertTrue(r.labelledEstimate)
        assertTrue(!r.fromRrIntervals)
    }

    @Test
    fun dashboardBuildsFromLocalStore() {
        val store = InMemoryLocalHealthStore()
        val start = kotlin.time.Clock.System.now() - 2.minutes
        store.upsertWorkout(
            LocalHealthWorkout(
                id = "w2",
                identity = HealthSyncIdentity("sync-w2"),
                activityType = WorkoutActivityType.SWIMMING,
                startAt = start,
                endAt = start + 40.minutes,
                activeEnergyKcal = 400.0,
                distanceMetres = 2000.0,
                origin = HealthRecordOrigin.OMNIGART,
                updatedAt = start
            )
        )
        val dash = InsightsDashboardBuilder(store).build()
        assertTrue(!dash.empty)
        assertEquals(1, dash.allTime.workouts)
        assertNotNull(dash.fitness)
        assertTrue(dash.sportBars12Months.isNotEmpty())
    }

    @Test
    fun aggregateCacheHitsOnSameFingerprint() {
        val store = InMemoryLocalHealthStore()
        val cache = InsightsAggregateCache()
        val builder = InsightsDashboardBuilder(store = store, cache = cache)
        builder.build()
        val fp = builder.fingerprint(store.listWorkouts()) + "|60.0|190.0"
        assertNotNull(cache.get(fp))
    }
}
