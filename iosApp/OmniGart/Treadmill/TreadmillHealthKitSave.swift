import Foundation
import HealthKit

/// Append-only indoor running workout write for treadmill sessions.
/// Distance comes from the treadmill; elevation/incline is stored as metadata only.
enum TreadmillHealthKitSave {
    @discardableResult
    static func save(
        snapshot: TreadmillFinishSnapshot,
        syncIdentifier: String
    ) async throws -> UUID? {
        let hrPoints: [HealthKitWorkoutWriter.HeartRatePoint] = zip(
            snapshot.heartRateEpochMs,
            snapshot.heartRateBpm
        ).map { epoch, bpm in
            HealthKitWorkoutWriter.HeartRatePoint(
                date: Date(timeIntervalSince1970: Double(epoch) / 1000.0),
                bpm: bpm
            )
        }

        // Prefer the shared writer for energy/HR/sync; then attach distance if present.
        let result = try await HealthKitWorkoutWriter.save(
            options: .init(
                activityType: .running,
                start: snapshot.start,
                end: snapshot.end,
                energy: .init(
                    activeKcal: snapshot.activeKcal ?? 0,
                    basalKcal: 0,
                    isEstimate: snapshot.activeKcal == nil
                ),
                heartRate: hrPoints,
                indoor: true,
                syncIdentifier: syncIdentifier,
                syncVersion: 1
            )
        )

        if let distance = snapshot.distanceMetres, distance > 0 {
            try await appendDistanceSample(
                metres: distance,
                start: snapshot.start,
                end: snapshot.end,
                inclinePercent: snapshot.averageInclinePercent,
                syncIdentifier: syncIdentifier
            )
        }

        return result?.workoutUUID
    }

    /// Append-only distance (+ incline metadata) quantity sample. Never deletes.
    private static func appendDistanceSample(
        metres: Double,
        start: Date,
        end: Date,
        inclinePercent: Double?,
        syncIdentifier: String
    ) async throws {
        guard HKHealthStore.isHealthDataAvailable() else { return }
        guard let distanceType = HKQuantityType.quantityType(forIdentifier: .distanceWalkingRunning)
        else { return }
        let store = HKHealthStore()
        try await store.requestAuthorization(toShare: [distanceType], read: [distanceType])

        var metadata: [String: Any] = [
            HKMetadataKeySyncIdentifier: "\(syncIdentifier).distance",
            HKMetadataKeySyncVersion: 1,
            "omnigart.treadmill": true,
        ]
        if let inclinePercent {
            metadata["omnigart.averageInclinePercent"] = inclinePercent
        }
        let sample = HKQuantitySample(
            type: distanceType,
            quantity: HKQuantity(unit: .meter(), doubleValue: metres),
            start: start,
            end: end,
            metadata: metadata
        )
        try await store.save(sample)
    }
}
