import SwiftUI

/// Local-notification reminder settings (weigh-in nudge, measurements, pause).
struct NutritionReminderSettingsView: View {
    @State private var paused = false
    @State private var weighInNudge = true
    @State private var weeks = 3

    var body: some View {
        Section("nutrition_reminders_title") {
            Toggle("nutrition_reminders_pause", isOn: $paused)
                .accessibilityIdentifier("nutrition_reminders_pause")
            Toggle("nutrition_reminders_weigh_in", isOn: $weighInNudge)
                .disabled(paused)
                .accessibilityIdentifier("nutrition_reminders_weigh_in")
            Stepper(value: $weeks, in: 2...4) {
                Text("nutrition_reminders_measurements")
                Text("\(weeks) wk")
                    .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
            }
            .disabled(paused)
            .accessibilityIdentifier("nutrition_reminders_measurements")
            NavigationLink("nutrition_checkin_title") {
                NutritionWeeklyCheckInView()
            }
        }
        .onAppear(perform: load)
        .onChange(of: paused) { _, _ in persist() }
        .onChange(of: weighInNudge) { _, _ in persist() }
        .onChange(of: weeks) { _, _ in persist() }
    }

    private func load() {
        let prefs = NutritionLocalStore.shared.loadReminders()
        paused = prefs.paused
        weighInNudge = prefs.weighInNudgeEnabled
        weeks = prefs.measurementWeeks
    }

    private func persist() {
        var prefs = NutritionLocalStore.shared.loadReminders()
        prefs.paused = paused
        prefs.weighInNudgeEnabled = weighInNudge
        prefs.measurementWeeks = weeks
        NutritionLocalStore.shared.saveReminders(prefs)
        var goal = NutritionLocalStore.shared.loadGoal()
        goal.remindersPaused = paused
        goal.weighInNudgeEnabled = weighInNudge
        goal.measurementReminderWeeks = weeks
        NutritionLocalStore.shared.saveGoal(goal)
        NutritionReminderScheduler.shared.reschedule()
    }
}
