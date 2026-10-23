import Foundation
import HealthKit
import Shared

/// HealthKit read helpers for the Health metrics tab.
enum HealthMetricsQueries {
    typealias QuantitySum = HealthMetricsCards.QuantitySum

    static func sumQuantity(
        _ id: HKQuantityTypeIdentifier,
        unit: HKUnit,
        from: Date,
        to: Date
    ) async -> QuantitySum {
        guard let type = HKQuantityType.quantityType(forIdentifier: id) else {
            return QuantitySum()
        }
        let store = HKHealthStore()
        let predicate = HKQuery.predicateForSamples(withStart: from, end: to)
        return await withCheckedContinuation { cont in
            let query = HKStatisticsQuery(
                quantityType: type,
                quantitySamplePredicate: predicate,
                options: .cumulativeSum
            ) { _, stats, _ in
                let value = stats?.sumQuantity()?.doubleValue(for: unit)
                let source = stats?.sources?.first?.name
                cont.resume(returning: QuantitySum(value: value, source: source))
            }
            store.execute(query)
        }
    }

    static func cumulativeQuantity(
        _ id: HKQuantityTypeIdentifier,
        unit: HKUnit,
        from: Date,
        to: Date
    ) async -> QuantitySum {
        guard let type = HKQuantityType.quantityType(forIdentifier: id) else {
            return QuantitySum()
        }
        let store = HKHealthStore()
        let predicate = HKQuery.predicateForSamples(withStart: from, end: to)
        return await withCheckedContinuation { cont in
            let query = HKStatisticsCollectionQuery(
                quantityType: type,
                quantitySamplePredicate: predicate,
                options: .cumulativeSum,
                anchorDate: from,
                intervalComponents: DateComponents(hour: 1)
            )
            query.initialResultsHandler = { _, collection, _ in
                var sum = QuantitySum()
                collection?.enumerateStatistics(from: from, to: to) { stats, _ in
                    let hour = Calendar.current.component(.hour, from: stats.startDate)
                    let amount = stats.sumQuantity()?.doubleValue(for: unit) ?? 0
                    if hour >= 0 && hour < 24 {
                        sum.byHour[hour] += amount
                    }
                    if sum.source == nil {
                        sum.source = stats.sources?.first?.name
                    }
                }
                sum.value = sum.total
                cont.resume(returning: sum)
            }
            store.execute(query)
        }
    }

    static func latestQuantity(
        _ id: HKQuantityTypeIdentifier,
        unit: HKUnit
    ) async -> QuantitySum {
        guard let type = HKQuantityType.quantityType(forIdentifier: id) else {
            return QuantitySum()
        }
        let store = HKHealthStore()
        let sort = NSSortDescriptor(key: HKSampleSortIdentifierEndDate, ascending: false)
        return await withCheckedContinuation { cont in
            let query = HKSampleQuery(
                sampleType: type,
                predicate: nil,
                limit: 1,
                sortDescriptors: [sort]
            ) { _, samples, _ in
                guard let sample = samples?.first as? HKQuantitySample else {
                    cont.resume(returning: QuantitySum())
                    return
                }
                cont.resume(
                    returning: QuantitySum(
                        value: sample.quantity.doubleValue(for: unit),
                        source: sample.sourceRevision.source.name
                    )
                )
            }
            store.execute(query)
        }
    }

    static func weekSeries(
        _ id: HKQuantityTypeIdentifier,
        unit: HKUnit
    ) async -> (points: [HealthSamplePoint], source: String?) {
        guard let type = HKQuantityType.quantityType(forIdentifier: id) else {
            return ([], nil)
        }
        let store = HKHealthStore()
        let end = Date()
        let start = Calendar.current.date(
            byAdding: .day,
            value: -6,
            to: Calendar.current.startOfDay(for: end)
        ) ?? end
        let predicate = HKQuery.predicateForSamples(withStart: start, end: end)
        let sort = NSSortDescriptor(key: HKSampleSortIdentifierEndDate, ascending: true)
        return await withCheckedContinuation { cont in
            let query = HKSampleQuery(
                sampleType: type,
                predicate: predicate,
                limit: HKObjectQueryNoLimit,
                sortDescriptors: [sort]
            ) { _, samples, _ in
                let points: [HealthSamplePoint] = (samples as? [HKQuantitySample] ?? [])
                    .enumerated()
                    .map { idx, sample in
                        HealthSamplePoint(
                            dayOffset: Int32(idx),
                            value: sample.quantity.doubleValue(for: unit),
                            hourFraction: nil
                        )
                    }
                let source = (samples?.first as? HKQuantitySample)?.sourceRevision.source.name
                cont.resume(returning: (points, source))
            }
            store.execute(query)
        }
    }

    static func sleepDonut(from: Date, to: Date) async -> SleepDonutModel {
        guard let sleepType = HKObjectType.categoryType(forIdentifier: .sleepAnalysis) else {
            return SleepDonutModel(
                asleepMinutes: 0,
                awakeMinutes: 0,
                startLabel: nil,
                endLabel: nil,
                sourceName: nil,
                needsPermission: true
            )
        }
        let store = HKHealthStore()
        let predicate = HKQuery.predicateForSamples(withStart: from, end: to)
        return await withCheckedContinuation { cont in
            let query = HKSampleQuery(
                sampleType: sleepType,
                predicate: predicate,
                limit: HKObjectQueryNoLimit,
                sortDescriptors: nil
            ) { _, samples, _ in
                cont.resume(returning: summarizeSleep(samples as? [HKCategorySample] ?? []))
            }
            store.execute(query)
        }
    }

    static func mindfulMinutes(from: Date, to: Date) async -> QuantitySum {
        guard let type = HKObjectType.categoryType(forIdentifier: .mindfulSession) else {
            return QuantitySum()
        }
        let store = HKHealthStore()
        let predicate = HKQuery.predicateForSamples(withStart: from, end: to)
        return await withCheckedContinuation { cont in
            let query = HKSampleQuery(
                sampleType: type,
                predicate: predicate,
                limit: HKObjectQueryNoLimit,
                sortDescriptors: nil
            ) { _, samples, _ in
                let minutes = (samples as? [HKCategorySample] ?? []).reduce(0.0) {
                    $0 + $1.endDate.timeIntervalSince($1.startDate) / 60.0
                }
                let source = (samples?.first as? HKCategorySample)?.sourceRevision.source.name
                cont.resume(
                    returning: QuantitySum(
                        value: minutes > 0 ? minutes : nil,
                        source: source
                    )
                )
            }
            store.execute(query)
        }
    }

    private static func summarizeSleep(_ samples: [HKCategorySample]) -> SleepDonutModel {
        var asleep: Double = 0
        var awake: Double = 0
        var start: Date?
        var end: Date?
        var source: String?
        for sample in samples {
            let minutes = sample.endDate.timeIntervalSince(sample.startDate) / 60.0
            if sample.value == HKCategoryValueSleepAnalysis.awake.rawValue {
                awake += minutes
            } else {
                asleep += minutes
            }
            if let existingStart = start {
                if sample.startDate < existingStart { start = sample.startDate }
            } else {
                start = sample.startDate
            }
            if let existingEnd = end {
                if sample.endDate > existingEnd { end = sample.endDate }
            } else {
                end = sample.endDate
            }
            source = sample.sourceRevision.source.name
        }
        let fmt = DateFormatter()
        fmt.timeStyle = .short
        return SleepDonutModel(
            asleepMinutes: asleep,
            awakeMinutes: awake,
            startLabel: start.map { fmt.string(from: $0) },
            endLabel: end.map { fmt.string(from: $0) },
            sourceName: source,
            needsPermission: false
        )
    }
}
