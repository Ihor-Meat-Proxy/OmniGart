import HealthKit
import Shared
import SwiftUI

/// Starts a companion watch live workout with exact kind selection; shows status on failure.
struct WorkoutWatchStartRow: View {
    let titleKey: LocalizedStringKey
    let activityType: HKWorkoutActivityType
    let indoor: Bool
    let accessibilityId: String
    var kindOverride: String? = nil
    var sessionKeyPrefix: String = "cat"

    @State private var status: String?

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Button {
                Task { await start() }
            } label: {
                Text(titleKey)
            }
            .accessibilityIdentifier(accessibilityId)
            if let status {
                Text(status)
                    .font(.caption)
                    .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
                    .accessibilityIdentifier("\(accessibilityId)_status")
            }
        }
    }

    private func start() async {
        let outcome = await HealthKitWatchLauncher.startOnWatch(
            activityType: activityType,
            indoor: indoor,
            kindOverride: kindOverride,
            sessionKey: "\(sessionKeyPrefix)-\(Int(Date().timeIntervalSince1970))"
        )
        await MainActor.run {
            switch outcome {
            case .watchStarted:
                status = String(localized: "workout_watch_started")
            case .watchUnavailable:
                status = String(localized: "workout_watch_unavailable")
            case .skippedByPreference:
                status = nil
            }
        }
    }
}
