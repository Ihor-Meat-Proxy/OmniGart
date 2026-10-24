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

    var body: some View {
        List {
            Section {
                Text("planner_intro")
                    .font(.footnote)
                    .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
                Text("planner_wellness_note")
                    .font(.caption2)
                    .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
            }

            Section("planner_calendar_week") {
                weekStrip
                    .accessibilityIdentifier("planner_week_strip")
            }

            Section("planner_calendar_month") {
                monthGrid
                    .accessibilityIdentifier("planner_month_grid")
            }

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
                Menu {
                    Button("watch_strength_start") {
                        addKind(PlannerSessionKind.guidedStrength)
                    }
                    Button("wear_strength_start") {
                        addKind(PlannerSessionKind.freeStrength)
                    }
                    Button("planner_kind_functional") {
                        addKind(PlannerSessionKind.functional)
                    }
                    Button("planner_kind_zone2") {
                        addKind(PlannerSessionKind.zone2)
                    }
                    Button("planner_kind_run") {
                        addKind(PlannerSessionKind.run)
                    }
                    Button("planner_kind_treadmill") {
                        addKind(PlannerSessionKind.treadmill)
                    }
                } label: {
                    Text("planner_add_session")
                }
                .accessibilityIdentifier("planner_add_session")
            }

            Section("planner_scheme_title") {
                Text("planner_scheme_replace_hint")
                    .font(.footnote)
                    .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
                Button("planner_scheme_generate") {
                    generateScheme()
                }
                .accessibilityIdentifier("planner_scheme_generate")
            }

            if !suggestions.isEmpty {
                Section("planner_suggestions_title") {
                    ForEach(suggestions, id: \.id) { suggestion in
                        suggestionRow(suggestion)
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
        .onAppear {
            if selectedDateIso.isEmpty {
                selectedDateIso = planner.todayIso()
            }
        }
    }

    private var weekStrip: some View {
        HStack(spacing: 4) {
            ForEach(calendar.week, id: \.dateIso) { day in
                dayCell(
                    dateIso: day.dateIso,
                    dayOfMonth: day.dayOfMonth,
                    inMonth: day.inMonth,
                    hasPlanned: day.hasPlanned,
                    hasCompleted: day.hasCompleted,
                    compact: true
                )
            }
        }
    }

    private var monthGrid: some View {
        let columns = Array(repeating: GridItem(.flexible(), spacing: 4), count: 7)
        return LazyVGrid(columns: columns, spacing: 6) {
            ForEach(calendar.month, id: \.dateIso) { day in
                dayCell(
                    dateIso: day.dateIso,
                    dayOfMonth: day.dayOfMonth,
                    inMonth: day.inMonth,
                    hasPlanned: day.hasPlanned,
                    hasCompleted: day.hasCompleted,
                    compact: false
                )
            }
        }
    }

    private func dayCell(
        dateIso: String,
        dayOfMonth: Int32,
        inMonth: Bool,
        hasPlanned: Bool,
        hasCompleted: Bool,
        compact: Bool
    ) -> some View {
        let selected = dateIso == calendar.selectedDateIso
        return Button {
            selectedDateIso = dateIso
            tick += 1
        } label: {
            VStack(spacing: 2) {
                Text("\(dayOfMonth)")
                    .font(compact ? .caption.weight(.semibold) : .footnote.weight(.medium))
                    .foregroundStyle(
                        inMonth
                            ? Color.OmniGart.colorTextLightPrimary
                            : Color.OmniGart.colorTextLightSecondary.opacity(0.45)
                    )
                HStack(spacing: 2) {
                    if hasPlanned {
                        Circle()
                            .fill(Color.OmniGart.colorBrandAccent)
                            .frame(width: 5, height: 5)
                    }
                    if hasCompleted {
                        Circle()
                            .fill(Color.OmniGart.colorReadinessEasy)
                            .frame(width: 5, height: 5)
                    }
                }
                .frame(height: 6)
            }
            .frame(maxWidth: .infinity, minHeight: compact ? 36 : 40)
            .background(
                RoundedRectangle(cornerRadius: 8)
                    .fill(selected ? Color.OmniGart.colorBrandAccentMuted : Color.clear)
            )
        }
        .buttonStyle(.plain)
        .accessibilityIdentifier("planner_day_\(dateIso)")
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
                    let next = nextDay(iso: workout.scheduledDateIso)
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

    @ViewBuilder
    private func suggestionRow(_ suggestion: AdaptiveSuggestion) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(LocalizedStringKey(suggestion.reasonKey))
                .font(.subheadline)
            Text(actionLabel(suggestion))
                .font(.caption)
                .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
            HStack {
                Button("planner_suggest_accept") {
                    _ = planner.acceptSuggestion(profileId: profileId, suggestion: suggestion)
                    tick += 1
                }
                .accessibilityIdentifier("planner_suggest_accept_\(suggestion.id)")
                Button("planner_suggest_dismiss") {
                    planner.dismissSuggestion(suggestion: suggestion)
                    tick += 1
                }
                .accessibilityIdentifier("planner_suggest_dismiss_\(suggestion.id)")
            }
            .font(.caption)
        }
    }

    private func actionLabel(_ suggestion: AdaptiveSuggestion) -> String {
        switch suggestion.action {
        case .move:
            let date = suggestion.proposedDateIso ?? ""
            let base = String(localized: "planner_suggest_action_move")
            return date.isEmpty ? base : "\(base) (\(date))"
        case .swap:
            return String(localized: "planner_suggest_action_swap")
        case .skip:
            return String(localized: "planner_suggest_action_skip")
        default:
            return suggestion.action.name
        }
    }

    private func addKind(_ kind: PlannerSessionKind) {
        let iso = calendar.selectedDateIso
        _ = planner.addSession(
            profileId: profileId,
            dateIso: iso,
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

    private func nextDay(iso: String) -> String {
        guard let date = isoDate(iso) else { return iso }
        return isoString(date.addingTimeInterval(86_400))
    }

    private func isoDate(_ iso: String) -> Date? {
        let f = DateFormatter()
        f.calendar = Calendar(identifier: .gregorian)
        f.locale = Locale(identifier: "en_GB_POSIX")
        f.timeZone = TimeZone.current
        f.dateFormat = "yyyy-MM-dd"
        return f.date(from: iso)
    }

    private func isoString(_ date: Date) -> String {
        let f = DateFormatter()
        f.calendar = Calendar(identifier: .gregorian)
        f.locale = Locale(identifier: "en_GB_POSIX")
        f.timeZone = TimeZone.current
        f.dateFormat = "yyyy-MM-dd"
        return f.string(from: date)
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
