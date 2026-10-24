import Foundation
import Shared
import WatchConnectivity

/// Single WCSession owner on iPhone. Fans out equipment sync, finished-workout inbox,
/// and phone→watch workout selection so only one `WCSessionDelegate` is registered.
@MainActor
final class PhoneWatchConnectivityHub: NSObject, WCSessionDelegate {
    static let shared = PhoneWatchConnectivityHub()

    private var core: OmniGartCore?
    private var session: WCSession?
    private var lastSelectionJson: String?
    private var lastEquipmentIds: [String] = []
    private var lastEquipmentPreset: String = ""

    override private init() {
        super.init()
    }

    func start(core: OmniGartCore) {
        self.core = core
        guard WCSession.isSupported() else { return }
        let session = WCSession.default
        session.delegate = self
        session.activate()
        self.session = session
    }

    /// Publishes owned equipment ids (guided strength on watch).
    func publishEquipment(from profile: UserEquipmentProfile) {
        lastEquipmentIds = profile.ownedIds.map { String(describing: $0) }.sorted()
        lastEquipmentPreset = String(describing: profile.activePreset)
        pushApplicationContext()
    }

    /// Sends the exact phone selection so the watch starts the matching OmniGart kind / programme.
    /// Prefer `sendMessage` when reachable; always also update application context and queue
    /// `transferUserInfo` so launch works if the watch wakes slightly later.
    func sendWorkoutSelection(_ selection: PhoneWorkoutSelection) {
        let json = PhoneWorkoutSelectionCodec.shared.encode(selection: selection)
        lastSelectionJson = json
        pushApplicationContext()
        guard let session, session.activationState == .activated else { return }
        let payload: [String: Any] = [PhoneWorkoutSelection.companion.PAYLOAD_KEY: json]
        session.transferUserInfo(payload)
        if session.isReachable {
            session.sendMessage(payload, replyHandler: nil, errorHandler: nil)
        }
    }

    private func pushApplicationContext() {
        guard let session, session.activationState == .activated else { return }
        var payload: [String: Any] = [:]
        if !lastEquipmentIds.isEmpty {
            payload["equipmentIds"] = lastEquipmentIds
            payload["preset"] = lastEquipmentPreset
        }
        if let lastSelectionJson {
            payload[PhoneWorkoutSelection.companion.PAYLOAD_KEY] = lastSelectionJson
        }
        guard !payload.isEmpty else { return }
        try? session.updateApplicationContext(payload)
    }

    private func ingestTransfer(json: String) {
        guard let core else { return }
        // Local store only — watch already wrote Apple Health.
        _ = core.localHealth.ingestDeviceTransferJson(utf8: json)
    }

    nonisolated func session(
        _ session: WCSession,
        activationDidCompleteWith activationState: WCSessionActivationState,
        error: Error?
    ) {}

    nonisolated func sessionDidBecomeInactive(_ session: WCSession) {}

    nonisolated func sessionDidDeactivate(_ session: WCSession) {
        session.activate()
    }

    nonisolated func session(_ session: WCSession, didReceiveUserInfo userInfo: [String: Any] = [:]) {
        // Literal matches DeviceWorkoutTransfer.PAYLOAD_KEY (avoid MainActor-isolated static).
        if let json = userInfo["omnigart.workout.transfer.json"] as? String {
            Task { @MainActor in
                PhoneWatchConnectivityHub.shared.ingestTransfer(json: json)
            }
        }
    }

    nonisolated func session(_ session: WCSession, didReceiveMessage message: [String: Any]) {
        if let json = message["omnigart.workout.transfer.json"] as? String {
            Task { @MainActor in
                PhoneWatchConnectivityHub.shared.ingestTransfer(json: json)
            }
        }
    }

    nonisolated func session(_ session: WCSession, didReceive file: WCSessionFile) {
        let url = file.fileURL
        guard let data = try? Data(contentsOf: url),
              let json = String(data: data, encoding: .utf8) else { return }
        Task { @MainActor in
            PhoneWatchConnectivityHub.shared.ingestTransfer(json: json)
        }
    }
}
