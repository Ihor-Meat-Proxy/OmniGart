import HealthKit
import SwiftUI

/// Starts a companion watch live workout; shows status on failure (no fake session).
struct WorkoutWatchStartRow: View {
    let titleKey: LocalizedStringKey
    let activityType: HKWorkoutActivityType
    let indoor: Bool
    let accessibilityId: String

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
            indoor: indoor
        )
        await MainActor.run {
            switch outcome {
            case .watchStarted:
                status = String(localized: "workout_watch_started")
            case .watchUnavailable:
                status = String(localized: "workout_watch_unavailable")
            }
        }
    }
}
