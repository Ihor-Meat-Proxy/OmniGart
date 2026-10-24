import Shared
import SwiftUI

/// Fitness, load ratio/focus, and goals cards for the Insights tab.
@MainActor
enum InsightsDashboardCards {
    @ViewBuilder
    static func fitness(_ dash: InsightsDashboard) -> some View {
        if let fitness = dash.fitness {
            DarkInsightCard(titleKey: "insights_fitness_title") {
                HStack(spacing: 16) {
                    metric("insights_ctl", String(format: "%.0f", fitness.ctl))
                    metric("insights_atl", String(format: "%.0f", fitness.atl))
                    metric("insights_tsb", String(format: "%.0f", fitness.tsb))
                }
                CompareLineChart(
                    current: Array(fitness.series),
                    comparison: [],
                    nowFraction: fitness.nowMarkerFraction,
                    showBand: true
                )
                Text("insights_fitness_caption")
                    .font(.caption2)
                    .foregroundStyle(InsightsChartPalette.subtitle)
            }
        }
    }

    static func loadRatio(_ dash: InsightsDashboard) -> some View {
        DarkInsightCard(titleKey: "insights_tlr_title") {
            let ratioValue = optionalDouble(dash.loadRatio.ratio)
            let ratio = ratioValue.map { String(format: "%.2f", $0) } ?? "—"
            Text(ratio)
                .font(.system(size: 34, weight: .semibold, design: .rounded))
                .foregroundStyle(ratioColor(dash.loadRatio.bandName))
            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    Capsule().fill(Color.white.opacity(0.12))
                    Capsule()
                        .fill(InsightsChartPalette.current.opacity(0.35))
                        .frame(width: geo.size.width * sweetWidth(dash))
                        .offset(x: geo.size.width * sweetOrigin(dash))
                    if let ratioPoint = ratioValue {
                        Circle()
                            .fill(InsightsChartPalette.current)
                            .frame(width: 12, height: 12)
                            .offset(
                                x: geo.size.width * CGFloat(min(max(ratioPoint / 2.0, 0), 1)) - 6
                            )
                    }
                }
            }
            .frame(height: 14)
            Text(LocalizedStringKey(dash.loadRatio.explanationKey))
                .font(.caption)
                .foregroundStyle(InsightsChartPalette.subtitle)
        }
    }

    static func loadFocus(_ dash: InsightsDashboard) -> some View {
        DarkInsightCard(titleKey: "insights_focus_title") {
            focusRow("insights_focus_anaerobic", dash.loadFocus.anaerobicShare)
            focusRow("insights_focus_high_aerobic", dash.loadFocus.highAerobicShare)
            focusRow("insights_focus_low_aerobic", dash.loadFocus.lowAerobicShare)
            Text("insights_focus_caption")
                .font(.caption2)
                .foregroundStyle(InsightsChartPalette.subtitle)
        }
    }

    static func goals(
        _ dash: InsightsDashboard,
        onAdd: @escaping () -> Void
    ) -> some View {
        DarkInsightCard(titleKey: "insights_goals_title") {
            if dash.goals.isEmpty {
                Text("insights_goals_empty")
                    .font(.subheadline)
                    .foregroundStyle(InsightsChartPalette.subtitle)
            } else {
                ForEach(Array(dash.goals.enumerated()), id: \.offset) { _, goal in
                    HStack {
                        Text(goal.title)
                            .foregroundStyle(InsightsChartPalette.title)
                        Spacer()
                        Text(String(format: "%.0f %@", goal.targetValue, goal.unitLabel))
                            .foregroundStyle(InsightsChartPalette.subtitle)
                    }
                }
            }
            Button(action: onAdd) {
                Label("insights_add_goal", systemImage: "plus.circle.fill")
                    .foregroundStyle(InsightsChartPalette.current)
            }
            .accessibilityIdentifier("insights_add_goal")
        }
    }

    static func metric(_ key: LocalizedStringKey, _ value: String) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(key)
                .font(.caption2)
                .foregroundStyle(InsightsChartPalette.subtitle)
            Text(value)
                .font(.title3.weight(.semibold))
                .foregroundStyle(InsightsChartPalette.title)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private static func focusRow(_ key: LocalizedStringKey, _ share: Double) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack {
                Text(key)
                    .foregroundStyle(InsightsChartPalette.title)
                Spacer()
                Text(String(format: "%.0f%%", share * 100))
                    .foregroundStyle(InsightsChartPalette.current)
            }
            GeometryReader { geo in
                Capsule()
                    .fill(InsightsChartPalette.current)
                    .frame(width: geo.size.width * CGFloat(share), height: 6)
            }
            .frame(height: 6)
        }
    }

    private static func ratioColor(_ band: String) -> Color {
        switch band {
        case "SWEET": return InsightsChartPalette.current
        case "ELEVATED": return Color(red: 0.84, green: 0.37, blue: 0.0)
        case "LOW": return InsightsChartPalette.comparison
        default: return InsightsChartPalette.subtitle
        }
    }

    private static func sweetOrigin(_ dash: InsightsDashboard) -> CGFloat {
        CGFloat(dash.loadRatio.sweetMin / 2.0)
    }

    private static func sweetWidth(_ dash: InsightsDashboard) -> CGFloat {
        CGFloat((dash.loadRatio.sweetMax - dash.loadRatio.sweetMin) / 2.0)
    }
}
