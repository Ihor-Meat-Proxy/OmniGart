import Foundation
import Shared

/// Calendar day window for diary queries (midnight → next midnight, local).
enum FoodDayBounds {
    static func range(dayOffset: Int, calendar: Calendar = .current) -> (startMs: Int64, endMs: Int64, dayKey: String) {
        let startOfToday = calendar.startOfDay(for: Date())
        guard let start = calendar.date(byAdding: .day, value: dayOffset, to: startOfToday),
              let end = calendar.date(byAdding: .day, value: 1, to: start)
        else {
            let now = Int64(Date().timeIntervalSince1970 * 1000)
            return (now, now + 86_400_000, "unknown")
        }
        let startMs = Int64(start.timeIntervalSince1970 * 1000)
        let endMs = Int64(end.timeIntervalSince1970 * 1000)
        let formatter = DateFormatter()
        formatter.calendar = calendar
        formatter.locale = Locale(identifier: "en_GB_POSIX")
        formatter.dateFormat = "yyyy-MM-dd"
        return (startMs, endMs, formatter.string(from: start))
    }

    static func title(dayOffset: Int) -> String {
        if dayOffset == 0 {
            return String(localized: "food_day_today")
        }
        let bounds = range(dayOffset: dayOffset)
        let formatter = DateFormatter()
        formatter.dateStyle = .medium
        formatter.timeStyle = .none
        let date = Date(timeIntervalSince1970: TimeInterval(bounds.startMs) / 1000)
        return formatter.string(from: date)
    }
}
