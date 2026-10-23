import Foundation
import HealthKit
import Shared

/// Loads read-only HealthKit samples into [HealthMetricsDashboard] off the main thread.
enum HealthMetricsLoader {
    struct Outcome {
        let authorized: Bool
        let dashboard: HealthMetricsDashboard
    }

    static func loadToday() async -> Outcome {
        guard HKHealthStore.isHealthDataAvailable() else {
            return Outcome(authorized: false, dashboard: HealthMetricsAssembler.shared.emptyPermissionDashboard())
        }
        let store = HKHealthStore()
        let readTypes = HealthMetricsReadTypes.allReadTypes()
        // HealthKit does not expose definitive read status; attempt queries and treat failures as empty.
        do {
            try await store.requestAuthorization(toShare: [], read: readTypes)
        } catch {
            return Outcome(authorized: false, dashboard: HealthMetricsAssembler.shared.emptyPermissionDashboard())
        }

        let cal = Calendar.current
        let now = Date()
        let startOfDay = cal.startOfDay(for: now)
        let hour = Double(cal.component(.hour, from: now)) + Double(cal.component(.minute, from: now)) / 60.0

        async let active = cumulativeQuantity(.activeEnergyBurned, unit: .kilocalorie(), from: startOfDay, to: now, byHour: true)
        async let basal = sumQuantity(.basalEnergyBurned, unit: .kilocalorie(), from: startOfDay, to: now)
        async let exercise = sumQuantity(.appleExerciseTime, unit: .minute(), from: startOfDay, to: now)
        async let steps = sumQuantity(.stepCount, unit: .count(), from: startOfDay, to: now)
        async let rhr = latestQuantity(.restingHeartRate, unit: HKUnit.count().unitDivided(by: .minute()))
        async let hrv = latestQuantity(.heartRateVariabilitySDNN, unit: .secondUnit(with: .milli))
        async let spo2 = latestQuantity(.oxygenSaturation, unit: .percent())
        async let resp = latestQuantity(.respiratoryRate, unit: HKUnit.count().unitDivided(by: .minute()))
        async let wrist = weekSeries(.appleSleepingWristTemperature, unit: .degreeCelsius())
        async let protein = sumQuantity(.dietaryProtein, unit: .gram(), from: startOfDay, to: now)
        async let carbs = sumQuantity(.dietaryCarbohydrates, unit: .gram(), from: startOfDay, to: now)
        async let fat = sumQuantity(.dietaryFatTotal, unit: .gram(), from: startOfDay, to: now)
        async let dietEnergy = sumQuantity(.dietaryEnergyConsumed, unit: .kilocalorie(), from: startOfDay, to: now)
        async let sleep = sleepDonut(from: startOfDay.addingTimeInterval(-12 * 3600), to: now)
        async let mindful = mindfulMinutes(from: startOfDay, to: now)
        async let sys = latestQuantity(.bloodPressureSystolic, unit: .millimeterOfMercury())
        async let dia = latestQuantity(.bloodPressureDiastolic, unit: .millimeterOfMercury())

        let activeSeries = await active
        let avgPerHour = (activeSeries.total / 14.0) / 24.0
        let avgActive = Array(repeating: avgPerHour, count: 24)
        let todayCard = cumulativeCard(
            id: "active_energy",
            titleKey: "health_active_energy",
            unitKey: "health_unit_kcal",
            todayByHour: activeSeries.byHour,
            averageByHour: avgActive,
            sourceName: activeSeries.source,
            nowHourFraction: hour
        )

        let hrvValue = await hrv
        let stress =
            HrvStressIndex.shared.fromHrvMagnitude(
                hrvMs: hrvValue.value.map { KotlinDouble(value: $0) }
            )

        let dashboard = HealthMetricsDashboard(
            todayActiveEnergy: todayCard,
            todayRestingEnergy: simpleCard(
                id: "resting_energy",
                titleKey: "health_resting_energy",
                value: await basal.value,
                unitKey: "health_unit_kcal",
                source: await basal.source
            ),
            todayExerciseMinutes: simpleCard(
                id: "exercise_minutes",
                titleKey: "health_exercise_minutes",
                value: await exercise.value,
                unitKey: "health_unit_min",
                source: await exercise.source
            ),
            todaySteps: simpleCard(
                id: "steps",
                titleKey: "health_steps",
                value: await steps.value,
                unitKey: "health_unit_steps",
                source: await steps.source
            ),
            restingHr: simpleCard(
                id: "resting_hr",
                titleKey: "health_resting_hr",
                value: await rhr.value,
                unitKey: "health_unit_bpm",
                source: await rhr.source
            ),
            hrv: simpleCard(
                id: "hrv",
                titleKey: "health_hrv",
                value: hrvValue.value,
                unitKey: "health_unit_ms",
                source: hrvValue.source
            ),
            hrvStress: HealthMetricCard(
                id: "hrv_stress",
                titleKey: "health_hrv_stress",
                valueText: stress.index.map { String(format: "%.1f", $0.doubleValue) },
                unitKey: nil,
                sourceName: hrvValue.source,
                series: [],
                comparisonSeries: [],
                nowMarkerFraction: 1,
                empty: stress.index == nil,
                needsPermission: false,
                estimateLabelKey: "health_hrv_stress_estimate"
            ),
            recoveryHrv: simpleCard(
                id: "recovery_hrv",
                titleKey: "health_recovery_hrv",
                value: hrvValue.value,
                unitKey: "health_unit_ms",
                source: hrvValue.source
            ),
            bloodPressure: bloodPressureCard(sys: await sys, dia: await dia),
            oxygenSaturation: simpleCard(
                id: "spo2",
                titleKey: "health_spo2",
                value: await spo2.value.map { $0 * 100 },
                unitKey: "health_unit_percent",
                source: await spo2.source
            ),
            respiratoryRate: simpleCard(
                id: "respiratory_rate",
                titleKey: "health_respiratory_rate",
                value: await resp.value,
                unitKey: "health_unit_brpm",
                source: await resp.source
            ),
            wristTemperature: weekCard(await wrist),
            dietaryEnergy: simpleCard(
                id: "dietary_energy",
                titleKey: "health_dietary_energy",
                value: await dietEnergy.value,
                unitKey: "health_unit_kcal",
                source: await dietEnergy.source
            ),
            dietaryMacros: macrosCard(
                protein: await protein.value,
                carbs: await carbs.value,
                fat: await fat.value,
                source: await protein.source
            ),
            sleep: await sleep,
            mindfulMinutes: simpleCard(
                id: "mindful",
                titleKey: "health_mindful_minutes",
                value: await mindful.value,
                unitKey: "health_unit_min",
                source: await mindful.source
            ),
            past7Days: [],
            past30Days: [],
            allTime: []
        )
        return Outcome(authorized: true, dashboard: dashboard)
    }

    private struct QuantitySum {
        var value: Double?
        var source: String?
        var byHour: [Double] = Array(repeating: 0, count: 24)
        var total: Double { byHour.reduce(0, +) }
    }

    private static func cumulativeCard(
        id: String,
        titleKey: String,
        unitKey: String,
        todayByHour: [Double],
        averageByHour: [Double],
        sourceName: String?,
        nowHourFraction: Double
    ) -> HealthMetricCard {
        if todayByHour.allSatisfy({ $0 == 0 }) && averageByHour.allSatisfy({ $0 == 0 }) {
            return simpleCard(id: id, titleKey: titleKey, value: nil, unitKey: unitKey, source: sourceName)
        }
        var t = 0.0
        var a = 0.0
        var current: [HealthSamplePoint] = []
        var comparison: [HealthSamplePoint] = []
        let n = max(todayByHour.count, averageByHour.count, 1)
        for i in 0..<n {
            t += i < todayByHour.count ? todayByHour[i] : 0
            a += i < averageByHour.count ? averageByHour[i] : 0
            current.append(
                HealthSamplePoint(
                    dayOffset: Int32(i),
                    value: t,
                    hourFraction: KotlinDouble(value: Double(i) / 24.0)
                )
            )
            comparison.append(
                HealthSamplePoint(
                    dayOffset: Int32(i),
                    value: a,
                    hourFraction: KotlinDouble(value: Double(i) / 24.0)
                )
            )
        }
        return HealthMetricCard(
            id: id,
            titleKey: titleKey,
            valueText: String(format: "%.0f", t),
            unitKey: unitKey,
            sourceName: sourceName,
            series: current,
            comparisonSeries: comparison,
            nowMarkerFraction: min(max(nowHourFraction / 24.0, 0), 1),
            empty: false,
            needsPermission: false,
            estimateLabelKey: nil
        )
    }

    private static func simpleCard(
        id: String,
        titleKey: String,
        value: Double?,
        unitKey: String,
        source: String?
    ) -> HealthMetricCard {
        HealthMetricCard(
            id: id,
            titleKey: titleKey,
            valueText: value.map { String(format: "%.0f", $0) },
            unitKey: unitKey,
            sourceName: source,
            series: [],
            comparisonSeries: [],
            nowMarkerFraction: 1,
            empty: value == nil,
            needsPermission: false,
            estimateLabelKey: nil
        )
    }

    private static func bloodPressureCard(sys: QuantitySum, dia: QuantitySum) -> HealthMetricCard {
        if let s = sys.value, let d = dia.value {
            return HealthMetricCard(
                id: "blood_pressure",
                titleKey: "health_blood_pressure",
                valueText: String(format: "%.0f/%.0f", s, d),
                unitKey: "health_unit_mmhg",
                sourceName: sys.source ?? dia.source,
                series: [],
                comparisonSeries: [],
                nowMarkerFraction: 1,
                empty: false,
                needsPermission: false,
                estimateLabelKey: nil
            )
        }
        return simpleCard(id: "blood_pressure", titleKey: "health_blood_pressure", value: nil, unitKey: "health_unit_mmhg", source: nil)
    }

    private static func macrosCard(protein: Double?, carbs: Double?, fat: Double?, source: String?) -> HealthMetricCard {
        guard protein != nil || carbs != nil || fat != nil else {
            return simpleCard(id: "dietary_macros", titleKey: "health_dietary_macros", value: nil, unitKey: "health_unit_g", source: nil)
        }
        let text = String(
            format: "P %.0f · C %.0f · F %.0f",
            protein ?? 0,
            carbs ?? 0,
            fat ?? 0
        )
        return HealthMetricCard(
            id: "dietary_macros",
            titleKey: "health_dietary_macros",
            valueText: text,
            unitKey: "health_unit_g",
            sourceName: source,
            series: [],
            comparisonSeries: [],
            nowMarkerFraction: 1,
            empty: false,
            needsPermission: false,
            estimateLabelKey: nil
        )
    }

    private static func weekCard(_ series: (points: [HealthSamplePoint], source: String?)) -> HealthMetricCard {
        HealthMetricCard(
            id: "wrist_temperature",
            titleKey: "health_wrist_temperature",
            valueText: series.points.last.map { String(format: "%.1f", $0.value) },
            unitKey: "health_unit_celsius",
            sourceName: series.source,
            series: series.points,
            comparisonSeries: [],
            nowMarkerFraction: 1,
            empty: series.points.isEmpty,
            needsPermission: false,
            estimateLabelKey: nil
        )
    }

    private static func sumQuantity(
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
            let query = HKStatisticsQuery(quantityType: type, quantitySamplePredicate: predicate, options: .cumulativeSum) { _, stats, _ in
                let value = stats?.sumQuantity()?.doubleValue(for: unit)
                let source = stats?.sources?.first?.name
                cont.resume(returning: QuantitySum(value: value, source: source))
            }
            store.execute(query)
        }
    }

    private static func cumulativeQuantity(
        _ id: HKQuantityTypeIdentifier,
        unit: HKUnit,
        from: Date,
        to: Date,
        byHour: Bool
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
                    let v = stats.sumQuantity()?.doubleValue(for: unit) ?? 0
                    if hour >= 0 && hour < 24 {
                        sum.byHour[hour] += v
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

    private static func latestQuantity(_ id: HKQuantityTypeIdentifier, unit: HKUnit) async -> QuantitySum {
        guard let type = HKQuantityType.quantityType(forIdentifier: id) else {
            return QuantitySum()
        }
        let store = HKHealthStore()
        let sort = NSSortDescriptor(key: HKSampleSortIdentifierEndDate, ascending: false)
        return await withCheckedContinuation { cont in
            let query = HKSampleQuery(sampleType: type, predicate: nil, limit: 1, sortDescriptors: [sort]) { _, samples, _ in
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

    private static func weekSeries(_ id: HKQuantityTypeIdentifier, unit: HKUnit) async -> (points: [HealthSamplePoint], source: String?) {
        guard let type = HKQuantityType.quantityType(forIdentifier: id) else {
            return ([], nil)
        }
        let store = HKHealthStore()
        let end = Date()
        let start = Calendar.current.date(byAdding: .day, value: -6, to: Calendar.current.startOfDay(for: end)) ?? end
        let predicate = HKQuery.predicateForSamples(withStart: start, end: end)
        let sort = NSSortDescriptor(key: HKSampleSortIdentifierEndDate, ascending: true)
        return await withCheckedContinuation { cont in
            let query = HKSampleQuery(sampleType: type, predicate: predicate, limit: HKObjectQueryNoLimit, sortDescriptors: [sort]) { _, samples, _ in
                let points: [HealthSamplePoint] = (samples as? [HKQuantitySample] ?? []).enumerated().map { idx, sample in
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

    private static func sleepDonut(from: Date, to: Date) async -> SleepDonutModel {
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
            let query = HKSampleQuery(sampleType: sleepType, predicate: predicate, limit: HKObjectQueryNoLimit, sortDescriptors: nil) { _, samples, _ in
                var asleep: Double = 0
                var awake: Double = 0
                var start: Date?
                var end: Date?
                var source: String?
                for sample in samples as? [HKCategorySample] ?? [] {
                    let minutes = sample.endDate.timeIntervalSince(sample.startDate) / 60.0
                    let value = sample.value
                    if value == HKCategoryValueSleepAnalysis.awake.rawValue {
                        awake += minutes
                    } else {
                        asleep += minutes
                    }
                    if start == nil || sample.startDate < start! { start = sample.startDate }
                    if end == nil || sample.endDate > end! { end = sample.endDate }
                    source = sample.sourceRevision.source.name
                }
                let fmt = DateFormatter()
                fmt.timeStyle = .short
                cont.resume(
                    returning: SleepDonutModel(
                        asleepMinutes: asleep,
                        awakeMinutes: awake,
                        startLabel: start.map { fmt.string(from: $0) },
                        endLabel: end.map { fmt.string(from: $0) },
                        sourceName: source,
                        needsPermission: false
                    )
                )
            }
            store.execute(query)
        }
    }

    private static func mindfulMinutes(from: Date, to: Date) async -> QuantitySum {
        guard let type = HKObjectType.categoryType(forIdentifier: .mindfulSession) else {
            return QuantitySum()
        }
        let store = HKHealthStore()
        let predicate = HKQuery.predicateForSamples(withStart: from, end: to)
        return await withCheckedContinuation { cont in
            let query = HKSampleQuery(sampleType: type, predicate: predicate, limit: HKObjectQueryNoLimit, sortDescriptors: nil) { _, samples, _ in
                let minutes = (samples as? [HKCategorySample] ?? []).reduce(0.0) {
                    $0 + $1.endDate.timeIntervalSince($1.startDate) / 60.0
                }
                let source = (samples?.first as? HKCategorySample)?.sourceRevision.source.name
                cont.resume(returning: QuantitySum(value: minutes > 0 ? minutes : nil, source: source))
            }
            store.execute(query)
        }
    }
}
