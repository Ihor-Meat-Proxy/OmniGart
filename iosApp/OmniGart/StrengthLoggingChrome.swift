import SwiftUI
import Shared

struct StrengthHeaderStat: View {
    let title: String
    let value: String

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(title).font(.caption2).foregroundStyle(Color.OmniGart.colorTextLightSecondary)
            Text(value).font(.subheadline.monospacedDigit().weight(.semibold))
        }
    }
}

struct StrengthLiveHeaderState {
    var elapsedLabel: String
    var volumeDisplay: Double
    var unitIsLb: Bool
    var setCount: Int
    var muscles: [String]
    var restRunning: Bool
    var restRemaining: Int
    var savingHealth: Bool
}

struct StrengthLiveHeaderView: View {
    let state: StrengthLiveHeaderState
    let onRestTap: () -> Void
    let onFinish: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(spacing: 12) {
                StrengthHeaderStat(title: strengthCatalog("workout_summary_duration"), value: state.elapsedLabel)
                StrengthHeaderStat(
                    title: strengthCatalog("workout_volume_short"),
                    value: String(format: "%.0f %@", state.volumeDisplay, state.unitIsLb ? "lb" : "kg")
                )
                StrengthHeaderStat(title: strengthCatalog("workout_sets_count"), value: "\(state.setCount)")
            }
            .accessibilityIdentifier("workout_status")

            HStack(spacing: 8) {
                miniBodyMap
                Spacer(minLength: 4)
                restButton
                Button(strengthCatalog("workout_finish")) {
                    onFinish()
                }
                .disabled(state.savingHealth)
                .accessibilityIdentifier("workout_end")
            }
        }
    }

    private var restButton: some View {
        Button {
            onRestTap()
        } label: {
            Text(
                state.restRunning
                    ? "\(strengthCatalog("workout_rest_timer")) \(state.restRemaining)s"
                    : strengthCatalog("workout_rest_timer")
            )
            .font(.caption.weight(.semibold))
        }
        .accessibilityIdentifier("workout_rest_timer")
    }

    private var miniBodyMap: some View {
        HStack(spacing: 4) {
            Image(systemName: "figure.strengthtraining.traditional")
                .foregroundStyle(Color.OmniGart.colorBrandAccent)
            Text(
                state.muscles.isEmpty
                    ? strengthCatalog("workout_body_map")
                    : state.muscles.prefix(3).joined(separator: " · ")
            )
            .font(.caption2)
            .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
            .lineLimit(1)
        }
        .accessibilityLabel(Text(strengthCatalog("workout_body_map")))
    }
}

struct StrengthSummaryBlockView: View {
    let summaryLines: [String]
    let lastSessionId: String?
    let onExport: (String) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(strengthCatalog("workout_summary_title"))
                .font(.subheadline.weight(.semibold))
                .accessibilityIdentifier("workout_summary")
            ForEach(summaryLines, id: \.self) { line in
                Text(line).font(.footnote)
            }
            if let lastSessionId {
                Button("pdf_export_button") {
                    onExport(lastSessionId)
                }
                .accessibilityIdentifier("pdf_export_button")
                Text(strengthCatalog("workout_workoutkit_limit"))
                    .font(.caption2)
                    .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
    }
}

struct StrengthNoteSheet: View {
    @Binding var text: String
    let onSave: () -> Void

    var body: some View {
        NavigationStack {
            TextField(strengthCatalog("workout_note"), text: $text, axis: .vertical)
                .padding()
            Spacer()
                .navigationTitle(strengthCatalog("workout_note"))
                .toolbar {
                    ToolbarItem(placement: .confirmationAction) {
                        Button(strengthCatalog("workout_finish")) {
                            onSave()
                        }
                    }
                }
        }
    }
}

struct StrengthRpeSheet: View {
    @Binding var text: String
    let onSave: () -> Void

    var body: some View {
        NavigationStack {
            TextField(strengthCatalog("workout_record_rpe"), text: $text)
                .keyboardType(.decimalPad)
                .padding()
            Spacer()
                .navigationTitle(strengthCatalog("workout_record_rpe"))
                .toolbar {
                    ToolbarItem(placement: .confirmationAction) {
                        Button(strengthCatalog("workout_finish")) {
                            onSave()
                        }
                    }
                }
        }
    }
}
