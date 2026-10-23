import Foundation
import HealthKit
import Shared

/// Fetches and assembles today's Health metrics dashboard from HealthKit samples.
enum HealthMetricsTodayLoad {
    struct Samples {
        var active: HealthMetricsCards.QuantitySum
        var basal: HealthMetricsCards.QuantitySum
        var exercise: HealthMetricsCards.QuantitySum
        var steps: HealthMetricsCards.QuantitySum
        var restingHr: HealthMetricsCards.QuantitySum
        var hrv: HealthMetricsCards.QuantitySum
        var spo2: HealthMetricsCards.QuantitySum
        var respiratory: HealthMetricsCards.QuantitySum
        var wrist: (points: [HealthSamplePoint], source: String?)
        var protein: HealthMetricsCards.QuantitySum
        var carbs: HealthMetricsCards.QuantitySum
        var fat: HealthMetricsCards.QuantitySum
        var dietEnergy: HealthMetricsCards.QuantitySum
        var sleep: SleepDonutModel
        var mindful: HealthMetricsCards.QuantitySum
        var systolic: HealthMetricsCards.QuantitySum
        var diastolic: HealthMetricsCards.QuantitySum
        var nowHourFraction: Double
    }

    private struct ActivityBundle {
        var active: HealthMetricsCards.QuantitySum
        var basal: HealthMetricsCards.QuantitySum
        var exercise: HealthMetricsCards.QuantitySum
        var steps: HealthMetricsCards.QuantitySum
    }

    private struct VitalsBundle {
        var restingHr: HealthMetricsCards.QuantitySum
        var hrv: HealthMetricsCards.QuantitySum
        var spo2: HealthMetricsCards.QuantitySum
        var respiratory: HealthMetricsCards.QuantitySum
        var wrist: (points: [HealthSamplePoint], source: String?)
        var systolic: HealthMetricsCards.QuantitySum
        var diastolic: HealthMetricsCards.QuantitySum
    }

    private struct NutritionBundle {
        var protein: HealthMetricsCards.QuantitySum
        var carbs: HealthMetricsCards.QuantitySum
        var fat: HealthMetricsCards.QuantitySum
        var dietEnergy: HealthMetricsCards.QuantitySum
    }

    private struct RecoveryBundle {
        var sleep: SleepDonutModel
        var mindful: HealthMetricsCards.QuantitySum
    }

    static func fetch() async -> Samples {
        let cal = Calendar.current
        let now = Date()
        let startOfDay = cal.startOfDay(for: now)
        let hour =
            Double(cal.component(.hour, from: now))
            + Double(cal.component(.minute, from: now)) / 60.0
        let activity = await fetchActivity(from: startOfDay, to: now)
        let vitals = await fetchVitals()
        let nutrition = await fetchNutrition(from: startOfDay, to: now)
        let recovery = await fetchRecovery(from: startOfDay, to: now)
        return Samples(
            active: activity.active,
            basal: activity.basal,
            exercise: activity.exercise,
            steps: activity.steps,
            restingHr: vitals.restingHr,
            hrv: vitals.hrv,
            spo2: vitals.spo2,
            respiratory: vitals.respiratory,
            wrist: vitals.wrist,
            protein: nutrition.protein,
            carbs: nutrition.carbs,
            fat: nutrition.fat,
            dietEnergy: nutrition.dietEnergy,
            sleep: recovery.sleep,
            mindful: recovery.mindful,
            systolic: vitals.systolic,
            diastolic: vitals.diastolic,
            nowHourFraction: hour
        )
    }

    static func assemble(_ samples: Samples) -> HealthMetricsDashboard {
        let stress = HrvStressIndex.shared.fromHrvMagnitude(
            hrvMs: samples.hrv.value.map { KotlinDouble(value: $0) }
        )
        return HealthMetricsDashboard(
            todayActiveEnergy: activeEnergyCard(samples),
            todayRestingEnergy: simple("resting_energy", "health_resting_energy", samples.basal, "health_unit_kcal"),
            todayExerciseMinutes: simple("exercise_minutes", "health_exercise_minutes", samples.exercise, "health_unit_min"),
            todaySteps: simple("steps", "health_steps", samples.steps, "health_unit_steps"),
            restingHr: simple("resting_hr", "health_resting_hr", samples.restingHr, "health_unit_bpm"),
            hrv: simple("hrv", "health_hrv", samples.hrv, "health_unit_ms"),
            hrvStress: HealthMetricsCards.hrvStressCard(index: stress.index, source: samples.hrv.source),
            recoveryHrv: simple("recovery_hrv", "health_recovery_hrv", samples.hrv, "health_unit_ms"),
            bloodPressure: HealthMetricsCards.bloodPressureCard(sys: samples.systolic, dia: samples.diastolic),
            oxygenSaturation: HealthMetricsCards.simpleCard(
                id: "spo2",
                titleKey: "health_spo2",
                value: samples.spo2.value.map { $0 * 100 },
                unitKey: "health_unit_percent",
                source: samples.spo2.source
            ),
            respiratoryRate: simple(
                "respiratory_rate",
                "health_respiratory_rate",
                samples.respiratory,
                "health_unit_brpm"
            ),
            wristTemperature: HealthMetricsCards.weekCard(samples.wrist),
            dietaryEnergy: simple(
                "dietary_energy",
                "health_dietary_energy",
                samples.dietEnergy,
                "health_unit_kcal"
            ),
            dietaryMacros: HealthMetricsCards.macrosCard(
                protein: samples.protein.value,
                carbs: samples.carbs.value,
                fat: samples.fat.value,
                source: samples.protein.source
            ),
            sleep: samples.sleep,
            mindfulMinutes: simple("mindful", "health_mindful_minutes", samples.mindful, "health_unit_min"),
            past7Days: [],
            past30Days: [],
            allTime: []
        )
    }

    private static func simple(
        _ id: String,
        _ titleKey: String,
        _ sum: HealthMetricsCards.QuantitySum,
        _ unitKey: String
    ) -> HealthMetricCard {
        HealthMetricsCards.simpleCard(
            id: id,
            titleKey: titleKey,
            value: sum.value,
            unitKey: unitKey,
            source: sum.source
        )
    }

    private static func activeEnergyCard(_ samples: Samples) -> HealthMetricCard {
        let avgPerHour = (samples.active.total / 14.0) / 24.0
        return HealthMetricsCards.cumulativeCard(
            .init(
                id: "active_energy",
                titleKey: "health_active_energy",
                unitKey: "health_unit_kcal",
                todayByHour: samples.active.byHour,
                averageByHour: Array(repeating: avgPerHour, count: 24),
                sourceName: samples.active.source,
                nowHourFraction: samples.nowHourFraction
            )
        )
    }

    private static func fetchActivity(from: Date, to: Date) async -> ActivityBundle {
        async let active = HealthMetricsQueries.cumulativeQuantity(
            .activeEnergyBurned, unit: .kilocalorie(), from: from, to: to
        )
        async let basal = HealthMetricsQueries.sumQuantity(
            .basalEnergyBurned, unit: .kilocalorie(), from: from, to: to
        )
        async let exercise = HealthMetricsQueries.sumQuantity(
            .appleExerciseTime, unit: .minute(), from: from, to: to
        )
        async let steps = HealthMetricsQueries.sumQuantity(
            .stepCount, unit: .count(), from: from, to: to
        )
        return ActivityBundle(
            active: await active,
            basal: await basal,
            exercise: await exercise,
            steps: await steps
        )
    }

    private static func fetchVitals() async -> VitalsBundle {
        let bpm = HKUnit.count().unitDivided(by: .minute())
        async let restingHr = HealthMetricsQueries.latestQuantity(.restingHeartRate, unit: bpm)
        async let hrv = HealthMetricsQueries.latestQuantity(
            .heartRateVariabilitySDNN, unit: .secondUnit(with: .milli)
        )
        async let spo2 = HealthMetricsQueries.latestQuantity(.oxygenSaturation, unit: .percent())
        async let respiratory = HealthMetricsQueries.latestQuantity(.respiratoryRate, unit: bpm)
        async let wrist = HealthMetricsQueries.weekSeries(
            .appleSleepingWristTemperature, unit: .degreeCelsius()
        )
        async let systolic = HealthMetricsQueries.latestQuantity(
            .bloodPressureSystolic, unit: .millimeterOfMercury()
        )
        async let diastolic = HealthMetricsQueries.latestQuantity(
            .bloodPressureDiastolic, unit: .millimeterOfMercury()
        )
        return VitalsBundle(
            restingHr: await restingHr,
            hrv: await hrv,
            spo2: await spo2,
            respiratory: await respiratory,
            wrist: await wrist,
            systolic: await systolic,
            diastolic: await diastolic
        )
    }

    private static func fetchNutrition(from: Date, to: Date) async -> NutritionBundle {
        async let protein = HealthMetricsQueries.sumQuantity(
            .dietaryProtein, unit: .gram(), from: from, to: to
        )
        async let carbs = HealthMetricsQueries.sumQuantity(
            .dietaryCarbohydrates, unit: .gram(), from: from, to: to
        )
        async let fat = HealthMetricsQueries.sumQuantity(
            .dietaryFatTotal, unit: .gram(), from: from, to: to
        )
        async let dietEnergy = HealthMetricsQueries.sumQuantity(
            .dietaryEnergyConsumed, unit: .kilocalorie(), from: from, to: to
        )
        return NutritionBundle(
            protein: await protein,
            carbs: await carbs,
            fat: await fat,
            dietEnergy: await dietEnergy
        )
    }

    private static func fetchRecovery(from: Date, to: Date) async -> RecoveryBundle {
        async let sleep = HealthMetricsQueries.sleepDonut(
            from: from.addingTimeInterval(-12 * 3600),
            to: to
        )
        async let mindful = HealthMetricsQueries.mindfulMinutes(from: from, to: to)
        return RecoveryBundle(sleep: await sleep, mindful: await mindful)
    }
}
