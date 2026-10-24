import SwiftUI

/// Yoga mindful-minutes write is opt-in (default off) per App Review / wellness framing.
struct MindfulMinutesSettingsToggle: View {
    @AppStorage("omnigart.yoga.mindful_minutes.enabled") private var enabled = false

    var body: some View {
        Toggle(isOn: $enabled) {
            VStack(alignment: .leading, spacing: 4) {
                Text("workout_mindful_minutes_toggle")
                Text("workout_mindful_minutes_body")
                    .font(.caption)
                    .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
            }
        }
        .accessibilityIdentifier("profile_mindful_minutes_toggle")
    }
}
