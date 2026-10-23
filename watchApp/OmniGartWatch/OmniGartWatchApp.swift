import SwiftUI
import Shared

@main
struct OmniGartWatchApp: App {
    @WKApplicationDelegateAdaptor(WatchAppDelegate.self) private var appDelegate
    private let summary: [String]

    init() {
        let core = OmniGartCore(driverFactory: DatabaseDriverFactory())
        let profile = core.ensureDemoProfile()
        let lines = core.summaryLines(profile: profile)
        summary = lines.isEmpty ? ["OmniGart"] : lines
    }

    var body: some Scene {
        WindowGroup {
            WatchStartView(summaryLines: summary)
        }
    }
}
