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
