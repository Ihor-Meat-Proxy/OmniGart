import SwiftUI

#if DEBUG
/// Developer-only tooling. Entire type is compiled out of release builds.
/// Keep this a flat Group — ProfileTabView already wraps sections in a List;
/// a nested List here breaks tab-bar accessibility for UITests.
struct DebugMenu: View {
    var body: some View {
        Group {
            Text("DebugMenu — development only")
                .accessibilityIdentifier("debug_menu")
            NavigationLink("treadmill_diagnostics_title") {
                TreadmillDiagnosticsDebugView()
            }
        }
    }
}

private struct TreadmillDiagnosticsDebugView: View {
    @StateObject private var ble = TreadmillBleManager()

    var body: some View {
        List {
            Button("treadmill_scan") { ble.startScan() }
            ForEach(ble.discovered) { device in
                Button(device.name) { ble.connect(id: device.id) }
            }
            Text(ble.diagnosticsText.isEmpty ? "—" : ble.diagnosticsText)
                .font(.system(.caption2, design: .monospaced))
                .textSelection(.enabled)
            if let url = ble.exportDiagnosticsURL() {
                ShareLink(item: url) { Text("treadmill_diagnostics_export") }
            }
        }
        .navigationTitle(Text("treadmill_diagnostics_title"))
    }
}
#endif
