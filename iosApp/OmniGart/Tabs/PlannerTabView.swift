import Shared
import SwiftUI

/// Planner: week/month calendar, suggested scheme, adaptive on-device suggestions.
struct PlannerTabView: View {
    let core: OmniGartCore
    let profileId: String

    @State private var selectedDateIso: String = ""
    @State private var tick = 0
    @State private var scheduleMessage: String?
    @State private var scheduledStart = Date().addingTimeInterval(3600)

    private var planner: PlannerService { core.planner }

    private var calendar: PlannerCalendarSnapshot {
        _ = tick
        let iso = selectedDateIso.isEmpty ? planner.todayIso() : selectedDateIso
        return planner.calendar(profileId: profileId, selectedDateIso: iso)
    }

    private var suggestions: [AdaptiveSuggestion] {
        _ = tick
        return planner.suggestions(profileId: profileId).suggestions
    }

    private var profile: UserProfile? {
        core.profiles.get(id: profileId)
    }

    private var weekModels: [PlannerDayCellModel] {
        calendar.week.map {
            PlannerDayCellModel(
                dateIso: $0.dateIso,
                dayOfMonth: $0.dayOfMonth,
                inMonth: $0.inMonth,
                hasPlanned: $0.hasPlanned,
                hasCompleted: $0.hasCompleted
            )
        }
    }

    private var monthModels: [PlannerDayCellModel] {
        calendar.month.map {
            PlannerDayCellModel(
                dateIso: $0.dateIso,
                dayOfMonth: $0.dayOfMonth,
                inMonth: $0.inMonth,
                hasPlanned: $0.hasPlanned,
                hasCompleted: $0.hasCompleted
            )
        }
    }

    var body: some View {
        List {
            introSection
            calendarSections
            selectedDaySection
            schemeSection
            suggestionsSection
            PlannerScheduleSection(
                scheduledStart: $scheduledStart,
                scheduleMessage: scheduleMessage,
                onSchedule: scheduleZone2
            )
        }
        .navigationTitle(Text("tab_planner"))
        .scrollContentBackground(.hidden)
        .background(Color.OmniGart.colorSurfaceLightCanvas.ignoresSafeArea())
        .onAppear {
            if selectedDateIso.isEmpty {
                selectedDateIso = planner.todayIso()
            }
        }
    }

    private var introSection: some View {
        Section {
            Text("planner_intro")
                .font(.footnote)
                .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
            Text("planner_wellness_note")
                .font(.caption2)
                .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
        }
    }

    private var calendarSections: some View {
        Group {
            Section("planner_calendar_week") {
                PlannerWeekStripView(
                    days: weekModels,
                    selectedDateIso: calendar.selectedDateIso,
                    onSelect: selectDay
                )
                .accessibilityIdentifier("planner_week_strip")
            }
            Section("planner_calendar_month") {
                PlannerMonthGridView(
                    days: monthModels,
                    selectedDateIso: calendar.selectedDateIso,
                    onSelect: selectDay
                )
                .accessibilityIdentifier("planner_month_grid")
            }
        }
    }

    private var selectedDaySection: some View {
        Section {
            Text("\(String(localized: "planner_selected_day")): \(calendar.selectedDateIso)")
                .font(.footnote)
                .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
            if calendar.completedCountForSelected > 0 {
                Text("planner_completed_marker")
                    .font(.caption)
                    .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
            }
            if calendar.plannedForSelected.isEmpty {
                Text("planner_empty")
                    .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
            } else {
                ForEach(calendar.plannedForSelected, id: \.id) { workout in
                    plannedRow(workout)
                }
            }
            addSessionMenu
        }
    }

    private var addSessionMenu: some View {
        Menu {
            Button("watch_strength_start") { addKind(.guidedStrength) }
            Button("wear_strength_start") { addKind(.freeStrength) }
            Button("planner_kind_functional") { addKind(.functional) }
            Button("planner_kind_zone2") { addKind(.zone2) }
            Button("planner_kind_run") { addKind(.run) }
            Button("planner_kind_treadmill") { addKind(.treadmill) }
        } label: {
            Text("planner_add_session")
        }
        .accessibilityIdentifier("planner_add_session")
    }

    private var schemeSection: some View {
        Section("planner_scheme_title") {
            Text("planner_scheme_replace_hint")
                .font(.footnote)
                .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
            Button("planner_scheme_generate") { generateScheme() }
                .accessibilityIdentifier("planner_scheme_generate")
        }
    }

    @ViewBuilder
    private var suggestionsSection: some View {
        if !suggestions.isEmpty {
            Section("planner_suggestions_title") {
                ForEach(suggestions, id: \.id) { suggestion in
                    PlannerSuggestionRow(
                        suggestion: suggestion,
                        onAccept: {
                            _ = planner.acceptSuggestion(profileId: profileId, suggestion: suggestion)
                            tick += 1
                        },
                        onDismiss: {
                            planner.dismissSuggestion(suggestion: suggestion)
                            tick += 1
                        }
                    )
                }
            }
        }
    }

    @ViewBuilder
    private func plannedRow(_ workout: PlannedWorkout) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(LocalizedStringKey(workout.titleKey))
            Text(workout.kind.name)
                .font(.caption2)
                .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
            HStack {
                Button("planner_move_tomorrow") {
                    let next = PlannerDateSupport.nextDay(iso: workout.scheduledDateIso)
                    _ = planner.movePlanned(id: workout.id, newDateIso: next)
                    tick += 1
                }
                .font(.caption)
                Button("planner_delete", role: .destructive) {
                    planner.deletePlanned(id: workout.id)
                    tick += 1
                }
                .font(.caption)
            }
        }
        .accessibilityIdentifier("planner_workout_\(workout.id)")
    }

    private func selectDay(_ iso: String) {
        selectedDateIso = iso
        tick += 1
    }

    private func addKind(_ kind: PlannerSessionKind) {
        _ = planner.addSession(
            profileId: profileId,
            dateIso: calendar.selectedDateIso,
            kind: kind,
            intensity: kind == .zone2 || kind == .rest ? .easy : .moderate
        )
        tick += 1
    }

    private func generateScheme() {
        let weekStart = planner.mondayOfWeekContaining(dateIso: calendar.selectedDateIso)
        let goal = profile?.primaryGoal.name ?? "GENERAL_FITNESS"
        let options = GenerateSchemeOptions(
            weekStartIso: weekStart,
            availableDaysPerWeek: 4,
            primaryGoal: goal,
            split: .mixed,
            includeRuns: true,
            includeTreadmill: false,
            includeZone2: true,
            hasStrengthEquipment: true,
            replaceWeek: true
        )
        _ = planner.generateAndPersistScheme(profileId: profileId, options: options)
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
