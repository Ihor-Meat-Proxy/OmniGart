import Foundation
import HealthKit

/// Builds `HKWorkoutEvent` date intervals that satisfy HealthKit's duration rules.
///
/// HealthKit throws `NSInvalidArgumentException` when pause / resume / motionPaused /
/// motionResumed / marker events have a non-zero `DateInterval` duration. Lap and
/// segment events require a non-zero duration.
enum OmniGartWorkoutEventBuilder {
    /// Point-in-time marker duration used when a segment/lap has no explicit end.
    static let defaultSegmentDuration: TimeInterval = 1

    /// Whether HealthKit requires a zero-duration interval for this event type.
    static func requiresZeroDuration(_ type: HKWorkoutEventType) -> Bool {
        switch type {
        case .pause, .resume, .motionPaused, .motionResumed, .marker:
            return true
        case .lap, .segment:
            return false
        @unknown default:
            // Prefer zero for unknown point-like events; callers can pass an explicit end.
            return true
        }
    }

    /// Date interval that is valid for `type` when creating an `HKWorkoutEvent`.
    static func dateInterval(
        for type: HKWorkoutEventType,
        start: Date,
        end: Date? = nil
    ) -> DateInterval {
        if requiresZeroDuration(type) {
            return DateInterval(start: start, duration: 0)
        }
        if let end, end > start {
            return DateInterval(start: start, end: end)
        }
        return DateInterval(start: start, duration: defaultSegmentDuration)
    }

    /// Creates an `HKWorkoutEvent` with a HealthKit-valid date interval.
    static func makeEvent(
        type: HKWorkoutEventType,
        at date: Date,
        end: Date? = nil,
        metadata: [String: Any]? = nil
    ) -> HKWorkoutEvent {
        HKWorkoutEvent(
            type: type,
            dateInterval: dateInterval(for: type, start: date, end: end),
            metadata: metadata
        )
    }
}
