import Shared
import SwiftUI

/// Insights tab: fitness, load ratio/focus, goals, period cards, heatmaps, totals.
/// Aggregates run off the main actor and are cached in shared InsightsAggregateCache.
struct InsightsTabView: View {
    let core: OmniGartCore
    let profileId: String

    @State private var dashboard: InsightsDashboard?
    @State private var loading = true
    @State private var showAddGoal = false
    @State private var goalTitle = ""
    @State private var weekPage = 0
    @State private var monthPage = 0

    private let builder: InsightsDashboardBuilder
    private let goalStore = InMemoryInsightGoalStore()

    init(core: OmniGartCore, profileId: String) {
        self.core = core
        self.profileId = profileId
        self.builder = InsightsDashboardBuilder(
            store: core.localHealthStore,
            calorieProvider: StubCalorieEnergyProvider(),
            goalStore: goalStore
        )
    }

    var body: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 14) {
                Text("insights_intro")
                    .font(.footnote)
                    .foregroundStyle(InsightsChartPalette.subtitle)

                if loading && dashboard == nil {
                    ProgressView()
                        .tint(InsightsChartPalette.current)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 24)
                } else if let dashboard, dashboard.empty {
                    emptyState
                } else if let dashboard {
                    fitnessCard(dashboard)
                    loadRatioCard(dashboard)
                    loadFocusCard(dashboard)
                    goalsCard(dashboard)
                    periodPager(dashboard.thisWeek, page: $weekPage)
                    periodPager(dashboard.thisMonth, page: $monthPage)
                    sportBars(dashboard)
                    ytdCard(dashboard)
                    heatmapCard(title: "insights_heatmap_workouts", cells: dashboard.workoutHeatmap)
                    heatmapCard(title: "insights_heatmap_exercise", cells: dashboard.exerciseMinutesHeatmap)
                    allTimeCard(dashboard)
                }

                Text("insights_wellness_note")
                    .font(.caption2)
                    .foregroundStyle(InsightsChartPalette.subtitle)
                    .padding(.bottom, 24)
            }
            .padding(.horizontal, 16)
            .padding(.top, 8)
        }
        .background(Color(red: 0.04, green: 0.07, blue: 0.12).ignoresSafeArea())
        .navigationTitle(Text("tab_insights"))
        .task { await reload() }
        .refreshable { await reload() }
        .sheet(isPresented: $showAddGoal) { addGoalSheet }
        .accessibilityIdentifier("insights_tab")
    }

    private var emptyState: some View {
        DarkInsightCard(titleKey: "insights_empty_title") {
            Text("insights_empty_body")
                .font(.subheadline)
                .foregroundStyle(InsightsChartPalette.subtitle)
        }
    }

    @ViewBuilder
    private func fitnessCard(_ dash: InsightsDashboard) -> some View {
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

    private func loadRatioCard(_ dash: InsightsDashboard) -> some View {
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
                    if let r = ratioValue {
                        Circle()
                            .fill(InsightsChartPalette.current)
                            .frame(width: 12, height: 12)
                            .offset(x: geo.size.width * CGFloat(min(max(r / 2.0, 0), 1)) - 6)
                    }
                }
            }
            .frame(height: 14)
            Text(LocalizedStringKey(dash.loadRatio.explanationKey))
                .font(.caption)
                .foregroundStyle(InsightsChartPalette.subtitle)
        }
    }

    private func loadFocusCard(_ dash: InsightsDashboard) -> some View {
        DarkInsightCard(titleKey: "insights_focus_title") {
            focusRow("insights_focus_anaerobic", dash.loadFocus.anaerobicShare)
            focusRow("insights_focus_high_aerobic", dash.loadFocus.highAerobicShare)
            focusRow("insights_focus_low_aerobic", dash.loadFocus.lowAerobicShare)
            Text("insights_focus_caption")
                .font(.caption2)
                .foregroundStyle(InsightsChartPalette.subtitle)
        }
    }

    private func goalsCard(_ dash: InsightsDashboard) -> some View {
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
            Button {
                showAddGoal = true
            } label: {
                Label("insights_add_goal", systemImage: "plus.circle.fill")
                    .foregroundStyle(InsightsChartPalette.current)
            }
            .accessibilityIdentifier("insights_add_goal")
        }
    }

    private func periodPager(_ card: PeriodSwipeCard, page: Binding<Int>) -> some View {
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

    private func periodPage(_ title: LocalizedStringKey, _ series: PeriodCompareSeries) -> some View {
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

    private func sportBars(_ dash: InsightsDashboard) -> some View {
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
                            .frame(width: geo.size.width * CGFloat(bar.value / max(maxVal, 0.1)), height: 6)
                    }
                    .frame(height: 6)
                }
            }
        }
    }

    private func ytdCard(_ dash: InsightsDashboard) -> some View {
        DarkInsightCard(titleKey: "insights_ytd_load") {
            CompareLineChart(
                current: Array(dash.ytdCumulativeLoad),
                comparison: [],
                nowFraction: 1
            )
        }
    }

    private func heatmapCard(title: LocalizedStringKey, cells: [HeatmapCell]) -> some View {
        DarkInsightCard(titleKey: title) {
            HeatmapGrid(cells: Array(cells))
        }
    }

    private func allTimeCard(_ dash: InsightsDashboard) -> some View {
        DarkInsightCard(titleKey: "insights_all_time") {
            HStack {
                metric("insights_workouts", "\(dash.allTime.workouts)")
                metric("insights_active_kcal", String(format: "%.0f", dash.allTime.activeEnergyKcal))
                metric("insights_hours", String(format: "%.1f", dash.allTime.hours))
            }
        }
    }

    private var addGoalSheet: some View {
        NavigationStack {
            Form {
                TextField("insights_goal_title_field", text: $goalTitle)
            }
            .navigationTitle(Text("insights_add_goal"))
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("insights_cancel") { showAddGoal = false }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("insights_save_goal") {
                        let title = goalTitle.trimmingCharacters(in: .whitespacesAndNewlines)
                        guard !title.isEmpty else { return }
                        goalStore.add(
                            goal: InsightGoal(
                                id: UUID().uuidString,
                                kind: .custom,
                                title: title,
                                targetValue: 1,
                                unitLabel: ""
                            )
                        )
                        goalTitle = ""
                        showAddGoal = false
                        Task { await reload() }
                    }
                }
            }
        }
        .presentationDetents([.medium])
    }

    private func metric(_ key: LocalizedStringKey, _ value: String) -> some View {
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

    private func focusRow(_ key: LocalizedStringKey, _ share: Double) -> some View {
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

    private func ratioColor(_ band: String) -> Color {
        switch band {
        case "SWEET": return InsightsChartPalette.current
        case "ELEVATED": return Color(red: 0.84, green: 0.37, blue: 0.0)
        case "LOW": return InsightsChartPalette.comparison
        default: return InsightsChartPalette.subtitle
        }
    }

    private func sweetOrigin(_ dash: InsightsDashboard) -> CGFloat {
        CGFloat(dash.loadRatio.sweetMin / 2.0)
    }

    private func sweetWidth(_ dash: InsightsDashboard) -> CGFloat {
        CGFloat((dash.loadRatio.sweetMax - dash.loadRatio.sweetMin) / 2.0)
    }

    @MainActor
    private func reload() async {
        loading = true
        let profile = core.profiles.get(id: profileId)
        let maxHr = max(120.0, 220.0 - Double(profileAgeYears(profile)))
        let anchors = InsightsDashboardBuilder.ProfileAnchors(
            restingHrBpm: 60,
            maxHrBpm: maxHr,
            weightKg: profile.map { KotlinDouble(value: $0.weightKg) }
        )
        // Yield so the progress indicator can paint. Aggregates are cached in-process;
        // avoid Task.detached with non-Sendable KMP types under Swift 6 strict concurrency.
        await Task.yield()
        dashboard = builder.build(anchors: anchors)
        loading = false
    }

    private func profileAgeYears(_ profile: UserProfile?) -> Int {
        guard let profile else { return 30 }
        // Approximate from birth year for max-HR estimate.
        let year = Calendar.current.component(.year, from: Date())
        return max(16, year - Int(profile.dateOfBirth.year))
    }
}
