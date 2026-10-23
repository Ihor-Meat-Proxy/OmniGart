import Foundation
import HealthKit

/// Saves completed OmniGart sessions into Apple Health via `HKWorkoutBuilder`
/// so they appear in the Fitness app with duration, energy, heart rate, and events.
enum HealthKitWorkoutWriter {
    static func activeEnergyType() -> HKQuantityType? {
        HKQuantityType.quantityType(forIdentifier: .activeEnergyBurned)
    }

    static func basalEnergyType() -> HKQuantityType? {
        HKQuantityType.quantityType(forIdentifier: .basalEnergyBurned)
    }

    static func mindfulType() -> HKCategoryType? {
        HKObjectType.categoryType(forIdentifier: .mindfulSession)
    }

    /// Present on newer OS builds; raw identifier keeps older SDKs compiling.
    static func heartRateRecoveryType() -> HKQuantityType? {
        HKQuantityType.quantityType(
            forIdentifier: HKQuantityTypeIdentifier(
                rawValue: "HKQuantityTypeIdentifierHeartRateRecoveryOneMinute"
            )
        )
    }

    static func workoutEffortType() -> HKQuantityType? {
        HKQuantityType.quantityType(
            forIdentifier: HKQuantityTypeIdentifier(
                rawValue: "HKQuantityTypeIdentifierEstimatedWorkoutEffortScore"
            )
        )
    }

    /// Read + write types needed for workouts, energy, HR, recovery, effort, mindful.
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
        if let recovery = heartRateRecoveryType() {
            read.insert(recovery)
            write.insert(recovery)
        }
        if let effort = workoutEffortType() {
            read.insert(effort)
            write.insert(effort)
        }
        if let mindful = mindfulType() {
            read.insert(mindful)
            write.insert(mindful)
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

    @discardableResult
    static func save(options: SaveOptions) async throws -> Result? {
        try await persist(options)
    }

    @discardableResult
    static func saveStrengthWorkout(options: SaveOptions) async throws -> Result? {
        var copy = options
        copy.activityType = .traditionalStrengthTraining
        copy.indoor = true
        return try await persist(copy)
    }

    @discardableResult
    static func saveWorkout(options: SaveOptions) async throws -> Result? {
        try await persist(options)
    }

    private static func persist(_ options: SaveOptions) async throws -> Result? {
        guard HKHealthStore.isHealthDataAvailable() else { return nil }
        guard options.end >= options.start else { return nil }

        let store = HKHealthStore()
        try await requestWorkoutAuthorization()

        let configuration = HKWorkoutConfiguration()
        configuration.activityType = options.activityType
        configuration.locationType = options.indoor ? .indoor : .outdoor

        let builder = HKWorkoutBuilder(
            healthStore: store,
            configuration: configuration,
            device: .local()
        )
        try await builder.beginCollection(at: options.start)

        let samples = buildSamples(for: options)
        if !samples.isEmpty {
            try await builder.addSamples(samples)
        }

        let events = buildEvents(for: options)
        if !events.isEmpty {
            try await builder.addWorkoutEvents(events)
        }

        try await builder.addMetadata(buildMetadata(for: options))
        try await builder.endCollection(at: options.end)
        guard let workout = try await builder.finishWorkout() else { return nil }

        let bpms = options.heartRate.map(\.bpm).filter { $0 > 0 }
        return Result(
            workoutUUID: workout.uuid,
            activeKcal: options.energy.activeKcal,
            basalKcal: options.energy.basalKcal,
            averageHeartRateBpm: bpms.isEmpty ? nil : bpms.reduce(0, +) / Double(bpms.count),
            maxHeartRateBpm: bpms.max(),
            energyIsEstimate: options.energy.isEstimate,
            effortScore: options.estimatedEffortScore,
            heartRateRecoveryBpm: options.heartRateRecoveryBpm
        )
    }
}
