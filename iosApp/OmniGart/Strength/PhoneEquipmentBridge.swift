import Foundation
import Shared
import WatchConnectivity

/// Publishes the phone equipment catalogue to the paired watch via application context.
/// Does not touch HealthKit session recording.
@MainActor
final class PhoneEquipmentBridge: NSObject, WCSessionDelegate {
    static let shared = PhoneEquipmentBridge()

    private var session: WCSession?

    override private init() {
        super.init()
        guard WCSession.isSupported() else { return }
        let session = WCSession.default
        session.delegate = self
        session.activate()
        self.session = session
    }

    func publish(from profile: UserEquipmentProfile) {
        guard let session, session.activationState == .activated else { return }
        let ids = profile.ownedIds.map { String(describing: $0) }.sorted()
        let payload: [String: Any] = [
            "equipmentIds": ids,
            "preset": String(describing: profile.activePreset)
        ]
        try? session.updateApplicationContext(payload)
    }

    nonisolated func session(
        _ session: WCSession,
        activationDidCompleteWith activationState: WCSessionActivationState,
        error: Error?
    ) {}

    #if os(iOS)
    nonisolated func sessionDidBecomeInactive(_ session: WCSession) {}

    nonisolated func sessionDidDeactivate(_ session: WCSession) {
        session.activate()
    }
    #endif
}
