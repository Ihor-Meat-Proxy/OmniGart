import HealthKit

/// Append-only weigh-in writer. Exist-checks sync id metadata and never deletes/updates.
enum NutritionWeighInWriter {
    private static let store = HKHealthStore()

    static func appendIfAbsent(localId: String, massKg: Double, date: Date = Date()) async -> Bool {
        guard HKHealthStore.isHealthDataAvailable(),
              let type = HKQuantityType.quantityType(forIdentifier: .bodyMass)
        else { return false }
        let syncId = "omnigart.weight.\(localId)"
        if await exists(syncId: syncId, type: type) { return true }
        do {
            try await store.requestAuthorization(toShare: [type], read: [type])
            let quantity = HKQuantity(unit: .gramUnit(with: .kilo), doubleValue: massKg)
            let metadata: [String: Any] = [
                HKMetadataKeySyncIdentifier: syncId,
                HKMetadataKeySyncVersion: 1
            ]
            let sample = HKQuantitySample(
                type: type,
                quantity: quantity,
                start: date,
                end: date,
                metadata: metadata
            )
            try await store.save(sample)
            return true
        } catch {
            return false
        }
    }

    private static func exists(syncId: String, type: HKQuantityType) async -> Bool {
        await withCheckedContinuation { continuation in
            let predicate = HKQuery.predicateForObjects(
                withMetadataKey: HKMetadataKeySyncIdentifier,
                allowedValues: [syncId]
            )
            let query = HKSampleQuery(
                sampleType: type,
                predicate: predicate,
                limit: 1,
                sortDescriptors: nil
            ) { _, samples, _ in
                continuation.resume(returning: !(samples ?? []).isEmpty)
            }
            store.execute(query)
        }
    }
}
