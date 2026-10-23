#if DEBUG
import OmniGartSensors
import SwiftUI

/// Hidden under Settings › Developer (DEBUG). Scans BLE, lists GATT, logs raw hex.
struct TreadmillDiagnosticsView: View {
    @State private var central = BleDiagnosticsCentral()
    @State private var sharePayload: String?

    var body: some View {
        List {
            Section {
                Text("treadmill_diag_body")
                    .font(.footnote)
                    .foregroundStyle(Color.OmniGart.colorTextLightSecondary)
                LabeledContent("treadmill_connection", value: central.connection.rawValue)
                if let lastError = central.lastError {
                    Text(lastError)
                        .font(.caption)
                        .foregroundStyle(.red)
                }
            }
            Section("treadmill_actions_title") {
                Button("treadmill_scan") {
                    central.startScan()
                }
                .accessibilityIdentifier("treadmill_diag_scan")
                Button("treadmill_diag_clear") {
                    central.clearLog()
                }
                Button("treadmill_diag_export") {
                    sharePayload = central.exportJSON()
                }
                .accessibilityIdentifier("treadmill_diag_export")
            }
            Section("treadmill_devices_title") {
                ForEach(central.discovered, id: \.id) { device in
                    Button {
                        central.connect(deviceId: device.id)
                    } label: {
                        VStack(alignment: .leading) {
                            Text(device.name ?? "treadmill_unnamed")
                            Text(device.id)
                                .font(.caption2)
                                .foregroundStyle(.secondary)
                        }
                    }
                }
            }
            Section("treadmill_diag_services") {
                ForEach(central.deviceCatalog) { device in
                    VStack(alignment: .leading, spacing: 4) {
                        Text(device.name ?? device.deviceId)
                            .font(.headline)
                        ForEach(device.characteristics, id: \.characteristicUuid) { ch in
                            Text("\(ch.serviceUuid) / \(ch.characteristicUuid)")
                                .font(.caption2.monospaced())
                            Text(ch.properties.joined(separator: ", "))
                                .font(.caption2)
                                .foregroundStyle(.secondary)
                        }
                    }
                }
            }
            Section("treadmill_diag_log") {
                ForEach(central.entries.suffix(200).reversed()) { entry in
                    VStack(alignment: .leading, spacing: 2) {
                        Text(entry.timestamp.formatted(date: .omitted, time: .standard))
                            .font(.caption2)
                        Text(entry.characteristicUuid)
                            .font(.caption2.monospaced())
                        Text(entry.hex)
                            .font(.caption2.monospaced())
                            .textSelection(.enabled)
                    }
                }
            }
        }
        .navigationTitle(Text("treadmill_diag_title"))
        .sheet(isPresented: Binding(
            get: { sharePayload != nil },
            set: { if !$0 { sharePayload = nil } }
        )) {
            if let sharePayload {
                ShareLink(item: sharePayload) {
                    Text("treadmill_diag_share")
                }
                .padding()
            }
        }
        .onDisappear {
            central.disconnect()
            central.stopScan()
        }
    }
}
#endif
