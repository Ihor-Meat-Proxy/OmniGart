import Shared
import SwiftUI
import UIKit

/// Exercise detail with Summary / History / How to tabs. Illustration key `ex_<id>`.
struct ExerciseDetailView: View {
    let core: OmniGartCore
    let profileId: String
    let exerciseId: String

    private enum Tab: String, CaseIterable, Identifiable {
        case summary
        case history
        case howTo
        var id: String { rawValue }
        var titleKey: String {
            switch self {
            case .summary: return "exercise_tab_summary"
            case .history: return "exercise_tab_history"
            case .howTo: return "exercise_tab_howto"
            }
        }
    }

    @State private var tab: Tab = .summary
    @State private var detail: ExerciseDetail?

    var body: some View {
        List {
            if let detail {
                Section {
                    ExerciseIllustrationView(illustrationKey: detail.illustrationKey)
                        .frame(height: 160)
                        .frame(maxWidth: .infinity)
                    Text(LocalizedStringKey(detail.nameKey))
                        .font(.title3.weight(.semibold))
                    if !detail.aliases.isEmpty {
                        Text(detail.aliases.joined(separator: " · "))
                            .font(.caption)
                            .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
                    }
                    Picker("exercise_tab_picker", selection: $tab) {
                        ForEach(Tab.allCases) { item in
                            Text(LocalizedStringKey(item.titleKey)).tag(item)
                        }
                    }
                    .pickerStyle(.segmented)
                    .accessibilityIdentifier("exercise_detail_tabs")
                }
                switch tab {
                case .summary:
                    summarySections(detail)
                case .history:
                    historySection(detail)
                case .howTo:
                    howToSection(detail)
                }
            } else {
                Text("exercise_detail_missing")
                    .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
            }
        }
        .navigationTitle(Text("exercise_detail_title"))
        .onAppear {
            detail = core.routineLibrary.exerciseDetail(profileId: profileId, exerciseId: exerciseId)
        }
    }

    @ViewBuilder
    private func summarySections(_ detail: ExerciseDetail) -> some View {
        Section("exercise_muscles_primary") {
            Text(detail.primaryMuscles.joined(separator: ", "))
                .font(.body)
            MuscleMapIcons(muscles: detail.primaryMuscles.map { String(describing: $0) })
        }
        Section("exercise_muscles_secondary") {
            Text(detail.secondaryMuscles.isEmpty ? "—" : detail.secondaryMuscles.joined(separator: ", "))
                .font(.body)
        }
        Section("exercise_progress_title") {
            metricRow(
                labelKey: "exercise_pr_heaviest",
                value: detail.heaviestWeightKg.map { "\(formatKg($0.doubleValue)) kg" } ?? "—"
            )
            metricRow(
                labelKey: "exercise_pr_e1rm",
                value: detail.estimated1RmKg.map { "\(formatKg($0.doubleValue)) kg" } ?? "—"
            )
            metricRow(
                labelKey: "exercise_pr_volume",
                value: detail.bestSetVolumeKg.map { "\(formatKg($0.doubleValue))" } ?? "—"
            )
            // Simple bar stand-in for a progress chart until history density grows.
            ProgressChartPlaceholder(
                heaviest: detail.heaviestWeightKg?.doubleValue,
                e1rm: detail.estimated1RmKg?.doubleValue,
                volume: detail.bestSetVolumeKg?.doubleValue
            )
        }
        if !detail.personalRecords.isEmpty {
            Section("exercise_prs_title") {
                ForEach(Array(detail.personalRecords.enumerated()), id: \.offset) { _, pr in
                    HStack {
                        Text(LocalizedStringKey(pr.labelKey))
                        Spacer()
                        Text(pr.value)
                            .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
                    }
                }
            }
        }
    }

    @ViewBuilder
    private func historySection(_ detail: ExerciseDetail) -> some View {
        Section("exercise_history_title") {
            if detail.historySets.isEmpty {
                Text("exercise_history_empty")
                    .font(.footnote)
                    .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
            } else {
                ForEach(Array(detail.historySets.enumerated()), id: \.offset) { _, set in
                    HStack {
                        Text(set.weightKg.map { "\(formatKg($0.doubleValue)) kg" } ?? "BW")
                        Text("×\(set.reps)")
                        if let rir = set.rir {
                            Text("RIR \(rir)")
                                .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
                        }
                        Spacer()
                        if set.isWarmup {
                            Text("exercise_warmup_badge")
                                .font(.caption2)
                        }
                    }
                    .font(.caption)
                }
            }
        }
    }

    @ViewBuilder
    private func howToSection(_ detail: ExerciseDetail) -> some View {
        Section("exercise_howto_title") {
            let steps: [(Int, String)] = detail.howToSteps.enumerated().compactMap { index, key in
                let text = NSLocalizedString(key, comment: "")
                guard text != key, !text.isEmpty else { return nil }
                return (index, text)
            }
            if steps.isEmpty {
                Text("exercise_howto_missing")
                    .font(.footnote)
                    .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
            } else {
                ForEach(steps, id: \.0) { index, text in
                    HStack(alignment: .top, spacing: OmniGartMetrics.space2) {
                        Text("\(index + 1).")
                            .fontWeight(.semibold)
                        Text(text)
                    }
                    .accessibilityIdentifier("exercise_howto_step_\(index)")
                }
            }
        }
    }

    private func metricRow(labelKey: String, value: String) -> some View {
        HStack {
            Text(LocalizedStringKey(labelKey))
            Spacer()
            Text(value)
                .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
        }
    }

    private func formatKg(_ value: Double) -> String {
        String(format: "%.1f", value)
    }
}

private struct ProgressChartPlaceholder: View {
    let heaviest: Double?
    let e1rm: Double?
    let volume: Double?

    var body: some View {
        HStack(alignment: .bottom, spacing: 8) {
            bar(height: normalized(heaviest), label: "W")
            bar(height: normalized(e1rm), label: "1RM")
            bar(height: normalized(volume.map { $0 / 20 }), label: "Vol")
        }
        .frame(height: 72)
        .accessibilityIdentifier("exercise_progress_chart")
    }

    private func normalized(_ value: Double?) -> CGFloat {
        guard let value, value > 0 else { return 8 }
        return CGFloat(min(max(value / 100.0, 0.15), 1.0)) * 64
    }

    private func bar(height: CGFloat, label: String) -> some View {
        VStack {
            RoundedRectangle(cornerRadius: 4)
                .fill(Color.OmniGart.colorBrandAccent.opacity(0.7))
                .frame(width: 28, height: height)
            Text(label)
                .font(.caption2)
        }
    }
}
