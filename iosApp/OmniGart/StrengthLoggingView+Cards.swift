import SwiftUI
import Shared

extension StrengthLoggingView {
    func exerciseCard(sessionId id: String, session: Shared.Session, exerciseId: String) -> some View {
        let demo = demos.first { $0.exercise.id == exerciseId }
        let sets = StrengthSetOps.setsFor(session, exerciseId)
        let target = ProgressionChip.shared.label(
            targetWeightKg: sets.first?.targetWeightKg,
            targetReps: sets.first?.targetReps.map { KotlinInt(value: $0.int32Value) },
            unit: massUnit
        )
        return StrengthExerciseCardView(
            exerciseId: exerciseId,
            name: demo?.displayName ?? exerciseId,
            target: target,
            session: session,
            unit: massUnit,
            draftWeight: $draftWeight,
            draftReps: $draftReps,
            setActions: setActions(sessionId: id),
            actions: StrengthExerciseCardActions(
                editNote: { beginNote(for: sets.last) },
                replace: {
                    selectedExerciseId = exerciseId
                    showReplace = true
                },
                remove: {
                    _ = core.strengthLogging.removeExercise(sessionId: id, exerciseId: exerciseId)
                    refreshSummary(id: id)
                },
                addSet: { addSet(sessionId: id, exerciseId: exerciseId, unilateral: demo?.unilateral ?? false) }
            )
        )
    }

    func setActions(sessionId id: String) -> StrengthSetActions {
        StrengthSetActions(
            complete: { applyDraftAndComplete(sessionId: id, set: $0) },
            patch: { updateSet(sessionId: id, set: $0, patch: $1) },
            recordRpe: { set in
                rpeSetId = set.id
                rpeDraft = set.rpe.map { String(format: "%.0f", $0.doubleValue) } ?? "7"
            },
            editNote: { beginNote(for: $0) }
        )
    }

    func beginNote(for set: WorkoutSet?) {
        noteSetId = set?.id
        noteDraft = set?.note ?? ""
    }
}
