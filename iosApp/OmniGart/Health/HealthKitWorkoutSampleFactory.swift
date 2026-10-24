import Foundation
import HealthKit

extension HealthKitWorkoutWriter {
    static func buildSamples(for options: SaveOptions) -> [HKSample] {
        var samples: [HKSample] = []
        samples.append(contentsOf: energySamples(for: options))
        samples.append(contentsOf: heartRateSamples(for: options))
        samples.append(contentsOf: recoveryAndEffortSamples(for: options))
        if options.saveMindfulMinutes, let mindful = mindfulType() {
            samples.append(
                HKCategorySample(
                    type: mindful,
                    value: 0,
                    start: options.start,
                    end: options.end
                )
            )
        }
        return samples
    }

    private static func energySamples(for options: SaveOptions) -> [HKSample] {
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
        return samples
    }

    private static func heartRateSamples(for options: SaveOptions) -> [HKSample] {
        guard let heartType = HealthKitPlatformBridge.heartRateType() else { return [] }
        let hrUnit = HKUnit.count().unitDivided(by: .minute())
        return options.heartRate.compactMap { point in
            guard point.bpm > 0 else { return nil }
            let quantity = HKQuantity(unit: hrUnit, doubleValue: point.bpm)
            return HKQuantitySample(
                type: heartType,
                quantity: quantity,
                start: point.date,
                end: point.date
            )
        }
    }

    private static func recoveryAndEffortSamples(for options: SaveOptions) -> [HKSample] {
        var samples: [HKSample] = []
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
        if let effort = options.estimatedEffortScore, effort > 0,
           let effortType = workoutEffortType() {
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
        return samples
    }

    static func buildEvents(for options: SaveOptions) -> [HKWorkoutEvent] {
        var events: [HKWorkoutEvent] = []
        for pair in options.pauseResumeDates {
            events.append(
                HKWorkoutEvent(
                    type: .pause,
                    dateInterval: DateInterval(start: pair.pause, duration: 1),
                    metadata: nil
                )
            )
            if let resume = pair.resume {
                events.append(
                    HKWorkoutEvent(
                        type: .resume,
                        dateInterval: DateInterval(start: resume, duration: 1),
                        metadata: nil
                    )
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
        return events
    }

    static func buildMetadata(for options: SaveOptions) -> [String: Any] {
        var metadata: [String: Any] = [
            HKMetadataKeyIndoorWorkout: options.indoor,
            // Append-only: sync id + version 1; never bump version / never updateRecords.
            HKMetadataKeySyncIdentifier: options.syncIdentifier,
            HKMetadataKeySyncVersion: NSNumber(value: 1)
        ]
        if options.energy.isEstimate {
            metadata["OmniGartEnergyIsEstimate"] = true
        }
        if let strength = options.strength {
            metadata["OmniGartTotalSets"] = strength.totalSets
            metadata["OmniGartTotalReps"] = strength.totalReps
            metadata["OmniGartTotalWeightKg"] = strength.totalWeightKg
        }
        return metadata
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
}
