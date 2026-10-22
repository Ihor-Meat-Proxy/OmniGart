import SwiftUI
import Shared

@main
struct OmniGartWatchApp: App {
    private let summary: [String]

    init() {
        let core = OmniGartCore(driverFactory: DatabaseDriverFactory())
        let profile = core.ensureDemoProfile()
        // Kotlin List<String> exports as Swift [String] on current K/N.
        let lines = core.summaryLines(profile: profile)
        summary = lines.isEmpty ? ["OmniGart"] : lines
    }

    var body: some Scene {
        WindowGroup {
            WatchStartView(summaryLines: summary)
        }
    }
}
