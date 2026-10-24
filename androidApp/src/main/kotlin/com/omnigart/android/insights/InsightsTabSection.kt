package com.omnigart.android.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.omnigart.android.R
import com.omnigart.design.theme.OmniGartSpacing
import com.omnigart.shared.OmniGartCore
import com.omnigart.shared.insights.InMemoryInsightGoalStore
import com.omnigart.shared.insights.InsightGoal
import com.omnigart.shared.insights.InsightGoalKind
import com.omnigart.shared.insights.InsightsDashboard
import com.omnigart.shared.insights.InsightsDashboardBuilder
import com.omnigart.shared.insights.PeriodCompareSeries
import com.omnigart.shared.insights.PeriodSwipeCard
import com.omnigart.shared.insights.StubCalorieEnergyProvider
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
@Suppress("LongMethod")
fun InsightsTabSection(core: OmniGartCore, profileId: String) {
    val goalStore = remember { InMemoryInsightGoalStore() }
    val builder =
        remember(core, goalStore) {
            InsightsDashboardBuilder(
                store = core.localHealthStore,
                calorieProvider = StubCalorieEnergyProvider(),
                goalStore = goalStore
            )
        }
    var dashboard by remember { mutableStateOf<InsightsDashboard?>(null) }
    var loading by remember { mutableStateOf(true) }
    var showAddGoal by remember { mutableStateOf(false) }
    var goalTitle by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    fun reload() {
        scope.launch {
            loading = true
            val profile = core.profiles.get(profileId)
            val anchors =
                InsightsDashboardBuilder.ProfileAnchors(
                    restingHrBpm = 60.0,
                    maxHrBpm = 190.0,
                    weightKg = profile?.weightKg
                )
            val built =
                withContext(Dispatchers.Default) {
                    builder.build(anchors)
                }
            dashboard = built
            loading = false
        }
    }

    LaunchedEffect(profileId) { reload() }

    Column(
        modifier =
        Modifier
            .fillMaxWidth()
            .background(InsightsChartPalette.canvas)
            .padding(vertical = OmniGartSpacing.space2)
            .testTag("insights_tab"),
        verticalArrangement = Arrangement.spacedBy(OmniGartSpacing.space3)
    ) {
        Text(
            text = stringResource(R.string.insights_intro),
            color = InsightsChartPalette.subtitle,
            style = MaterialTheme.typography.bodySmall
        )
        if (loading && dashboard == null) {
            CircularProgressIndicator(color = InsightsChartPalette.current)
        }
        val dash = dashboard
        if (dash != null) {
            if (dash.empty) {
                DarkInsightCard(title = stringResource(R.string.insights_empty_title)) {
                    Text(stringResource(R.string.insights_empty_body), color = InsightsChartPalette.subtitle)
                }
            } else {
                FitnessBlock(dash)
                LoadRatioBlock(dash)
                LoadFocusBlock(dash)
                GoalsBlock(
                    dash = dash,
                    showAddGoal = showAddGoal,
                    goalTitle = goalTitle,
                    onTitleChange = { goalTitle = it },
                    onToggleAdd = { showAddGoal = !showAddGoal },
                    onSave = {
                        val title = goalTitle.trim()
                        if (title.isNotEmpty()) {
                            goalStore.add(
                                InsightGoal(
                                    id = UUID.randomUUID().toString(),
                                    kind = InsightGoalKind.CUSTOM,
                                    title = title,
                                    targetValue = 1.0,
                                    unitLabel = ""
                                )
                            )
                            goalTitle = ""
                            showAddGoal = false
                            reload()
                        }
                    }
                )
                PeriodBlock(dash.thisWeek, stringResource(R.string.insights_this_week))
                PeriodBlock(dash.thisMonth, stringResource(R.string.insights_this_month))
                SportBarsBlock(dash)
                DarkInsightCard(title = stringResource(R.string.insights_ytd_load)) {
                    CompareLineChart(dash.ytdCumulativeLoad, emptyList(), nowFraction = 1.0)
                }
                DarkInsightCard(title = stringResource(R.string.insights_heatmap_workouts)) {
                    HeatmapGrid(dash.workoutHeatmap)
                }
                DarkInsightCard(title = stringResource(R.string.insights_heatmap_exercise)) {
                    HeatmapGrid(dash.exerciseMinutesHeatmap)
                }
                DarkInsightCard(title = stringResource(R.string.insights_all_time)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Metric(stringResource(R.string.insights_workouts), "${dash.allTime.workouts}")
                        Metric(stringResource(R.string.insights_active_kcal), "%.0f".format(dash.allTime.activeEnergyKcal))
                        Metric(stringResource(R.string.insights_hours), "%.1f".format(dash.allTime.hours))
                    }
                }
            }
        }
        Text(
            text = stringResource(R.string.insights_wellness_note),
            color = InsightsChartPalette.subtitle,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun FitnessBlock(dash: InsightsDashboard) {
    val fitness = dash.fitness ?: return
    DarkInsightCard(title = stringResource(R.string.insights_fitness_title)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Metric(stringResource(R.string.insights_ctl), "%.0f".format(fitness.ctl))
            Metric(stringResource(R.string.insights_atl), "%.0f".format(fitness.atl))
            Metric(stringResource(R.string.insights_tsb), "%.0f".format(fitness.tsb))
        }
        CompareLineChart(fitness.series, emptyList(), fitness.nowMarkerFraction, showBand = true)
        Text(
            stringResource(R.string.insights_fitness_caption),
            color = InsightsChartPalette.subtitle,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun LoadRatioBlock(dash: InsightsDashboard) {
    DarkInsightCard(title = stringResource(R.string.insights_tlr_title)) {
        val ratio = dash.loadRatio.ratio
        Text(
            text = ratio?.let { "%.2f".format(it) } ?: "—",
            color = InsightsChartPalette.current,
            style = MaterialTheme.typography.headlineMedium
        )
        val explanation =
            when (dash.loadRatio.explanationKey) {
                "insights_tlr_sweet" -> stringResource(R.string.insights_tlr_sweet)
                "insights_tlr_low" -> stringResource(R.string.insights_tlr_low)
                "insights_tlr_elevated" -> stringResource(R.string.insights_tlr_elevated)
                else -> stringResource(R.string.insights_tlr_need_fitness)
            }
        Text(explanation, color = InsightsChartPalette.subtitle, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun LoadFocusBlock(dash: InsightsDashboard) {
    DarkInsightCard(title = stringResource(R.string.insights_focus_title)) {
        FocusRow(stringResource(R.string.insights_focus_anaerobic), dash.loadFocus.anaerobicShare)
        FocusRow(stringResource(R.string.insights_focus_high_aerobic), dash.loadFocus.highAerobicShare)
        FocusRow(stringResource(R.string.insights_focus_low_aerobic), dash.loadFocus.lowAerobicShare)
        Text(
            stringResource(R.string.insights_focus_caption),
            color = InsightsChartPalette.subtitle,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun GoalsBlock(
    dash: InsightsDashboard,
    showAddGoal: Boolean,
    goalTitle: String,
    onTitleChange: (String) -> Unit,
    onToggleAdd: () -> Unit,
    onSave: () -> Unit
) {
    DarkInsightCard(title = stringResource(R.string.insights_goals_title)) {
        if (dash.goals.isEmpty()) {
            Text(stringResource(R.string.insights_goals_empty), color = InsightsChartPalette.subtitle)
        } else {
            dash.goals.forEach { goal ->
                Text("${goal.title} · ${"%.0f".format(goal.targetValue)} ${goal.unitLabel}", color = InsightsChartPalette.title)
            }
        }
        TextButton(onClick = onToggleAdd, modifier = Modifier.testTag("insights_add_goal")) {
            Text(stringResource(R.string.insights_add_goal), color = InsightsChartPalette.current)
        }
        if (showAddGoal) {
            OutlinedTextField(
                value = goalTitle,
                onValueChange = onTitleChange,
                label = { Text(stringResource(R.string.insights_goal_title_field)) },
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = onSave,
                colors = ButtonDefaults.buttonColors(containerColor = InsightsChartPalette.current)
            ) {
                Text(stringResource(R.string.insights_save_goal))
            }
        }
    }
}

@Composable
private fun PeriodBlock(card: PeriodSwipeCard, title: String) {
    val pager = rememberPagerState(pageCount = { 4 })
    DarkInsightCard(title = title) {
        HorizontalPager(state = pager, modifier = Modifier.fillMaxWidth().height(220.dp)) { page ->
            val series: PeriodCompareSeries =
                when (page) {
                    0 -> card.trainingLoad
                    1 -> card.workoutMinutes
                    2 -> card.activeKcal
                    else -> card.workoutCount
                }
            val label =
                when (page) {
                    0 -> stringResource(R.string.insights_metric_load)
                    1 -> stringResource(R.string.insights_metric_time)
                    2 -> stringResource(R.string.insights_metric_kcal)
                    else -> stringResource(R.string.insights_metric_count)
                }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(label, color = InsightsChartPalette.title)
                    Text("%.0f".format(series.currentTotal), color = InsightsChartPalette.current)
                }
                CompareLineChart(series.current, series.comparison, series.nowMarkerFraction)
                Text(
                    stringResource(R.string.insights_vs_previous),
                    color = InsightsChartPalette.subtitle,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
private fun SportBarsBlock(dash: InsightsDashboard) {
    DarkInsightCard(title = stringResource(R.string.insights_past_12_months)) {
        if (dash.sportBars12Months.isEmpty()) {
            Text(stringResource(R.string.insights_no_sport_distance), color = InsightsChartPalette.subtitle)
        } else {
            val maxVal = dash.sportBars12Months.maxOf { it.value }.coerceAtLeast(0.1)
            dash.sportBars12Months.forEach { bar ->
                Text("${bar.label} · ${"%.1f".format(bar.value)} ${bar.unit}", color = InsightsChartPalette.title)
                BoxBar(fraction = (bar.value / maxVal).toFloat())
            }
        }
    }
}

@Composable
private fun FocusRow(label: String, share: Double) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = InsightsChartPalette.title)
            Text("%.0f%%".format(share * 100), color = InsightsChartPalette.current)
        }
        BoxBar(fraction = share.toFloat())
    }
}

@Composable
private fun BoxBar(fraction: Float) {
    Box(
        modifier =
        Modifier
            .fillMaxWidth()
            .height(6.dp)
            .background(InsightsChartPalette.subtitle.copy(alpha = 0.2f), RoundedCornerShape(3.dp))
    ) {
        Box(
            modifier =
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(6.dp)
                .background(InsightsChartPalette.current, RoundedCornerShape(3.dp))
        )
    }
}

@Composable
private fun Metric(label: String, value: String) {
    Column {
        Text(label, color = InsightsChartPalette.subtitle, style = MaterialTheme.typography.labelSmall)
        Text(value, color = InsightsChartPalette.title, style = MaterialTheme.typography.titleMedium)
    }
}
