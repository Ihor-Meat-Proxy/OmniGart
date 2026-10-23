import SwiftUI
import Shared
import HealthKit

/// iPhone yoga / pilates session: prefer watch live workout; else MET estimate fallback.
struct MindfulBodySessionView: View {
    enum Kind: String, CaseIterable, Identifiable {
        case yoga
        case pilates

        var id: String { rawValue }

        var activityType: HKWorkoutActivityType {
            switch self {
            case .yoga: return .yoga
            case .pilates: return .pilates
            }
        }

        var titleKey: String {
            switch self {
            case .yoga: return "workout_yoga_title"
            case .pilates: return "workout_pilates_title"
            }
        }
    }

    let core: OmniGartCore
    let profileId: String
    let kind: Kind

    @AppStorage("omnigart.yoga.mindful_minutes.enabled") private var mindfulMinutesEnabled = false
    @State private var running = false
    @State private var startedAt: Date?
    @State private var summaryLines: [String] = []
    @State private var watchStatus: String?
    @State private var saving = false

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(catalog(kind.titleKey))
                .font(.subheadline.weight(.semibold))
            Text(catalog("workout_mindful_body_intro"))
                .font(.footnote)
                .foregroundStyle(Color.OmniGart.colorTextLightSecondary)

            Button(catalog("workout_start_on_watch")) {
                Task { await startOnWatch() }
            }
            .accessibilityIdentifier("\(kind.rawValue)_start_on_watch")

            if let watchStatus {
                Text(watchStatus)
                    .font(.caption2)
                    .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
            }

            if !running {
                Button(catalog("workout_start_iphone_estimate")) {
                    running = true
                    startedAt = Date()
                    summaryLines = []
                }
                .accessibilityIdentifier("\(kind.rawValue)_start_iphone")
            } else {
                Text(catalog("workout_active"))
                    .accessibilityIdentifier("\(kind.rawValue)_status")
                Button(catalog("workout_end")) {
                    endAndSave()
                }
                .disabled(saving)
                .accessibilityIdentifier("\(kind.rawValue)_end")
            }

            if kind == .yoga {
                Toggle(isOn: $mindfulMinutesEnabled) {
                    Text(catalog("workout_mindful_minutes_toggle"))
                        .font(.footnote)
                }
                .accessibilityIdentifier("yoga_mindful_toggle")
            }

            if !summaryLines.isEmpty {
                Text(catalog("workout_summary_title"))
                    .font(.subheadline.weight(.semibold))
                ForEach(summaryLines, id: \.self) { line in
                    Text(line).font(.footnote)
                }
            }
        }
    }

    private func startOnWatch() async {
        let outcome = await HealthKitWatchLauncher.startOnWatch(
            activityType: kind.activityType,
            indoor: true
        )
        await MainActor.run {
            switch outcome {
            case .watchStarted:
                watchStatus = catalog("workout_watch_started")
            case .watchUnavailable:
                watchStatus = catalog("workout_watch_unavailable")
            }
        }
    }

    private func endAndSave() {
        guard let start = startedAt else { return }
        let end = Date()
        running = false
        saving = true
        guard let profile = core.profiles.get(id: profileId) else {
            saving = false
            return
        }
        let durationHours = max(0, end.timeIntervalSince(start) / 3600.0)
        let age = Int(core.ageYears(profile: profile))
        let breakdown: WorkoutEnergyBreakdown =
            kind == .yoga
            ? WorkoutEnergyEstimate.shared.yogaBreakdown(
                weightKg: profile.weightKg,
                heightCm: profile.heightCm,
                ageYears: Int32(age),
                sex: profile.biologicalSex,
                durationHours: durationHours
            )
            : WorkoutEnergyEstimate.shared.pilatesBreakdown(
                weightKg: profile.weightKg,
                heightCm: profile.heightCm,
                ageYears: Int32(age),
                sex: profile.biologicalSex,
                durationHours: durationHours
            )
        let energy = HealthKitWorkoutWriter.EnergyInput(
            activeKcal: breakdown.activeKcal,
            basalKcal: breakdown.basalKcal,
            isEstimate: true
        )
        let saveMindful = kind == .yoga && mindfulMinutesEnabled

        Task {
            defer { saving = false }
            do {
                let recoveryEnd = end.addingTimeInterval(90)
                let hrSamples =
                    (try? await HealthKitWorkoutWriter.fetchHeartRate(from: start, to: recoveryEnd)) ?? []
                let during = hrSamples.filter { $0.date <= end }
                let avg = during.map(\.bpm).filter { $0 > 0 }
                let avgBpm = avg.isEmpty ? nil : avg.reduce(0, +) / Double(avg.count)
                let recovery = HealthKitWorkoutWriter.estimateHeartRateRecoveryBpm(
                    samples: hrSamples,
                    workoutEnd: end
                )
                let effort = HealthKitWorkoutWriter.estimateEffortScore(
                    averageBpm: avgBpm,
                    ageYears: age
                )
                let saved = try await HealthKitWorkoutWriter.save(
                    options: .init(
                        activityType: kind.activityType,
                        start: start,
                        end: end,
                        energy: energy,
                        heartRate: during,
                        indoor: true,
                        saveMindfulMinutes: saveMindful,
                        estimatedEffortScore: effort,
                        heartRateRecoveryBpm: recovery
                    )
                )
                await MainActor.run {
                    summaryLines = makeSummaryLines(
                        start: start,
                        end: end,
                        result: saved,
                        estimate: energy
                    )
                }
            } catch {
                await MainActor.run {
                    summaryLines = makeSummaryLines(
                        start: start,
                        end: end,
                        result: nil,
                        estimate: energy
                    )
                }
            }
        }
    }

    private func makeSummaryLines(
        start: Date,
        end: Date,
        result: HealthKitWorkoutWriter.Result?,
        estimate: HealthKitWorkoutWriter.EnergyInput
    ) -> [String] {
        let active = result?.activeKcal ?? estimate.activeKcal
        let basal = result?.basalKcal ?? estimate.basalKcal
        var lines = [
            "\(catalog("workout_summary_duration")): \(formatDuration(end.timeIntervalSince(start)))",
            "\(catalog("workout_summary_active_kcal")): \(String(format: "%.0f", active))",
            "\(catalog("workout_summary_total_kcal")): \(String(format: "%.0f", active + basal))",
            catalog("workout_summary_energy_estimate")
        ]
        if let avg = result?.averageHeartRateBpm {
            lines.append("\(catalog("workout_summary_avg_hr")): \(String(format: "%.0f", avg))")
        }
        if let maxHr = result?.maxHeartRateBpm {
            lines.append("\(catalog("workout_summary_max_hr")): \(String(format: "%.0f", maxHr))")
        }
        if let effort = result?.effortScore {
            lines.append("\(catalog("workout_summary_effort")): \(String(format: "%.0f", effort))")
        }
        if let recovery = result?.heartRateRecoveryBpm {
            lines.append(
                "\(catalog("workout_summary_hr_recovery")): \(String(format: "%.0f", recovery))"
            )
        }
        return lines
    }

    private func formatDuration(_ seconds: TimeInterval) -> String {
        let total = max(0, Int(seconds.rounded()))
        let minutes = total / 60
        let secs = total % 60
        return String(format: "%d:%02d", minutes, secs)
    }

    private func catalog(_ key: String) -> String {
        StringCatalog.shared.t(key: key, locale: StringCatalog.shared.SOURCE_LOCALE)
    }
}
