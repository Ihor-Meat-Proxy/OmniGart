import Shared
import SwiftUI

/// Search hit across workouts and programmes (opens the matching category hub).
struct WorkoutSearchHit: Identifiable {
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
        case .readyMade, .routine, .category, .zone2, .yoga, .pilates:
            WorkoutCategoryDetailView(core: core, profileId: profileId, category: category)
        case .treadmillRun:
            TreadmillRunView(core: core, profileId: profileId, initialKind: .run)
        case .treadmillWalk:
            TreadmillRunView(core: core, profileId: profileId, initialKind: .walk)
        }
    }
}

private struct StaticSearchEntry {
    let titleKey: String
    let subtitleKey: String
    let category: WorkoutCategory
    let kind: WorkoutSearchHit.Kind
}

enum WorkoutSearchIndex {
    static func hits(
        core: OmniGartCore,
        profileId: String,
        query: String,
        tick: Int
    ) -> [WorkoutSearchHit] {
        _ = tick
        let needle = query.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        if needle.isEmpty { return [] }

        var results: [WorkoutSearchHit] = []
        appendStaticHits(query: needle, into: &results)
        appendReadyMade(core: core, profileId: profileId, query: needle, into: &results)
        appendRoutines(core: core, profileId: profileId, query: needle, into: &results)
        return results
    }

    private static let staticEntries: [StaticSearchEntry] = [
        StaticSearchEntry(
            titleKey: "workouts_zone2_title",
            subtitleKey: "workouts_zone2_body",
            category: .running,
            kind: .zone2
        ),
        StaticSearchEntry(
            titleKey: "treadmill_run_title",
            subtitleKey: "treadmill_run_body",
            category: .running,
            kind: .treadmillRun
        ),
        StaticSearchEntry(
            titleKey: "treadmill_walk_title",
            subtitleKey: "treadmill_walk_body",
            category: .walking,
            kind: .treadmillWalk
        ),
        StaticSearchEntry(
            titleKey: "workout_yoga_title",
            subtitleKey: "workout_mindful_body_intro",
            category: .yogaPilates,
            kind: .yoga
        ),
        StaticSearchEntry(
            titleKey: "workout_pilates_title",
            subtitleKey: "workout_mindful_body_intro",
            category: .yogaPilates,
            kind: .pilates
        ),
        StaticSearchEntry(
            titleKey: "watch_strength_start",
            subtitleKey: "workouts_program_intro",
            category: .strength,
            kind: .category(.strength)
        ),
        StaticSearchEntry(
            titleKey: "watch.workout.strength_traditional",
            subtitleKey: "workouts_log_title",
            category: .strength,
            kind: .category(.strength)
        ),
        StaticSearchEntry(
            titleKey: "watch.workout.strength_functional",
            subtitleKey: "workouts_log_title",
            category: .strength,
            kind: .category(.strength)
        ),
        StaticSearchEntry(
            titleKey: "explore_title",
            subtitleKey: "explore_open",
            category: .strength,
            kind: .category(.strength)
        ),
        StaticSearchEntry(
            titleKey: "equipment_catalogue_open",
            subtitleKey: "workouts_program_intro",
            category: .strength,
            kind: .category(.strength)
        ),
        StaticSearchEntry(
            titleKey: "workout_cat_pool_swim",
            subtitleKey: "workout_cat_swimming_intro",
            category: .swimming,
            kind: .category(.swimming)
        ),
        StaticSearchEntry(
            titleKey: "workout_cat_open_water",
            subtitleKey: "workout_cat_swimming_intro",
            category: .swimming,
            kind: .category(.swimming)
        ),
        StaticSearchEntry(
            titleKey: "workout_cat_outdoor_run",
            subtitleKey: "workouts_zone2_body",
            category: .running,
            kind: .category(.running)
        ),
        StaticSearchEntry(
            titleKey: "workout_cat_outdoor_walk",
            subtitleKey: "treadmill_walk_body",
            category: .walking,
            kind: .category(.walking)
        )
    ]

    private static func appendStaticHits(query: String, into results: inout [WorkoutSearchHit]) {
        let matching = staticEntries.filter { entryMatches($0, query: query) }
        for entry in matching {
            results.append(
                WorkoutSearchHit(
                    id: "static-\(entry.titleKey)",
                    titleKey: entry.titleKey,
                    subtitleKey: entry.subtitleKey,
                    category: entry.category,
                    kind: entry.kind
                )
            )
        }
    }

    private static func appendReadyMade(
        core: OmniGartCore,
        profileId: String,
        query: String,
        into results: inout [WorkoutSearchHit]
    ) {
        let workouts = core.strengthPrograms.readyMadeWorkouts(profileId: profileId)
        let matching = workouts.filter { matchesLocalized($0.nameKey, query) }
        for workout in matching {
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

    private static func appendRoutines(
        core: OmniGartCore,
        profileId: String,
        query: String,
        into results: inout [WorkoutSearchHit]
    ) {
        for category in core.routineLibrary.routineCategories() {
            let routines = core.routineLibrary.routines(category: category, profileId: profileId)
            let matching = routines.filter { matchesLocalized($0.nameKey, query) }
            for routine in matching {
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

    private static func entryMatches(_ entry: StaticSearchEntry, query: String) -> Bool {
        matchesLocalized(entry.titleKey, query) || matchesLocalized(entry.subtitleKey, query)
    }

    private static func matchesLocalized(_ key: String, _ query: String) -> Bool {
        let value = String(localized: String.LocalizationValue(key)).lowercased()
        return value.contains(query) || key.lowercased().contains(query)
    }
}
