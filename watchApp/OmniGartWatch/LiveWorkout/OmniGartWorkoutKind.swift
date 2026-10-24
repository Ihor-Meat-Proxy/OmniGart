import Foundation
import HealthKit
import SwiftUI

/// Indoor / Zone 2 workout kinds the watch can run as live `HKWorkoutSession`s.
enum OmniGartWorkoutKind: String, CaseIterable, Identifiable, Hashable, Sendable {
    case traditionalStrength
    case functionalStrength
    case yoga
    case pilates
    case zone2Run
    case treadmillRun
    case treadmillWalk

    var id: String { rawValue }

    /// Traditional / functional strength — crown set logging + rest haptics.
    var isStrength: Bool {
        switch self {
        case .traditionalStrength, .functionalStrength: return true
        default: return false
        }
    }

    var activityType: HKWorkoutActivityType {
        switch self {
        case .traditionalStrength: return .traditionalStrengthTraining
        case .functionalStrength: return .functionalStrengthTraining
        case .yoga: return .yoga
        case .pilates: return .pilates
        case .zone2Run, .treadmillRun: return .running
        case .treadmillWalk: return .walking
        }
    }

    var locationType: HKWorkoutSessionLocationType {
        switch self {
        case .zone2Run: return .outdoor
        case .treadmillRun, .treadmillWalk: return .indoor
        default: return .indoor
        }
    }

    var titleKey: LocalizedStringKey {
        switch self {
        case .traditionalStrength: return "watch.workout.strength_traditional"
        case .functionalStrength: return "watch.workout.strength_functional"
        case .yoga: return "watch.workout.yoga"
        case .pilates: return "watch.workout.pilates"
        case .zone2Run: return "watch.zone2.start"
        case .treadmillRun: return "treadmill_run_title"
        case .treadmillWalk: return "treadmill_walk_title"
        }
    }

    var isIndoor: Bool { locationType == .indoor }

    static func from(configuration: HKWorkoutConfiguration) -> OmniGartWorkoutKind? {
        switch configuration.activityType {
        case .traditionalStrengthTraining: return .traditionalStrength
        case .functionalStrengthTraining: return .functionalStrength
        case .yoga: return .yoga
        case .pilates: return .pilates
        case .running where configuration.locationType == .outdoor: return .zone2Run
        case .running where configuration.locationType == .indoor: return .treadmillRun
        case .walking where configuration.locationType == .indoor: return .treadmillWalk
        default: return nil
        }
    }

    /// Maps shared [PhoneWorkoutKindIds] (except guided → traditional + programme UI).
    static func from(selectionKind: String) -> OmniGartWorkoutKind? {
        switch selectionKind {
        case "traditionalStrength", "guidedStrength": return .traditionalStrength
        case "functionalStrength": return .functionalStrength
        case "yoga": return .yoga
        case "pilates": return .pilates
        case "zone2Run": return .zone2Run
        case "treadmillRun": return .treadmillRun
        case "treadmillWalk": return .treadmillWalk
        default: return nil
        }
    }

    func makeConfiguration() -> HKWorkoutConfiguration {
        let config = HKWorkoutConfiguration()
        config.activityType = activityType
        config.locationType = locationType
        return config
    }
}
