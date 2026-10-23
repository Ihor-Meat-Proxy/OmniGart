import Foundation
import HealthKit

/// Saves completed OmniGart sessions into Apple Health via `HKWorkoutBuilder`
/// so they appear in the Fitness app with duration, energy, heart rate, and events.
enum HealthKitWorkoutWriter {
    struct EnergyInput: Sendable {
        var activeKcal: Double
        var basalKcal: Double
        /// When true, metadata marks energy as a wellness estimate (iPhone-only path).
        var isEstimate: Bool = false
    }

    struct HeartRatePoint: Sendable {
        var date: Date
        var bpm: Double
    }

    struct Segment: Sendable {
        var start: Date
        var end: Date
        var label: String
    }

    struct StrengthMetadata: Sendable {
        var totalSets: Int
        var totalReps: Int
        var totalWeightKg: Double
    }

    struct SaveOptions: Sendable {
        var activityType: HKWorkoutActivityType
        var start: Date
        var end: Date
        var energy: EnergyInput
        var heartRate: [HeartRatePoint] = []
        var pauseResumeDates: [(pause: Date, resume: Date?)] = []
        var segments: [Segment] = []
        var strength: StrengthMetadata? = nil
        var indoor: Bool = true
        var saveMindfulMinutes: Bool = false
        var estimatedEffortScore: Double? = nil
        var heartRateRecoveryBpm: Double? = nil
        /// Walking/running distance (metres). Prefer treadmill belt distance when linked.
        var distanceMetres: Double? = nil
        /// Stable OmniGart sync id (`HKMetadataKeySyncIdentifier`).
        /// If a workout with this id already exists in Apple Health, the write is skipped.
        var syncIdentifier: String? = nil
        /// `HKMetadataKeySyncVersion`: 1 on first insert; bump only for user-initiated restore
        /// after platform deletion (never used to overwrite an existing object).
        var syncVersion: Int = 1
    }

    struct Result: Sendable {
        var workoutUUID: UUID
        var activeKcal: Double
        var basalKcal: Double
        var averageHeartRateBpm: Double?
        var maxHeartRateBpm: Double?
        var energyIsEstimate: Bool
        var effortScore: Double?
        var heartRateRecoveryBpm: Double?
    }

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
    static func saveStrengthWorkout(
        start: Date,
        end: Date,
        energy: EnergyInput,
        heartRate: [HeartRatePoint] = [],
        strength: StrengthMetadata? = nil
    ) async throws -> Result? {
        try await save(
            SaveOptions(
                activityType: .traditionalStrengthTraining,
                start: start,
                end: end,
                energy: energy,
                heartRate: heartRate,
                strength: strength,
                indoor: true
            )
        )
    }

    @discardableResult
    static func save(options: SaveOptions) async throws -> Result? {
        try await save(options)
    }

    @discardableResult
    static func saveWorkout(
        activityType: HKWorkoutActivityType,
        start: Date,
        end: Date,
        energy: EnergyInput,
        heartRate: [HeartRatePoint] = []
    ) async throws -> Result? {
        try await save(
            SaveOptions(
                activityType: activityType,
                start: start,
                end: end,
                energy: energy,
                heartRate: heartRate
            )
        )
    }

    /// True when Apple Health already has a workout with this `HKMetadataKeySyncIdentifier`.
    /// Append-only rule: never delete or save-over; skip the write instead.
    static func workoutExists(syncIdentifier: String) async throws -> Bool {
        guard HKHealthStore.isHealthDataAvailable(), !syncIdentifier.isEmpty else { return false }
        let store = HKHealthStore()
        let workoutType = HealthKitPlatformBridge.workoutType()
        let predicate = HKQuery.predicateForObjects(
            withMetadataKey: HKMetadataKeySyncIdentifier,
            allowedValues: [syncIdentifier]
        )
        return try await withCheckedThrowingContinuation { continuation in
            let query = HKSampleQuery(
                sampleType: workoutType,
                predicate: predicate,
                limit: 1,
                sortDescriptors: nil
            ) { _, samples, error in
                if let error {
                    continuation.resume(throwing: error)
                    return
                }
                continuation.resume(returning: !(samples ?? []).isEmpty)
            }
            store.execute(query)
        }
    }

    private static func save(_ options: SaveOptions) async throws -> Result? {
        guard HKHealthStore.isHealthDataAvailable() else { return nil }
        guard options.end >= options.start else { return nil }

        if let syncId = options.syncIdentifier, !syncId.isEmpty {
            if try await workoutExists(syncIdentifier: syncId) {
                // Already on Apple Health — never rewrite or delete.
                return nil
            }
        }

        let store = HKHealthStore()
        try await requestWorkoutAuthorization()

        let configuration = HKWorkoutConfiguration()
        configuration.activityType = options.activityType
        configuration.locationType = options.indoor ? .indoor : .outdoor

        let builder = HKWorkoutBuilder(healthStore: store, configuration: configuration, device: .local())
        try await builder.beginCollection(at: options.start)

        var samples: [HKSample] = []
        let kcalUnit = HKUnit.kilocalorie()

        if options.energy.activeKcal > 0, let activeType = activeEnergyType() {
            let quantity = HKQuantity(unit: kcalUnit, doubleValue: options.energy.activeKcal)
            samples.append(
                HKQuantitySample(
                    type: activeType,
                    quantity: quantity,
                    start: options.start,
                    end: options.end
                )
            )
        }
        if options.energy.basalKcal > 0, let basalType = basalEnergyType() {
            let quantity = HKQuantity(unit: kcalUnit, doubleValue: options.energy.basalKcal)
            samples.append(
                HKQuantitySample(
                    type: basalType,
                    quantity: quantity,
                    start: options.start,
                    end: options.end
                )
            )
        }

        if let metres = options.distanceMetres, metres > 0,
           let distanceType = HKQuantityType.quantityType(forIdentifier: .distanceWalkingRunning) {
            let quantity = HKQuantity(unit: .meter(), doubleValue: metres)
            samples.append(
                HKQuantitySample(
                    type: distanceType,
                    quantity: quantity,
                    start: options.start,
                    end: options.end
                )
            )
        }

        if let heartType = HealthKitPlatformBridge.heartRateType() {
            let hrUnit = HKUnit.count().unitDivided(by: .minute())
            for point in options.heartRate where point.bpm > 0 {
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

        if let recoveryBpm = options.heartRateRecoveryBpm, recoveryBpm > 0,
           let recoveryType = heartRateRecoveryType() {
            let quantity = HKQuantity(
                unit: HKUnit.count().unitDivided(by: .minute()),
                doubleValue: recoveryBpm
            )
            samples.append(
                HKQuantitySample(
                    type: recoveryType,
                    quantity: quantity,
                    start: options.end,
                    end: options.end.addingTimeInterval(60)
                )
            )
        }

        if let effort = options.estimatedEffortScore, effort > 0, let effortType = workoutEffortType() {
            let quantity = HKQuantity(unit: .count(), doubleValue: min(10, max(0, effort)))
            samples.append(
                HKQuantitySample(
                    type: effortType,
                    quantity: quantity,
                    start: options.start,
                    end: options.end
                )
            )
        }

        if options.saveMindfulMinutes, let mindful = mindfulType() {
            let sample = HKCategorySample(
                type: mindful,
                value: 0,
                start: options.start,
                end: options.end
            )
            samples.append(sample)
        }

        if !samples.isEmpty {
            try await builder.addSamples(samples)
        }

        var events: [HKWorkoutEvent] = []
        for pair in options.pauseResumeDates {
            events.append(
                HKWorkoutEvent(type: .pause, dateInterval: DateInterval(start: pair.pause, duration: 0))
            )
            if let resume = pair.resume {
                events.append(
                    HKWorkoutEvent(type: .resume, dateInterval: DateInterval(start: resume, duration: 0))
                )
            }
        }
        for segment in options.segments {
            let interval = DateInterval(start: segment.start, end: max(segment.end, segment.start))
            events.append(
                HKWorkoutEvent(
                    type: .segment,
                    dateInterval: interval,
                    metadata: ["OmniGartSegmentLabel": segment.label]
                )
            )
        }
        if !events.isEmpty {
            try await builder.addWorkoutEvents(events)
        }

        var metadata: [String: Any] = [
            HKMetadataKeyIndoorWorkout: options.indoor
        ]
        if let syncId = options.syncIdentifier, !syncId.isEmpty {
            metadata[HKMetadataKeySyncIdentifier] = syncId
            metadata[HKMetadataKeySyncVersion] = options.syncVersion
            metadata["omnigart.origin"] = "omnigart"
            metadata["omnigart.healthSchemaVersion"] = 1
        }
        if options.energy.isEstimate {
            metadata["OmniGartEnergyIsEstimate"] = true
        }
        if let strength = options.strength {
            metadata["OmniGartTotalSets"] = strength.totalSets
            metadata["OmniGartTotalReps"] = strength.totalReps
            metadata["OmniGartTotalWeightKg"] = strength.totalWeightKg
        }
        try await builder.addMetadata(metadata)

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

    /// Heart-rate samples already in Health for the session window (when readable).
    static func fetchHeartRate(from start: Date, to end: Date) async throws -> [HeartRatePoint] {
        guard let heartType = HealthKitPlatformBridge.heartRateType() else { return [] }
        let store = HKHealthStore()
        let predicate = HKQuery.predicateForSamples(
            withStart: start,
            end: end,
            options: .strictStartDate
        )
        return try await withCheckedThrowingContinuation { continuation in
            let query = HKSampleQuery(
                sampleType: heartType,
                predicate: predicate,
                limit: HKObjectQueryNoLimit,
                sortDescriptors: [
                    NSSortDescriptor(key: HKSampleSortIdentifierStartDate, ascending: true)
                ]
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

    /// Rough 1-minute HR recovery: peak near end minus HR ~60s after end (when samples exist).
    static func estimateHeartRateRecoveryBpm(samples: [HeartRatePoint], workoutEnd: Date) -> Double? {
        let nearEnd = samples.filter {
            $0.date >= workoutEnd.addingTimeInterval(-90) && $0.date <= workoutEnd
        }
        let after = samples.filter {
            $0.date > workoutEnd && $0.date <= workoutEnd.addingTimeInterval(75)
        }
        guard let peak = nearEnd.map(\.bpm).max(), let recovered = after.last?.bpm else { return nil }
        let delta = peak - recovered
        return delta > 0 ? delta : nil
    }

    /// Wellness effort score 0–10 from average HR / estimated max (Tanaka-style 208 − 0.7·age).
    static func estimateEffortScore(averageBpm: Double?, ageYears: Int) -> Double? {
        guard let averageBpm, averageBpm > 0, ageYears > 0 else { return nil }
        let hrMax = 208.0 - 0.7 * Double(ageYears)
        guard hrMax > 0 else { return nil }
        return min(10, max(0, (averageBpm / hrMax) * 10.0))
    }
}
