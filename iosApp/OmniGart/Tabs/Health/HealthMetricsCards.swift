import Foundation
import Shared

/// Builds [HealthMetricCard] models from HealthKit query sums.
enum HealthMetricsCards {
    struct QuantitySum {
        var value: Double?
        var source: String?
        var byHour: [Double] = Array(repeating: 0, count: 24)
        var total: Double { byHour.reduce(0, +) }
    }

    struct CumulativeInput {
        var id: String
        var titleKey: String
        var unitKey: String
        var todayByHour: [Double]
        var averageByHour: [Double]
        var sourceName: String?
        var nowHourFraction: Double
    }

    static func cumulativeCard(_ input: CumulativeInput) -> HealthMetricCard {
        if input.todayByHour.allSatisfy({ $0 == 0 }) && input.averageByHour.allSatisfy({ $0 == 0 }) {
            return simpleCard(
                id: input.id,
                titleKey: input.titleKey,
                value: nil,
                unitKey: input.unitKey,
                source: input.sourceName
            )
        }
        var todayTotal = 0.0
        var averageTotal = 0.0
        var current: [HealthSamplePoint] = []
        var comparison: [HealthSamplePoint] = []
        let count = max(input.todayByHour.count, input.averageByHour.count, 1)
        for index in 0..<count {
            todayTotal += index < input.todayByHour.count ? input.todayByHour[index] : 0
            averageTotal += index < input.averageByHour.count ? input.averageByHour[index] : 0
            current.append(
                HealthSamplePoint(
                    dayOffset: Int32(index),
                    value: todayTotal,
                    hourFraction: KotlinDouble(value: Double(index) / 24.0)
                )
            )
            comparison.append(
                HealthSamplePoint(
                    dayOffset: Int32(index),
                    value: averageTotal,
                    hourFraction: KotlinDouble(value: Double(index) / 24.0)
                )
            )
        }
        return HealthMetricCard(
            id: input.id,
            titleKey: input.titleKey,
            valueText: String(format: "%.0f", todayTotal),
            unitKey: input.unitKey,
            sourceName: input.sourceName,
            series: current,
            comparisonSeries: comparison,
            nowMarkerFraction: min(max(input.nowHourFraction / 24.0, 0), 1),
            empty: false,
            needsPermission: false,
            estimateLabelKey: nil
        )
    }

    static func simpleCard(
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

    static func bloodPressureCard(
        sys: QuantitySum,
        dia: QuantitySum
    ) -> HealthMetricCard {
        if let systolic = sys.value, let diastolic = dia.value {
            return HealthMetricCard(
                id: "blood_pressure",
                titleKey: "health_blood_pressure",
                valueText: String(format: "%.0f/%.0f", systolic, diastolic),
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
        return simpleCard(
            id: "blood_pressure",
            titleKey: "health_blood_pressure",
            value: nil,
            unitKey: "health_unit_mmhg",
            source: nil
        )
    }

    static func macrosCard(
        protein: Double?,
        carbs: Double?,
        fat: Double?,
        source: String?
    ) -> HealthMetricCard {
        guard protein != nil || carbs != nil || fat != nil else {
            return simpleCard(
                id: "dietary_macros",
                titleKey: "health_dietary_macros",
                value: nil,
                unitKey: "health_unit_g",
                source: nil
            )
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

    static func weekCard(
        _ series: (points: [HealthSamplePoint], source: String?)
    ) -> HealthMetricCard {
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

    static func hrvStressCard(
        index: KotlinDouble?,
        source: String?
    ) -> HealthMetricCard {
        HealthMetricCard(
            id: "hrv_stress",
            titleKey: "health_hrv_stress",
            valueText: index.map { String(format: "%.1f", $0.doubleValue) },
            unitKey: nil,
            sourceName: source,
            series: [],
            comparisonSeries: [],
            nowMarkerFraction: 1,
            empty: index == nil,
            needsPermission: false,
            estimateLabelKey: "health_hrv_stress_estimate"
        )
    }
}
