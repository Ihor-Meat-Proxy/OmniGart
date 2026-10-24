import Shared
import SwiftUI
import UIKit

/// Workouts › Explore: programmes, routine categories, and ready routines.
struct WorkoutsExploreView: View {
    let core: OmniGartCore
    let profileId: String

    @State private var tick = 0
    @State private var selectedCategory: RoutineCategory = .bodyweight

    private var library: RoutineLibraryService {
        _ = tick
        return core.routineLibrary
    }

    var body: some View {
        List {
            Section("explore_programmes_title") {
                Text("explore_programmes_intro")
                    .font(.footnote)
                    .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
                ForEach(library.programmes(), id: \.id) { plan in
                    NavigationLink {
                        ProgrammeDetailView(core: core, profileId: profileId, planId: plan.id)
                    } label: {
                        VStack(alignment: .leading, spacing: 2) {
                            Text(LocalizedStringKey(plan.nameKey))
                            Text(LocalizedStringKey(plan.descriptionKey))
                                .font(.caption)
                                .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
                        }
                    }
                    .accessibilityIdentifier("explore_programme_\(plan.id)")
                }
            }
            Section("explore_routines_title") {
                Picker("explore_routine_category", selection: $selectedCategory) {
                    ForEach(library.routineCategories(), id: \.self) { category in
                        Text(LocalizedStringKey(categoryNameKey(category))).tag(category)
                    }
                }
                .pickerStyle(.menu)
                .accessibilityIdentifier("explore_routine_category")
                ForEach(library.routines(category: selectedCategory, profileId: profileId), id: \.id) { routine in
                    NavigationLink {
                        RoutineDetailView(core: core, profileId: profileId, routineId: routine.id)
                    } label: {
                        VStack(alignment: .leading, spacing: 2) {
                            Text(LocalizedStringKey(routine.nameKey))
                            Text(LocalizedStringKey(routine.equipmentSummaryKey))
                                .font(.caption)
                                .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
                            Text("\(Int(routine.estimatedMinutes)) min · \(routine.exerciseCount) exercises")
                                .font(.caption2)
                                .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
                        }
                    }
                    .accessibilityIdentifier("explore_routine_\(routine.id)")
                }
            }
        }
        .navigationTitle(Text("explore_title"))
        .onAppear { tick += 1 }
    }

    private func categoryNameKey(_ category: RoutineCategory) -> String {
        switch category {
        case .atHome: return "routine_category_at_home"
        case .travel: return "routine_category_travel"
        case .dumbbellsOnly: return "routine_category_dumbbells_only"
        case .band: return "routine_category_band"
        case .cardioHiit: return "routine_category_cardio_hiit"
        case .gym: return "routine_category_gym"
        case .bodyweight: return "routine_category_bodyweight"
        case .suspension: return "routine_category_suspension"
        default: return "routine_category_bodyweight"
        }
    }
}

struct ProgrammeDetailView: View {
    let core: OmniGartCore
    let profileId: String
    let planId: String

    private var plan: ProgrammePlan? {
        core.routineLibrary.programmes().first { $0.id == planId }
    }

    var body: some View {
        List {
            if let plan {
                Section {
                    Text(LocalizedStringKey(plan.descriptionKey))
                        .font(.footnote)
                }
                Section("explore_programme_sessions") {
                    ForEach(plan.sessionRoutineIds, id: \.self) { routineId in
                        NavigationLink {
                            RoutineDetailView(core: core, profileId: profileId, routineId: routineId)
                        } label: {
                            Text(
                                routineId
                                    .replacingOccurrences(of: "routine-", with: "")
                                    .replacingOccurrences(of: "-", with: " ")
                                    .capitalized
                            )
                        }
                    }
                }
            }
        }
        .navigationTitle(Text(plan.map { LocalizedStringKey($0.nameKey) } ?? LocalizedStringKey("explore_programmes_title")))
    }
}

struct RoutineDetailView: View {
    let core: OmniGartCore
    let profileId: String
    let routineId: String

    @State private var session: EditableRoutineSession?
    @State private var saved = false
    @State private var tick = 0

    private var card: NextWorkoutCard? {
        _ = tick
        return core.routineLibrary.nextWorkoutCard(
            profileId: profileId,
            routineId: routineId,
            weeklyDone: Int32(0),
            weeklyTarget: Int32(3)
        )
    }

    var body: some View {
        List {
            if let card {
                Section {
                    Text("\(card.weeklyGoalDone)/\(card.weeklyGoalTarget)")
                        .font(.title2.weight(.semibold))
                        .accessibilityIdentifier("routine_weekly_goal")
                    Text(LocalizedStringKey(card.nameKey))
                        .font(.headline)
                    Text(card.muscleSummary.joined(separator: ", "))
                        .font(.caption)
                        .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
                    BodyMapHighlightView(front: card.frontMuscles, back: card.backMuscles)
                    HStack {
                        Label("\(card.exerciseCount)", systemImage: "list.bullet")
                        Spacer()
                        Label("\(card.estimatedMinutes) min", systemImage: "clock")
                    }
                    .font(.caption)
                    if let notes = card.notesKey {
                        Text(LocalizedStringKey(notes))
                            .font(.footnote)
                            .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
                    }
                }
                Section("routine_exercises_title") {
                    ForEach(Array((session?.exercises ?? card.exercises).enumerated()), id: \.offset) { index, exercise in
                        NavigationLink {
                            ExerciseDetailView(core: core, profileId: profileId, exerciseId: exercise.exerciseId)
                        } label: {
                            HStack(spacing: OmniGartMetrics.space2) {
                                ExerciseIllustrationView(illustrationKey: exercise.illustrationKey)
                                    .frame(width: 44, height: 44)
                                VStack(alignment: .leading, spacing: 2) {
                                    Text(LocalizedStringKey(exercise.nameKey))
                                    Text("\(exercise.sets)×\(exercise.repsLabel)")
                                        .font(.caption)
                                        .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
                                    if let hint = card.progressionHints.first(where: { $0.exerciseId == exercise.exerciseId }) {
                                        Text(LocalizedStringKey(hint.messageKey))
                                            .font(.caption2)
                                            .foregroundStyle(Color.OmniGart.colorBrandAccent)
                                    }
                                }
                                Spacer()
                                Menu {
                                    Button("routine_action_replace") {
                                        replace(at: index)
                                    }
                                    Button("routine_action_remove", role: .destructive) {
                                        remove(at: index)
                                    }
                                    if index > 0 {
                                        Button("routine_action_move_up") { reorder(from: index, to: index - 1) }
                                    }
                                    if index < (session?.exercises.count ?? card.exercises.count) - 1 {
                                        Button("routine_action_move_down") { reorder(from: index, to: index + 1) }
                                    }
                                } label: {
                                    Image(systemName: "ellipsis.circle")
                                }
                                .accessibilityIdentifier("routine_exercise_menu_\(exercise.exerciseId)")
                            }
                        }
                    }
                    Button("routine_add_exercise") {
                        // Placeholder: append a bodyweight squat as a safe default add.
                        ensureSession()
                        if var current = session {
                            let add = card.exercises.first
                            if let add {
                                current = EditableRoutineSession(
                                    routineId: current.routineId,
                                    nameKey: current.nameKey,
                                    exercises: current.exercises + [add],
                                    estimatedMinutes: current.estimatedMinutes,
                                    notesKey: current.notesKey
                                )
                                session = current
                            }
                        }
                    }
                    .accessibilityIdentifier("routine_add_exercise")
                }
                Section {
                    Button("routine_start_workout") {
                        // Logging PR owns the active set table — hand off by selecting the routine.
                        saved = true
                    }
                    .accessibilityIdentifier("routine_start_workout")
                    Button(saved ? "routine_saved" : "routine_save") {
                        saved = true
                    }
                    .accessibilityIdentifier("routine_save")
                }
            }
        }
        .navigationTitle(Text("explore_routine_detail"))
        .onAppear {
            session = core.routineLibrary.editableSession(profileId: profileId, routineId: routineId)
            tick += 1
        }
    }

    private func ensureSession() {
        if session == nil {
            session = core.routineLibrary.editableSession(profileId: profileId, routineId: routineId)
        }
    }

    private func replace(at index: Int) {
        ensureSession()
        guard let current = session else { return }
        session = core.routineLibrary.replaceExercise(
            session: current,
            index: Int32(index),
            preferredIds: [],
            profileId: profileId
        )
        tick += 1
    }

    private func remove(at index: Int) {
        ensureSession()
        guard let current = session else { return }
        session = core.routineLibrary.removeExercise(session: current, index: Int32(index))
        tick += 1
    }

    private func reorder(from: Int, to: Int) {
        ensureSession()
        guard let current = session else { return }
        session = core.routineLibrary.reorderExercise(session: current, from: Int32(from), to: Int32(to))
        tick += 1
    }
}

struct BodyMapHighlightView: View {
    let front: [String]
    let back: [String]

    init(front: [Any], back: [Any]) {
        self.front = front.map { String(describing: $0) }
        self.back = back.map { String(describing: $0) }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: OmniGartMetrics.space2) {
            Text("routine_body_map_front")
                .font(.caption)
            MuscleMapIcons(muscles: front)
            Text(front.joined(separator: ", "))
                .font(.caption2)
                .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
            Text("routine_body_map_back")
                .font(.caption)
            MuscleMapIcons(muscles: back)
            Text(back.joined(separator: ", "))
                .font(.caption2)
                .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
        }
        .accessibilityIdentifier("routine_body_maps")
    }
}
