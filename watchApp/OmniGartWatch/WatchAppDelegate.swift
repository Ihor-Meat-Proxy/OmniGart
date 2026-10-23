import Foundation
import HealthKit
import WatchKit

/// Holds a workout kind requested from the iPhone via `startWatchApp`.
@MainActor
final class PendingWatchWorkout: ObservableObject {
    static let shared = PendingWatchWorkout()
    @Published var kind: OmniGartWorkoutKind?
}

/// Receives `startWatchApp` / mirrored workout configurations from the iPhone.
final class WatchAppDelegate: NSObject, WKApplicationDelegate {
    func handle(_ workoutConfiguration: HKWorkoutConfiguration) {
        let kind = OmniGartWorkoutKind.from(configuration: workoutConfiguration)
        Task { @MainActor in
            PendingWatchWorkout.shared.kind = kind
        }
    }
}
