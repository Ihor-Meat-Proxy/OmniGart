import Shared
import SwiftUI

@main
struct OmniGartWatchApp: App {
    @WKApplicationDelegateAdaptor(WatchAppDelegate.self) private var appDelegate
    private let core: OmniGartCore
    private let profileId: String
    private let summary: [String]

    init() {
        let core = OmniGartCore(driverFactory: DatabaseDriverFactory())
        let profile = core.ensureDemoProfile()
        self.core = core
        profileId = profile.id
        let lines = core.summaryLines(profile: profile)
        summary = lines.isEmpty ? ["OmniGart"] : lines
    }

    var body: some Scene {
        WindowGroup {
            WatchStartView(core: core, profileId: profileId, summaryLines: summary)
        }
    }
}
