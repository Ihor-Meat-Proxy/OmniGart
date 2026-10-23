import Foundation
import HealthKit

/// HealthKit authorisation, observer queries, background delivery, and typed writes.
/// Kotlin `HealthKitClientImpl` talks to a `HealthKitPlatformApi` registered from app startup
/// (Swift adapter) — this file owns the HKStore side.
///
/// Simulator note: many quantity types accept saves via `HealthKitSampleInjector`;
/// background delivery is limited on Simulator — verify on device.
final class HealthKitPlatformBridge: NSObject {
    static let shared = HealthKitPlatformBridge()

    private let store = HKHealthStore()
    private var observers: [String: HKObserverQuery] = [:]

    var isHealthDataAvailable: Bool { HKHealthStore.isHealthDataAvailable() }

    func requestAuthorization(
        readTypes: Set<HKObjectType>,
        writeTypes: Set<HKSampleType>
    ) async throws {
        try await store.requestAuthorization(toShare: writeTypes, read: readTypes)
    }

    func enableBackgroundDelivery(
        for type: HKObjectType,
        frequency: HKUpdateFrequency = .immediate
    ) async throws {
        try await store.enableBackgroundDelivery(for: type, frequency: frequency)
    }

    func startObserver(id: String, type: HKSampleType, handler: @escaping () -> Void) {
        let query = HKObserverQuery(sampleType: type, predicate: nil) { _, completionHandler, error in
            if error == nil {
                handler()
            }
            completionHandler()
        }
        observers[id] = query
        store.execute(query)
    }

    func stopObserver(id: String) {
        if let query = observers.removeValue(forKey: id) {
            store.stop(query)
        }
    }

    static func heartRateType() -> HKQuantityType? {
        HKQuantityType.quantityType(forIdentifier: .heartRate)
    }

    static func restingHeartRateType() -> HKQuantityType? {
        HKQuantityType.quantityType(forIdentifier: .restingHeartRate)
    }

    static func hrvType() -> HKQuantityType? {
        HKQuantityType.quantityType(forIdentifier: .heartRateVariabilitySDNN)
    }

    static func workoutType() -> HKSampleType { HKObjectType.workoutType() }

    static func sleepType() -> HKCategoryType? {
        HKObjectType.categoryType(forIdentifier: .sleepAnalysis)
    }

    static func dietaryWaterType() -> HKQuantityType? {
        HKQuantityType.quantityType(forIdentifier: .dietaryWater)
    }
}

/// Sample injection helpers for XCTest / Simulator validation of HealthKit write paths.
enum HealthKitSampleInjector {
    static func injectHeartRate(
        bpm: Double,
        date: Date = Date(),
        store: HKHealthStore = HKHealthStore()
    ) async throws {
        guard let type = HealthKitPlatformBridge.heartRateType() else { return }
        let quantity = HKQuantity(
            unit: HKUnit.count().unitDivided(by: .minute()),
            doubleValue: bpm
        )
        let sample = HKQuantitySample(type: type, quantity: quantity, start: date, end: date)
        try await store.save(sample)
    }

    static func injectRestingHeartRate(
        bpm: Double,
        date: Date = Date(),
        store: HKHealthStore = HKHealthStore()
    ) async throws {
        guard let type = HealthKitPlatformBridge.restingHeartRateType() else { return }
        let quantity = HKQuantity(
            unit: HKUnit.count().unitDivided(by: .minute()),
            doubleValue: bpm
        )
        let sample = HKQuantitySample(type: type, quantity: quantity, start: date, end: date)
        try await store.save(sample)
    }

    static func injectDietaryWater(
        ml: Double,
        date: Date = Date(),
        store: HKHealthStore = HKHealthStore()
    ) async throws {
        guard let type = HealthKitPlatformBridge.dietaryWaterType() else { return }
        let quantity = HKQuantity(unit: .literUnit(with: .milli), doubleValue: ml)
        let sample = HKQuantitySample(type: type, quantity: quantity, start: date, end: date)
        try await store.save(sample)
    }
}
