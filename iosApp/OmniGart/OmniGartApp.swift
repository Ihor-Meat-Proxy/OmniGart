import SwiftUI
import Shared

@main
struct OmniGartApp: App {
    private let core: OmniGartCore
    private let profileId: String
    private let summary: [String]

    init() {
        // HealthKit bridge is constructed for JIT use when FeatureFlags.healthSyncEnabled.
        // Sync UI stays off until the flag is flipped; store remains local-only by default.
        _ = HealthKitPlatformBridge.shared

        let core = OmniGartCore(driverFactory: DatabaseDriverFactory())
        let profile = core.ensureDemoProfile()
        // Kotlin List<String> exports as Swift [String] on current K/N.
        let lines = core.summaryLines(profile: profile)
        self.core = core
        self.profileId = profile.id
        summary = lines.isEmpty ? ["OmniGart"] : lines
    }

    var body: some Scene {
        WindowGroup {
            PhoneRootView(core: core, profileId: profileId, summaryLines: summary)
        }
    }
}
