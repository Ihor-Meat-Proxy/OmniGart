import Foundation
import HealthKit
import Shared

/// Loads read-only HealthKit samples into [HealthMetricsDashboard] off the main thread.
enum HealthMetricsLoader {
    struct Outcome {
        let authorized: Bool
        let dashboard: HealthMetricsDashboard
    }

    static func loadToday() async -> Outcome {
        guard HKHealthStore.isHealthDataAvailable() else {
            return Outcome(
                authorized: false,
                dashboard: HealthMetricsAssembler.shared.emptyPermissionDashboard()
            )
        }
        let store = HKHealthStore()
        let readTypes = HealthMetricsReadTypes.allReadTypes()
        // HealthKit does not expose definitive read status; attempt queries and treat failures as empty.
        do {
            try await store.requestAuthorization(toShare: [], read: readTypes)
        } catch {
            return Outcome(
                authorized: false,
                dashboard: HealthMetricsAssembler.shared.emptyPermissionDashboard()
            )
        }

        let samples = await HealthMetricsTodayLoad.fetch()
        return Outcome(
            authorized: true,
            dashboard: HealthMetricsTodayLoad.assemble(samples)
        )
    }
}
