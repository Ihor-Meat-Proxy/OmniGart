import Foundation
import Shared
import WatchConnectivity

/// Single WCSession owner on watchOS. Receives phone workout selection + equipment sync;
/// outbound finished-workout transfers still go through [WatchPhoneWorkoutTransfer] helpers
/// that enqueue via this hub.
@MainActor
final class WatchConnectivityHub: NSObject, ObservableObject, WCSessionDelegate {
    static let shared = WatchConnectivityHub()

    private static let selectionPayloadKey = "omnigart.workout.selection.json"

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

    private func applyIncoming(equipmentIds: [String]?, selectionJson: String?) {
        if let equipmentIds, !equipmentIds.isEmpty {
            enabledEquipmentIds = Set(equipmentIds)
        }
        if let selectionJson,
           let selection = PhoneWorkoutSelectionCodec.shared.decodeOrNull(utf8: selectionJson) {
            lastSelection = selection
            PendingWatchWorkout.shared.apply(selection: selection)
        }
    }

    /// Pull Sendable fields off WCSession dictionaries before hopping to the main actor.
    private nonisolated static func extract(_ dictionary: [String: Any]) -> (ids: [String]?, json: String?) {
        let ids = dictionary["equipmentIds"] as? [String]
        let json = dictionary[selectionPayloadKey] as? String
        return (ids, json)
    }

    nonisolated func session(
        _ session: WCSession,
        activationDidCompleteWith activationState: WCSessionActivationState,
        error: Error?
    ) {
        let extracted = Self.extract(session.receivedApplicationContext)
        Task { @MainActor in
            WatchPhoneWorkoutTransfer.shared.markActivated(activationState == .activated)
            self.applyIncoming(equipmentIds: extracted.ids, selectionJson: extracted.json)
        }
    }

    nonisolated func session(_ session: WCSession, didReceiveApplicationContext applicationContext: [String: Any]) {
        let extracted = Self.extract(applicationContext)
        Task { @MainActor in
            self.applyIncoming(equipmentIds: extracted.ids, selectionJson: extracted.json)
        }
    }

    nonisolated func session(_ session: WCSession, didReceiveUserInfo userInfo: [String: Any] = [:]) {
        let extracted = Self.extract(userInfo)
        Task { @MainActor in
            self.applyIncoming(equipmentIds: extracted.ids, selectionJson: extracted.json)
        }
    }

    nonisolated func session(_ session: WCSession, didReceiveMessage message: [String: Any]) {
        let extracted = Self.extract(message)
        Task { @MainActor in
            self.applyIncoming(equipmentIds: extracted.ids, selectionJson: extracted.json)
        }
    }
}
