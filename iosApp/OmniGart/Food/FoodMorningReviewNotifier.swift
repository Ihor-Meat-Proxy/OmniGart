import Foundation
import Shared
import UserNotifications

/// Schedules the optional local morning food-review notification (UserNotifications).
final class FoodMorningReviewNotifier: MorningReviewNotifier {
    private let center = UNUserNotificationCenter.current()

    func scheduleDaily(profileId: String, minutesAfterMidnight: Int32, enabled: Bool) {
        let id = "omnigart.food.morning.\(profileId)"
        center.removePendingNotificationRequests(withIdentifiers: [id])
        guard enabled else { return }
        center.requestAuthorization(options: [.alert, .sound]) { granted, _ in
            guard granted else { return }
            let content = UNMutableNotificationContent()
            content.title = String(localized: "food_review_title")
            content.body = String(localized: "food_review_notification_body")
            content.sound = .default
            var date = DateComponents()
            date.hour = Int(minutesAfterMidnight) / 60
            date.minute = Int(minutesAfterMidnight) % 60
            let trigger = UNCalendarNotificationTrigger(dateMatching: date, repeats: true)
            let request = UNNotificationRequest(identifier: id, content: content, trigger: trigger)
            self.center.add(request)
        }
    }

    func cancel(profileId: String) {
        let id = "omnigart.food.morning.\(profileId)"
        center.removePendingNotificationRequests(withIdentifiers: [id])
        center.removeDeliveredNotifications(withIdentifiers: [id])
    }
}
