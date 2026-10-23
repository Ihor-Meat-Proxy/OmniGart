import Foundation
import UserNotifications

/// Schedules local-only nutrition reminders (no remote push).
final class NutritionReminderScheduler {
    static let shared = NutritionReminderScheduler()

    func requestAuthorizationIfNeeded() {
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound]) { _, _ in }
    }

    func reschedule() {
        let center = UNUserNotificationCenter.current()
        center.removePendingNotificationRequests(
            withIdentifiers: [
                "nutrition.weigh_in_nudge",
                "nutrition.measurements",
                "nutrition.weekly_checkin"
            ]
        )
        let prefs = NutritionLocalStore.shared.loadReminders()
        if prefs.paused { return }
        requestAuthorizationIfNeeded()
        if prefs.weighInNudgeEnabled {
            schedule(
                id: "nutrition.weigh_in_nudge",
                titleKey: "nutrition_reminders_weigh_in",
                bodyKey: "nutrition_reason_logging_break",
                afterDays: 3
            )
        }
        schedule(
            id: "nutrition.measurements",
            titleKey: "nutrition_reminders_measurements",
            bodyKey: "nutrition_open_measurements",
            afterDays: prefs.measurementWeeks * 7
        )
        let weekday = NutritionLocalStore.shared.loadGoal().weeklyCheckInWeekday + 1
        scheduleWeekly(
            id: "nutrition.weekly_checkin",
            titleKey: "nutrition_checkin_title",
            bodyKey: "nutrition_target_disclaimer",
            weekday: weekday
        )
    }

    private func schedule(id: String, titleKey: String, bodyKey: String, afterDays: Int) {
        let content = UNMutableNotificationContent()
        content.title = String(localized: String.LocalizationValue(titleKey))
        content.body = String(localized: String.LocalizationValue(bodyKey))
        let trigger = UNTimeIntervalNotificationTrigger(
            timeInterval: TimeInterval(max(afterDays, 1) * 86_400),
            repeats: false
        )
        let request = UNNotificationRequest(identifier: id, content: content, trigger: trigger)
        UNUserNotificationCenter.current().add(request)
    }

    private func scheduleWeekly(id: String, titleKey: String, bodyKey: String, weekday: Int) {
        let content = UNMutableNotificationContent()
        content.title = String(localized: String.LocalizationValue(titleKey))
        content.body = String(localized: String.LocalizationValue(bodyKey))
        var date = DateComponents()
        date.weekday = min(max(weekday, 1), 7)
        date.hour = 9
        let trigger = UNCalendarNotificationTrigger(dateMatching: date, repeats: true)
        let request = UNNotificationRequest(identifier: id, content: content, trigger: trigger)
        UNUserNotificationCenter.current().add(request)
    }
}
