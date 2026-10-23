import Foundation
import HealthKit

/// Saves completed OmniGart sessions into Apple Health via `HKWorkoutBuilder`
/// so they appear in the Fitness app with duration, energy, and heart rate.
enum HealthKitWorkoutWriter {
    struct EnergyInput: Sendable {
        var activeKcal: Double
        var basalKcal: Double
    }

    struct HeartRatePoint: Sendable {
        var date: Date
        var bpm: Double
    }

    struct Result: Sendable {
        var workoutUUID: UUID
        var activeKcal: Double
        var basalKcal: Double
        var averageHeartRateBpm: Double?
        var maxHeartRateBpm: Double?
    }

    static func activeEnergyType() -> HKQuantityType? {
        HKQuantityType.quantityType(forIdentifier: .activeEnergyBurned)
    }

    static func basalEnergyType() -> HKQuantityType? {
        HKQuantityType.quantityType(forIdentifier: .basalEnergyBurned)
    }

    /// Read + write types needed to persist workouts with energy and HR for Fitness.
    static func authorizationTypes() -> (read: Set<HKObjectType>, write: Set<HKSampleType>) {
        var read: Set<HKObjectType> = [HealthKitPlatformBridge.workoutType()]
        var write: Set<HKSampleType> = [HealthKitPlatformBridge.workoutType()]
        if let active = activeEnergyType() {
            read.insert(active)
            write.insert(active)
        }
        if let basal = basalEnergyType() {
            read.insert(basal)
            write.insert(basal)
        }
        if let heart = HealthKitPlatformBridge.heartRateType() {
            read.insert(heart)
            write.insert(heart)
        }
        return (read, write)
    }

    static func requestWorkoutAuthorization() async throws {
        let types = authorizationTypes()
        try await HealthKitPlatformBridge.shared.requestAuthorization(
            readTypes: types.read,
            writeTypes: types.write
        )
    }

    /// Persist a strength session. Uses estimated energy when measured values are unavailable.
    @discardableResult
    static func saveStrengthWorkout(
        start: Date,
        end: Date,
        energy: EnergyInput,
        heartRate: [HeartRatePoint] = []
    ) async throws -> Result? {
        try await saveWorkout(
            activityType: .traditionalStrengthTraining,
            start: start,
            end: end,
            energy: energy,
            heartRate: heartRate
        )
    }

    @discardableResult
    static func saveWorkout(
        activityType: HKWorkoutActivityType,
        start: Date,
        end: Date,
        energy: EnergyInput,
        heartRate: [HeartRatePoint] = []
    ) async throws -> Result? {
        guard HKHealthStore.isHealthDataAvailable() else { return nil }
        guard end >= start else { return nil }

        let store = HKHealthStore()
        try await requestWorkoutAuthorization()

        let configuration = HKWorkoutConfiguration()
        configuration.activityType = activityType
        configuration.locationType = .indoor

        let builder = HKWorkoutBuilder(healthStore: store, configuration: configuration, device: .local())
        try await builder.beginCollection(at: start)

        var samples: [HKSample] = []
        let kcalUnit = HKUnit.kilocalorie()

        if energy.activeKcal > 0, let activeType = activeEnergyType() {
            let quantity = HKQuantity(unit: kcalUnit, doubleValue: energy.activeKcal)
            samples.append(
                HKQuantitySample(type: activeType, quantity: quantity, start: start, end: end)
            )
        }
        if energy.basalKcal > 0, let basalType = basalEnergyType() {
            let quantity = HKQuantity(unit: kcalUnit, doubleValue: energy.basalKcal)
            samples.append(
                HKQuantitySample(type: basalType, quantity: quantity, start: start, end: end)
            )
        }

        if let heartType = HealthKitPlatformBridge.heartRateType() {
            let hrUnit = HKUnit.count().unitDivided(by: .minute())
            for point in heartRate where point.bpm > 0 {
                let quantity = HKQuantity(unit: hrUnit, doubleValue: point.bpm)
                samples.append(
                    HKQuantitySample(
                        type: heartType,
                        quantity: quantity,
                        start: point.date,
                        end: point.date
                    )
                )
            }
        }

        if !samples.isEmpty {
            try await builder.addSamples(samples)
        }

        try await builder.endCollection(at: end)
        guard let workout = try await builder.finishWorkout() else { return nil }

        let bpms = heartRate.map(\.bpm).filter { $0 > 0 }
        return Result(
            workoutUUID: workout.uuid,
            activeKcal: energy.activeKcal,
            basalKcal: energy.basalKcal,
            averageHeartRateBpm: bpms.isEmpty ? nil : bpms.reduce(0, +) / Double(bpms.count),
            maxHeartRateBpm: bpms.max()
        )
    }

    /// Heart-rate samples already in Health for the session window (when readable).
    static func fetchHeartRate(from start: Date, to end: Date) async throws -> [HeartRatePoint] {
        guard let heartType = HealthKitPlatformBridge.heartRateType() else { return [] }
        let store = HKHealthStore()
        let predicate = HKQuery.predicateForSamples(withStart: start, end: end, options: .strictStartDate)
        return try await withCheckedThrowingContinuation { continuation in
            let query = HKSampleQuery(
                sampleType: heartType,
                predicate: predicate,
                limit: HKObjectQueryNoLimit,
                sortDescriptors: [NSSortDescriptor(key: HKSampleSortIdentifierStartDate, ascending: true)]
            ) { _, samples, error in
                if let error {
                    continuation.resume(throwing: error)
                    return
                }
                let unit = HKUnit.count().unitDivided(by: .minute())
                let points: [HeartRatePoint] = (samples as? [HKQuantitySample] ?? []).compactMap { sample in
                    let bpm = sample.quantity.doubleValue(for: unit)
                    guard bpm > 0 else { return nil }
                    return HeartRatePoint(date: sample.startDate, bpm: bpm)
                }
                continuation.resume(returning: points)
            }
            store.execute(query)
        }
    }
}
