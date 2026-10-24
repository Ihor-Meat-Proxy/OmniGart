import Shared
import SwiftUI

/// Exercise library with muscle maps, strength logging, and Zone 2 start.
struct WorkoutsTabView: View {
    let core: OmniGartCore
    let profileId: String

    @State private var zone2Message: String?
    @State private var tick = 0

    private var demos: [DemoStrengthExercise] {
        _ = tick
        return core.strengthLogging.ensureDemoExercises()
    }

    private var profile: UserProfile? {
        _ = tick
        return core.profiles.get(id: profileId)
    }

    var body: some View {
        List {
            Section {
                Text("workout_intro")
                    .font(.footnote)
                    .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
            }
            Section("explore_title") {
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
                if let card = core.routineLibrary.nextWorkoutCard(profileId: profileId) {
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
            StrengthProgramSection(core: core, profileId: profileId)
            Section("workouts_library_title") {
                ForEach(demos, id: \.exercise.id) { demo in
                    NavigationLink {
                        ExerciseDetailView(core: core, profileId: profileId, exerciseId: demo.exercise.id)
                    } label: {
                        HStack(spacing: OmniGartMetrics.space2) {
                            ExerciseIllustrationView(illustrationKey: "ex_\(demo.exercise.id.replacingOccurrences(of: "-", with: "_"))")
                                .frame(width: 40, height: 40)
                            MuscleMapIcons(muscles: muscleList(demo.exercise.primaryMuscles))
                            VStack(alignment: .leading, spacing: 2) {
                                Text(demo.displayName)
                                    .foregroundStyle(Color.primary)
                                Text(muscleList(demo.exercise.primaryMuscles).joined(separator: ", "))
                                    .font(.caption)
                                    .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
                            }
                        }
                    }
                    .accessibilityIdentifier("library_exercise_\(demo.exercise.id)")
                }
            }
            Section("workouts_zone2_title") {
                Text("workouts_zone2_body")
                    .font(.footnote)
                    .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
                Button("workouts_zone2_start") {
                    startZone2()
                }
                .accessibilityIdentifier("workouts_zone2_start")
                if let zone2Message {
                    Text(zone2Message)
                        .font(.caption)
                        .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
                        .accessibilityIdentifier("workouts_zone2_status")
                }
            }
            Section("workouts_log_title") {
                StrengthLoggingView(core: core, profileId: profileId)
            }
            Section("workout_yoga_title") {
                MindfulBodySessionView(core: core, profileId: profileId, kind: .yoga)
            }
            Section("workout_pilates_title") {
                MindfulBodySessionView(core: core, profileId: profileId, kind: .pilates)
            }
        }
        .navigationTitle(Text("tab_workouts"))
        .scrollContentBackground(.hidden)
        .background(Color.OmniGart.colorSurfaceLightCanvas.ignoresSafeArea())
    }

    private func startZone2() {
        let age = profile.map { Int(core.ageYears(profile: $0)) } ?? 30
        let prefs = core.formulaSettings.getSelection(profileId: profileId).toPreferences()
        let hrMax = Int(prefs.hrMax.hrMax(ageYears: Int32(age)))
        let hrRest = 60
        Task { @MainActor in
            let outcome = await Zone2WorkoutKitScheduler.schedule(
                id: "zone2-\(Int(Date().timeIntervalSince1970))",
                hrMax: hrMax,
                hrRest: hrRest,
                workSeconds: 30 * 60
            )
            if outcome.ok {
                zone2Message = String(localized: "workouts_zone2_scheduled")
            } else if FeatureFlags.shared.workoutKitSchedulingEnabled {
                zone2Message = outcome.detail ?? String(localized: "workouts_zone2_unavailable")
            } else {
                zone2Message = String(localized: "workouts_zone2_flag_off")
            }
            tick += 1
        }
    }

    private func muscleList(_ muscles: [String]) -> [String] { muscles }
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
