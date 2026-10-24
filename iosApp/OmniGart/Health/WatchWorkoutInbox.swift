import Foundation
import Shared

/// Compatibility façade — workout inbox now lives on [PhoneWatchConnectivityHub].
/// Kept so older call sites and docs still resolve; prefer the hub directly.
enum WatchWorkoutInbox {
    static let shared = WatchWorkoutInboxProxy()

    final class WatchWorkoutInboxProxy {
        func start(core: OmniGartCore) {
            PhoneWatchConnectivityHub.shared.start(core: core)
        }
    }
}
