import SwiftUI

#if DEBUG
/// Developer-only tooling. Entire type is compiled out of release builds.
struct DebugMenu: View {
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("debug_menu_title")
                .font(.headline)
                .accessibilityIdentifier("debug_menu")
            Text("debug_menu_developer_section")
                .font(.subheadline)
                .foregroundStyle(.secondary)
            NavigationLink {
                TreadmillDiagnosticsView()
            } label: {
                Text("treadmill_diag_title")
            }
            .accessibilityIdentifier("treadmill_diagnostics_link")
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}
#endif
