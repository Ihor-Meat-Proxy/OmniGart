import SwiftUI

/// Apple Watch treadmill run/walk — indoor HK session + optional BLE treadmill metrics.
struct TreadmillLiveView: View {
    var kind: OmniGartWorkoutKind = .treadmillRun

    @StateObject private var sessionHolder = SessionHolder()
    @StateObject private var ble = TreadmillBleManager()
    @State private var showDiagnostics = false

    var body: some View {
        ScrollView {
            VStack(spacing: OmniGartMetrics.space2) {
                Text(kind.titleKey)
                    .font(.headline)
                    .onLongPressGesture(minimumDuration: 1.2) {
                        showDiagnostics = true
                    }
                Text(
                    ble.live.connected
                        ? ble.live.protocolKind.rawValue
                        : String(localized: "treadmill_no_connection")
                )
                .font(.caption2)
                .foregroundStyle(Color.OmniGart.colorTextLightSecondary)

                metric("treadmill_speed", ble.live.speedKmh.map { String(format: "%.1f", $0) })
                metric("treadmill_pace", ble.live.paceMinPerKm.map { String(format: "%.2f", $0) })
                metric("treadmill_incline", ble.live.inclinePercent.map { String(format: "%.1f%%", $0) })
                metric("treadmill_distance", distanceLabel)
                metric(
                    "treadmill_hr",
                    (ble.live.heartRateBpm ?? sessionHolder.session.heartRateBpm).description
                )
                metric("treadmill_zone", ble.live.zone.map { "Z\($0)" })
                metric(
                    "treadmill_calories",
                    String(format: "%.0f", ble.live.energyKcal ?? sessionHolder.session.activeEnergyKcal)
                )

                if !sessionHolder.session.isRunning {
                    Button("treadmill_start") {
                        Task {
                            ble.watchHeartRateBpm = sessionHolder.session.heartRateBpm
                            ble.connectRemembered()
                            await sessionHolder.session.start()
                        }
                    }
                    .tint(Color.OmniGart.colorBrandAccent)
                    .accessibilityIdentifier("watch_treadmill_start")
                    Button("treadmill_scan") { ble.startScan() }
                    ForEach(ble.discovered.prefix(3)) { device in
                        Button(device.name) { ble.connect(id: device.id) }
                    }
                } else {
                    Button(sessionHolder.session.isPaused ? "treadmill_resume" : "treadmill_pause") {
                        if sessionHolder.session.isPaused {
                            sessionHolder.session.resume()
                        } else {
                            sessionHolder.session.pause()
                        }
                    }
                    Button("treadmill_finish") {
                        Task {
                            _ = await sessionHolder.session.stop()
                            ble.disconnect()
                        }
                    }
                }
                if let err = sessionHolder.session.lastError ?? ble.lastError {
                    Text(err)
                        .font(.caption2)
                        .foregroundStyle(.red)
                }
            }
            .padding(OmniGartMetrics.space2)
        }
        .sheet(isPresented: $showDiagnostics) {
            ScrollView {
                Text(ble.diagnosticsText.isEmpty ? "—" : ble.diagnosticsText)
                    .font(.system(.caption2, design: .monospaced))
            }
        }
        .onAppear {
            sessionHolder.ensure(kind: kind)
            ble.start()
        }
        .onChange(of: sessionHolder.session.heartRateBpm) { _, bpm in
            ble.watchHeartRateBpm = bpm
        }
    }

    private var distanceLabel: String {
        if let metres = ble.live.distanceMetres {
            return String(format: "%.0f m", metres)
        }
        return "—"
    }

    @ViewBuilder
    private func metric(_ key: LocalizedStringKey, _ value: String?) -> some View {
        HStack {
            Text(key).font(.caption2)
            Spacer()
            Text(value ?? "—").font(.caption)
        }
    }
}

@MainActor
private final class SessionHolder: ObservableObject {
    @Published var session = OmniGartLiveWorkoutSession(kind: .treadmillRun)

    func ensure(kind: OmniGartWorkoutKind) {
        if session.kind != kind {
            session = OmniGartLiveWorkoutSession(kind: kind)
        }
    }
}
