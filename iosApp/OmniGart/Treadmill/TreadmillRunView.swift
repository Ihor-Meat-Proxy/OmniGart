// swiftlint:disable type_body_length
import Shared
import SwiftUI
#if canImport(UIKit)
import UIKit
#endif

/// Phone treadmill run/walk: optional BLE treadmill, programs, chest-strap HR, indoor save.
struct TreadmillRunView: View {
    let core: OmniGartCore
    let profileId: String

    @StateObject private var ble = TreadmillBleManager()
    @StateObject private var chestHr = TreadmillChestStrapHr()
    @State private var sessionKind: TreadmillSessionKindSwift = .run
    @State private var selectedProgramId: String = "easy_run"
    @State private var running = false
    @State private var startedAt: Date?
    @State private var allowControl = false
    @State private var showDiagnostics = false
    @State private var statusMessage: String?
    @State private var calibrationMetres: String = ""
    @State private var programPrompt: String?
    @State private var hrAlertMessage: String?
    @State private var alertsEnabled = true
    @State private var hrMaxText = "190"
    @State private var lastAutoStepIndex: Int = -1

    var body: some View {
        List {
            headerSection
            modeAndProgramSection
            connectionSection
            liveSection
            hrSection
            if allowControl { controlSection }
            sessionSection
            if !ble.live.connected { calibrationSection }
        }
        .navigationTitle(Text(LocalizedStringKey(sessionKind.titleKey)))
        .sheet(isPresented: $showDiagnostics) { diagnosticsSheet }
        .onAppear { ble.start(); chestHr.start() }
        .onDisappear {
            if !running {
                ble.disconnect()
                chestHr.stop()
            }
        }
        .onChange(of: chestHr.bpm) { _, bpm in
            ble.watchHeartRateBpm = bpm
        }
        .onReceive(Timer.publish(every: 1, on: .main, in: .common).autoconnect()) { _ in
            guard running else { return }
            tickProgramAndAlerts()
        }
    }

    private var headerSection: some View {
        Section {
            Text(LocalizedStringKey(
                sessionKind == .walk ? "treadmill_walk_body" : "treadmill_run_body"
            ))
            .font(.footnote)
            .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
            .onLongPressGesture(minimumDuration: 1.2) { showDiagnostics = true }
            if let programPrompt {
                Text(LocalizedStringKey(programPrompt)).font(.callout)
            }
            if let hrAlertMessage {
                Text(LocalizedStringKey(hrAlertMessage))
                    .font(.callout)
                    .foregroundStyle(Color.OmniGart.colorAccent)
            }
        }
    }

    private var modeAndProgramSection: some View {
        Section("treadmill_program_section") {
            Picker("treadmill_mode_title", selection: $sessionKind) {
                Text("treadmill_run_title").tag(TreadmillSessionKindSwift.run)
                Text("treadmill_walk_title").tag(TreadmillSessionKindSwift.walk)
            }
            .pickerStyle(.segmented)
            .disabled(running)
            .onChange(of: sessionKind) { _, kind in
                selectedProgramId = kind == .walk ? "recovery_walk" : "easy_run"
            }
            ForEach(TreadmillProgramSwift.programs(for: sessionKind), id: \.id) { program in
                Button { selectedProgramId = program.id } label: {
                    HStack {
                        Text(LocalizedStringKey(program.titleKey))
                        Spacer()
                        if selectedProgramId == program.id {
                            Image(systemName: "checkmark")
                        }
                    }
                }
                .disabled(running)
            }
            Button("treadmill_export_workoutkit") {
                Task {
                    let outcome = await TreadmillWorkoutKitScheduler.schedule(
                        programId: selectedProgramId,
                        hrMax: Int(hrMaxText) ?? 190
                    )
                    statusMessage = outcome.ok
                        ? String(localized: "treadmill_export_ok")
                        : (outcome.detail ?? String(localized: "treadmill_export_failed"))
                }
            }
        }
    }

    private var connectionSection: some View {
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
            Button("treadmill_scan") { ble.startScan() }
                .accessibilityIdentifier("treadmill_scan")
            Button("treadmill_reconnect") { ble.connectRemembered() }
            ForEach(ble.discovered) { device in
                Button { ble.connect(id: device.id) } label: {
                    VStack(alignment: .leading) {
                        Text(device.name)
                        Text(device.protocolHint)
                            .font(.caption2)
                            .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
                    }
                }
            }
            Toggle("treadmill_allow_control", isOn: $allowControl)
                .onChange(of: allowControl) { _, value in ble.allowRemoteControl = value }
        }
    }

    private var liveSection: some View {
        Section("treadmill_live_title") {
            metric("treadmill_speed", value: ble.live.speedKmh.map { String(format: "%.1f km/h", $0) })
            metric("treadmill_pace", value: ble.live.paceMinPerKm.map { String(format: "%.2f min/km", $0) })
            metric("treadmill_incline", value: ble.live.inclinePercent.map { String(format: "%.1f %%", $0) })
            metric("treadmill_distance", value: ble.live.distanceMetres.map { String(format: "%.0f m", $0) })
            metric("treadmill_hr", value: displayHr.map(String.init))
            metric("treadmill_zone", value: displayZone.map { "Z\($0)" })
            metric("treadmill_calories", value: ble.live.energyKcal.map { String(format: "%.0f kcal", $0) })
        }
    }

    private var hrSection: some View {
        Section("treadmill_hr_settings_title") {
            Toggle("treadmill_hr_alerts_enabled", isOn: $alertsEnabled)
            TextField("treadmill_hr_max", text: $hrMaxText)
                .keyboardType(.numberPad)
            Button("treadmill_hr_strap_scan") { chestHr.startScan() }
            if chestHr.connected {
                Text("treadmill_hr_strap_connected").font(.caption)
            }
        }
    }

    private var controlSection: some View {
        Section("treadmill_control_title") {
            Button("treadmill_speed_up") { ble.setTargetSpeedKmh((ble.live.speedKmh ?? 5) + 0.5) }
            Button("treadmill_speed_down") {
                ble.setTargetSpeedKmh(max(0.5, (ble.live.speedKmh ?? 5) - 0.5))
            }
            Button("treadmill_incline_up") {
                ble.setTargetInclinePercent((ble.live.inclinePercent ?? 0) + 0.5)
            }
            Button("treadmill_incline_down") {
                ble.setTargetInclinePercent((ble.live.inclinePercent ?? 0) - 0.5)
            }
        }
    }

    private var sessionSection: some View {
        Section {
            if !running {
                Button("treadmill_start") {
                    running = true
                    startedAt = Date()
                    lastAutoStepIndex = -1
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
    }

    private var calibrationSection: some View {
        Section("treadmill_calibration_title") {
            Text("treadmill_calibration_body").font(.footnote)
            TextField("treadmill_calibration_metres", text: $calibrationMetres)
                .keyboardType(.decimalPad)
        }
    }

    private var diagnosticsSheet: some View {
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
                        ShareLink(item: url) { Text("treadmill_diagnostics_export") }
                    }
                }
                ToolbarItem(placement: .cancellationAction) {
                    Button("treadmill_diagnostics_close") { showDiagnostics = false }
                }
            }
        }
    }

    private var displayHr: Int? { chestHr.bpm ?? ble.live.heartRateBpm }

    private var displayZone: Int? {
        TreadmillHeartRateZone.zone(bpm: displayHr, hrMax: Int(hrMaxText) ?? 190)
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

    private func tickProgramAndAlerts() {
        guard let startedAt,
              let program = TreadmillProgramSwift.byId(selectedProgramId) else { return }
        let elapsed = Date().timeIntervalSince(startedAt)
        let prompt = TreadmillProgramSwift.prompt(
            program: program,
            elapsedSeconds: elapsed,
            allowRemoteControl: allowControl,
            controlAvailable: ble.controlAvailable
        )
        programPrompt = prompt.step.titleKey
        if prompt.stepIndex != lastAutoStepIndex {
            lastAutoStepIndex = prompt.stepIndex
            if let speed = prompt.autoSpeedKmh {
                ble.setTargetSpeedKmh(speed)
            }
            if let incline = prompt.autoInclinePercent {
                ble.setTargetInclinePercent(incline)
            }
        }
        let alert = TreadmillHrAlertSwift.evaluate(
            bpm: displayHr,
            hrMax: Int(hrMaxText) ?? 190,
            targetZone: prompt.step.hrZone,
            alertsEnabled: alertsEnabled,
            secondsSinceLastSample: chestHr.connected ? chestHr.secondsSinceLastSample : nil
        )
        if alert != hrAlertMessage, alert != nil {
            #if canImport(UIKit)
            UINotificationFeedbackGenerator().notificationOccurred(.warning)
            #endif
        }
        hrAlertMessage = alert
    }

    private func finishAndSave() async {
        let end = Date()
        let start = startedAt ?? end
        running = false
        statusMessage = await TreadmillSessionSave.save(
            .init(
                kind: sessionKind,
                start: start,
                end: end,
                live: ble.live,
                deviceName: ble.discovered.first?.name,
                calibrationMetres: Double(calibrationMetres),
                watchEstimatedMetres: nil,
                core: core,
                programId: selectedProgramId
            )
        )
        _ = profileId
        ble.disconnect()
        chestHr.stop()
    }
}

// swiftlint:enable type_body_length
