import Foundation
import HealthKit

/// Saves completed OmniGart sessions into Apple Health via `HKWorkoutBuilder`
/// so they appear in the Fitness app with duration, energy, heart rate, and events.
///
/// **Append-only:** never deletes or rewrites Health samples (including OmniGart’s own).
/// Each workout is written once; a matching `HKMetadataKeySyncIdentifier` skips the save.
enum HealthKitWorkoutWriter {
    static func activeEnergyType() -> HKQuantityType? {
        HKQuantityType.quantityType(forIdentifier: .activeEnergyBurned)
    }

    static func basalEnergyType() -> HKQuantityType? {
        HKQuantityType.quantityType(forIdentifier: .basalEnergyBurned)
    }

    static func distanceWalkingRunningType() -> HKQuantityType? {
        HKQuantityType.quantityType(forIdentifier: .distanceWalkingRunning)
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

    /// Read + write types needed for workouts, energy, HR, distance, recovery, effort, mindful.
    static func authorizationTypes() -> (read: Set<HKObjectType>, write: Set<HKSampleType>) {
        var read: Set<HKObjectType> = [HealthKitPlatformBridge.workoutType()]
        var write: Set<HKSampleType> = [HealthKitPlatformBridge.workoutType()]
        for type in [
            activeEnergyType(),
            basalEnergyType(),
            HealthKitPlatformBridge.heartRateType(),
            distanceWalkingRunningType(),
            heartRateRecoveryType(),
            workoutEffortType()
        ] {
            if let type {
                read.insert(type)
                write.insert(type)
            }
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
        guard options.end >= options.start, !options.syncIdentifier.isEmpty else { return nil }
        try await requestWorkoutAuthorization()
        if let existing = try await existingWorkoutUUID(syncIdentifier: options.syncIdentifier) {
            return makeResult(options: options, workoutUUID: existing, skipped: true)
        }
        return try await finishNewWorkout(options)
    }

    private static func finishNewWorkout(_ options: SaveOptions) async throws -> Result? {
        let store = HKHealthStore()
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
        if !samples.isEmpty { try await builder.addSamples(samples) }
        let events = buildEvents(for: options)
        if !events.isEmpty { try await builder.addWorkoutEvents(events) }
        try await builder.addMetadata(buildMetadata(for: options))
        try await builder.endCollection(at: options.end)
        guard let workout = try await builder.finishWorkout() else { return nil }
        return makeResult(options: options, workoutUUID: workout.uuid, skipped: false)
    }

    private static func makeResult(
        options: SaveOptions,
        workoutUUID: UUID,
        skipped: Bool
    ) -> Result {
        let bpms = options.heartRate.map(\.bpm).filter { $0 > 0 }
        return Result(
            workoutUUID: workoutUUID,
            activeKcal: options.energy.activeKcal,
            basalKcal: options.energy.basalKcal,
            averageHeartRateBpm: bpms.isEmpty ? nil : bpms.reduce(0, +) / Double(bpms.count),
            maxHeartRateBpm: bpms.max(),
            energyIsEstimate: options.energy.isEstimate,
            effortScore: options.estimatedEffortScore,
            heartRateRecoveryBpm: options.heartRateRecoveryBpm,
            skippedBecauseAlreadyExists: skipped
        )
    }
}
