import Shared
import SwiftUI

/// Recent local sessions for a workout category (no fake rows).
struct WorkoutCategoryHistorySection: View {
    let core: OmniGartCore
    let category: WorkoutCategory
    @State private var tick = 0

    private var ids: [String] {
        _ = tick
        return core.localHealth
            .listRecentWorkoutIds(activityTypeNames: category.historyActivityTypes, limit: 12)
            .map { $0 as String }
    }

    var body: some View {
        Section("workout_cat_history_title") {
            if ids.isEmpty {
                VStack(alignment: .leading, spacing: 4) {
                    Text("workout_cat_empty_title")
                        .font(.subheadline.weight(.semibold))
                    Text("workout_cat_empty_body")
                        .font(.footnote)
                        .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
                }
                .accessibilityElement(children: .combine)
                .accessibilityIdentifier("workout_cat_empty_\(category.rawValue)")
            } else {
                ForEach(ids, id: \.self) { id in
                    NavigationLink {
                        LocalHealthWorkoutDetailView(core: core, workoutId: id)
                    } label: {
                        VStack(alignment: .leading, spacing: 2) {
                            Text(core.localHealth.workoutDetailTitle(localId: id))
                                .font(.body)
                            Text(core.localHealth.workoutDetailSubtitle(localId: id))
                                .font(.caption)
                                .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
                        }
                    }
                    .accessibilityIdentifier("workout_cat_history_\(id)")
                }
            }
        }
        .onAppear { tick += 1 }
    }
}
