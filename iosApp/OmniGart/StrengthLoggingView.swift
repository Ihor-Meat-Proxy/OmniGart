import SwiftUI
import Shared

/// Thin Workouts-tab strength logger over Shared StrengthLoggingService.
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

    private var demos: [DemoStrengthExercise] {
        _ = tick
        return core.strengthLogging.ensureDemoExercises()
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(catalog("workout_intro"))
                .font(.footnote)
                .foregroundStyle(Color.OmniGart.colorTextLightSecondary)

            Text(sessionId == nil ? catalog("workout_idle") : "\(catalog("workout_active")) · \(setCount)")
                .accessibilityIdentifier("workout_status")

            if sessionId == nil {
                Button(catalog("workout_start")) {
                    let started = core.strengthLogging.startSession()
                    sessionId = started.id
                    lastSessionId = started.id
                    summaryLines = []
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
                    _ = core.strengthLogging.endSession(sessionId: id)
                    refreshSummary(id: id)
                    lastSessionId = id
                    sessionId = nil
                    tick += 1
                }
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
        var lines = [
            "\(catalog("workout_volume_load")): \(String(format: "%.1f", volume)) kg",
            "\(catalog("workout_total_weight_lifted")): \(String(format: "%.1f", lifted)) kg",
            "\(catalog("workout_hard_sets")): \(summary.volume.hardSets)"
        ]
        lines.append(catalog("workout_per_exercise"))
        for row in summary.volume.perExercise {
            lines.append("\(row.exerciseId): \(String(format: "%.1f", row.volumeKg)) kg · \(row.hardSets)")
        }
        summaryLines = lines
        tick += 1
    }

    private func catalog(_ key: String) -> String {
        StringCatalog.shared.t(key: key, locale: StringCatalog.shared.SOURCE_LOCALE)
    }
}
