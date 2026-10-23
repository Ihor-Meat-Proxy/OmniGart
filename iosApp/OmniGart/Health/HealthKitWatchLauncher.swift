import Foundation
import HealthKit

/// Starts a mirrored / companion watch workout when possible; otherwise callers use iPhone fallback.
enum HealthKitWatchLauncher {
    enum LaunchOutcome: Sendable {
        case watchStarted
        case watchUnavailable(String)
    }

    static func startOnWatch(activityType: HKWorkoutActivityType, indoor: Bool = true) async -> LaunchOutcome {
        guard HKHealthStore.isHealthDataAvailable() else {
            return .watchUnavailable("health_unavailable")
        }
        let configuration = HKWorkoutConfiguration()
        configuration.activityType = activityType
        configuration.locationType = indoor ? .indoor : .outdoor
        let store = HKHealthStore()
        do {
            try await HealthKitWorkoutWriter.requestWorkoutAuthorization()
            try await withCheckedThrowingContinuation { (continuation: CheckedContinuation<Void, Error>) in
                store.startWatchApp(with: configuration) { success, error in
                    if let error {
                        continuation.resume(throwing: error)
                    } else if success {
                        continuation.resume()
                    } else {
                        continuation.resume(
                            throwing: NSError(
                                domain: "OmniGart.HealthKitWatchLauncher",
                                code: 1,
                                userInfo: [
                                    NSLocalizedDescriptionKey: "Watch app did not start"
                                ]
                            )
                        )
                    }
                }
            }
            return .watchStarted
        } catch {
            return .watchUnavailable(error.localizedDescription)
        }
    }
}
