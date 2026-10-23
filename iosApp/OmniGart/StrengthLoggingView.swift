import SwiftUI
import Shared

/// Thin Workouts-tab strength logger over Shared StrengthLoggingService.
/// Prefers a live watch `HKWorkoutSession`; falls back to iPhone MET estimate labelled as such.
struct StrengthLoggingView: View {
    let core: OmniGartCore
    let profileId: String

    @State private var sessionId: String?
    @State private var selectedExerciseId: String = "barbell-bench-press"
    @State private var weightText: String = "60"
    @State private var repsText: String = "8"
    @State private var rirText: String = "2"
    @State private var isWarmup: Bool = false
    @State private var includeWarmups: Bool = false
    @State private var lastSessionId: String?
    @State private var summaryLines: [String] = []
    @State private var setCount: Int = 0
    @State private var tick = 0
    @State private var lastEnergy: HealthKitWorkoutWriter.Result?
    @State private var savingHealth = false
    @State private var watchStatus: String?

    private var demos: [DemoStrengthExercise] {
        _ = tick
        return core.strengthLogging.ensureDemoExercises()
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(catalog("workout_intro"))
                .font(.footnote)
                .foregroundStyle(Color.OmniGart.colorTextLightSecondary)

            Button(catalog("workout_start_on_watch")) {
                Task { await startOnWatch() }
            }
            .accessibilityIdentifier("workout_start_on_watch")
            if let watchStatus {
                Text(watchStatus)
                    .font(.caption2)
                    .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
                    .accessibilityIdentifier("workout_watch_status")
            }

            Text(sessionId == nil ? catalog("workout_idle") : "\(catalog("workout_active")) · \(setCount)")
                .accessibilityIdentifier("workout_status")

            if sessionId == nil {
                Button(catalog("workout_start")) {
                    let started = core.strengthLogging.startSession()
                    sessionId = started.id
                    lastSessionId = started.id
                    summaryLines = []
                    lastEnergy = nil
                    setCount = 0
                    tick += 1
                }
                .accessibilityIdentifier("workout_start")
            } else {
                Text(catalog("workout_exercise"))
                    .font(.subheadline.weight(.semibold))
                ForEach(demos, id: \.exercise.id) { demo in
                    Button {
                        selectedExerciseId = demo.exercise.id
                        tick += 1
                    } label: {
                        Text(selectedExerciseId == demo.exercise.id ? "● \(demo.displayName)" : "○ \(demo.displayName)")
                    }
                    .buttonStyle(.plain)
                    .accessibilityIdentifier("workout_exercise_\(demo.exercise.id)")
                }

                labeledField(catalog("workout_weight_kg"), text: $weightText, id: "workout_weight")
                labeledField(catalog("workout_reps"), text: $repsText, id: "workout_reps")
                labeledField(catalog("workout_rir"), text: $rirText, id: "workout_rir")

                Button {
                    isWarmup.toggle()
                    tick += 1
                } label: {
                    Text(isWarmup ? "● \(catalog("workout_warmup"))" : "○ \(catalog("workout_warmup"))")
                }
                .buttonStyle(.plain)
                .accessibilityIdentifier("workout_warmup_toggle")

                Button(catalog("workout_add_set")) {
                    guard let id = sessionId else { return }
                    let unilateral = demos.first { $0.exercise.id == selectedExerciseId }?.unilateral ?? false
                    let weight = Double(weightText).map { KotlinDouble(value: $0) }
                    let rir = Double(rirText).map { KotlinDouble(value: $0) }
                    _ = core.strengthLogging.addSet(
                        sessionId: id,
                        exerciseId: selectedExerciseId,
                        reps: Int32(Int(repsText) ?? 0),
                        weightKg: weight,
                        rir: rir,
                        isWarmup: isWarmup,
                        assistanceKg: nil,
                        loggedPerHand: unilateral,
                        completed: true
                    )
                    refreshSummary(id: id)
                }
                .accessibilityIdentifier("workout_add_set")

                Button(catalog("workout_end")) {
                    guard let id = sessionId else { return }
                    endAndSave(sessionId: id)
                }
                .disabled(savingHealth)
                .accessibilityIdentifier("workout_end")
            }

            Button {
                includeWarmups.toggle()
                if let id = sessionId ?? lastSessionId {
                    refreshSummary(id: id)
                }
                tick += 1
            } label: {
                Text(includeWarmups ? "● \(catalog("workout_include_warmups"))" : "○ \(catalog("workout_include_warmups"))")
            }
            .buttonStyle(.plain)
            .accessibilityIdentifier("workout_include_warmups")

            if !summaryLines.isEmpty {
                Text(catalog("workout_summary_title"))
                    .font(.subheadline.weight(.semibold))
                    .accessibilityIdentifier("workout_summary")
                ForEach(summaryLines, id: \.self) { line in
                    Text(line)
                        .font(.footnote)
                }
            }
        }
    }

    private func labeledField(_ label: String, text: Binding<String>, id: String) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(label).font(.caption)
            TextField(label, text: text)
                .textFieldStyle(.roundedBorder)
                .keyboardType(.decimalPad)
                .accessibilityIdentifier(id)
        }
    }

    private func startOnWatch() async {
        let outcome = await HealthKitWatchLauncher.startOnWatch(
            activityType: .traditionalStrengthTraining,
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

    private func endAndSave(sessionId id: String) {
        savingHealth = true
        _ = core.strengthLogging.endSession(sessionId: id)
        lastSessionId = id
        sessionId = nil
        refreshSummary(id: id)
        tick += 1

        guard let profile = core.profiles.get(id: profileId) else {
            savingHealth = false
            return
        }
        let start = dateFromEpochMillis(core.strengthLogging.sessionStartEpochMillis(sessionId: id))
        let end = dateFromEpochMillis(core.strengthLogging.sessionEndEpochMillis(sessionId: id))
        let durationHours = max(0, end.timeIntervalSince(start) / 3600.0)
        let age = Int(core.ageYears(profile: profile))
        let breakdown = WorkoutEnergyEstimate.shared.strengthBreakdown(
            weightKg: profile.weightKg,
            heightCm: profile.heightCm,
            ageYears: Int32(age),
            sex: profile.biologicalSex,
            durationHours: durationHours,
            met: WorkoutEnergyEstimate.shared.STRENGTH_MET
        )
        let energy = HealthKitWorkoutWriter.EnergyInput(
            activeKcal: breakdown.activeKcal,
            basalKcal: breakdown.basalKcal,
            isEstimate: true
        )
        let volumeSummary = core.strengthLogging.sessionSummary(
            sessionId: id,
            includeWarmups: includeWarmups,
            bodyweightKg: KotlinDouble(value: profile.weightKg),
            unit: .kg
        )
        let sets = core.strengthLogging.getSession(sessionId: id)?.sets ?? []
        var totalReps = 0
        for set in sets {
            if let reps = set.reps {
                totalReps += Int(reps)
            }
        }
        let strengthMeta = HealthKitWorkoutWriter.StrengthMetadata(
            totalSets: sets.count,
            totalReps: totalReps,
            totalWeightKg: volumeSummary.totalWeightLiftedDisplay
        )

        Task {
            defer { savingHealth = false }
            do {
                let recoveryEnd = end.addingTimeInterval(90)
                let hrSamples =
                    (try? await HealthKitWorkoutWriter.fetchHeartRate(from: start, to: recoveryEnd)) ?? []
                let avg = hrSamples.map(\.bpm).filter { $0 > 0 }
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
                        activityType: .traditionalStrengthTraining,
                        start: start,
                        end: end,
                        energy: energy,
                        heartRate: hrSamples.filter { $0.date <= end },
                        strength: strengthMeta,
                        indoor: true,
                        estimatedEffortScore: effort,
                        heartRateRecoveryBpm: recovery
                    )
                )
                await MainActor.run {
                    lastEnergy = saved
                    refreshSummary(id: id)
                }
            } catch {
                await MainActor.run {
                    lastEnergy = HealthKitWorkoutWriter.Result(
                        workoutUUID: UUID(),
                        activeKcal: energy.activeKcal,
                        basalKcal: energy.basalKcal,
                        averageHeartRateBpm: nil,
                        maxHeartRateBpm: nil,
                        energyIsEstimate: true,
                        effortScore: nil,
                        heartRateRecoveryBpm: nil
                    )
                    refreshSummary(id: id)
                }
            }
        }
    }

    private func refreshSummary(id: String) {
        let profile = core.profiles.get(id: profileId)
        let bw = profile.map { KotlinDouble(value: $0.weightKg) }
        let summary = core.strengthLogging.sessionSummary(
            sessionId: id,
            includeWarmups: includeWarmups,
            bodyweightKg: bw,
            unit: .kg
        )
        setCount = Int(core.strengthLogging.getSession(sessionId: id)?.sets.count ?? 0)
        let volume = summary.volume.totalVolumeDisplay
        let lifted = summary.totalWeightLiftedDisplay

        var lines: [String] = []
        if core.strengthLogging.getSession(sessionId: id) != nil {
            let start = dateFromEpochMillis(core.strengthLogging.sessionStartEpochMillis(sessionId: id))
            let end = dateFromEpochMillis(core.strengthLogging.sessionEndEpochMillis(sessionId: id))
            let seconds = max(0, end.timeIntervalSince(start))
            lines.append(
                "\(catalog("workout_summary_duration")): \(formatDuration(seconds))"
            )
        }
        if let energy = lastEnergy {
            lines.append(
                "\(catalog("workout_summary_active_kcal")): \(String(format: "%.0f", energy.activeKcal))"
            )
            lines.append(
                "\(catalog("workout_summary_total_kcal")): \(String(format: "%.0f", energy.activeKcal + energy.basalKcal))"
            )
            if energy.energyIsEstimate {
                lines.append(catalog("workout_summary_energy_estimate"))
            }
            if let avg = energy.averageHeartRateBpm {
                lines.append(
                    "\(catalog("workout_summary_avg_hr")): \(String(format: "%.0f", avg))"
                )
            }
            if let maxHr = energy.maxHeartRateBpm {
                lines.append(
                    "\(catalog("workout_summary_max_hr")): \(String(format: "%.0f", maxHr))"
                )
            }
            if let effort = energy.effortScore {
                lines.append(
                    "\(catalog("workout_summary_effort")): \(String(format: "%.0f", effort))"
                )
            }
            if let recovery = energy.heartRateRecoveryBpm {
                lines.append(
                    "\(catalog("workout_summary_hr_recovery")): \(String(format: "%.0f", recovery))"
                )
            }
        }
        lines.append(contentsOf: [
            "\(catalog("workout_volume_load")): \(String(format: "%.1f", volume)) kg",
            "\(catalog("workout_total_weight_lifted")): \(String(format: "%.1f", lifted)) kg",
            "\(catalog("workout_hard_sets")): \(summary.volume.hardSets)"
        ])
        lines.append(catalog("workout_per_exercise"))
        for row in summary.volume.perExercise {
            lines.append("\(row.exerciseId): \(String(format: "%.1f", row.volumeKg)) kg · \(row.hardSets)")
        }
        summaryLines = lines
        tick += 1
    }

    private func dateFromEpochMillis(_ millis: Int64) -> Date {
        Date(timeIntervalSince1970: TimeInterval(millis) / 1000.0)
    }

    private func formatDuration(_ seconds: TimeInterval) -> String {
        let total = max(0, Int(seconds.rounded()))
        let hours = total / 3600
        let minutes = (total % 3600) / 60
        let secs = total % 60
        if hours > 0 {
            return String(format: "%d:%02d:%02d", hours, minutes, secs)
        }
        return String(format: "%d:%02d", minutes, secs)
    }

    private func catalog(_ key: String) -> String {
        StringCatalog.shared.t(key: key, locale: StringCatalog.shared.SOURCE_LOCALE)
    }
}
