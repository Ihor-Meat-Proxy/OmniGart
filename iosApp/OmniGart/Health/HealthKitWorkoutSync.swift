import Foundation
import HealthKit

extension HealthKitWorkoutWriter {
    /// Prefix for OmniGart-owned sync identifiers written into Health metadata.
    static let syncIdPrefix = "omnigart.workout."

    static func syncIdentifier(forSessionId sessionId: String) -> String {
        "\(syncIdPrefix)\(sessionId)"
    }

    /// True when a workout with [syncIdentifier] is already in Health (append-only guard).
    static func workoutExists(syncIdentifier: String) async throws -> Bool {
        try await existingWorkoutUUID(syncIdentifier: syncIdentifier) != nil
    }

    static func existingWorkoutUUID(syncIdentifier: String) async throws -> UUID? {
        guard !syncIdentifier.isEmpty else { return nil }
        let store = HKHealthStore()
        let predicate = HKQuery.predicateForObjects(
            withMetadataKey: HKMetadataKeySyncIdentifier,
            allowedValues: [syncIdentifier]
        )
        return try await withCheckedThrowingContinuation { continuation in
            let query = HKSampleQuery(
                sampleType: HealthKitPlatformBridge.workoutType(),
                predicate: predicate,
                limit: 1,
                sortDescriptors: nil
            ) { _, samples, error in
                if let error {
                    continuation.resume(throwing: error)
                    return
                }
                continuation.resume(returning: (samples?.first as? HKWorkout)?.uuid)
            }
            store.execute(query)
        }
    }
}
