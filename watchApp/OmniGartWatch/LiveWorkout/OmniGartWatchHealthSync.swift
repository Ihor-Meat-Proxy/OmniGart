import Foundation
import HealthKit

/// Append-only helpers for watch Health writes (never delete / rewrite).
enum OmniGartWatchHealthSync {
    static let syncIdPrefix = "omnigart.workout."

    static func makeSyncIdentifier() -> String {
        "\(syncIdPrefix)\(UUID().uuidString)"
    }

    static func workoutExists(syncIdentifier: String, store: HKHealthStore) async -> Bool {
        guard !syncIdentifier.isEmpty else { return false }
        let predicate = HKQuery.predicateForObjects(
            withMetadataKey: HKMetadataKeySyncIdentifier,
            allowedValues: [syncIdentifier]
        )
        return await withCheckedContinuation { continuation in
            let query = HKSampleQuery(
                sampleType: HKObjectType.workoutType(),
                predicate: predicate,
                limit: 1,
                sortDescriptors: nil
            ) { _, samples, _ in
                continuation.resume(returning: samples?.isEmpty == false)
            }
            store.execute(query)
        }
    }

    static func syncMetadata(syncIdentifier: String, indoor: Bool) -> [String: Any] {
        [
            HKMetadataKeyIndoorWorkout: indoor,
            HKMetadataKeySyncIdentifier: syncIdentifier,
            HKMetadataKeySyncVersion: NSNumber(value: 1)
        ]
    }
}
