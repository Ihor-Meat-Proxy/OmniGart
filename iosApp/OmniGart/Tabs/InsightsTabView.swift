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
                    dashboardContent(dashboard)
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

    @ViewBuilder
    private func dashboardContent(_ dashboard: InsightsDashboard) -> some View {
        InsightsDashboardCards.fitness(dashboard)
        InsightsDashboardCards.loadRatio(dashboard)
        InsightsDashboardCards.loadFocus(dashboard)
        InsightsDashboardCards.goals(dashboard) { showAddGoal = true }
        InsightsPeriodCards.periodPager(dashboard.thisWeek, page: $weekPage)
        InsightsPeriodCards.periodPager(dashboard.thisMonth, page: $monthPage)
        InsightsPeriodCards.sportBars(dashboard)
        InsightsPeriodCards.ytd(dashboard)
        InsightsPeriodCards.heatmap(
            title: "insights_heatmap_workouts",
            cells: dashboard.workoutHeatmap
        )
        InsightsPeriodCards.heatmap(
            title: "insights_heatmap_exercise",
            cells: dashboard.exerciseMinutesHeatmap
        )
        InsightsPeriodCards.allTime(dashboard)
    }

    private var emptyState: some View {
        DarkInsightCard(titleKey: "insights_empty_title") {
            Text("insights_empty_body")
                .font(.subheadline)
                .foregroundStyle(InsightsChartPalette.subtitle)
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
                    Button("insights_save_goal") { saveGoal() }
                }
            }
        }
        .presentationDetents([.medium])
    }

    private func saveGoal() {
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
