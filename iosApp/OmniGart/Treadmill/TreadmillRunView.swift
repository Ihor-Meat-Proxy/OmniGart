import Shared
import SwiftUI

/// Live treadmill run: speed/pace, incline, distance, time, HR, calories, cadence.
struct TreadmillRunView: View {
    let core: OmniGartCore
    let profileId: String

    @State private var session = TreadmillRunSession()
    @State private var saveMessage: String?
    @State private var showDevicePicker = false

    var body: some View {
        List {
            Section {
                Text("treadmill_run_body")
                    .font(.footnote)
                    .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
                LabeledContent("treadmill_connection", value: session.connectionLabel)
                LabeledContent("treadmill_protocol", value: session.protocolLabel)
                if let statusMessage = session.statusMessage {
                    Text(statusMessage)
                        .font(.caption)
                        .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
                }
            }

            Section("treadmill_live_title") {
                metricRow("treadmill_metric_speed", "\(session.speedText) km/h")
                metricRow("treadmill_metric_pace", "\(session.paceText) /km")
                metricRow(
                    "treadmill_metric_incline",
                    session.sample.inclinePercent.map { String(format: "%.1f %%", $0) } ?? "—"
                )
                metricRow(
                    "treadmill_metric_distance",
                    session.sample.distanceMetres.map { String(format: "%.2f km", $0 / 1000) } ?? "—"
                )
                metricRow(
                    "treadmill_metric_time",
                    formatElapsed(session.sample.elapsedSeconds)
                )
                metricRow(
                    "treadmill_metric_hr",
                    session.sample.heartRateBpm.map { "\($0) bpm" } ?? "—"
                )
                metricRow(
                    "treadmill_metric_calories",
                    session.sample.totalEnergyKcal.map { String(format: "%.0f kcal", $0) } ?? "—"
                )
                metricRow(
                    "treadmill_metric_cadence",
                    session.sample.cadenceSpm.map { String(format: "%.0f spm", $0) }
                        ?? session.sample.steps.map { "\($0) steps" }
                        ?? "—"
                )
            }

            Section("treadmill_presets_title") {
                Text("treadmill_presets_body")
                    .font(.footnote)
                    .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
                ForEach(presetIds, id: \.self) { id in
                    Button(LocalizedStringKey(presetLabelKey(id))) {
                        session.applyPreset(id: id)
                    }
                    .accessibilityIdentifier("treadmill_preset_\(id)")
                }
            }

            Section("treadmill_actions_title") {
                Button("treadmill_use_simulator") {
                    session.useSimulator()
                }
                .accessibilityIdentifier("treadmill_use_simulator")
                Button("treadmill_scan") {
                    session.startBleScan()
                    showDevicePicker = true
                }
                .accessibilityIdentifier("treadmill_scan")
                Button("treadmill_arm_control") {
                    session.armControl()
                }
                .accessibilityIdentifier("treadmill_arm_control")
                Button(session.isRunning ? "treadmill_stop_belt" : "treadmill_start_belt") {
                    if session.isRunning {
                        session.stopBelt()
                    } else {
                        session.startBelt()
                    }
                }
                .accessibilityIdentifier("treadmill_toggle_belt")
                Button("treadmill_save") {
                    Task { await saveWorkout() }
                }
                .accessibilityIdentifier("treadmill_save")
                if let saveMessage {
                    Text(saveMessage)
                        .font(.caption)
                        .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
                }
            }
        }
        .navigationTitle(Text("treadmill_run_title"))
        .scrollContentBackground(.hidden)
        .background(Color.OmniGart.colorSurfaceLightCanvas.ignoresSafeArea())
        .onAppear {
            session.useSimulator()
        }
        .onDisappear {
            session.stop()
        }
        .sheet(isPresented: $showDevicePicker) {
            NavigationStack {
                List {
                    ForEach(session.discovered, id: \.id) { device in
                        Button {
                            session.connect(deviceId: device.id)
                            showDevicePicker = false
                        } label: {
                            VStack(alignment: .leading) {
                                Text(device.name ?? device.id)
                                Text(device.id)
                                    .font(.caption2)
                                    .foregroundStyle(.secondary)
                            }
                        }
                    }
                }
                .navigationTitle(Text("treadmill_devices_title"))
                .toolbar {
                    ToolbarItem(placement: .cancellationAction) {
                        Button("treadmill_close") { showDevicePicker = false }
                    }
                    ToolbarItem(placement: .primaryAction) {
                        Button("treadmill_refresh") { session.refreshDiscovered() }
                    }
                }
            }
        }
    }

    private var presetIds: [String] {
        (TreadmillBridge.shared.presetIds() as? [String]) ?? [
            "easy", "steady", "tempo", "hill", "walk",
        ]
    }

    private func presetLabelKey(_ id: String) -> String {
        TreadmillBridge.shared.presetLabelKey(id: id) ?? "treadmill_preset_\(id)"
    }

    private func metricRow(_ title: LocalizedStringKey, _ value: String) -> some View {
        HStack {
            Text(title)
            Spacer()
            Text(value)
                .font(.title3.monospacedDigit())
                .foregroundStyle(Color.OmniGart.colorBrandAccent)
        }
    }

    private func formatElapsed(_ seconds: Int?) -> String {
        guard let seconds, seconds >= 0 else { return "—" }
        let h = seconds / 3600
        let m = (seconds % 3600) / 60
        let s = seconds % 60
        if h > 0 {
            return String(format: "%d:%02d:%02d", h, m, s)
        }
        return String(format: "%d:%02d", m, s)
    }

    private func saveWorkout() async {
        let snap = session.finishedSnapshot()
        let localId = "treadmill-\(UUID().uuidString)"
        let avgHr =
            snap.heartRateBpm.isEmpty
            ? nil
            : snap.heartRateBpm.reduce(0, +) / Double(snap.heartRateBpm.count)
        let maxHr = snap.heartRateBpm.max()
        let syncId = core.localHealth.recordOmniGartWorkout(
            localId: localId,
            activityTypeName: "RUNNING",
            startEpochMs: Int64(snap.start.timeIntervalSince1970 * 1000),
            endEpochMs: Int64(snap.end.timeIntervalSince1970 * 1000),
            activeEnergyKcal: snap.activeKcal.map { KotlinDouble(value: $0) },
            basalEnergyKcal: nil,
            distanceMetres: snap.distanceMetres.map { KotlinDouble(value: $0) },
            averageHeartRateBpm: avgHr.map { KotlinDouble(value: $0) },
            maxHeartRateBpm: maxHr.map { KotlinDouble(value: $0) },
            sessionId: localId,
            planId: nil,
            heartRateEpochMs: snap.heartRateEpochMs,
            heartRateBpm: snap.heartRateBpm,
            syncVersion: 1
        )
        do {
            let saved = try await TreadmillHealthKitSave.save(
                snapshot: snap,
                syncIdentifier: syncId
            )
            if let saved {
                core.localHealth.markPlatformLinked(
                    localId: localId,
                    platformRecordId: saved.uuidString
                )
            }
            saveMessage = String(localized: "treadmill_save_ok")
        } catch {
            saveMessage = String(localized: "treadmill_save_local_only")
        }
        session.stopBelt()
    }
}
