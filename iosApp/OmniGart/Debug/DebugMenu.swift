import SwiftUI

#if DEBUG
/// Developer-only tooling. Entire type is compiled out of release builds.
struct DebugMenu: View {
    var body: some View {
        Text("DebugMenu — development only")
            .accessibilityIdentifier("debug_menu")
    }
}
#endif
