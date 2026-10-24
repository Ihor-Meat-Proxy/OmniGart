import SwiftUI

/// Browse categories for the Workouts tab (2-column tile grid).
enum WorkoutCategory: String, CaseIterable, Identifiable, Hashable {
    case running
    case strength
    case yogaPilates
    case swimming
    case walking

    var id: String { rawValue }

    var titleKey: LocalizedStringKey {
        switch self {
        case .running: return "workout_cat_running"
        case .strength: return "workout_cat_strength"
        case .yogaPilates: return "workout_cat_yoga_pilates"
        case .swimming: return "workout_cat_swimming"
        case .walking: return "workout_cat_walking"
        }
    }

    var titleLocalizationKey: String {
        switch self {
        case .running: return "workout_cat_running"
        case .strength: return "workout_cat_strength"
        case .yogaPilates: return "workout_cat_yoga_pilates"
        case .swimming: return "workout_cat_swimming"
        case .walking: return "workout_cat_walking"
        }
    }

    /// SF Symbol used as the coloured glyph (top-left of the tile).
    /// Joined segments avoid CI mistaking symbol names for String Catalog keys.
    var systemImage: String {
        switch self {
        case .running: return ["figure", "run"].joined(separator: ".")
        case .strength: return ["dumbbell", "fill"].joined(separator: ".")
        case .yogaPilates: return ["figure", "yoga"].joined(separator: ".")
        case .swimming: return ["figure", "pool", "swim"].joined(separator: ".")
        case .walking: return ["figure", "walk"].joined(separator: ".")
        }
    }

    /// Distinct accent per category (Okabe–Ito / readiness palette).
    var accent: Color {
        switch self {
        case .running: return Color.OmniGart.colorHrZoneZ4
        case .strength: return Color.OmniGart.colorBrandAccent
        case .yogaPilates: return Color.OmniGart.colorReadinessRest
        case .swimming: return Color.OmniGart.colorHrZoneZ1
        case .walking: return Color.OmniGart.colorHrZoneZ2
        }
    }

    /// Local health activity type names for recent history.
    var historyActivityTypes: [String] {
        switch self {
        case .running: return ["RUNNING"]
        case .strength: return ["STRENGTH", "HIIT"]
        case .yogaPilates: return ["YOGA", "PILATES"]
        case .swimming: return ["SWIMMING"]
        case .walking: return ["WALKING"]
        }
    }

    /// Extra tokens matched by the Workouts search field (en-GB stems; locale titles also match).
    var searchTokens: [String] {
        switch self {
        case .running:
            return ["run", "zone 2", "zone2", "treadmill", "outdoor", "aerobic"]
        case .strength:
            return [
                "strength", "guided", "free", "functional", "bodyweight",
                "equipment", "routine", "set", "gym", "dumbbell"
            ]
        case .yogaPilates:
            return ["yoga", "pilates", "mindful", "mobility"]
        case .swimming:
            return ["swim", "pool", "open water", "lap"]
        case .walking:
            return ["walk", "treadmill", "outdoor", "steps"]
        }
    }

    func matches(query: String) -> Bool {
        let needle = query.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        if needle.isEmpty { return true }
        let title = String(localized: String.LocalizationValue(titleLocalizationKey)).lowercased()
        if title.contains(needle) { return true }
        return searchTokens.contains { $0.contains(needle) || needle.contains($0) }
    }
}
