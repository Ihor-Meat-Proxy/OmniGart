import Foundation
import Shared

/// Compatibility façade — workout inbox now lives on [PhoneWatchConnectivityHub].
/// Kept so older call sites and docs still resolve; prefer the hub directly.
enum WatchWorkoutInbox {
    @MainActor static let shared = WatchWorkoutInboxProxy()

    @MainActor
    final class WatchWorkoutInboxProxy {
        func start(core: OmniGartCore) {
            PhoneWatchConnectivityHub.shared.start(core: core)
        }
    }
}
