package com.omnigart.shared.insights

import kotlinx.serialization.Serializable

@Serializable
data class ChartPoint(
    val dayOffset: Int,
    val value: Double,
    val secondary: Double? = null,
    val low: Double? = null,
    val high: Double? = null,
    val projected: Boolean = false
)

@Serializable
data class FitnessCardModel(
    val ctl: Double,
    val atl: Double,
    val tsb: Double,
    val series: List<ChartPoint>,
    /** Fraction along the series where “now” sits (0–1), before projected points. */
    val nowMarkerFraction: Double
)

@Serializable
data class TrainingLoadRatioModel(
    val ratio: Double?,
    val bandName: String,
    val explanationKey: String,
    val sweetMin: Double = TrainingLoadRatio.SWEET_MIN,
    val sweetMax: Double = TrainingLoadRatio.SWEET_MAX
)

@Serializable
data class TrainingLoadFocusModel(
    val anaerobicShare: Double,
    val highAerobicShare: Double,
    val lowAerobicShare: Double,
    val windowDays: Int = 28
)

@Serializable
data class PeriodCompareSeries(
    val current: List<ChartPoint>,
    val comparison: List<ChartPoint>,
    val nowMarkerFraction: Double,
    val currentTotal: Double,
    val comparisonTotal: Double
)

@Serializable
data class PeriodSwipeCard(
    val titleKey: String,
    val trainingLoad: PeriodCompareSeries,
    val workoutMinutes: PeriodCompareSeries,
    val activeKcal: PeriodCompareSeries,
    val workoutCount: PeriodCompareSeries
)

@Serializable
data class SportBarPoint(val sportKey: String, val label: String, val value: Double, val unit: String)

@Serializable
data class HeatmapCell(val dayOffset: Int, val intensity: Double)

@Serializable
data class AllTimeTotals(val workouts: Int, val activeEnergyKcal: Double, val hours: Double)

@Serializable
data class InsightsDashboard(
    val fitness: FitnessCardModel?,
    val loadRatio: TrainingLoadRatioModel,
    val loadFocus: TrainingLoadFocusModel,
    val goals: List<InsightGoal>,
    val thisWeek: PeriodSwipeCard,
    val thisMonth: PeriodSwipeCard,
    val sportBars12Months: List<SportBarPoint>,
    val ytdCumulativeLoad: List<ChartPoint>,
    val workoutHeatmap: List<HeatmapCell>,
    val exerciseMinutesHeatmap: List<HeatmapCell>,
    val allTime: AllTimeTotals,
    val empty: Boolean
)
