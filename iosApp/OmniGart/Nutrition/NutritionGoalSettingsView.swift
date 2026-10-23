import SwiftUI

/// Profile setting for energy goal, rate, target mode, start-of-day, and exercise credit.
struct NutritionGoalSettingsView: View {
    @State private var kind = "MAINTAIN"
    @State private var mode = "GUIDED"
    @State private var ratePercent = 0.5
    @State private var dayStartHour = 4
    @State private var addExercise = false
    @State private var targetWeight = ""
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
                HStack {
                    Text("nutrition_target_weight")
                    TextField("nutrition_target_weight", text: $targetWeight)
                        .keyboardType(.decimalPad)
                        .multilineTextAlignment(.trailing)
                        .accessibilityIdentifier("nutrition_target_weight")
                }
            }
            Stepper(value: $dayStartHour, in: 0...12) {
                Text("nutrition_day_start")
                Text(String(format: "%02d:00", dayStartHour))
                    .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
            }
            .accessibilityIdentifier("nutrition_day_start")
            Toggle("nutrition_add_exercise", isOn: $addExercise)
                .accessibilityIdentifier("nutrition_add_exercise")
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
        dayStartHour = goal.dayStartMinutes / 60
        addExercise = goal.addExerciseCalories
        if let tw = goal.targetWeightKg {
            targetWeight = String(format: "%.1f", tw)
        }
        showFirstGoalNote = !goal.firstGoalAcknowledged
    }

    private func save() {
        var goal = NutritionLocalStore.shared.loadGoal()
        goal.kind = kind
        goal.mode = mode
        goal.rateFractionPerWeek = kind == "MAINTAIN" ? 0 : ratePercent / 100.0
        goal.dayStartMinutes = dayStartHour * 60
        goal.addExerciseCalories = addExercise
        goal.targetWeightKg = Double(targetWeight)
        goal.firstGoalAcknowledged = true
        NutritionLocalStore.shared.saveGoal(goal)
        showFirstGoalNote = false
        saved = true
        NutritionReminderScheduler.shared.reschedule()
    }
}
