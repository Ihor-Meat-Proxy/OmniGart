import Charts
import Shared
import SwiftUI

/// Health metrics tab — read-only HealthKit samples with dark cards and empty states.
struct HealthTabView: View {
    let core: OmniGartCore
    let profileId: String

    @State private var dashboard: HealthMetricsDashboard = HealthMetricsAssembler.shared.emptyPermissionDashboard()
    @State private var authorized = false
    @State private var loading = true

    var body: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 14) {
                Text("health_metrics_intro")
                    .font(.footnote)
                    .foregroundStyle(InsightsChartPalette.subtitle)

                if loading {
                    ProgressView()
                        .tint(InsightsChartPalette.current)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 24)
                }

                sectionTitle("health_section_today")
                metricCard(dashboard.todayActiveEnergy, chart: true)
                metricCard(dashboard.todayRestingEnergy)
                metricCard(dashboard.todayExerciseMinutes)
                metricCard(dashboard.todaySteps)

                sectionTitle("health_section_heart")
                metricCard(dashboard.restingHr)
                metricCard(dashboard.hrv)
                metricCard(dashboard.hrvStress)
                metricCard(dashboard.recoveryHrv)

                sectionTitle("health_section_vitals")
                metricCard(dashboard.bloodPressure)
                metricCard(dashboard.oxygenSaturation)
                metricCard(dashboard.respiratoryRate)
                metricCard(dashboard.wristTemperature, chart: true)

                sectionTitle("health_section_nutrition")
                metricCard(dashboard.dietaryEnergy)
                metricCard(dashboard.dietaryMacros)

                sectionTitle("health_section_sleep")
                sleepCard(dashboard.sleep)

                sectionTitle("health_section_mindful")
                metricCard(dashboard.mindfulMinutes)

                if !dashboard.past7Days.isEmpty {
                    sectionTitle("health_section_past7")
                    ForEach(Array(dashboard.past7Days.enumerated()), id: \.offset) { _, card in
                        metricCard(card, chart: true)
                    }
                }
                if !dashboard.past30Days.isEmpty {
                    sectionTitle("health_section_past30")
                    ForEach(Array(dashboard.past30Days.enumerated()), id: \.offset) { _, card in
                        metricCard(card, chart: true)
                    }
                }
                if !dashboard.allTime.isEmpty {
                    sectionTitle("health_section_all_time")
                    ForEach(Array(dashboard.allTime.enumerated()), id: \.offset) { _, card in
                        metricCard(card)
                    }
                }

                Text("health_wellness_note")
                    .font(.caption2)
                    .foregroundStyle(InsightsChartPalette.subtitle)
                    .padding(.bottom, 24)
            }
            .padding(.horizontal, 16)
            .padding(.top, 8)
        }
        .background(Color(red: 0.04, green: 0.07, blue: 0.12).ignoresSafeArea())
        .navigationTitle(Text("tab_health"))
        .task { await reload() }
        .refreshable { await reload() }
        .accessibilityIdentifier("health_tab")
    }

    private func sectionTitle(_ key: LocalizedStringKey) -> some View {
        Text(key)
            .font(.title3.weight(.semibold))
            .foregroundStyle(InsightsChartPalette.title)
            .padding(.top, 6)
    }

    @ViewBuilder
    private func metricCard(_ card: HealthMetricCard, chart: Bool = false) -> some View {
        DarkInsightCard(titleKey: LocalizedStringKey(card.titleKey), sourceName: card.sourceName) {
            if card.needsPermission || card.empty {
                Text(card.needsPermission ? "health_permission_needed" : "health_no_data")
                    .font(.subheadline)
                    .foregroundStyle(InsightsChartPalette.subtitle)
                if card.needsPermission {
                    Button("health_request_access") {
                        Task {
                            await HealthMetricsReadTypes.requestReadAccess()
                            await reload()
                        }
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(InsightsChartPalette.current)
                    .accessibilityIdentifier("health_request_access")
                }
            } else {
                if let value = card.valueText {
                    HStack(alignment: .firstTextBaseline, spacing: 6) {
                        Text(value)
                            .font(.title2.weight(.semibold))
                            .foregroundStyle(InsightsChartPalette.title)
                        if let unitKey = card.unitKey {
                            Text(LocalizedStringKey(unitKey))
                                .font(.caption)
                                .foregroundStyle(InsightsChartPalette.subtitle)
                        }
                    }
                }
                if let estimate = card.estimateLabelKey {
                    Text(LocalizedStringKey(estimate))
                        .font(.caption2)
                        .foregroundStyle(InsightsChartPalette.current)
                }
                if chart, !card.series.isEmpty {
                    HealthCompareChart(
                        current: card.series,
                        comparison: card.comparisonSeries,
                        nowFraction: card.nowMarkerFraction
                    )
                }
            }
        }
    }

    private func sleepCard(_ sleep: SleepDonutModel) -> some View {
        DarkInsightCard(titleKey: "health_sleep", sourceName: sleep.sourceName) {
            if sleep.needsPermission || (sleep.asleepMinutes == 0 && sleep.awakeMinutes == 0) {
                Text(sleep.needsPermission ? "health_permission_needed" : "health_no_data")
                    .foregroundStyle(InsightsChartPalette.subtitle)
                if sleep.needsPermission {
                    Button("health_request_access") {
                        Task {
                            await HealthMetricsReadTypes.requestReadAccess()
                            await reload()
                        }
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(InsightsChartPalette.current)
                }
            } else {
                SleepDonutView(asleep: sleep.asleepMinutes, awake: sleep.awakeMinutes)
                    .frame(height: 140)
                HStack {
                    if let start = sleep.startLabel {
                        Text("\(NSLocalizedString("health_sleep_start", comment: "")) \(start)")
                            .foregroundStyle(InsightsChartPalette.subtitle)
                    }
                    Spacer()
                    if let end = sleep.endLabel {
                        Text("\(NSLocalizedString("health_sleep_end", comment: "")) \(end)")
                            .foregroundStyle(InsightsChartPalette.subtitle)
                    }
                }
                .font(.caption)
            }
        }
    }

    @MainActor
    private func reload() async {
        loading = true
        let loaded = await HealthMetricsLoader.loadToday()
        dashboard = loaded.dashboard
        authorized = loaded.authorized
        loading = false
    }
}

struct HealthCompareChart: View {
    let current: [HealthSamplePoint]
    let comparison: [HealthSamplePoint]
    let nowFraction: Double

    var body: some View {
        let maxX = Double(max(current.map { Int($0.dayOffset) }.max() ?? 1, 1))
        Chart {
            ForEach(Array(comparison.enumerated()), id: \.offset) { _, point in
                LineMark(
                    x: .value("day", Double(point.dayOffset)),
                    y: .value("cmp", point.value)
                )
                .foregroundStyle(InsightsChartPalette.comparison)
                .lineStyle(StrokeStyle(lineWidth: 1.5, dash: [4, 3]))
            }
            ForEach(Array(current.enumerated()), id: \.offset) { _, point in
                LineMark(
                    x: .value("day", Double(point.dayOffset)),
                    y: .value("cur", point.value)
                )
                .foregroundStyle(InsightsChartPalette.current)
                .lineStyle(StrokeStyle(lineWidth: 2.2))
            }
            RuleMark(x: .value("now", max(0, min(1, nowFraction)) * maxX))
                .foregroundStyle(InsightsChartPalette.now)
                .lineStyle(StrokeStyle(lineWidth: 1, dash: [3, 3]))
        }
        .chartXAxis(.hidden)
        .chartYAxis(.hidden)
        .frame(height: 120)
    }
}

struct SleepDonutView: View {
    let asleep: Double
    let awake: Double

    var body: some View {
        let total = max(asleep + awake, 1)
        Chart {
            SectorMark(angle: .value("asleep", asleep), innerRadius: .ratio(0.55))
                .foregroundStyle(InsightsChartPalette.current)
            SectorMark(angle: .value("awake", awake), innerRadius: .ratio(0.55))
                .foregroundStyle(InsightsChartPalette.comparison)
        }
        .chartLegend(.hidden)
        .overlay {
            VStack(spacing: 2) {
                Text("health_asleep")
                    .font(.caption2)
                    .foregroundStyle(InsightsChartPalette.subtitle)
                Text(String(format: "%.0f%%", asleep / total * 100))
                    .font(.headline)
                    .foregroundStyle(InsightsChartPalette.title)
            }
        }
    }
}
