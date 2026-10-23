import Foundation
import HealthKit
import Shared

/// Append-only dietary nutrition writes to HealthKit.
/// Exist-check by `HKMetadataKeySyncIdentifier`; never delete or rewrite.
enum FoodNutritionHealthWriter {
    private static let store = HKHealthStore()

    struct Result: Equatable {
        let syncIdentifier: String
        let skippedAlreadyPresent: Bool
        let ok: Bool
        let errorMessage: String?
    }

    static func appendDietary(
        syncIdentifier: String,
        syncVersion: Int64,
        displayName: String,
        energyKcal: Double?,
        proteinG: Double?,
        carbsG: Double?,
        fatG: Double?,
        date: Date
    ) async -> Result {
        guard HKHealthStore.isHealthDataAvailable() else {
            return Result(
                syncIdentifier: syncIdentifier,
                skippedAlreadyPresent: false,
                ok: false,
                errorMessage: "Health data unavailable"
            )
        }
        if await syncIdentifierExists(syncIdentifier) {
            return Result(
                syncIdentifier: syncIdentifier,
                skippedAlreadyPresent: true,
                ok: true,
                errorMessage: nil
            )
        }
        var samples: [HKQuantitySample] = []
        let meta: [String: Any] = [
            HKMetadataKeySyncIdentifier: syncIdentifier,
            HKMetadataKeySyncVersion: NSNumber(value: syncVersion),
            "omnigart.origin": "omnigart.food",
            "omnigart.foodName": displayName
        ]
        func add(_ typeId: HKQuantityTypeIdentifier, unit: HKUnit, value: Double?) {
            guard let value, value > 0,
                  let type = HKQuantityType.quantityType(forIdentifier: typeId) else { return }
            samples.append(
                HKQuantitySample(
                    type: type,
                    quantity: HKQuantity(unit: unit, doubleValue: value),
                    start: date,
                    end: date,
                    metadata: meta
                )
            )
        }
        add(.dietaryEnergyConsumed, unit: .kilocalorie(), value: energyKcal)
        add(.dietaryProtein, unit: .gram(), value: proteinG)
        add(.dietaryCarbohydrates, unit: .gram(), value: carbsG)
        add(.dietaryFatTotal, unit: .gram(), value: fatG)
        guard !samples.isEmpty else {
            return Result(
                syncIdentifier: syncIdentifier,
                skippedAlreadyPresent: false,
                ok: true,
                errorMessage: nil
            )
        }
        do {
            try await store.save(samples)
            return Result(
                syncIdentifier: syncIdentifier,
                skippedAlreadyPresent: false,
                ok: true,
                errorMessage: nil
            )
        } catch {
            return Result(
                syncIdentifier: syncIdentifier,
                skippedAlreadyPresent: false,
                ok: false,
                errorMessage: error.localizedDescription
            )
        }
    }

    private static func syncIdentifierExists(_ syncId: String) async -> Bool {
        guard let energy = HKQuantityType.quantityType(forIdentifier: .dietaryEnergyConsumed) else {
            return false
        }
        let predicate = HKQuery.predicateForObjects(
            withMetadataKey: HKMetadataKeySyncIdentifier,
            allowedValues: [syncId]
        )
        return await withCheckedContinuation { cont in
            let query = HKSampleQuery(
                sampleType: energy,
                predicate: predicate,
                limit: 1,
                sortDescriptors: nil
            ) { _, samples, _ in
                cont.resume(returning: !(samples ?? []).isEmpty)
            }
            store.execute(query)
        }
    }
}
