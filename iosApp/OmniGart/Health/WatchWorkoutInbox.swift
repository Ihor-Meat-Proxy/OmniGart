import Foundation
import Shared
import WatchConnectivity

/// Receives Apple Watch workout transfers via WCSession and stores them in the local health store.
/// Never writes to HealthKit — the watch already saved the HKWorkout.
@MainActor
final class WatchWorkoutInbox: NSObject, WCSessionDelegate {
    static let shared = WatchWorkoutInbox()

    private static let payloadKey = "omnigart.workout.transfer.json"
    private var core: OmniGartCore?

    func start(core: OmniGartCore) {
        self.core = core
        guard WCSession.isSupported() else { return }
        let session = WCSession.default
        session.delegate = self
        session.activate()
    }

    private func ingest(json: String) {
        guard let core else { return }
        // Local store only — do not call HealthKitWorkoutWriter for watch-originated rows.
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
        // Extract a Sendable String before hopping to MainActor (Swift 6).
        let json = userInfo[Self.payloadKey] as? String
        Task { @MainActor in
            if let json { ingest(json: json) }
        }
    }

    nonisolated func session(_ session: WCSession, didReceiveMessage message: [String: Any]) {
        let json = message[Self.payloadKey] as? String
        Task { @MainActor in
            if let json { ingest(json: json) }
        }
    }

    nonisolated func session(
        _ session: WCSession,
        didReceive file: WCSessionFile
    ) {
        let json: String?
        if let data = try? Data(contentsOf: file.fileURL) {
            json = String(data: data, encoding: .utf8)
        } else {
            json = nil
        }
        Task { @MainActor in
            if let json { ingest(json: json) }
        }
    }
}
