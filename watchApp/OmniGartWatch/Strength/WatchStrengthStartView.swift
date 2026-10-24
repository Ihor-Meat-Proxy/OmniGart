import Shared
import SwiftUI
import WatchConnectivity

/// Pre-start strength session: quick equipment toggles synced from iPhone, then regenerate.
struct WatchStrengthStartView: View {
    let core: OmniGartCore
    let profileId: String

    @StateObject private var sync = WatchEquipmentSync.shared
    @State private var tick = 0
    @State private var dayIndex = 0

    private var programs: StrengthProgramService {
        _ = tick
        return core.strengthPrograms
    }

    private var session: GeneratedStrengthSession {
        _ = tick
        let ids = Array(sync.enabledIds)
        return programs.regenerateSession(
            profileId: profileId,
            dayIndex: Int32(dayIndex),
            sessionEquipmentIds: ids
        )
    }

    var body: some View {
        List {
            Section {
                Text("workouts_adjust_equipment")
                    .font(.headline)
                Text("watch_equipment_sync_hint")
                    .font(.caption2)
                    .foregroundStyle(.secondary)
            }
            Section {
                ForEach(sync.catalogueIds, id: \.self) { id in
                    Toggle(isOn: binding(for: id)) {
                        Text(LocalizedStringKey("equipment_item_\(id)"))
                            .font(.caption)
                    }
                    .accessibilityIdentifier("watch_equipment_\(id)")
                }
            }
            Section("workouts_week_title") {
                SessionMusclePreviewWatch(muscles: session.workedMuscles.map { String(describing: $0) })
                ForEach(Array(session.slots.enumerated()), id: \.offset) { _, slot in
                    VStack(alignment: .leading, spacing: 2) {
                        Text(humanize(String(describing: slot.exerciseId)))
                            .font(.caption)
                        Text("\(slot.sets)×\(slot.repsLabel)")
                            .font(.caption2)
                            .foregroundStyle(.secondary)
                    }
                }
            }
        }
        .navigationTitle(Text("tab_workouts"))
        .onAppear {
            sync.activateIfNeeded()
            sync.catalogueIds = programs.catalogueIds().map { String(describing: $0) }
            if sync.enabledIds.isEmpty {
                let owned = programs.equipment(profileId: profileId).ownedIds
                sync.enabledIds = Set(owned.map { String(describing: $0) })
            }
            tick += 1
        }
    }

    private func binding(for id: String) -> Binding<Bool> {
        Binding(
            get: { sync.enabledIds.contains(id) },
            set: { enabled in
                if enabled {
                    sync.enabledIds.insert(id)
                } else {
                    sync.enabledIds.remove(id)
                }
                tick += 1
            }
        )
    }

    private func humanize(_ id: String) -> String {
        id.split(separator: "-").map { part in
            part.prefix(1).uppercased() + part.dropFirst()
        }.joined(separator: " ")
    }
}

struct SessionMusclePreviewWatch: View {
    let muscles: [String]

    var body: some View {
        Text(muscles.prefix(4).joined(separator: ", "))
            .font(.caption2)
            .foregroundStyle(.secondary)
            .accessibilityIdentifier("watch_session_muscles")
    }
}

@MainActor
final class WatchEquipmentSync: NSObject, ObservableObject, WCSessionDelegate {
    static let shared = WatchEquipmentSync()

    @Published var enabledIds: Set<String> = []
    @Published var catalogueIds: [String] = []

    private var activated = false

    func activateIfNeeded() {
        guard WCSession.isSupported(), !activated else { return }
        activated = true
        let session = WCSession.default
        session.delegate = self
        session.activate()
    }

    nonisolated func session(
        _ session: WCSession,
        activationDidCompleteWith activationState: WCSessionActivationState,
        error: Error?
    ) {
        let ids = (session.receivedApplicationContext["equipmentIds"] as? [String]) ?? []
        Task { @MainActor in
            self.apply(ids: ids)
        }
    }

    nonisolated func session(_ session: WCSession, didReceiveApplicationContext applicationContext: [String: Any]) {
        let ids = (applicationContext["equipmentIds"] as? [String]) ?? []
        Task { @MainActor in
            self.apply(ids: ids)
        }
    }

    private func apply(ids: [String]) {
        guard !ids.isEmpty else { return }
        enabledIds = Set(ids)
    }
}
