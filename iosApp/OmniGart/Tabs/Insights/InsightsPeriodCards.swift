import Shared
import SwiftUI

/// Period pager, sport bars, YTD, heatmaps, and all-time cards for Insights.
enum InsightsPeriodCards {
    static func periodPager(_ card: PeriodSwipeCard, page: Binding<Int>) -> some View {
        DarkInsightCard(titleKey: LocalizedStringKey(card.titleKey)) {
            TabView(selection: page) {
                periodPage("insights_metric_load", card.trainingLoad).tag(0)
                periodPage("insights_metric_time", card.workoutMinutes).tag(1)
                periodPage("insights_metric_kcal", card.activeKcal).tag(2)
                periodPage("insights_metric_count", card.workoutCount).tag(3)
            }
            .tabViewStyle(.page(indexDisplayMode: .automatic))
            .frame(height: 210)
        }
    }

    static func sportBars(_ dash: InsightsDashboard) -> some View {
        DarkInsightCard(titleKey: "insights_past_12_months") {
            if dash.sportBars12Months.isEmpty {
                Text("insights_no_sport_distance")
                    .foregroundStyle(InsightsChartPalette.subtitle)
            } else {
                ForEach(Array(dash.sportBars12Months.enumerated()), id: \.offset) { _, bar in
                    HStack {
                        Text(bar.label.capitalized)
                            .foregroundStyle(InsightsChartPalette.title)
                        Spacer()
                        Text(String(format: "%.1f %@", bar.value, bar.unit))
                            .foregroundStyle(InsightsChartPalette.current)
                    }
                    GeometryReader { geo in
                        let maxVal = dash.sportBars12Months.map { $0.value }.max() ?? 1
                        Capsule()
                            .fill(InsightsChartPalette.current)
                            .frame(
                                width: geo.size.width * CGFloat(bar.value / max(maxVal, 0.1)),
                                height: 6
                            )
                    }
                    .frame(height: 6)
                }
            }
        }
    }

    static func ytd(_ dash: InsightsDashboard) -> some View {
        DarkInsightCard(titleKey: "insights_ytd_load") {
            CompareLineChart(
                current: Array(dash.ytdCumulativeLoad),
                comparison: [],
                nowFraction: 1
            )
        }
    }

    static func heatmap(title: LocalizedStringKey, cells: [HeatmapCell]) -> some View {
        DarkInsightCard(titleKey: title) {
            HeatmapGrid(cells: Array(cells))
        }
    }

    static func allTime(_ dash: InsightsDashboard) -> some View {
        DarkInsightCard(titleKey: "insights_all_time") {
            HStack {
                InsightsDashboardCards.metric("insights_workouts", "\(dash.allTime.workouts)")
                InsightsDashboardCards.metric(
                    "insights_active_kcal",
                    String(format: "%.0f", dash.allTime.activeEnergyKcal)
                )
                InsightsDashboardCards.metric(
                    "insights_hours",
                    String(format: "%.1f", dash.allTime.hours)
                )
            }
        }
    }

    private static func periodPage(
        _ title: LocalizedStringKey,
        _ series: PeriodCompareSeries
    ) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text(title)
                    .foregroundStyle(InsightsChartPalette.title)
                Spacer()
                Text(String(format: "%.0f", series.currentTotal))
                    .foregroundStyle(InsightsChartPalette.current)
            }
            CompareLineChart(
                current: Array(series.current),
                comparison: Array(series.comparison),
                nowFraction: series.nowMarkerFraction
            )
            Text("insights_vs_previous")
                .font(.caption2)
                .foregroundStyle(InsightsChartPalette.subtitle)
        }
    }
}
