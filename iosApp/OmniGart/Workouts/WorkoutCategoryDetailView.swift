import HealthKit
import Shared
import SwiftUI

/// Category hub: start flows, programmes, and recent history (reuses existing screens).
struct WorkoutCategoryDetailView: View {
    let core: OmniGartCore
    let profileId: String
    let category: WorkoutCategory

    @State private var zone2Message: String?
    @State private var tick = 0
    @Environment(\.colorScheme) private var colorScheme

    private var profile: UserProfile? {
        _ = tick
        return core.profiles.get(id: profileId)
    }

    var body: some View {
        List {
            switch category {
            case .running:
                runningContent
            case .strength:
                strengthContent
            case .yogaPilates:
                yogaPilatesContent
            case .swimming:
                swimmingContent
            case .walking:
                walkingContent
            }
            WorkoutCategoryHistorySection(core: core, category: category)
        }
        .navigationTitle(Text(category.titleKey))
        .scrollContentBackground(.hidden)
        .background(canvas.ignoresSafeArea())
        .navigationDestination(for: WorkoutsRoute.self) { route in
            switch route {
            case .treadmill(let kind):
                TreadmillRunView(core: core, profileId: profileId, initialKind: kind)
            }
        }
    }

    private var canvas: Color {
        colorScheme == .dark
            ? Color.OmniGart.colorSurfaceDarkCanvas
            : Color.OmniGart.colorSurfaceLightCanvas
    }

    // MARK: - Running

    @ViewBuilder
    private var runningContent: some View {
        Section("workout_cat_sessions_title") {
            WorkoutWatchStartRow(
                titleKey: "workout_cat_outdoor_run",
                activityType: .running,
                indoor: false,
                accessibilityId: "workout_cat_outdoor_run",
                sessionKeyPrefix: "outdoor-run"
            )
            Button("workouts_zone2_start") { startZone2() }
                .accessibilityIdentifier("workouts_zone2_start")
            if let zone2Message {
                Text(zone2Message)
                    .font(.caption)
                    .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
                    .accessibilityIdentifier("workouts_zone2_status")
            }
            Text("workouts_zone2_body")
                .font(.footnote)
                .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
            NavigationLink("treadmill_run_open", value: WorkoutsRoute.treadmill(.run))
                .accessibilityIdentifier("workouts_treadmill_open")
        }
    }

    // MARK: - Strength

    @ViewBuilder
    private var strengthContent: some View {
        Section("workout_cat_sessions_title") {
            NavigationLink {
                WorkoutsExploreView(core: core, profileId: profileId)
            } label: {
                Text("explore_open")
            }
            .accessibilityIdentifier("workouts_explore_open")
            NavigationLink {
                EquipmentCataloguePickerView(core: core, profileId: profileId) {
                    tick += 1
                }
            } label: {
                Text("equipment_catalogue_open")
            }
            .accessibilityIdentifier("workouts_equipment_catalogue_open")
            WorkoutWatchStartRow(
                titleKey: "watch.workout.strength_traditional",
                activityType: .traditionalStrengthTraining,
                indoor: true,
                accessibilityId: "workout_cat_free_strength",
                kindOverride: PhoneWorkoutKindIds.shared.TRADITIONAL_STRENGTH,
                sessionKeyPrefix: "strength-trad"
            )
            WorkoutWatchStartRow(
                titleKey: "watch.workout.strength_functional",
                activityType: .functionalStrengthTraining,
                indoor: true,
                accessibilityId: "workout_cat_functional",
                kindOverride: PhoneWorkoutKindIds.shared.FUNCTIONAL_STRENGTH,
                sessionKeyPrefix: "strength-func"
            )
        }
        StrengthProgramSection(core: core, profileId: profileId)
        Section("workouts_library_title") {
            ForEach(demos, id: \.exercise.id) { demo in
                NavigationLink {
                    ExerciseDetailView(core: core, profileId: profileId, exerciseId: demo.exercise.id)
                } label: {
                    HStack(spacing: OmniGartMetrics.space2) {
                        ExerciseIllustrationView(
                            illustrationKey: "ex_\(demo.exercise.id.replacingOccurrences(of: "-", with: "_"))"
                        )
                        .frame(width: 40, height: 40)
                        MuscleMapIcons(muscles: demo.exercise.primaryMuscles)
                        VStack(alignment: .leading, spacing: 2) {
                            Text(demo.displayName)
                                .foregroundStyle(Color.primary)
                            Text(demo.exercise.primaryMuscles.joined(separator: ", "))
                            .font(.caption)
                            .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
                        }
                    }
                }
                .accessibilityIdentifier("library_exercise_\(demo.exercise.id)")
            }
        }
        Section("workouts_log_title") {
            StrengthLoggingView(core: core, profileId: profileId)
        }
    }

    private var demos: [DemoStrengthExercise] {
        _ = tick
        return core.strengthLogging.ensureDemoExercises()
    }

    // MARK: - Yoga & Pilates

    @ViewBuilder
    private var yogaPilatesContent: some View {
        Section("workout_yoga_title") {
            MindfulBodySessionView(core: core, profileId: profileId, kind: .yoga)
        }
        Section("workout_pilates_title") {
            MindfulBodySessionView(core: core, profileId: profileId, kind: .pilates)
        }
    }

    // MARK: - Swimming

    @ViewBuilder
    private var swimmingContent: some View {
        Section("workout_cat_sessions_title") {
            Text("workout_cat_swimming_intro")
                .font(.footnote)
                .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
            WorkoutWatchStartRow(
                titleKey: "workout_cat_pool_swim",
                activityType: .swimming,
                indoor: true,
                accessibilityId: "workout_cat_pool_swim",
                sessionKeyPrefix: "pool-swim"
            )
            WorkoutWatchStartRow(
                titleKey: "workout_cat_open_water",
                activityType: .swimming,
                indoor: false,
                accessibilityId: "workout_cat_open_water",
                sessionKeyPrefix: "open-water"
            )
        }
    }

    // MARK: - Walking

    @ViewBuilder
    private var walkingContent: some View {
        Section("workout_cat_sessions_title") {
            WorkoutWatchStartRow(
                titleKey: "workout_cat_outdoor_walk",
                activityType: .walking,
                indoor: false,
                accessibilityId: "workout_cat_outdoor_walk",
                sessionKeyPrefix: "outdoor-walk"
            )
            NavigationLink("treadmill_walk_open", value: WorkoutsRoute.treadmill(.walk))
                .accessibilityIdentifier("workouts_treadmill_walk_open")
        }
    }

    private func startZone2() {
        let age = profile.map { Int(core.ageYears(profile: $0)) } ?? 30
        let prefs = core.formulaSettings.getSelection(profileId: profileId).toPreferences()
        let hrMax = Int(prefs.hrMax.hrMax(ageYears: Int32(age)))
        let hrRest = 60
        Task { @MainActor in
            let watchOutcome = await HealthKitWatchLauncher.startOnWatch(
                activityType: .running,
                indoor: false,
                kindOverride: PhoneWorkoutKindIds.shared.ZONE2_RUN,
                sessionKey: "zone2-\(Int(Date().timeIntervalSince1970))"
            )
            switch watchOutcome {
            case .watchStarted:
                zone2Message = String(localized: "workout_watch_started")
                tick += 1
                return
            case .watchUnavailable:
                zone2Message = String(localized: "workout_watch_phone_only_notice")
            case .skippedByPreference:
                break
            }
            let outcome = await Zone2WorkoutKitScheduler.schedule(
                id: "zone2-\(Int(Date().timeIntervalSince1970))",
                hrMax: hrMax,
                hrRest: hrRest,
                workSeconds: 30 * 60
            )
            if outcome.ok {
                zone2Message = String(localized: "workouts_zone2_scheduled")
            } else if case .watchUnavailable = watchOutcome {
                zone2Message = String(localized: "workout_watch_phone_only_notice")
            } else if FeatureFlags.shared.workoutKitSchedulingEnabled {
                zone2Message = outcome.detail ?? String(localized: "workouts_zone2_unavailable")
            } else {
                zone2Message = String(localized: "workouts_zone2_flag_off")
            }
            tick += 1
        }
    }
}

enum WorkoutsRoute: Hashable {
    case treadmill(TreadmillSessionKindSwift)
}
