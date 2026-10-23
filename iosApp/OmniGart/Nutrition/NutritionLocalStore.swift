import Foundation

/// On-device preferences and measurement samples for the nutrition target card.
final class NutritionLocalStore: ObservableObject {
    static let shared = NutritionLocalStore()

    private let defaults = UserDefaults.standard
    private let goalKey = "nutrition.goal.settings.v1"
    private let weightsKey = "nutrition.weights.v1"
    private let measurementsKey = "nutrition.measurements.v1"
    private let bodyFatKey = "nutrition.bodyfat.v1"
    private let lastTargetKey = "nutrition.lastTargetKcal.v1"
    private let remindersKey = "nutrition.reminders.v1"

    struct GoalPrefs: Codable {
        var kind: String = "MAINTAIN"
        var rateFractionPerWeek: Double = 0.0
        var targetWeightKg: Double?
        var mode: String = "GUIDED"
        var manualDailyKcal: Double?
        var flexibleWeeklyBudgetKcal: Double?
        var addExerciseCalories: Bool = false
        var dayStartMinutes: Int = 4 * 60
        var weeklyCheckInWeekday: Int = 0
        var remindersPaused: Bool = false
        var weighInNudgeEnabled: Bool = true
        var measurementReminderWeeks: Int = 3
        var firstGoalAcknowledged: Bool = false
    }

    struct WeightSample: Codable, Identifiable {
        var id: String
        var epochMs: Int64
        var massKg: Double
    }

    struct GirthSample: Codable, Identifiable {
        var id: String
        var epochMs: Int64
        var site: String
        var valueCm: Double
    }

    struct BodyFatSample: Codable, Identifiable {
        var id: String
        var epochMs: Int64
        var bodyFatPercent: Double
    }

    struct ReminderPrefs: Codable {
        var paused: Bool = false
        var weighInNudgeEnabled: Bool = true
        var measurementWeeks: Int = 3
    }

    func loadGoal() -> GoalPrefs {
        guard let data = defaults.data(forKey: goalKey),
              let decoded = try? JSONDecoder().decode(GoalPrefs.self, from: data)
        else { return GoalPrefs() }
        return decoded
    }

    func saveGoal(_ prefs: GoalPrefs) {
        encode(prefs, key: goalKey)
    }

    func loadWeights() -> [WeightSample] {
        decode([WeightSample].self, key: weightsKey) ?? []
    }

    func appendWeight(massKg: Double) {
        var list = loadWeights()
        list.append(
            WeightSample(
                id: UUID().uuidString,
                epochMs: Int64(Date().timeIntervalSince1970 * 1000),
                massKg: massKg
            )
        )
        encode(list, key: weightsKey)
    }

    func loadGirths() -> [GirthSample] {
        decode([GirthSample].self, key: measurementsKey) ?? []
    }

    func appendGirth(site: String, valueCm: Double) {
        var list = loadGirths()
        list.append(
            GirthSample(
                id: UUID().uuidString,
                epochMs: Int64(Date().timeIntervalSince1970 * 1000),
                site: site,
                valueCm: valueCm
            )
        )
        encode(list, key: measurementsKey)
    }

    func loadBodyFat() -> [BodyFatSample] {
        decode([BodyFatSample].self, key: bodyFatKey) ?? []
    }

    func appendBodyFat(percent: Double) {
        var list = loadBodyFat()
        list.append(
            BodyFatSample(
                id: UUID().uuidString,
                epochMs: Int64(Date().timeIntervalSince1970 * 1000),
                bodyFatPercent: percent
            )
        )
        encode(list, key: bodyFatKey)
    }

    func lastTargetKcal() -> Double? {
        let value = defaults.double(forKey: lastTargetKey)
        return value > 0 ? value : nil
    }

    func saveLastTargetKcal(_ kcal: Double) {
        defaults.set(kcal, forKey: lastTargetKey)
    }

    func loadReminders() -> ReminderPrefs {
        decode(ReminderPrefs.self, key: remindersKey) ?? ReminderPrefs()
    }

    func saveReminders(_ prefs: ReminderPrefs) {
        encode(prefs, key: remindersKey)
    }

    private func encode<T: Encodable>(_ value: T, key: String) {
        if let data = try? JSONEncoder().encode(value) {
            defaults.set(data, forKey: key)
        }
    }

    private func decode<T: Decodable>(_ type: T.Type, key: String) -> T? {
        guard let data = defaults.data(forKey: key) else { return nil }
        return try? JSONDecoder().decode(type, from: data)
    }
}
