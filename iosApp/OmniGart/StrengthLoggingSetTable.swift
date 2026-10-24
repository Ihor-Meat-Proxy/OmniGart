import SwiftUI
import Shared

struct StrengthSetActions {
    var complete: (WorkoutSet) -> Void
    var patch: (WorkoutSet, StrengthSetPatch) -> Void
    var recordRpe: (WorkoutSet) -> Void
    var editNote: (WorkoutSet) -> Void
}

struct StrengthSetTableView: View {
    let session: Shared.Session
    let exerciseId: String
    let unit: MassUnit
    @Binding var draftWeight: [String: String]
    @Binding var draftReps: [String: String]
    let actions: StrengthSetActions

    var body: some View {
        let sets = StrengthSetOps.setsFor(session, exerciseId)
        VStack(spacing: 4) {
            headerRow
            ForEach(sets, id: \.id) { set in
                StrengthSetRowView(
                    set: set,
                    label: StrengthSetOps.setLabel(sets, setId: set.id),
                    previous: StrengthSetOps.previousSet(session, exerciseId: exerciseId, before: Int(set.setIndex)),
                    unit: unit,
                    draftWeight: $draftWeight,
                    draftReps: $draftReps,
                    actions: actions
                )
            }
        }
    }

    private var headerRow: some View {
        HStack {
            Text(strengthCatalog("workout_col_set")).frame(width: 36, alignment: .leading)
            Text(strengthCatalog("workout_col_previous")).frame(maxWidth: .infinity, alignment: .leading)
            Text(unit == .lb ? strengthCatalog("workout_col_lb") : strengthCatalog("workout_col_kg"))
                .frame(width: 56, alignment: .trailing)
            Text(strengthCatalog("workout_col_reps")).frame(width: 44, alignment: .trailing)
            Text(strengthCatalog("workout_col_done")).frame(width: 28)
        }
        .font(.caption2.weight(.semibold))
        .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
    }
}

struct StrengthSetRowView: View {
    let set: WorkoutSet
    let label: String
    let previous: WorkoutSet?
    let unit: MassUnit
    @Binding var draftWeight: [String: String]
    @Binding var draftReps: [String: String]
    let actions: StrengthSetActions

    private var weightBinding: Binding<String> {
        let key = set.id + "-w"
        return Binding(
            get: {
                draftWeight[key] ?? StrengthSetOps.displayWeightText(set: set, previous: previous, unit: unit)
            },
            set: { draftWeight[key] = $0 }
        )
    }

    private var repsBinding: Binding<String> {
        let key = set.id + "-r"
        return Binding(
            get: { draftReps[key] ?? StrengthSetOps.displayRepsText(set: set, previous: previous) },
            set: { draftReps[key] = $0 }
        )
    }

    var body: some View {
        HStack(spacing: 4) {
            Text(label).frame(width: 36, alignment: .leading).font(.caption.monospacedDigit())
            Text(StrengthSetOps.previousDisplay(previous, unit: unit) ?? "—")
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
            doneButton
            flagsMenu
        }
    }

    private var doneButton: some View {
        Button {
            actions.complete(set)
        } label: {
            Image(systemName: set.completed ? "checkmark.circle.fill" : "circle")
                .foregroundStyle(
                    set.completed ? Color.OmniGart.colorBrandAccent : Color.OmniGart.colorTextLightSecondary
                )
        }
        .buttonStyle(.plain)
        .frame(width: 28)
        .accessibilityIdentifier("workout_set_done_\(set.id)")
    }

    private var flagsMenu: some View {
        Menu {
            Button(strengthCatalog("workout_mark_warmup")) {
                actions.patch(set, StrengthSetPatch(isWarmup: !set.isWarmup))
            }
            Button(strengthCatalog("workout_mark_drop")) {
                actions.patch(set, StrengthSetPatch(isDropSet: !set.isDropSet))
            }
            Button(strengthCatalog("workout_mark_failure")) {
                actions.patch(set, StrengthSetPatch(isFailure: !set.isFailure))
            }
            Button(strengthCatalog("workout_record_rpe")) {
                actions.recordRpe(set)
            }
            Button(strengthCatalog("workout_note")) {
                actions.editNote(set)
            }
        } label: {
            Image(systemName: "ellipsis")
                .font(.caption)
        }
        .accessibilityLabel(Text(strengthCatalog("workout_set_menu")))
    }
}
