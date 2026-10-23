import SwiftUI
import Shared

/// In-workout strength logger: header (duration, volume, sets, mini body map, rest, Finish),
/// exercise cards with set table, and append-only local + HealthKit save on finish.
struct StrengthLoggingView: View {
    let core: OmniGartCore
    let profileId: String

    @State private var sessionId: String?
    @State private var selectedExerciseId: String = "barbell-bench-press"
    @State private var includeWarmups: Bool = false
    @State private var lastSessionId: String?
    @State private var summaryLines: [String] = []
    @State private var setCount: Int = 0
    @State private var volumeDisplay: Double = 0
    @State private var elapsedLabel: String = "0:00"
    @State private var tick = 0
    @State private var lastEnergy: HealthKitWorkoutWriter.Result?
    @State private var savingHealth = false
    @State private var watchStatus: String?
    @State private var restRemaining: Int = 0
    @State private var restRunning = false
    @State private var draftWeight: [String: String] = [:]
    @State private var draftReps: [String: String] = [:]
    @State private var noteDraft: String = ""
    @State private var noteSetId: String?
    @State private var rpeDraft: String = "7"
    @State private var rpeSetId: String?
    @State private var showReplace = false

    private let defaultRest = 90

    private var demos: [DemoStrengthExercise] {
        _ = tick
        return core.strengthLogging.ensureDemoExercises()
    }

    private var massUnit: MassUnit {
        let profile = core.profiles.get(id: profileId)
        return MassUnits.shared.fromUnitSystem(units: profile?.preferredUnits ?? .metric)
    }

    private var unitIsLb: Bool { massUnit == .lb }

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
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

            if sessionId == nil {
                idleHeader
            } else {
                liveHeader
                liveExercises
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
                summaryBlock
            }
        }
        .onReceive(Timer.publish(every: 1, on: .main, in: .common).autoconnect()) { _ in
            guard sessionId != nil else { return }
            refreshElapsed()
            if restRunning {
                restRemaining = max(0, restRemaining - 1)
                if restRemaining == 0 {
                    restRunning = false
                }
            }
            tick += 1
        }
        .sheet(isPresented: Binding(
            get: { noteSetId != nil },
            set: { if !$0 { noteSetId = nil } }
        )) {
            noteSheet
        }
        .sheet(isPresented: Binding(
            get: { rpeSetId != nil },
            set: { if !$0 { rpeSetId = nil } }
        )) {
            rpeSheet
        }
        .confirmationDialog(catalog("workout_replace_exercise"), isPresented: $showReplace) {
            ForEach(demos, id: \.exercise.id) { demo in
                Button(demo.displayName) {
                    guard let id = sessionId else { return }
                    _ = core.strengthLogging.replaceExercise(
                        sessionId: id,
                        fromExerciseId: selectedExerciseId,
                        toExerciseId: demo.exercise.id
                    )
                    selectedExerciseId = demo.exercise.id
                    refreshSummary(id: id)
                }
            }
        }
    }

    private var idleHeader: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(catalog("workout_idle"))
                .accessibilityIdentifier("workout_status")
            Button(catalog("workout_start")) {
                let started = core.strengthLogging.startSession()
                sessionId = started.id
                lastSessionId = started.id
                summaryLines = []
                lastEnergy = nil
                setCount = 0
                volumeDisplay = 0
                seedStarterSets(sessionId: started.id)
                tick += 1
            }
            .accessibilityIdentifier("workout_start")
        }
    }

    private var liveHeader: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(spacing: 12) {
                headerStat(catalog("workout_summary_duration"), elapsedLabel)
                headerStat(
                    catalog("workout_volume_short"),
                    String(format: "%.0f %@", volumeDisplay, unitIsLb ? "lb" : "kg")
                )
                headerStat(catalog("workout_sets_count"), "\(setCount)")
            }
            .accessibilityIdentifier("workout_status")

            HStack(spacing: 8) {
                miniBodyMap
                Spacer(minLength: 4)
                Button {
                    if restRunning {
                        restRunning = false
                        restRemaining = 0
                    } else {
                        restRemaining = defaultRest
                        restRunning = true
                    }
                } label: {
                    Text(
                        restRunning
                            ? "\(catalog("workout_rest_timer")) \(restRemaining)s"
                            : catalog("workout_rest_timer")
                    )
                    .font(.caption.weight(.semibold))
                }
                .accessibilityIdentifier("workout_rest_timer")

                Button(catalog("workout_finish")) {
                    guard let id = sessionId else { return }
                    endAndSave(sessionId: id)
                }
                .disabled(savingHealth)
                .accessibilityIdentifier("workout_end")
            }
        }
    }

    private var miniBodyMap: some View {
        let muscles = activeMuscleKeys()
        return HStack(spacing: 4) {
            Image(systemName: "figure.strengthtraining.traditional")
                .foregroundStyle(Color.OmniGart.colorBrandAccent)
            Text(muscles.isEmpty ? catalog("workout_body_map") : muscles.prefix(3).joined(separator: " · "))
                .font(.caption2)
                .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
                .lineLimit(1)
        }
        .accessibilityLabel(Text(catalog("workout_body_map")))
    }

    private var liveExercises: some View {
        VStack(alignment: .leading, spacing: 12) {
            if let id = sessionId, let session = core.strengthLogging.getSession(sessionId: id) {
                let order = exerciseOrder(session)
                ForEach(order, id: \.self) { exerciseId in
                    exerciseCard(sessionId: id, session: session, exerciseId: exerciseId)
                }
            }
        }
    }

    private func exerciseCard(sessionId id: String, session: Shared.Session, exerciseId: String) -> some View {
        let demo = demos.first { $0.exercise.id == exerciseId }
        let name = demo?.displayName ?? exerciseId
        let sets = setsFor(session, exerciseId)
        let target = ProgressionChip.shared.label(
            targetWeightKg: sets.first?.targetWeightKg,
            targetReps: sets.first?.targetReps.map { KotlinInt(value: $0.int32Value) },
            unit: massUnit
        )
        return VStack(alignment: .leading, spacing: 6) {
            HStack(spacing: 8) {
                Image(systemName: "dumbbell.fill")
                    .foregroundStyle(Color.OmniGart.colorBrandAccent)
                    .accessibilityHidden(true)
                Text(name)
                    .font(.subheadline.weight(.semibold))
                    .accessibilityIdentifier("workout_exercise_\(exerciseId)")
                Spacer()
                if let target {
                    Text(target)
                        .font(.caption2.weight(.semibold))
                        .padding(.horizontal, 6)
                        .padding(.vertical, 2)
                        .background(Color.OmniGart.colorBrandAccent.opacity(0.15))
                        .clipShape(Capsule())
                        .accessibilityLabel(Text(catalog("workout_progression")))
                }
                Button(catalog("workout_note")) {
                    noteSetId = sets.last?.id
                    noteDraft = sets.last?.note ?? ""
                }
                .font(.caption)
                Menu {
                    Button(catalog("workout_replace_exercise")) {
                        selectedExerciseId = exerciseId
                        showReplace = true
                    }
                    Button(catalog("workout_remove_exercise"), role: .destructive) {
                        _ = core.strengthLogging.removeExercise(sessionId: id, exerciseId: exerciseId)
                        refreshSummary(id: id)
                    }
                } label: {
                    Image(systemName: "ellipsis.circle")
                }
                .accessibilityLabel(Text(catalog("workout_set_menu")))
            }

            setTable(sessionId: id, session: session, exerciseId: exerciseId, sets: sets)

            Button(catalog("workout_add_set")) {
                addSet(sessionId: id, exerciseId: exerciseId, unilateral: demo?.unilateral ?? false)
            }
            .accessibilityIdentifier("workout_add_set")
        }
        .padding(.vertical, 4)
    }

    private func setTable(
        sessionId id: String,
        session: Shared.Session,
        exerciseId: String,
        sets: [WorkoutSet]
    ) -> some View {
        VStack(spacing: 4) {
            HStack {
                Text(catalog("workout_col_set")).frame(width: 36, alignment: .leading)
                Text(catalog("workout_col_previous")).frame(maxWidth: .infinity, alignment: .leading)
                Text(unitIsLb ? catalog("workout_col_lb") : catalog("workout_col_kg"))
                    .frame(width: 56, alignment: .trailing)
                Text(catalog("workout_col_reps")).frame(width: 44, alignment: .trailing)
                Text(catalog("workout_col_done")).frame(width: 28)
            }
            .font(.caption2.weight(.semibold))
            .foregroundStyle(Color.OmniGart.colorTextLightSecondary)

            ForEach(sets, id: \.id) { set in
                setRow(sessionId: id, session: session, exerciseId: exerciseId, set: set)
            }
        }
    }

    private func setRow(
        sessionId id: String,
        session: Shared.Session,
        exerciseId: String,
        set: WorkoutSet
    ) -> some View {
        let label = setLabel(setsFor(session, exerciseId), setId: set.id)
        let previous = previousSet(session, exerciseId: exerciseId, before: Int(set.setIndex))
        let prevText = previousDisplay(previous) ?? "—"
        let weightKey = set.id + "-w"
        let repsKey = set.id + "-r"
        let weightBinding = Binding(
            get: { draftWeight[weightKey] ?? displayWeightText(set: set, previous: previous) },
            set: { draftWeight[weightKey] = $0 }
        )
        let repsBinding = Binding(
            get: {
                draftReps[repsKey]
                    ?? "\(set.reps?.intValue ?? previous?.reps?.intValue ?? 8)"
            },
            set: { draftReps[repsKey] = $0 }
        )

        return HStack(spacing: 4) {
            Text(label).frame(width: 36, alignment: .leading).font(.caption.monospacedDigit())
            Text(prevText)
                .font(.caption2)
                .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
                .frame(maxWidth: .infinity, alignment: .leading)
            TextField("", text: weightBinding)
                .keyboardType(.decimalPad)
                .multilineTextAlignment(.trailing)
                .frame(width: 56)
                .font(.caption.monospacedDigit())
                .accessibilityIdentifier("workout_weight")
            TextField("", text: repsBinding)
                .keyboardType(.numberPad)
                .multilineTextAlignment(.trailing)
                .frame(width: 44)
                .font(.caption.monospacedDigit())
                .accessibilityIdentifier("workout_reps")
            Button {
                applyDraftAndComplete(sessionId: id, set: set, weightKey: weightKey, repsKey: repsKey)
            } label: {
                Image(systemName: set.completed ? "checkmark.circle.fill" : "circle")
                    .foregroundStyle(
                        set.completed ? Color.OmniGart.colorBrandAccent : Color.OmniGart.colorTextLightSecondary
                    )
            }
            .buttonStyle(.plain)
            .frame(width: 28)
            .accessibilityIdentifier("workout_set_done_\(set.id)")

            Menu {
                Button(catalog("workout_mark_warmup")) {
                    updateSetFlags(sessionId: id, set: set, isWarmup: !set.isWarmup)
                }
                Button(catalog("workout_mark_drop")) {
                    updateSetFlags(sessionId: id, set: set, isDropSet: !set.isDropSet)
                }
                Button(catalog("workout_mark_failure")) {
                    updateSetFlags(sessionId: id, set: set, isFailure: !set.isFailure)
                }
                Button(catalog("workout_record_rpe")) {
                    rpeSetId = set.id
                    if let rpe = set.rpe {
                        rpeDraft = String(format: "%.0f", rpe.doubleValue)
                    } else {
                        rpeDraft = "7"
                    }
                }
                Button(catalog("workout_note")) {
                    noteSetId = set.id
                    noteDraft = set.note ?? ""
                }
            } label: {
                Image(systemName: "ellipsis")
                    .font(.caption)
            }
            .accessibilityLabel(Text(catalog("workout_set_menu")))
        }
    }

    private var noteSheet: some View {
        NavigationStack {
            TextField(catalog("workout_note"), text: $noteDraft, axis: .vertical)
                .padding()
            Spacer()
                .navigationTitle(catalog("workout_note"))
                .toolbar {
                    ToolbarItem(placement: .confirmationAction) {
                        Button(catalog("workout_finish")) {
                            saveNote()
                        }
                    }
                }
        }
    }

    private var rpeSheet: some View {
        NavigationStack {
            TextField(catalog("workout_record_rpe"), text: $rpeDraft)
                .keyboardType(.decimalPad)
                .padding()
            Spacer()
                .navigationTitle(catalog("workout_record_rpe"))
                .toolbar {
                    ToolbarItem(placement: .confirmationAction) {
                        Button(catalog("workout_finish")) {
                            saveRpe()
                        }
                    }
                }
        }
    }

    private var summaryBlock: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(catalog("workout_summary_title"))
                .font(.subheadline.weight(.semibold))
                .accessibilityIdentifier("workout_summary")
            ForEach(summaryLines, id: \.self) { line in
                Text(line).font(.footnote)
            }
            if let lastSessionId {
                Button("pdf_export_button") {
                    exportSummaryPdf(sessionId: lastSessionId)
                }
                .accessibilityIdentifier("pdf_export_button")
                Text(catalog("workout_workoutkit_limit"))
                    .font(.caption2)
                    .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
    }

    private func headerStat(_ title: String, _ value: String) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(title).font(.caption2).foregroundStyle(Color.OmniGart.colorTextLightSecondary)
            Text(value).font(.subheadline.monospacedDigit().weight(.semibold))
        }
    }

    private func exerciseOrder(_ session: Shared.Session) -> [String] {
        var seen: [String] = []
        for set in session.sets {
            if !seen.contains(set.exerciseId) {
                seen.append(set.exerciseId)
            }
        }
        return seen
    }

    private func setsFor(_ session: Shared.Session, _ exerciseId: String) -> [WorkoutSet] {
        session.sets.filter { $0.exerciseId == exerciseId }.sorted { $0.setIndex < $1.setIndex }
    }

    private func setLabel(_ sets: [WorkoutSet], setId: String) -> String {
        guard let target = sets.first(where: { $0.id == setId }) else { return "—" }
        if target.isWarmup { return "W" }
        var n = 0
        for set in sets where !set.isWarmup {
            n += 1
            if set.id == setId { return "\(n)" }
        }
        return "—"
    }

    private func previousSet(_ session: Shared.Session, exerciseId: String, before: Int) -> WorkoutSet? {
        session.sets
            .filter {
                $0.exerciseId == exerciseId && Int($0.setIndex) < before && $0.completed && !$0.isWarmup
            }
            .max(by: { $0.setIndex < $1.setIndex })
    }

    private func previousDisplay(_ previous: WorkoutSet?) -> String? {
        guard let previous, let reps = previous.reps else { return nil }
        guard let kg = previous.weightKg else { return "\(reps.intValue) reps" }
        let display = MassUnits.shared.kgToDisplay(kg: kg.doubleValue, unit: massUnit)
        let unitLabel = unitIsLb ? "lb" : "kg"
        return String(format: "%.0f%@ × %d", display, unitLabel, reps.intValue)
    }

    private func seedStarterSets(sessionId id: String) {
        for demo in demos.prefix(2) {
            _ = core.strengthLogging.addSet(
                sessionId: id,
                exerciseId: demo.exercise.id,
                reps: 8,
                weightKg: KotlinDouble(value: 60),
                rir: KotlinDouble(value: 2),
                isWarmup: false,
                assistanceKg: nil,
                loggedPerHand: demo.unilateral,
                completed: false,
                rpe: nil,
                isDropSet: false,
                isFailure: false,
                note: nil,
                restSeconds: KotlinInt(value: Int32(defaultRest)),
                targetWeightKg: KotlinDouble(value: 60),
                targetReps: KotlinInt(value: 8)
            )
        }
        selectedExerciseId = demos.first?.exercise.id ?? selectedExerciseId
        refreshSummary(id: id)
    }

    private func addSet(sessionId id: String, exerciseId: String, unilateral: Bool) {
        let session = core.strengthLogging.getSession(sessionId: id)
        let previous = session.flatMap { previousSet($0, exerciseId: exerciseId, before: Int($0.sets.count)) }
        let weight = previous?.weightKg ?? KotlinDouble(value: 60)
        let reps = previous?.reps?.int32Value ?? 8
        _ = core.strengthLogging.addSet(
            sessionId: id,
            exerciseId: exerciseId,
            reps: reps,
            weightKg: weight,
            rir: nil,
            isWarmup: false,
            assistanceKg: nil,
            loggedPerHand: unilateral,
            completed: false,
            rpe: nil,
            isDropSet: false,
            isFailure: false,
            note: nil,
            restSeconds: KotlinInt(value: Int32(defaultRest)),
            targetWeightKg: weight,
            targetReps: KotlinInt(value: reps)
        )
        refreshSummary(id: id)
    }

    private func applyDraftAndComplete(sessionId id: String, set: WorkoutSet, weightKey: String, repsKey: String) {
        let displayWeight = Double(draftWeight[weightKey] ?? displayWeightText(set: set, previous: nil)) ?? 0
        let kg = MassUnits.shared.displayToKg(value: displayWeight, unit: massUnit)
        let reps = Int32(draftReps[repsKey] ?? "\(set.reps?.intValue ?? 0)") ?? 0
        let updated = WorkoutSet(
            id: set.id,
            exerciseId: set.exerciseId,
            setIndex: set.setIndex,
            reps: KotlinInt(value: reps),
            weightKg: KotlinDouble(value: kg),
            durationSeconds: set.durationSeconds,
            distanceMetres: set.distanceMetres,
            rir: set.rir,
            rpe: set.rpe,
            completed: true,
            isWarmup: set.isWarmup,
            isDropSet: set.isDropSet,
            isFailure: set.isFailure,
            assistanceKg: set.assistanceKg,
            loggedPerHand: set.loggedPerHand,
            note: set.note,
            restSeconds: set.restSeconds,
            targetWeightKg: set.targetWeightKg,
            targetReps: set.targetReps
        )
        _ = core.strengthLogging.updateSet(sessionId: id, set: updated)
        let rest = Int(core.strengthLogging.completeSet(sessionId: id, setId: set.id))
        restRemaining = max(rest, 0)
        restRunning = restRemaining > 0
        refreshSummary(id: id)
    }

    private func updateSetFlags(
        sessionId id: String,
        set: WorkoutSet,
        isWarmup: Bool? = nil,
        isDropSet: Bool? = nil,
        isFailure: Bool? = nil,
        rpe: KotlinDouble? = nil,
        note: String? = nil
    ) {
        let updated = WorkoutSet(
            id: set.id,
            exerciseId: set.exerciseId,
            setIndex: set.setIndex,
            reps: set.reps,
            weightKg: set.weightKg,
            durationSeconds: set.durationSeconds,
            distanceMetres: set.distanceMetres,
            rir: set.rir,
            rpe: rpe ?? set.rpe,
            completed: set.completed,
            isWarmup: isWarmup ?? set.isWarmup,
            isDropSet: isDropSet ?? set.isDropSet,
            isFailure: isFailure ?? set.isFailure,
            assistanceKg: set.assistanceKg,
            loggedPerHand: set.loggedPerHand,
            note: note ?? set.note,
            restSeconds: set.restSeconds,
            targetWeightKg: set.targetWeightKg,
            targetReps: set.targetReps
        )
        _ = core.strengthLogging.updateSet(sessionId: id, set: updated)
        refreshSummary(id: id)
    }

    private func saveNote() {
        guard let sid = sessionId, let setId = noteSetId,
              let set = core.strengthLogging.getSession(sessionId: sid)?.sets.first(where: { $0.id == setId })
        else {
            noteSetId = nil
            return
        }
        updateSetFlags(sessionId: sid, set: set, note: noteDraft)
        noteSetId = nil
    }

    private func saveRpe() {
        guard let sid = sessionId, let setId = rpeSetId,
              let set = core.strengthLogging.getSession(sessionId: sid)?.sets.first(where: { $0.id == setId })
        else {
            rpeSetId = nil
            return
        }
        let rpe = Double(rpeDraft).map { KotlinDouble(value: $0) }
        updateSetFlags(sessionId: sid, set: set, rpe: rpe)
        rpeSetId = nil
    }

    private func displayWeightText(set: WorkoutSet, previous: WorkoutSet?) -> String {
        let kg = set.weightKg?.doubleValue
            ?? set.targetWeightKg?.doubleValue
            ?? previous?.weightKg?.doubleValue
            ?? 0
        let display = MassUnits.shared.kgToDisplay(kg: kg, unit: massUnit)
        return String(format: display.truncatingRemainder(dividingBy: 1) == 0 ? "%.0f" : "%.1f", display)
    }

    private func activeMuscleKeys() -> [String] {
        guard let id = sessionId,
              let session = core.strengthLogging.getSession(sessionId: id) else { return [] }
        var keys: [String] = []
        for exId in exerciseOrder(session) {
            if let demo = demos.first(where: { $0.exercise.id == exId }) {
                keys.append(contentsOf: demo.exercise.primaryMuscles)
            }
        }
        return Array(Set(keys)).sorted()
    }

    private func refreshElapsed() {
        guard let id = sessionId else { return }
        let start = dateFromEpochMillis(core.strengthLogging.sessionStartEpochMillis(sessionId: id))
        elapsedLabel = formatDuration(Date().timeIntervalSince(start))
    }

    private func exportSummaryPdf(sessionId: String) {
        let profile = core.profiles.get(id: profileId)
        let summary = core.strengthLogging.sessionSummary(
            sessionId: sessionId,
            includeWarmups: includeWarmups,
            bodyweightKg: profile.map { KotlinDouble(value: $0.weightKg) },
            unit: massUnit
        )
        var nameParts: [String] = []
        var muscleParts: [String] = []
        for row in summary.volume.perExercise {
            nameParts.append("\(row.exerciseId)=\(row.exerciseId)")
        }
        let muscleMap = summary.volume.perMuscleVolumeKg
        for key in muscleMap.keys {
            if let kg = muscleMap[key] as? Double {
                muscleParts.append("\(key)=\(kg)")
            } else if let boxed = muscleMap[key] {
                muscleParts.append("\(key)=\(boxed)")
            }
        }
        let strengthJson =
            "\(summary.totalWeightLiftedKg)|\(nameParts.joined(separator: ","))|\(muscleParts.joined(separator: ","))"
        WorkoutPdfExport.shareSingle(
            core: core,
            workoutId: sessionId,
            strengthJson: strengthJson
        )
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
        restRunning = false
        refreshSummary(id: id)
        tick += 1

        Task {
            let saved = await StrengthWorkoutHealthSave.saveEstimate(
                StrengthHealthSaveRequest(
                    core: core,
                    profileId: profileId,
                    sessionId: id,
                    includeWarmups: includeWarmups
                )
            )
            lastEnergy = saved
            refreshSummary(id: id)
            savingHealth = false
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
        volumeDisplay = built.volumeDisplay
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
