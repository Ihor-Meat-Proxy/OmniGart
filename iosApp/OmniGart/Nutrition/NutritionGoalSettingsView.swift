import SwiftUI

/// Profile setting for energy goal, rate, and target mode.
struct NutritionGoalSettingsView: View {
    @State private var kind = "MAINTAIN"
    @State private var mode = "GUIDED"
    @State private var ratePercent = 0.5
    @State private var showFirstGoalNote = false
    @State private var saved = false

    var body: some View {
        Section("nutrition_goal_settings_title") {
            if showFirstGoalNote {
                Text("nutrition_goal_first_set")
                    .font(.footnote)
                    .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
            }
            Picker("nutrition_goal_settings_title", selection: $kind) {
                Text("nutrition_goal_lose").tag("LOSE")
                Text("nutrition_goal_maintain").tag("MAINTAIN")
                Text("nutrition_goal_gain").tag("GAIN")
            }
            .accessibilityIdentifier("nutrition_goal_kind")
            Picker("nutrition_mode_guided", selection: $mode) {
                Text("nutrition_mode_guided").tag("GUIDED")
                Text("nutrition_mode_flexible").tag("FLEXIBLE_WEEKLY")
                Text("nutrition_mode_manual").tag("MANUAL")
            }
            .accessibilityIdentifier("nutrition_goal_mode")
            if kind != "MAINTAIN" {
                Stepper(value: $ratePercent, in: 0.25...1.0, step: 0.25) {
                    Text(String(format: "%.2f%% / wk", ratePercent))
                }
                .accessibilityIdentifier("nutrition_goal_rate")
            }
            Button("profile_save") {
                save()
            }
            .accessibilityIdentifier("nutrition_goal_save")
            if saved {
                Text("profile_saved")
                    .font(.caption)
                    .foregroundStyle(Color.OmniGart.colorBrandAccent)
            }
        }
        .onAppear(perform: load)
    }

    private func load() {
        let goal = NutritionLocalStore.shared.loadGoal()
        kind = goal.kind
        mode = goal.mode
        ratePercent = abs(goal.rateFractionPerWeek) * 100.0
        if ratePercent == 0 { ratePercent = 0.5 }
        showFirstGoalNote = !goal.firstGoalAcknowledged
    }

    private func save() {
        var goal = NutritionLocalStore.shared.loadGoal()
        goal.kind = kind
        goal.mode = mode
        goal.rateFractionPerWeek = kind == "MAINTAIN" ? 0 : ratePercent / 100.0
        goal.firstGoalAcknowledged = true
        NutritionLocalStore.shared.saveGoal(goal)
        showFirstGoalNote = false
        saved = true
        NutritionReminderScheduler.shared.reschedule()
    }
}
