import SwiftUI
import Shared

extension StrengthLoggingView {
    func toggleRest() {
        if restRunning {
            restRunning = false
            restRemaining = 0
        } else {
            restRemaining = defaultRest
            restRunning = true
        }
    }

    func handleTimerTick() {
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

    func replaceSelectedExercise(with newExerciseId: String) {
        guard let id = sessionId else { return }
        _ = core.strengthLogging.replaceExercise(
            sessionId: id,
            fromExerciseId: selectedExerciseId,
            toExerciseId: newExerciseId
        )
        selectedExerciseId = newExerciseId
        refreshSummary(id: id)
    }

    func startSession() {
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

    func seedStarterSets(sessionId id: String) {
        for demo in demos.prefix(2) {
            StrengthSetOps.add(
                StrengthNewSet(
                    exerciseId: demo.exercise.id,
                    reps: 8,
                    weightKg: KotlinDouble(value: 60),
                    rir: KotlinDouble(value: 2),
                    unilateral: demo.unilateral,
                    restSeconds: defaultRest
                ),
                sessionId: id,
                core: core
            )
        }
        selectedExerciseId = demos.first?.exercise.id ?? selectedExerciseId
        refreshSummary(id: id)
    }

    func addSet(sessionId id: String, exerciseId: String, unilateral: Bool) {
        let session = core.strengthLogging.getSession(sessionId: id)
        let previous = session.flatMap {
            StrengthSetOps.previousSet($0, exerciseId: exerciseId, before: Int($0.sets.count))
        }
        StrengthSetOps.add(
            StrengthNewSet(
                exerciseId: exerciseId,
                reps: previous?.reps?.int32Value ?? 8,
                weightKg: previous?.weightKg ?? KotlinDouble(value: 60),
                rir: nil,
                unilateral: unilateral,
                restSeconds: defaultRest
            ),
            sessionId: id,
            core: core
        )
        refreshSummary(id: id)
    }

    func applyDraftAndComplete(sessionId id: String, set: WorkoutSet) {
        let patch = StrengthSetOps.completionPatch(
            set: set,
            weightText: draftWeight[set.id + "-w"],
            repsText: draftReps[set.id + "-r"],
            unit: massUnit
        )
        _ = core.strengthLogging.updateSet(sessionId: id, set: StrengthSetOps.apply(patch, to: set))
        let rest = Int(
            core.strengthLogging.completeSet(
                sessionId: id,
                setId: set.id,
                defaultRestSeconds: Int32(defaultRest)
            )
        )
        restRemaining = max(rest, 0)
        restRunning = restRemaining > 0
        refreshSummary(id: id)
    }

    func updateSet(sessionId id: String, set: WorkoutSet, patch: StrengthSetPatch) {
        _ = core.strengthLogging.updateSet(sessionId: id, set: StrengthSetOps.apply(patch, to: set))
        refreshSummary(id: id)
    }

    func editedSet(setId: String?) -> (sessionId: String, set: WorkoutSet)? {
        guard let sid = sessionId, let setId,
              let set = core.strengthLogging.getSession(sessionId: sid)?.sets.first(where: { $0.id == setId })
        else { return nil }
        return (sid, set)
    }

    func saveNote() {
        defer { noteSetId = nil }
        guard let edited = editedSet(setId: noteSetId) else { return }
        updateSet(sessionId: edited.sessionId, set: edited.set, patch: StrengthSetPatch(note: noteDraft))
    }

    func saveRpe() {
        defer { rpeSetId = nil }
        guard let edited = editedSet(setId: rpeSetId) else { return }
        let rpe = Double(rpeDraft).map { KotlinDouble(value: $0) }
        updateSet(sessionId: edited.sessionId, set: edited.set, patch: StrengthSetPatch(rpe: rpe))
    }

    func activeMuscleKeys() -> [String] {
        guard let id = sessionId,
              let session = core.strengthLogging.getSession(sessionId: id) else { return [] }
        var keys: [String] = []
        for exId in StrengthSetOps.exerciseOrder(session) {
            if let demo = demos.first(where: { $0.exercise.id == exId }) {
                keys.append(contentsOf: demo.exercise.primaryMuscles)
            }
        }
        return Array(Set(keys)).sorted()
    }

    func refreshElapsed() {
        guard let id = sessionId else { return }
        let start = StrengthSetOps.dateFromEpochMillis(core.strengthLogging.sessionStartEpochMillis(sessionId: id))
        elapsedLabel = StrengthSetOps.formatDuration(Date().timeIntervalSince(start))
    }

    func exportSummaryPdf(sessionId: String) {
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

    func startOnWatch() async {
        let outcome = await HealthKitWatchLauncher.startOnWatch(
            activityType: .traditionalStrengthTraining,
            indoor: true
        )
        await MainActor.run {
            switch outcome {
            case .watchStarted:
                watchStatus = strengthCatalog("workout_watch_started")
            case .watchUnavailable:
                watchStatus = strengthCatalog("workout_watch_unavailable")
            }
        }
    }

    func endAndSave(sessionId id: String) {
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

    func refreshSummary(id: String) {
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
}
