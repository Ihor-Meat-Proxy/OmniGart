import SwiftUI
import Shared

struct StrengthExerciseCardActions {
    var editNote: () -> Void
    var replace: () -> Void
    var remove: () -> Void
    var addSet: () -> Void
}

struct StrengthExerciseCardView: View {
    let exerciseId: String
    let name: String
    let target: String?
    let session: Shared.Session
    let unit: MassUnit
    @Binding var draftWeight: [String: String]
    @Binding var draftReps: [String: String]
    let setActions: StrengthSetActions
    let actions: StrengthExerciseCardActions

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            titleRow
            StrengthSetTableView(
                session: session,
                exerciseId: exerciseId,
                unit: unit,
                draftWeight: $draftWeight,
                draftReps: $draftReps,
                actions: setActions
            )
            Button(strengthCatalog("workout_add_set")) {
                actions.addSet()
            }
            .accessibilityIdentifier("workout_add_set")
        }
        .padding(.vertical, 4)
    }

    private var titleRow: some View {
        HStack(spacing: 8) {
            Image(systemName: "dumbbell.fill")
                .foregroundStyle(Color.OmniGart.colorBrandAccent)
                .accessibilityHidden(true)
            Text(name)
                .font(.subheadline.weight(.semibold))
                .accessibilityIdentifier("workout_exercise_\(exerciseId)")
            Spacer()
            if let target {
                targetChip(target)
            }
            Button(strengthCatalog("workout_note")) {
                actions.editNote()
            }
            .font(.caption)
            exerciseMenu
        }
    }

    private func targetChip(_ target: String) -> some View {
        Text(target)
            .font(.caption2.weight(.semibold))
            .padding(.horizontal, 6)
            .padding(.vertical, 2)
            .background(Color.OmniGart.colorBrandAccent.opacity(0.15))
            .clipShape(Capsule())
            .accessibilityLabel(Text(strengthCatalog("workout_progression")))
    }

    private var exerciseMenu: some View {
        Menu {
            Button(strengthCatalog("workout_replace_exercise")) {
                actions.replace()
            }
            Button(strengthCatalog("workout_remove_exercise"), role: .destructive) {
                actions.remove()
            }
        } label: {
            Image(systemName: "ellipsis.circle")
        }
        .accessibilityLabel(Text(strengthCatalog("workout_set_menu")))
    }
}
