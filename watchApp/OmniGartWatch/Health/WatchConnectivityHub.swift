import Foundation
import Shared
import WatchConnectivity

/// Single WCSession owner on watchOS. Receives phone workout selection + equipment sync;
/// outbound finished-workout transfers still go through [WatchPhoneWorkoutTransfer] helpers
/// that enqueue via this hub.
@MainActor
final class WatchConnectivityHub: NSObject, ObservableObject, WCSessionDelegate {
    static let shared = WatchConnectivityHub()

    @Published private(set) var lastSelection: PhoneWorkoutSelection?
    @Published var enabledEquipmentIds: Set<String> = []

    private var activated = false
    private weak var transfer: WatchPhoneWorkoutTransfer?

    func activate(transfer: WatchPhoneWorkoutTransfer = .shared) {
        self.transfer = transfer
        guard WCSession.isSupported(), !activated else { return }
        activated = true
        let session = WCSession.default
        session.delegate = self
        session.activate()
    }

    func enqueueTransfer(jsonPayload: String) {
        guard WCSession.isSupported() else { return }
        let session = WCSession.default
        let payload: [String: Any] = ["omnigart.workout.transfer.json": jsonPayload]
        if session.activationState == .activated {
            session.transferUserInfo(payload)
        }
    }

    private func applyIncoming(_ dictionary: [String: Any]) {
        if let ids = dictionary["equipmentIds"] as? [String], !ids.isEmpty {
            enabledEquipmentIds = Set(ids)
        }
        let key = PhoneWorkoutSelection.companion.PAYLOAD_KEY
        if let json = dictionary[key] as? String,
           let selection = PhoneWorkoutSelectionCodec.shared.decodeOrNull(utf8: json) {
            lastSelection = selection
            PendingWatchWorkout.shared.apply(selection: selection)
        }
    }

    nonisolated func session(
        _ session: WCSession,
        activationDidCompleteWith activationState: WCSessionActivationState,
        error: Error?
    ) {
        let context = session.receivedApplicationContext
        Task { @MainActor in
            WatchPhoneWorkoutTransfer.shared.markActivated(activationState == .activated)
            self.applyIncoming(context)
        }
    }

    nonisolated func session(_ session: WCSession, didReceiveApplicationContext applicationContext: [String: Any]) {
        Task { @MainActor in
            self.applyIncoming(applicationContext)
        }
    }

    nonisolated func session(_ session: WCSession, didReceiveUserInfo userInfo: [String: Any] = [:]) {
        Task { @MainActor in
            self.applyIncoming(userInfo)
        }
    }

    nonisolated func session(_ session: WCSession, didReceiveMessage message: [String: Any]) {
        Task { @MainActor in
            self.applyIncoming(message)
        }
    }
}
