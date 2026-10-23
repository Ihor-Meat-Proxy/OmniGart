import SwiftUI
import Shared

/// In-workout strength logger: header (duration, volume, sets, mini body map, rest, Finish),
/// exercise cards with set table, and append-only local + HealthKit save on finish.
struct StrengthLoggingView: View {
    let core: OmniGartCore
    let profileId: String

    @State var sessionId: String?
    @State var selectedExerciseId: String = "barbell-bench-press"
    @State var includeWarmups: Bool = false
    @State var lastSessionId: String?
    @State var summaryLines: [String] = []
    @State var setCount: Int = 0
    @State var volumeDisplay: Double = 0
    @State var elapsedLabel: String = "0:00"
    @State var tick = 0
    @State var lastEnergy: HealthKitWorkoutWriter.Result?
    @State var savingHealth = false
    @State var watchStatus: String?
    @State var restRemaining: Int = 0
    @State var restRunning = false
    @State var draftWeight: [String: String] = [:]
    @State var draftReps: [String: String] = [:]
    @State var noteDraft: String = ""
    @State var noteSetId: String?
    @State var rpeDraft: String = "7"
    @State var rpeSetId: String?
    @State var showReplace = false

    let defaultRest = 90

    var demos: [DemoStrengthExercise] {
        _ = tick
        return core.strengthLogging.ensureDemoExercises()
    }

    var massUnit: MassUnit {
        let profile = core.profiles.get(id: profileId)
        return MassUnits.shared.fromUnitSystem(units: profile?.preferredUnits ?? .metric)
    }

    var body: some View {
        content
            .onReceive(Timer.publish(every: 1, on: .main, in: .common).autoconnect()) { _ in
                handleTimerTick()
            }
            .sheet(isPresented: Binding(
                get: { noteSetId != nil },
                set: { if !$0 { noteSetId = nil } }
            )) {
                StrengthNoteSheet(text: $noteDraft, onSave: saveNote)
            }
            .sheet(isPresented: Binding(
                get: { rpeSetId != nil },
                set: { if !$0 { rpeSetId = nil } }
            )) {
                StrengthRpeSheet(text: $rpeDraft, onSave: saveRpe)
            }
            .confirmationDialog(strengthCatalog("workout_replace_exercise"), isPresented: $showReplace) {
                ForEach(demos, id: \.exercise.id) { demo in
                    Button(demo.displayName) {
                        replaceSelectedExercise(with: demo.exercise.id)
                    }
                }
            }
    }

    private var content: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(strengthCatalog("workout_intro"))
                .font(.footnote)
                .foregroundStyle(Color.OmniGart.colorTextLightSecondary)

            watchSection

            if sessionId == nil {
                idleHeader
            } else {
                liveHeader
                liveExercises
            }

            warmupsToggle

            if !summaryLines.isEmpty {
                StrengthSummaryBlockView(
                    summaryLines: summaryLines,
                    lastSessionId: lastSessionId,
                    onExport: exportSummaryPdf
                )
            }
        }
    }

    private var watchSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            Button(strengthCatalog("workout_start_on_watch")) {
                Task { await startOnWatch() }
            }
            .accessibilityIdentifier("workout_start_on_watch")
            if let watchStatus {
                Text(watchStatus)
                    .font(.caption2)
                    .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
                    .accessibilityIdentifier("workout_watch_status")
            }
        }
    }

    private var warmupsToggle: some View {
        Button {
            includeWarmups.toggle()
            if let id = sessionId ?? lastSessionId {
                refreshSummary(id: id)
            }
            tick += 1
        } label: {
            let key = "workout_include_warmups"
            Text(includeWarmups ? "● \(strengthCatalog(key))" : "○ \(strengthCatalog(key))")
        }
        .buttonStyle(.plain)
        .accessibilityIdentifier("workout_include_warmups")
    }

    private var idleHeader: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(strengthCatalog("workout_idle"))
                .accessibilityIdentifier("workout_status")
            Button(strengthCatalog("workout_start")) {
                startSession()
            }
            .accessibilityIdentifier("workout_start")
        }
    }

    private var liveHeader: some View {
        StrengthLiveHeaderView(
            state: StrengthLiveHeaderState(
                elapsedLabel: elapsedLabel,
                volumeDisplay: volumeDisplay,
                unitIsLb: massUnit == .lb,
                setCount: setCount,
                muscles: activeMuscleKeys(),
                restRunning: restRunning,
                restRemaining: restRemaining,
                savingHealth: savingHealth
            ),
            onRestTap: toggleRest,
            onFinish: {
                guard let id = sessionId else { return }
                endAndSave(sessionId: id)
            }
        )
    }

    private var liveExercises: some View {
        VStack(alignment: .leading, spacing: 12) {
            if let id = sessionId, let session = core.strengthLogging.getSession(sessionId: id) {
                ForEach(StrengthSetOps.exerciseOrder(session), id: \.self) { exerciseId in
                    exerciseCard(sessionId: id, session: session, exerciseId: exerciseId)
                }
            }
        }
    }
}
