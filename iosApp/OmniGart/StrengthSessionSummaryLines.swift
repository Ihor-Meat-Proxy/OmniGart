import Foundation
import Shared

struct StrengthSummaryInput {
    var core: OmniGartCore
    var profileId: String
    var sessionId: String
    var includeWarmups: Bool
    var lastEnergy: HealthKitWorkoutWriter.Result?
}

struct StrengthSummaryBuilt {
    var lines: [String]
    var setCount: Int
    var volumeDisplay: Double
}

enum StrengthSessionSummaryLines {
    static func build(_ input: StrengthSummaryInput) -> StrengthSummaryBuilt {
        let profile = input.core.profiles.get(id: input.profileId)
        let unit = MassUnits.shared.fromUnitSystem(units: profile?.preferredUnits ?? .metric)
        let unitLabel = unit == .lb ? "lb" : "kg"
        let bw = profile.map { KotlinDouble(value: $0.weightKg) }
        let summary = input.core.strengthLogging.sessionSummary(
            sessionId: input.sessionId,
            includeWarmups: input.includeWarmups,
            bodyweightKg: bw,
            unit: unit
        )
        let session = input.core.strengthLogging.getSession(sessionId: input.sessionId)
        let setCount = session?.sets.filter(\.completed).count ?? 0
        let volume = summary.volume.totalVolumeDisplay
        let lifted = summary.totalWeightLiftedDisplay

        var lines: [String] = []
        if session != nil {
            let start = dateFromEpochMillis(
                input.core.strengthLogging.sessionStartEpochMillis(sessionId: input.sessionId)
            )
            let end = dateFromEpochMillis(
                input.core.strengthLogging.sessionEndEpochMillis(sessionId: input.sessionId)
            )
            let seconds = max(0, end.timeIntervalSince(start))
            lines.append("\(catalog("workout_summary_duration")): \(formatDuration(seconds))")
        }
        if let energy = input.lastEnergy {
            lines.append(contentsOf: energyLines(energy))
        }
        lines.append(contentsOf: [
            "\(catalog("workout_volume_load")): \(String(format: "%.1f", volume)) \(unitLabel)",
            "\(catalog("workout_total_weight_lifted")): \(String(format: "%.1f", lifted)) \(unitLabel)",
            "\(catalog("workout_hard_sets")): \(summary.volume.hardSets)"
        ])
        lines.append(catalog("workout_per_exercise"))
        for row in summary.volume.perExercise {
            lines.append(
                "\(row.exerciseId): \(String(format: "%.1f", row.volumeKg)) \(unitLabel) · \(row.hardSets)"
            )
        }
        return StrengthSummaryBuilt(lines: lines, setCount: setCount, volumeDisplay: volume)
    }

    private static func energyLines(_ energy: HealthKitWorkoutWriter.Result) -> [String] {
        var lines = [
            "\(catalog("workout_summary_active_kcal")): \(String(format: "%.0f", energy.activeKcal))",
            "\(catalog("workout_summary_total_kcal")): \(String(format: "%.0f", energy.activeKcal + energy.basalKcal))"
        ]
        if energy.energyIsEstimate {
            lines.append(catalog("workout_summary_energy_estimate"))
        }
        if let avg = energy.averageHeartRateBpm {
            lines.append("\(catalog("workout_summary_avg_hr")): \(String(format: "%.0f", avg))")
        }
        if let maxHr = energy.maxHeartRateBpm {
            lines.append("\(catalog("workout_summary_max_hr")): \(String(format: "%.0f", maxHr))")
        }
        if let effort = energy.effortScore {
            lines.append("\(catalog("workout_summary_effort")): \(String(format: "%.0f", effort))")
        }
        if let recovery = energy.heartRateRecoveryBpm {
            lines.append(
                "\(catalog("workout_summary_hr_recovery")): \(String(format: "%.0f", recovery))"
            )
        }
        return lines
    }

    private static func dateFromEpochMillis(_ millis: Int64) -> Date {
        Date(timeIntervalSince1970: TimeInterval(millis) / 1000.0)
    }

    private static func formatDuration(_ seconds: TimeInterval) -> String {
        let total = max(0, Int(seconds.rounded()))
        let hours = total / 3600
        let minutes = (total % 3600) / 60
        let secs = total % 60
        if hours > 0 {
            return String(format: "%d:%02d:%02d", hours, minutes, secs)
        }
        return String(format: "%d:%02d", minutes, secs)
    }

    private static func catalog(_ key: String) -> String {
        StringCatalog.shared.t(key: key, locale: StringCatalog.shared.SOURCE_LOCALE)
    }
}
