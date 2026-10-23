import Shared
import SwiftUI

/// Planned workouts list and WorkoutKit Zone 2 scheduling.
struct PlannerTabView: View {
    let core: OmniGartCore
    let profileId: String

    @State private var scheduleMessage: String?
    @State private var scheduledStart = Date().addingTimeInterval(3600)
    @State private var tick = 0

    private var planned: [Workout] {
        _ = tick
        return core.workouts.getAll()
    }

    private var profile: UserProfile? {
        core.profiles.get(id: profileId)
    }

    var body: some View {
        List {
            Section {
                Text("planner_intro")
                    .font(.footnote)
                    .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
            }
            Section("planner_planned_title") {
                if planned.isEmpty {
                    Text("planner_empty")
                        .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
                    Button("planner_seed_demo") {
                        seedDemoPlan()
                    }
                    .accessibilityIdentifier("planner_seed_demo")
                } else {
                    ForEach(planned, id: \.id) { workout in
                        VStack(alignment: .leading, spacing: 2) {
                            Text(workout.name)
                            if let notes = workout.notes, !notes.isEmpty {
                                Text(notes)
                                    .font(.caption)
                                    .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
                            }
                        }
                        .accessibilityIdentifier("planner_workout_\(workout.id)")
                    }
                }
            }
            Section("planner_schedule_title") {
                Text("planner_schedule_body")
                    .font(.footnote)
                    .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
                DatePicker(
                    "planner_schedule_when",
                    selection: $scheduledStart,
                    in: Date()...
                )
                .accessibilityIdentifier("planner_schedule_when")
                Button("planner_schedule_zone2") {
                    scheduleZone2()
                }
                .accessibilityIdentifier("planner_schedule_zone2")
                if let scheduleMessage {
                    Text(scheduleMessage)
                        .font(.caption)
                        .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
                        .accessibilityIdentifier("planner_schedule_status")
                }
            }
        }
        .navigationTitle(Text("tab_planner"))
        .scrollContentBackground(.hidden)
        .background(Color.OmniGart.colorSurfaceLightCanvas.ignoresSafeArea())
    }

    private func seedDemoPlan() {
        let workout = Workout(
            id: "plan-zone2",
            name: String(localized: "planner_demo_zone2_name"),
            notes: String(localized: "planner_demo_zone2_notes"),
            exerciseIds: [String]()
        )
        core.workouts.upsert(workout: workout)
        tick += 1
    }

    private func scheduleZone2() {
        let age = profile.map { Int(core.ageYears(profile: $0)) } ?? 30
        let prefs = core.formulaSettings.getSelection(profileId: profileId).toPreferences()
        let hrMax = Int(prefs.hrMax.hrMax(ageYears: Int32(age)))
        Task { @MainActor in
            let outcome = await Zone2WorkoutKitScheduler.schedule(
                id: "plan-zone2-\(Int(scheduledStart.timeIntervalSince1970))",
                hrMax: hrMax,
                hrRest: 60,
                workSeconds: 40 * 60,
                scheduledStart: scheduledStart
            )
            if outcome.ok {
                scheduleMessage = String(localized: "planner_schedule_ok")
            } else if FeatureFlags.shared.workoutKitSchedulingEnabled {
                scheduleMessage = outcome.detail ?? String(localized: "planner_schedule_unavailable")
            } else {
                scheduleMessage = String(localized: "workouts_zone2_flag_off")
            }
        }
    }
}
