import SwiftUI

/// When on (default), starting a workout on the phone also launches the matching session on the watch.
struct StartWorkoutsOnWatchSettingsToggle: View {
    @AppStorage(HealthKitWatchLauncher.preferenceKey) private var enabled = true

    var body: some View {
        Toggle(isOn: $enabled) {
            VStack(alignment: .leading, spacing: 4) {
                Text("workout_start_on_watch_toggle")
                Text("workout_start_on_watch_toggle_body")
                    .font(.caption)
                    .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
            }
        }
        .accessibilityIdentifier("profile_start_workouts_on_watch_toggle")
    }
}
