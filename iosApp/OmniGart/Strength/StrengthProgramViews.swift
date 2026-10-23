import Shared
import SwiftUI

/// Equipment catalogue, programme picker, weekly view, and session preview for Workouts.
struct StrengthProgramSection: View {
    let core: OmniGartCore
    let profileId: String

    @State private var tick = 0
    @State private var selectedDay = 0
    @State private var showEquipment = false
    @State private var showAdjust = false
    @State private var sessionOverrideIds: [String] = []

    private var programs: StrengthProgramService {
        _ = tick
        return core.strengthPrograms
    }

    private var equipment: UserEquipmentProfile {
        _ = tick
        return programs.equipment(profileId: profileId)
    }

    private var prefs: StrengthProgramPreferences {
        _ = tick
        return programs.programPrefs(profileId: profileId)
    }

    private var program: GeneratedStrengthProgram {
        _ = tick
        if sessionOverrideIds.isEmpty {
            return programs.generateProgram(profileId: profileId)
        }
        return programs.generateProgram(profileId: profileId, sessionEquipmentIds: sessionOverrideIds)
    }

    var body: some View {
        Section("workouts_program_title") {
            Text("workouts_program_intro")
                .font(.footnote)
                .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
            Picker("workouts_program_split", selection: splitBinding) {
                Text("program_split_full_body").tag(ProgramSplit.fullBody)
                Text("program_split_upper_lower").tag(ProgramSplit.upperLower)
                Text("program_split_ppl").tag(ProgramSplit.pushPullLegs)
                Text("program_split_bro").tag(ProgramSplit.broSplit)
            }
            .accessibilityIdentifier("workouts_program_split")
            Picker("workouts_program_days", selection: daysBinding) {
                ForEach(allowedDays, id: \.self) { day in
                    Text("\(day)").tag(day)
                }
            }
            .accessibilityIdentifier("workouts_program_days")
            Button("workouts_equipment_edit") {
                showEquipment = true
            }
            .accessibilityIdentifier("workouts_equipment_edit")
            Text(equipmentSummary)
                .font(.caption)
                .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
        }
        Section("workouts_week_title") {
            Picker("workouts_week_day", selection: $selectedDay) {
                ForEach(Array(program.sessions.enumerated()), id: \.offset) { index, session in
                    Text(LocalizedStringKey(session.titleKey)).tag(index)
                }
            }
            .pickerStyle(.segmented)
            .accessibilityIdentifier("workouts_week_picker")
            if let session = program.sessions[safe: selectedDay] {
                SessionMusclePreview(muscles: session.workedMuscles.map { String(describing: $0) })
                ForEach(Array(session.slots.enumerated()), id: \.offset) { _, slot in
                    VStack(alignment: .leading, spacing: 2) {
                        Text(humanize(String(describing: slot.exerciseId)))
                            .font(.body)
                        Text(slotDetail(slot))
                            .font(.caption)
                            .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
                    }
                    .accessibilityIdentifier("workouts_slot_\(slot.exerciseId)")
                }
                Button("workouts_adjust_equipment") {
                    sessionOverrideIds = ownedIdList(equipment)
                    showAdjust = true
                }
                .accessibilityIdentifier("workouts_adjust_equipment")
            }
        }
        .sheet(isPresented: $showEquipment) {
            NavigationStack {
                EquipmentEditorView(core: core, profileId: profileId, sessionMode: false) {
                    tick += 1
                    PhoneEquipmentBridge.shared.publish(from: programs.equipment(profileId: profileId))
                }
            }
        }
        .sheet(isPresented: $showAdjust) {
            NavigationStack {
                EquipmentEditorView(
                    core: core,
                    profileId: profileId,
                    sessionMode: true,
                    sessionOwned: $sessionOverrideIds
                ) {
                    tick += 1
                }
            }
        }
        .onAppear {
            PhoneEquipmentBridge.shared.publish(from: equipment)
            selectedDay = min(selectedDay, max(program.sessions.count - 1, 0))
        }
    }

    private var allowedDays: [Int] {
        programs.allowedDays(split: prefs.split).map { Int($0) }
    }

    private var splitBinding: Binding<ProgramSplit> {
        Binding(
            get: { prefs.split },
            set: { newValue in
                _ = programs.setSplit(
                    profileId: profileId,
                    split: newValue,
                    daysPerWeek: Int32(prefs.daysPerWeek)
                )
                tick += 1
                selectedDay = 0
            }
        )
    }

    private var daysBinding: Binding<Int> {
        Binding(
            get: { Int(prefs.daysPerWeek) },
            set: { newValue in
                _ = programs.setSplit(
                    profileId: profileId,
                    split: prefs.split,
                    daysPerWeek: Int32(newValue)
                )
                tick += 1
                selectedDay = 0
            }
        )
    }

    private var equipmentSummary: String {
        let ids = ownedIdList(equipment)
        let preview = ids.prefix(4).joined(separator: ", ")
        if ids.count > 4 {
            return "\(preview) +\(ids.count - 4)"
        }
        return preview
    }

    private func ownedIdList(_ profile: UserEquipmentProfile) -> [String] {
        profile.ownedIds.map { String(describing: $0) }.sorted()
    }

    private func slotDetail(_ slot: GeneratedExerciseSlot) -> String {
        var parts = [
            "\(slot.sets)×\(slot.repsLabel)",
            "RIR \(slot.rirLabel)",
            "\(slot.restSeconds)s"
        ]
        if let loadNumber = slot.suggestedLoadKg {
            let load = Double(truncating: loadNumber)
            let rounded = String(format: "%.1f kg", load)
            parts.append(slot.loadIncludesBodyMass ? "~\(rounded)" : rounded)
        }
        if slot.substitutedFromId != nil {
            parts.append(String(localized: "workouts_substituted"))
        }
        return parts.joined(separator: " · ")
    }

    private func humanize(_ id: String) -> String {
        id.split(separator: "-").map { part in
            part.prefix(1).uppercased() + part.dropFirst()
        }.joined(separator: " ")
    }
}

struct SessionMusclePreview: View {
    let muscles: [String]

    var body: some View {
        VStack(alignment: .leading, spacing: OmniGartMetrics.space2) {
            Text("workouts_muscles_title")
                .font(.caption)
                .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
            MuscleMapIcons(muscles: muscles)
            Text(muscles.prefix(6).joined(separator: ", "))
                .font(.caption2)
                .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
                .accessibilityIdentifier("workouts_session_muscles")
        }
    }
}

struct EquipmentEditorView: View {
    let core: OmniGartCore
    let profileId: String
    let sessionMode: Bool
    var sessionOwned: Binding<[String]>?
    let onChange: () -> Void

    @Environment(\.dismiss) private var dismiss
    @State private var tick = 0

    init(
        core: OmniGartCore,
        profileId: String,
        sessionMode: Bool,
        sessionOwned: Binding<[String]>? = nil,
        onChange: @escaping () -> Void
    ) {
        self.core = core
        self.profileId = profileId
        self.sessionMode = sessionMode
        self.sessionOwned = sessionOwned
        self.onChange = onChange
    }

    private var programs: StrengthProgramService { core.strengthPrograms }

    private var owned: Set<String> {
        _ = tick
        if sessionMode, let sessionOwned {
            return Set(sessionOwned.wrappedValue)
        }
        return Set(
            programs.equipment(profileId: profileId).ownedIds.map { String(describing: $0) }
        )
    }

    var body: some View {
        List {
            if !sessionMode {
                Section("workouts_equipment_presets") {
                    Button("equipment_preset_home") {
                        _ = programs.applyPreset(profileId: profileId, kind: .home)
                        tick += 1
                        onChange()
                    }
                    Button("equipment_preset_gym") {
                        _ = programs.applyPreset(profileId: profileId, kind: .gym)
                        tick += 1
                        onChange()
                    }
                    Button("equipment_preset_travel") {
                        _ = programs.applyPreset(profileId: profileId, kind: .travel)
                        tick += 1
                        onChange()
                    }
                    Button("equipment_preset_save_home") {
                        _ = programs.saveNamedPreset(profileId: profileId, label: "Home", kind: .home)
                        tick += 1
                        onChange()
                    }
                }
            }
            Section(sessionMode ? "workouts_adjust_equipment" : "workouts_equipment_title") {
                ForEach(programs.catalogueIds().map { String(describing: $0) }, id: \.self) { id in
                    Toggle(isOn: binding(for: id)) {
                        Text(LocalizedStringKey("equipment_item_\(id)"))
                    }
                    .accessibilityIdentifier("equipment_toggle_\(id)")
                }
            }
        }
        .navigationTitle(Text(sessionMode ? "workouts_adjust_equipment" : "workouts_equipment_title"))
        .toolbar {
            ToolbarItem(placement: .confirmationAction) {
                Button("onboarding_finish") { dismiss() }
            }
        }
    }

    private func binding(for id: String) -> Binding<Bool> {
        Binding(
            get: { owned.contains(id) },
            set: { enabled in
                if sessionMode, let sessionOwned {
                    var next = Set(sessionOwned.wrappedValue)
                    if enabled { next.insert(id) } else { next.remove(id) }
                    sessionOwned.wrappedValue = Array(next).sorted()
                } else {
                    _ = programs.toggleEquipment(profileId: profileId, equipmentId: id, enabled: enabled)
                }
                tick += 1
                onChange()
            }
        )
    }
}

private extension Array {
    subscript(safe index: Int) -> Element? {
        indices.contains(index) ? self[index] : nil
    }
}
