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

    /// Macros for one dietary append (keeps call-site parameter count low).
    struct DietaryMacros {
        var energyKcal: Double?
        var proteinG: Double?
        var carbsG: Double?
        var fatG: Double?
    }

    static func appendDietary(
        syncIdentifier: String,
        syncVersion: Int64,
        displayName: String,
        macros: DietaryMacros,
        date: Date
    ) async -> Result {
        guard HKHealthStore.isHealthDataAvailable() else {
            return failure(syncIdentifier, "Health data unavailable")
        }
        if await syncIdentifierExists(syncIdentifier) {
            return Result(
                syncIdentifier: syncIdentifier,
                skippedAlreadyPresent: true,
                ok: true,
                errorMessage: nil
            )
        }
        let samples = DietarySampleFactory.make(
            DietarySampleFactory.Context(
                macros: macros,
                displayName: displayName,
                syncIdentifier: syncIdentifier,
                syncVersion: syncVersion,
                date: date
            )
        )
        guard !samples.isEmpty else {
            return success(syncIdentifier, skipped: false)
        }
        do {
            try await store.save(samples)
            return success(syncIdentifier, skipped: false)
        } catch {
            return failure(syncIdentifier, error.localizedDescription)
        }
    }

    private static func success(_ syncIdentifier: String, skipped: Bool) -> Result {
        Result(
            syncIdentifier: syncIdentifier,
            skippedAlreadyPresent: skipped,
            ok: true,
            errorMessage: nil
        )
    }

    private static func failure(_ syncIdentifier: String, _ message: String) -> Result {
        Result(
            syncIdentifier: syncIdentifier,
            skippedAlreadyPresent: false,
            ok: false,
            errorMessage: message
        )
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

private enum DietarySampleFactory {
    struct Context {
        let macros: FoodNutritionHealthWriter.DietaryMacros
        let displayName: String
        let syncIdentifier: String
        let syncVersion: Int64
        let date: Date
    }

    private struct Spec {
        let typeId: HKQuantityTypeIdentifier
        let unit: HKUnit
        let value: Double?
    }

    static func make(_ ctx: Context) -> [HKQuantitySample] {
        let meta: [String: Any] = [
            HKMetadataKeySyncIdentifier: ctx.syncIdentifier,
            HKMetadataKeySyncVersion: NSNumber(value: ctx.syncVersion),
            "omnigart.origin": "omnigart.food",
            "omnigart.foodName": ctx.displayName
        ]
        let specs = [
            Spec(typeId: .dietaryEnergyConsumed, unit: .kilocalorie(), value: ctx.macros.energyKcal),
            Spec(typeId: .dietaryProtein, unit: .gram(), value: ctx.macros.proteinG),
            Spec(typeId: .dietaryCarbohydrates, unit: .gram(), value: ctx.macros.carbsG),
            Spec(typeId: .dietaryFatTotal, unit: .gram(), value: ctx.macros.fatG)
        ]
        return specs.compactMap { sample(spec: $0, date: ctx.date, meta: meta) }
    }

    private static func sample(
        spec: Spec,
        date: Date,
        meta: [String: Any]
    ) -> HKQuantitySample? {
        guard let value = spec.value, value > 0,
              let type = HKQuantityType.quantityType(forIdentifier: spec.typeId) else { return nil }
        return HKQuantitySample(
            type: type,
            quantity: HKQuantity(unit: spec.unit, doubleValue: value),
            start: date,
            end: date,
            metadata: meta
        )
    }
}
