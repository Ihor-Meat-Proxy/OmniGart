package com.omnigart.shared.insights

import com.omnigart.shared.health.WorkoutActivityType
import com.omnigart.shared.health.store.LocalHealthStore
import com.omnigart.shared.health.store.LocalHealthWorkout
import kotlin.math.round
import kotlin.time.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

/**
 * Builds the Insights tab model from the local health store (background-safe).
 */
class InsightsDashboardBuilder(
    private val store: LocalHealthStore,
    private val calorieProvider: CalorieEnergyProvider = StubCalorieEnergyProvider(),
    private val goalStore: InsightGoalStore = InMemoryInsightGoalStore(),
    private val cache: InsightsAggregateCache = InsightsAggregateCache(),
    private val clock: Clock = Clock.System,
    private val timeZone: TimeZone = TimeZone.currentSystemDefault()
) {
    /** Swift-friendly: store only (Kotlin defaults are not exported to Apple). */
    constructor(store: LocalHealthStore) : this(
        store = store,
        calorieProvider = StubCalorieEnergyProvider(),
        goalStore = InMemoryInsightGoalStore(),
        cache = InsightsAggregateCache()
    )

    /** Swift-friendly: store + calorie + goals. */
    constructor(
        store: LocalHealthStore,
        calorieProvider: CalorieEnergyProvider,
        goalStore: InsightGoalStore
    ) : this(
        store = store,
        calorieProvider = calorieProvider,
        goalStore = goalStore,
        cache = InsightsAggregateCache()
    )

    data class ProfileAnchors(val restingHrBpm: Double = 60.0, val maxHrBpm: Double = 190.0, val weightKg: Double? = null) {
        constructor(restingHrBpm: Double, maxHrBpm: Double) : this(
            restingHrBpm = restingHrBpm,
            maxHrBpm = maxHrBpm,
            weightKg = null
        )
    }

    fun fingerprint(workouts: List<LocalHealthWorkout>): String =
        workouts.joinToString("|") { "${it.id}:${it.updatedAt.toEpochMilliseconds()}" }

    /** Swift-friendly no-arg build. */
    fun build(): InsightsDashboard = build(ProfileAnchors())

    fun build(anchors: ProfileAnchors): InsightsDashboard {
        val workouts = store.listWorkouts().filter { !it.deleted }
        val fp = fingerprint(workouts) + "|${anchors.restingHrBpm}|${anchors.maxHrBpm}"
        cache.get(fp)?.let { return it }

        val today = clock.now().toLocalDateTime(timeZone).date
        val hrAnchors =
            runCatching {
                TrainingLoadCalculator.HrAnchors(anchors.restingHrBpm, anchors.maxHrBpm)
            }.getOrNull()

        val sessionLoads =
            workouts.map { w ->
                w to
                    TrainingLoadCalculator.fromWorkout(
                        workout = w,
                        anchors = hrAnchors,
                        calorieProvider = calorieProvider,
                        weightKg = anchors.weightKg
                    )
            }

        val loadsByDay = mutableMapOf<Long, Double>()
        val minutesByDay = mutableMapOf<Long, Double>()
        val kcalByDay = mutableMapOf<Long, Double>()
        val countByDay = mutableMapOf<Long, Int>()
        var anaerobic = 0.0
        var highAerobic = 0.0
        var lowAerobic = 0.0
        val focusStart = today.minus(DatePeriod(days = 27))

        for ((workout, load) in sessionLoads) {
            val day = workout.startAt.toLocalDateTime(timeZone).date
            val dayIndex = day.toEpochDays()
            loadsByDay[dayIndex] = (loadsByDay[dayIndex] ?: 0.0) + load.load
            val minutes = workout.durationSeconds() / 60.0
            minutesByDay[dayIndex] = (minutesByDay[dayIndex] ?: 0.0) + minutes
            val kcal = workout.activeEnergyKcal ?: 0.0
            kcalByDay[dayIndex] = (kcalByDay[dayIndex] ?: 0.0) + kcal
            countByDay[dayIndex] = (countByDay[dayIndex] ?: 0) + 1
            if (day >= focusStart) {
                anaerobic += load.anaerobicMinutes
                highAerobic += load.highAerobicMinutes
                lowAerobic += load.lowAerobicMinutes
            }
        }

        // Ensure at least a short history window for EWMA warm-up.
        val historyStart = today.minus(DatePeriod(days = 119)).toEpochDays()
        val historyEnd = today.toEpochDays()
        for (d in historyStart..historyEnd) {
            if (d !in loadsByDay) loadsByDay[d] = 0.0
        }

        val fitnessSeries = FitnessBanister.compute(loadsByDay)
        val fitnessCard =
            if (fitnessSeries.points.isEmpty()) {
                null
            } else {
                val nonProjected = fitnessSeries.points.count { !it.projected }
                val origin = fitnessSeries.points.first().dayIndex
                FitnessCardModel(
                    ctl = round1(fitnessSeries.currentCtl),
                    atl = round1(fitnessSeries.currentAtl),
                    tsb = round1(fitnessSeries.currentTsb),
                    series =
                    fitnessSeries.points.map {
                        ChartPoint(
                            dayOffset = (it.dayIndex - origin).toInt(),
                            value = it.ctl,
                            secondary = it.atl,
                            low = it.ctlLow,
                            high = it.ctlHigh,
                            projected = it.projected
                        )
                    },
                    nowMarkerFraction =
                    if (fitnessSeries.points.isEmpty()) {
                        1.0
                    } else {
                        (nonProjected - 1).coerceAtLeast(0).toDouble() /
                            (fitnessSeries.points.size - 1).coerceAtLeast(1)
                    }
                )
            }

        val ratio =
            TrainingLoadRatio.compute(fitnessSeries.currentAtl, fitnessSeries.currentCtl)
        val focusTotal = (anaerobic + highAerobic + lowAerobic).coerceAtLeast(1e-6)
        val focus =
            TrainingLoadFocusModel(
                anaerobicShare = anaerobic / focusTotal,
                highAerobicShare = highAerobic / focusTotal,
                lowAerobicShare = lowAerobic / focusTotal
            )

        val weekStart = today.minus(DatePeriod(days = today.dayOfWeek.ordinal))
        val monthStart = LocalDate(today.year, today.month, 1)
        val thisWeek =
            periodCard(
                titleKey = "insights_this_week",
                periodStart = weekStart,
                periodEndInclusive = today,
                compareStart = weekStart.minus(DatePeriod(days = 7)),
                compareEndInclusive = weekStart.minus(DatePeriod(days = 1)),
                loadsByDay = loadsByDay,
                minutesByDay = minutesByDay,
                kcalByDay = kcalByDay,
                countByDay = countByDay,
                today = today
            )
        val thisMonth =
            periodCard(
                titleKey = "insights_this_month",
                periodStart = monthStart,
                periodEndInclusive = today,
                compareStart = monthStart.minus(DatePeriod(months = 1)),
                compareEndInclusive = monthStart.minus(DatePeriod(days = 1)),
                loadsByDay = loadsByDay,
                minutesByDay = minutesByDay,
                kcalByDay = kcalByDay,
                countByDay = countByDay,
                today = today
            )

        val yearAgo = today.minus(DatePeriod(months = 12))
        val sportBars = sportDistanceBars(workouts, yearAgo, today)
        val ytdStart = LocalDate(today.year, 1, 1)
        val ytd =
            cumulativeSeries(
                start = ytdStart,
                endInclusive = today,
                byDay = loadsByDay
            )

        val heatmapStart = today.minus(DatePeriod(days = 83))
        val workoutHeatmap =
            (0..83).map { offset ->
                val day = heatmapStart.plus(DatePeriod(days = offset))
                HeatmapCell(offset, (countByDay[day.toEpochDays()] ?: 0).toDouble())
            }
        val exerciseHeatmap =
            (0..83).map { offset ->
                val day = heatmapStart.plus(DatePeriod(days = offset))
                // Proxy: workout minutes / 60 as “exercise ring” intensity until platform minutes land.
                HeatmapCell(offset, ((minutesByDay[day.toEpochDays()] ?: 0.0) / 60.0).coerceAtMost(1.0))
            }

        val allTime =
            AllTimeTotals(
                workouts = workouts.size,
                activeEnergyKcal = round1(workouts.sumOf { it.activeEnergyKcal ?: 0.0 }),
                hours = round1(workouts.sumOf { it.durationSeconds() } / 3600.0)
            )

        val dashboard =
            InsightsDashboard(
                fitness = fitnessCard,
                loadRatio =
                TrainingLoadRatioModel(
                    ratio = ratio.ratio?.let { round2(it) },
                    bandName = ratio.band.name,
                    explanationKey = ratio.explanationKey
                ),
                loadFocus = focus,
                goals = goalStore.list(),
                thisWeek = thisWeek,
                thisMonth = thisMonth,
                sportBars12Months = sportBars,
                ytdCumulativeLoad = ytd,
                workoutHeatmap = workoutHeatmap,
                exerciseMinutesHeatmap = exerciseHeatmap,
                allTime = allTime,
                empty = workouts.isEmpty()
            )
        cache.put(fp, dashboard)
        return dashboard
    }

    private fun periodCard(
        titleKey: String,
        periodStart: LocalDate,
        periodEndInclusive: LocalDate,
        compareStart: LocalDate,
        compareEndInclusive: LocalDate,
        loadsByDay: Map<Long, Double>,
        minutesByDay: Map<Long, Double>,
        kcalByDay: Map<Long, Double>,
        countByDay: Map<Long, Int>,
        today: LocalDate
    ): PeriodSwipeCard {
        fun series(start: LocalDate, end: LocalDate, map: Map<Long, Double>, isCount: Boolean = false): PeriodCompareSeries {
            val len = start.daysUntil(end) + 1
            val currentPoints = mutableListOf<ChartPoint>()
            val comparePoints = mutableListOf<ChartPoint>()
            var curSum = 0.0
            var cmpSum = 0.0
            var curRunning = 0.0
            var cmpRunning = 0.0
            for (i in 0 until len) {
                val curDay = start.plus(DatePeriod(days = i))
                val cmpDay = compareStart.plus(DatePeriod(days = i))
                val c =
                    if (isCount) {
                        (countByDay[curDay.toEpochDays()] ?: 0).toDouble()
                    } else {
                        map[curDay.toEpochDays()] ?: 0.0
                    }
                val p =
                    if (cmpDay <= compareEndInclusive) {
                        if (isCount) {
                            (countByDay[cmpDay.toEpochDays()] ?: 0).toDouble()
                        } else {
                            map[cmpDay.toEpochDays()] ?: 0.0
                        }
                    } else {
                        0.0
                    }
                curRunning += c
                cmpRunning += p
                curSum += c
                cmpSum += p
                currentPoints += ChartPoint(i, curRunning)
                comparePoints += ChartPoint(i, cmpRunning)
            }
            val nowFrac =
                if (len <= 1) {
                    1.0
                } else {
                    start.daysUntil(today).coerceIn(0, len - 1).toDouble() / (len - 1)
                }
            return PeriodCompareSeries(
                current = currentPoints,
                comparison = comparePoints,
                nowMarkerFraction = nowFrac,
                currentTotal = round1(curSum),
                comparisonTotal = round1(cmpSum)
            )
        }

        return PeriodSwipeCard(
            titleKey = titleKey,
            trainingLoad = series(periodStart, periodEndInclusive, loadsByDay),
            workoutMinutes = series(periodStart, periodEndInclusive, minutesByDay),
            activeKcal = series(periodStart, periodEndInclusive, kcalByDay),
            workoutCount =
            series(
                periodStart,
                periodEndInclusive,
                emptyMap(),
                isCount = true
            )
        )
    }

    private fun cumulativeSeries(start: LocalDate, endInclusive: LocalDate, byDay: Map<Long, Double>): List<ChartPoint> {
        var running = 0.0
        val out = mutableListOf<ChartPoint>()
        val len = start.daysUntil(endInclusive)
        for (i in 0..len) {
            val day = start.plus(DatePeriod(days = i))
            running += byDay[day.toEpochDays()] ?: 0.0
            out += ChartPoint(i, running)
        }
        return out
    }

    private fun sportDistanceBars(workouts: List<LocalHealthWorkout>, from: LocalDate, to: LocalDate): List<SportBarPoint> {
        val fromInstant = from.atStartOfDayIn(timeZone)
        val toExclusive = to.plus(DatePeriod(days = 1)).atStartOfDayIn(timeZone)
        val metres = mutableMapOf<WorkoutActivityType, Double>()
        for (w in workouts) {
            if (w.startAt < fromInstant || w.startAt >= toExclusive) continue
            val d = w.distanceMetres ?: continue
            if (d <= 0) continue
            metres[w.activityType] = (metres[w.activityType] ?: 0.0) + d
        }
        return metres.entries
            .sortedByDescending { it.value }
            .map { (type, m) ->
                val km = m / 1000.0
                SportBarPoint(
                    sportKey = type.name,
                    label = type.name.lowercase().replace('_', ' '),
                    value = round1(km),
                    unit = "km"
                )
            }
    }

    private fun round1(v: Double): Double = round(v * 10.0) / 10.0

    private fun round2(v: Double): Double = round(v * 100.0) / 100.0
}
