import SwiftUI
import Shared

@main
struct OmniGartApp: App {
    private let core: OmniGartCore
    @State private var profile: UserProfile
    @State private var showOnboarding: Bool

    init() {
        // HealthKit bridge is constructed for JIT use when FeatureFlags.healthSyncEnabled.
        // Sync UI stays off until the flag is flipped; store remains local-only by default.
        _ = HealthKitPlatformBridge.shared

        let core = OmniGartCore(driverFactory: DatabaseDriverFactory())
        let args = ProcessInfo.processInfo.arguments
        let uiTesting = args.contains("-ui-testing")
        let forceOnboarding = args.contains("-ui-testing-onboarding")

        let profile: UserProfile
        if uiTesting && !forceOnboarding {
            profile = core.ensureUiTestProfile()
        } else if forceOnboarding {
            // Wipe any leftover local/demo profile so onboarding UI tests start clean.
            core.profiles.delete(id: OmniGartCore.companion.LOCAL_PROFILE_ID)
            core.profiles.delete(id: OmniGartCore.companion.DEMO_PROFILE_ID)
            profile = core.ensureDraftProfile()
        } else if core.needsOnboarding() {
            profile = core.ensureDraftProfile()
        } else {
            profile = core.activeProfile()
        }

        self.core = core
        _profile = State(initialValue: profile)
        _showOnboarding = State(
            initialValue: forceOnboarding || (!uiTesting && core.needsOnboarding())
        )
    }

    var body: some Scene {
        WindowGroup {
            Group {
                if showOnboarding {
                    OnboardingFlowView(core: core) { completed in
                        profile = completed
                        showOnboarding = false
                    }
                } else {
                    PhoneRootView(core: core, profileId: profile.id)
                }
            }
        }
    }
}
