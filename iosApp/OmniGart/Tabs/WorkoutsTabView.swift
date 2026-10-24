import Shared
import SwiftUI

/// Exercise library with muscle maps, strength logging, and Zone 2 start.
struct WorkoutsTabView: View {
    let core: OmniGartCore
    let profileId: String

    @State private var selectedExerciseId: String?
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
            Section("workouts_library_title") {
                ForEach(demos, id: \.exercise.id) { demo in
                    Button {
                        selectedExerciseId = demo.exercise.id
                        tick += 1
                    } label: {
                        HStack(spacing: OmniGartMetrics.space2) {
                            MuscleMapIcons(muscles: muscleList(demo.exercise.primaryMuscles))
                            VStack(alignment: .leading, spacing: 2) {
                                Text(demo.displayName)
                                    .foregroundStyle(Color.primary)
                                Text(muscleList(demo.exercise.primaryMuscles).joined(separator: ", "))
                                    .font(.caption)
                                    .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
                            }
                            Spacer()
                            if selectedExerciseId == demo.exercise.id {
                                Image(systemName: "checkmark.circle.fill")
                                    .foregroundStyle(Color.OmniGart.colorBrandAccent)
                            }
                        }
                    }
                    .buttonStyle(.plain)
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
            Section("treadmill_run_title") {
                // Lazy destination: avoid constructing TreadmillRunView (and its BLE
                // managers) while the Workouts tab is merely listed in the TabView.
                NavigationLink("treadmill_run_open", value: WorkoutsRoute.treadmill)
                    .accessibilityIdentifier("workouts_treadmill_open")
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
        .navigationDestination(for: WorkoutsRoute.self) { route in
            switch route {
            case .treadmill:
                TreadmillRunView(core: core, profileId: profileId)
            }
        }
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

private enum WorkoutsRoute: Hashable {
    case treadmill
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
