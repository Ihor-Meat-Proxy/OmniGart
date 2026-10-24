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

enum WorkoutSearchIndex {
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
        appendStaticHits(query: q, into: &results)
        appendReadyMade(core: core, profileId: profileId, query: q, into: &results)
        appendRoutines(core: core, profileId: profileId, query: q, into: &results)
        return results
    }

    private static func appendStaticHits(query: String, into results: inout [WorkoutSearchHit]) {
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
            if matchesLocalized(titleKey, query) || matchesLocalized(subtitleKey, query) {
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
    }

    private static func appendReadyMade(
        core: OmniGartCore,
        profileId: String,
        query: String,
        into results: inout [WorkoutSearchHit]
    ) {
        for workout in core.strengthPrograms.readyMadeWorkouts(profileId: profileId) {
            if matchesLocalized(workout.nameKey, query) {
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
    }

    private static func appendRoutines(
        core: OmniGartCore,
        profileId: String,
        query: String,
        into results: inout [WorkoutSearchHit]
    ) {
        for category in core.routineLibrary.routineCategories() {
            for routine in core.routineLibrary.routines(category: category, profileId: profileId) {
                if matchesLocalized(routine.nameKey, query) {
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
    }

    private static func matchesLocalized(_ key: String, _ query: String) -> Bool {
        let value = String(localized: String.LocalizationValue(key)).lowercased()
        return value.contains(query) || key.lowercased().contains(query)
    }
}
