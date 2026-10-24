import Shared
import SwiftUI

/// Workouts tab: category tile grid, search, and category hubs.
struct WorkoutsTabView: View {
    let core: OmniGartCore
    let profileId: String

    @State private var searchText = ""
    @State private var tick = 0
    @Environment(\.colorScheme) private var colorScheme

    private let columns = [
        GridItem(.flexible(), spacing: OmniGartMetrics.space2),
        GridItem(.flexible(), spacing: OmniGartMetrics.space2)
    ]

    private var filteredCategories: [WorkoutCategory] {
        WorkoutCategory.allCases.filter { $0.matches(query: searchText) }
    }

    private var searchHits: [WorkoutSearchHit] {
        WorkoutSearchIndex.hits(core: core, profileId: profileId, query: searchText, tick: tick)
    }

    var body: some View {
        List {
            Section {
                Text("workout_intro")
                    .font(.footnote)
                    .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
                searchField
            }
            Section("workout_cat_categories_title") {
                LazyVGrid(columns: columns, spacing: OmniGartMetrics.space2) {
                    ForEach(filteredCategories) { category in
                        NavigationLink(value: category) {
                            WorkoutCategoryTile(category: category)
                        }
                        .buttonStyle(.plain)
                    }
                }
                .listRowInsets(EdgeInsets(
                    top: OmniGartMetrics.space2,
                    leading: OmniGartMetrics.space3,
                    bottom: OmniGartMetrics.space2,
                    trailing: OmniGartMetrics.space3
                ))
                .listRowBackground(Color.clear)
                .accessibilityIdentifier("workout_cat_grid")
            }
            if !searchText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                searchResultsSection
            } else if let card = core.routineLibrary.nextWorkoutCard(profileId: profileId) {
                Section("explore_title") {
                    NavigationLink {
                        RoutineDetailView(core: core, profileId: profileId, routineId: card.routineId)
                    } label: {
                        VStack(alignment: .leading, spacing: 4) {
                            Text("next_workout_title")
                                .font(.caption)
                                .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
                            Text(LocalizedStringKey(card.nameKey))
                                .font(.headline)
                            Text("\(card.weeklyGoalDone)/\(card.weeklyGoalTarget) · \(card.estimatedMinutes) min")
                                .font(.caption)
                                .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
                        }
                    }
                    .accessibilityIdentifier("workouts_next_card")
                }
            }
        }
        .navigationTitle(Text("tab_workouts"))
        .scrollContentBackground(.hidden)
        .background(canvas.ignoresSafeArea())
        .navigationDestination(for: WorkoutCategory.self) { category in
            WorkoutCategoryDetailView(
                core: core,
                profileId: profileId,
                category: category
            )
        }
        .onAppear { tick += 1 }
    }

    private var canvas: Color {
        colorScheme == .dark
            ? Color.OmniGart.colorSurfaceDarkCanvas
            : Color.OmniGart.colorSurfaceLightCanvas
    }

    private var searchField: some View {
        HStack(spacing: OmniGartMetrics.space2) {
            Image(systemName: "magnifyingglass")
                .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
                .accessibilityHidden(true)
            TextField("workout_cat_search_placeholder", text: $searchText)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
                .accessibilityIdentifier("workout_cat_search")
            if !searchText.isEmpty {
                Button {
                    searchText = ""
                } label: {
                    Image(systemName: "xmark.circle.fill")
                        .foregroundStyle(Color.OmniGart.colorTextLightTertiary)
                }
                .accessibilityLabel(Text("workout_cat_search_clear"))
            }
        }
        .padding(.horizontal, OmniGartMetrics.space3)
        .padding(.vertical, OmniGartMetrics.space2)
        .background(
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .fill(
                    colorScheme == .dark
                        ? Color.OmniGart.colorSurfaceDarkSunken
                        : Color.OmniGart.colorSurfaceLightSunken
                )
        )
        .listRowInsets(EdgeInsets(
            top: OmniGartMetrics.space2,
            leading: OmniGartMetrics.space3,
            bottom: OmniGartMetrics.space2,
            trailing: OmniGartMetrics.space3
        ))
        .listRowBackground(Color.clear)
        .accessibilityElement(children: .contain)
    }

    @ViewBuilder
    private var searchResultsSection: some View {
        Section("workout_cat_search_results_title") {
            if searchHits.isEmpty && filteredCategories.isEmpty {
                Text("workout_cat_search_no_results")
                    .font(.footnote)
                    .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
                    .accessibilityIdentifier("workout_cat_search_empty")
            }
            ForEach(searchHits) { hit in
                NavigationLink {
                    hit.destination(core: core, profileId: profileId)
                } label: {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(LocalizedStringKey(hit.titleKey))
                            .font(.body)
                        Text(LocalizedStringKey(hit.subtitleKey))
                            .font(.caption)
                            .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
                    }
                }
                .accessibilityIdentifier("workout_cat_search_hit_\(hit.id)")
            }
        }
    }
}

// MARK: - Search index (workouts + programmes)

private struct WorkoutSearchHit: Identifiable {
    let id: String
    let titleKey: String
    let subtitleKey: String
    let category: WorkoutCategory
    let kind: Kind

    enum Kind {
        case readyMade(String)
        case routine(String)
        case category(WorkoutCategory)
        case treadmillRun
        case treadmillWalk
        case zone2
        case yoga
        case pilates
    }

    @ViewBuilder
    func destination(core: OmniGartCore, profileId: String) -> some View {
        switch kind {
        case .readyMade, .routine, .category:
            WorkoutCategoryDetailView(core: core, profileId: profileId, category: category)
        case .treadmillRun:
            TreadmillRunView(core: core, profileId: profileId, initialKind: .run)
        case .treadmillWalk:
            TreadmillRunView(core: core, profileId: profileId, initialKind: .walk)
        case .zone2, .yoga, .pilates:
            WorkoutCategoryDetailView(core: core, profileId: profileId, category: category)
        }
    }
}

private enum WorkoutSearchIndex {
    static func hits(
        core: OmniGartCore,
        profileId: String,
        query: String,
        tick: Int
    ) -> [WorkoutSearchHit] {
        _ = tick
        let q = query.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        if q.isEmpty { return [] }

        var results: [WorkoutSearchHit] = []

        let staticHits: [(String, String, WorkoutCategory, WorkoutSearchHit.Kind)] = [
            ("workouts_zone2_title", "workouts_zone2_body", .running, .zone2),
            ("treadmill_run_title", "treadmill_run_body", .running, .treadmillRun),
            ("treadmill_walk_title", "treadmill_walk_body", .walking, .treadmillWalk),
            ("workout_yoga_title", "workout_mindful_body_intro", .yogaPilates, .yoga),
            ("workout_pilates_title", "workout_mindful_body_intro", .yogaPilates, .pilates),
            ("watch_strength_start", "workouts_program_intro", .strength, .category(.strength)),
            ("watch.workout.strength_traditional", "workouts_log_title", .strength, .category(.strength)),
            ("watch.workout.strength_functional", "workouts_log_title", .strength, .category(.strength)),
            ("explore_title", "explore_open", .strength, .category(.strength)),
            ("equipment_catalogue_open", "workouts_program_intro", .strength, .category(.strength)),
            ("workout_cat_pool_swim", "workout_cat_swimming_intro", .swimming, .category(.swimming)),
            ("workout_cat_open_water", "workout_cat_swimming_intro", .swimming, .category(.swimming)),
            ("workout_cat_outdoor_run", "workouts_zone2_body", .running, .category(.running)),
            ("workout_cat_outdoor_walk", "treadmill_walk_body", .walking, .category(.walking))
        ]
        for (titleKey, subtitleKey, category, kind) in staticHits {
            if matchesLocalized(titleKey, q) || matchesLocalized(subtitleKey, q) {
                results.append(
                    WorkoutSearchHit(
                        id: "static-\(titleKey)",
                        titleKey: titleKey,
                        subtitleKey: subtitleKey,
                        category: category,
                        kind: kind
                    )
                )
            }
        }

        let ready = core.strengthPrograms.readyMadeWorkouts(profileId: profileId)
        for workout in ready {
            if matchesLocalized(workout.nameKey, q) {
                results.append(
                    WorkoutSearchHit(
                        id: "ready-\(workout.id)",
                        titleKey: workout.nameKey,
                        subtitleKey: "workouts_ready_title",
                        category: .strength,
                        kind: .readyMade(workout.id)
                    )
                )
            }
        }

        for category in core.routineLibrary.routineCategories() {
            for routine in core.routineLibrary.routines(category: category, profileId: profileId) {
                if matchesLocalized(routine.nameKey, q) {
                    results.append(
                        WorkoutSearchHit(
                            id: "routine-\(routine.id)",
                            titleKey: routine.nameKey,
                            subtitleKey: "explore_title",
                            category: .strength,
                            kind: .routine(routine.id)
                        )
                    )
                }
            }
        }

        return results
    }

    private static func matchesLocalized(_ key: String, _ query: String) -> Bool {
        let value = String(localized: String.LocalizationValue(key)).lowercased()
        return value.contains(query) || key.lowercased().contains(query)
    }
}

/// Compact muscle region icons for the exercise library (body-map cue).
struct MuscleMapIcons: View {
    let muscles: [String]

    var body: some View {
        HStack(spacing: 2) {
            ForEach(iconNames.prefix(3), id: \.self) { name in
                Image(name)
                    .renderingMode(.template)
                    .resizable()
                    .scaledToFit()
                    .frame(width: 22, height: 22)
                    .foregroundStyle(Color.OmniGart.colorBrandAccent)
            }
        }
        .accessibilityHidden(true)
    }

    private var iconNames: [String] {
        muscles.compactMap { muscle in
            let key = muscle.lowercased()
            if key.contains("chest") || key.contains("pec") { return "muscle-chest" }
            if key.contains("back") || key.contains("lat") { return "muscle-back" }
            if key.contains("shoulder") || key.contains("delt") { return "muscle-shoulders" }
            if key.contains("arm") || key.contains("bicep") || key.contains("tricep") {
                return "muscle-arms"
            }
            if key.contains("core") || key.contains("abs") { return "muscle-core" }
            if key.contains("glute") { return "muscle-glutes" }
            if key.contains("leg") || key.contains("quad") || key.contains("ham") {
                return "muscle-legs"
            }
            return nil
        }
    }
}
