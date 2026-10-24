import Shared
import SwiftUI

/// Detail for a local health workout, including one-tap restore when removed from Apple Health.
struct LocalHealthWorkoutDetailView: View {
    let core: OmniGartCore
    let workoutId: String

    @State private var message: String?
    @State private var restoring = false
    @State private var tick = 0

    private var removed: Bool {
        _ = tick
        return core.localHealth.isRemovedFromPlatform(localId: workoutId)
    }

    var body: some View {
        List {
            Section {
                Text(core.localHealth.workoutDetailTitle(localId: workoutId))
                    .font(.headline)
                    .accessibilityIdentifier("health_workout_detail_title")
                Text(core.localHealth.workoutDetailSubtitle(localId: workoutId))
                    .font(.footnote)
                    .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
                if removed {
                    Text("health_restore_removed_banner")
                        .font(.caption)
                        .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
                        .accessibilityIdentifier("health_restore_removed_banner")
                }
            }
            if removed {
                Section {
                    Button {
                        Task { await restoreOne() }
                    } label: {
                        if restoring {
                            ProgressView()
                        } else {
                            Text("health_restore_to_apple_health")
                        }
                    }
                    .disabled(restoring)
                    .accessibilityIdentifier("health_restore_to_apple_health")
                }
            }
            if let message {
                Section {
                    Text(message)
                        .font(.caption)
                        .accessibilityIdentifier("health_restore_message")
                }
            }
            Section {
                Button("pdf_export_button") {
                    WorkoutPdfExport.shareSingle(core: core, workoutId: workoutId)
                }
                .accessibilityIdentifier("health_workout_detail_pdf")
            }
        }
        .navigationTitle(Text("health_workout_detail_title"))
        .navigationBarTitleDisplayMode(.inline)
    }

    private func restoreOne() async {
        restoring = true
        defer { restoring = false }
        let outcome = await HealthKitWorkoutRestorer.restore(localId: workoutId, core: core)
        await MainActor.run {
            tick += 1
            switch outcome {
            case .restored:
                message = String(localized: "health_restore_success")
            case .alreadyPresent:
                message = String(localized: "health_restore_already_present")
            case .skipped:
                message = String(localized: "health_restore_skipped")
            case .failed(let reason):
                message = reason
            }
        }
    }
}
