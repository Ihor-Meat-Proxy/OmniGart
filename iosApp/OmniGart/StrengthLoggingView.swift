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
                activeSessionControls
            }

            Button {
                includeWarmups.toggle()
                if let id = sessionId ?? lastSessionId {
                    refreshSummary(id: id)
                }
                tick += 1
            } label: {
                Text(
                    includeWarmups
                        ? "● \(catalog("workout_include_warmups"))"
                        : "○ \(catalog("workout_include_warmups"))"
                )
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

    @ViewBuilder
    private var activeSessionControls: some View {
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

        Task {
            let saved = await StrengthWorkoutHealthSave.saveEstimate(
                core: core,
                profileId: profileId,
                sessionId: id,
                includeWarmups: includeWarmups
            )
            await MainActor.run {
                lastEnergy = saved
                refreshSummary(id: id)
                savingHealth = false
            }
        }
    }

    private func refreshSummary(id: String) {
        let built = StrengthSessionSummaryLines.build(
            StrengthSummaryInput(
                core: core,
                profileId: profileId,
                sessionId: id,
                includeWarmups: includeWarmups,
                lastEnergy: lastEnergy
            )
        )
        summaryLines = built.lines
        setCount = built.setCount
        tick += 1
    }

    private func catalog(_ key: String) -> String {
        StringCatalog.shared.t(key: key, locale: StringCatalog.shared.SOURCE_LOCALE)
    }
}
