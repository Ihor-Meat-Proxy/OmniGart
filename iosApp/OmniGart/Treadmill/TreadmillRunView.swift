import Shared
import SwiftUI

/// Phone Treadmill run: scan/pair, live metrics, optional remote control, save indoor run.
struct TreadmillRunView: View {
    let core: OmniGartCore
    let profileId: String

    @StateObject private var ble = TreadmillBleManager()
    @State private var running = false
    @State private var startedAt: Date?
    @State private var allowControl = false
    @State private var showDiagnostics = false
    @State private var statusMessage: String?
    @State private var calibrationMetres: String = ""
    @State private var watchEstimatedMetres: Double?

    var body: some View {
        List {
            Section {
                Text("treadmill_run_body")
                    .font(.footnote)
                    .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
                    .onLongPressGesture(minimumDuration: 1.2) {
                        showDiagnostics = true
                    }
            }
            Section("treadmill_connection_title") {
                HStack {
                    Text(ble.state.rawValue)
                    Spacer()
                    if ble.live.connected {
                        Text(ble.live.protocolKind.rawValue)
                            .font(.caption)
                            .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
                    }
                }
                Button("treadmill_scan") {
                    ble.startScan()
                }
                .accessibilityIdentifier("treadmill_scan")
                Button("treadmill_reconnect") {
                    ble.connectRemembered()
                }
                ForEach(ble.discovered) { device in
                    Button {
                        ble.connect(id: device.id)
                    } label: {
                        VStack(alignment: .leading) {
                            Text(device.name)
                            Text(device.protocolHint)
                                .font(.caption2)
                                .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
                        }
                    }
                }
                Toggle("treadmill_allow_control", isOn: $allowControl)
                    .onChange(of: allowControl) { _, value in
                        ble.allowRemoteControl = value
                    }
            }
            Section("treadmill_live_title") {
                metric("treadmill_speed", value: ble.live.speedKmh.map { String(format: "%.1f km/h", $0) })
                metric("treadmill_pace", value: ble.live.paceMinPerKm.map { String(format: "%.2f min/km", $0) })
                metric("treadmill_incline", value: ble.live.inclinePercent.map { String(format: "%.1f %%", $0) })
                metric("treadmill_distance", value: ble.live.distanceMetres.map { String(format: "%.0f m", $0) })
                metric("treadmill_hr", value: ble.live.heartRateBpm.map(String.init))
                metric("treadmill_zone", value: ble.live.zone.map { "Z\($0)" })
                metric("treadmill_calories", value: ble.live.energyKcal.map { String(format: "%.0f kcal", $0) })
            }
            if allowControl {
                Section("treadmill_control_title") {
                    Button("treadmill_speed_up") { ble.setTargetSpeedKmh((ble.live.speedKmh ?? 5) + 0.5) }
                    Button("treadmill_speed_down") { ble.setTargetSpeedKmh(max(0.5, (ble.live.speedKmh ?? 5) - 0.5)) }
                    Button("treadmill_incline_up") {
                        ble.setTargetInclinePercent((ble.live.inclinePercent ?? 0) + 0.5)
                    }
                    Button("treadmill_incline_down") {
                        ble.setTargetInclinePercent((ble.live.inclinePercent ?? 0) - 0.5)
                    }
                }
            }
            Section {
                if !running {
                    Button("treadmill_start") {
                        running = true
                        startedAt = Date()
                        ble.connectRemembered()
                    }
                    .accessibilityIdentifier("treadmill_start")
                } else {
                    Button("treadmill_finish") {
                        Task { await finishAndSave() }
                    }
                    .accessibilityIdentifier("treadmill_finish")
                }
                if let statusMessage {
                    Text(statusMessage)
                        .font(.caption)
                        .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
                }
            }
            if !ble.live.connected {
                Section("treadmill_calibration_title") {
                    Text("treadmill_calibration_body")
                        .font(.footnote)
                    TextField("treadmill_calibration_metres", text: $calibrationMetres)
                        .keyboardType(.decimalPad)
                }
            }
        }
        .navigationTitle(Text("treadmill_run_title"))
        .sheet(isPresented: $showDiagnostics) {
            NavigationStack {
                ScrollView {
                    Text(ble.diagnosticsText.isEmpty ? "—" : ble.diagnosticsText)
                        .font(.system(.caption, design: .monospaced))
                        .padding()
                        .textSelection(.enabled)
                }
                .navigationTitle(Text("treadmill_diagnostics_title"))
                .toolbar {
                    ToolbarItem(placement: .confirmationAction) {
                        if let url = ble.exportDiagnosticsURL() {
                            ShareLink(item: url) {
                                Text("treadmill_diagnostics_export")
                            }
                        }
                    }
                    ToolbarItem(placement: .cancellationAction) {
                        Button("treadmill_diagnostics_close") { showDiagnostics = false }
                    }
                }
            }
        }
        .onAppear {
            ble.start()
        }
        .onDisappear {
            if !running {
                ble.disconnect()
            }
        }
    }

    @ViewBuilder
    private func metric(_ key: LocalizedStringKey, value: String?) -> some View {
        HStack {
            Text(key)
            Spacer()
            Text(value ?? "—")
                .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
        }
    }

    private func finishAndSave() async {
        let end = Date()
        let start = startedAt ?? end
        running = false
        var distance = ble.live.distanceMetres
        var usedEstimate = false
        if distance == nil || distance == 0 {
            usedEstimate = true
            if let cal = Double(calibrationMetres), cal > 0 {
                distance = cal
            } else {
                distance = watchEstimatedMetres
            }
        }
        let notes = TreadmillSessionNotesSwift.encode(
            protocolKind: ble.live.protocolKind,
            deviceName: ble.discovered.first?.name,
            distanceMetres: distance,
            averageSpeedKmh: ble.live.speedKmh,
            averageInclinePercent: ble.live.inclinePercent,
            machineAverageHeartRateBpm: ble.live.heartRateBpm.map(Double.init),
            energyKcal: ble.live.energyKcal,
            steps: ble.live.steps,
            usedWatchEstimatedDistance: usedEstimate,
            calibrationFactor: nil
        )
        let localId = UUID().uuidString
        let syncId = core.localHealth.recordOmniGartWorkout(
            localId: localId,
            activityTypeName: "RUNNING",
            startEpochMs: Int64(start.timeIntervalSince1970 * 1000),
            endEpochMs: Int64(end.timeIntervalSince1970 * 1000),
            activeEnergyKcal: ble.live.energyKcal.map { KotlinDouble(value: $0) },
            basalEnergyKcal: nil,
            distanceMetres: distance.map { KotlinDouble(value: $0) },
            averageHeartRateBpm: ble.live.heartRateBpm.map { KotlinDouble(value: Double($0)) },
            maxHeartRateBpm: nil,
            sessionId: nil,
            planId: nil,
            heartRateEpochMs: [],
            heartRateBpm: [],
            syncVersion: 1,
            notes: notes,
            steps: ble.live.steps.map { KotlinLong(value: $0) },
            sourceName: "OmniGart Treadmill"
        )
        do {
            _ = try await HealthKitWorkoutWriter.save(
                options: HealthKitWorkoutWriter.SaveOptions(
                    activityType: .running,
                    start: start,
                    end: end,
                    energy: .init(
                        activeKcal: ble.live.energyKcal ?? 0,
                        basalKcal: 0
                    ),
                    heartRate: [],
                    indoor: true,
                    distanceMetres: distance,
                    syncIdentifier: syncId,
                    syncVersion: 1
                )
            )
            _ = profileId
            statusMessage = String(localized: "treadmill_saved")
        } catch {
            statusMessage = error.localizedDescription
        }
        ble.disconnect()
    }
}
