import SwiftUI

/// Weekly check-in with skippable sections (local-only).
struct NutritionWeeklyCheckInView: View {
    @State private var skipIncomplete = false
    @State private var skipWeighIn = false
    @State private var skipFasting = false
    @State private var acceptTarget = false
    @State private var done = false

    var body: some View {
        Form {
            Section("nutrition_checkin_title") {
                Toggle("nutrition_reason_paused_insufficient", isOn: $skipIncomplete)
                Toggle("nutrition_reminders_weigh_in", isOn: $skipWeighIn)
                Toggle("nutrition_reason_logging_break", isOn: $skipFasting)
                Toggle("nutrition_reason_title", isOn: $acceptTarget)
                Button("profile_save") {
                    done = true
                }
                .accessibilityIdentifier("nutrition_checkin_save")
                if done {
                    Text("profile_saved")
                        .font(.caption)
                        .foregroundStyle(Color.OmniGart.colorBrandAccent)
                }
            }
        }
        .navigationTitle(Text("nutrition_checkin_title"))
        .scrollContentBackground(.hidden)
        .background(Color.OmniGart.colorSurfaceLightCanvas.ignoresSafeArea())
    }
}
