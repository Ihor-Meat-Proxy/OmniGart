import Foundation
import Shared
import WatchConnectivity

/// Receives Apple Watch workout transfers via WCSession and stores them in the local health store.
/// Never writes to HealthKit — the watch already saved the HKWorkout.
final class WatchWorkoutInbox: NSObject, WCSessionDelegate, @unchecked Sendable {
    static let shared = WatchWorkoutInbox()

    private static let payloadKey = "omnigart.workout.transfer.json"
    private let stateLock = NSLock()
    private var core: OmniGartCore?

    func start(core: OmniGartCore) {
        stateLock.lock()
        self.core = core
        stateLock.unlock()
        guard WCSession.isSupported() else { return }
        let session = WCSession.default
        session.delegate = self
        session.activate()
    }

    private func ingest(json: String) {
        stateLock.lock()
        let core = self.core
        stateLock.unlock()
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
        // Extract Sendable payload before crossing isolation boundaries.
        let json = userInfo[Self.payloadKey] as? String
        if let json {
            WatchWorkoutInbox.shared.ingest(json: json)
        }
    }

    nonisolated func session(_ session: WCSession, didReceiveMessage message: [String: Any]) {
        let json = message[Self.payloadKey] as? String
        if let json {
            WatchWorkoutInbox.shared.ingest(json: json)
        }
    }

    nonisolated func session(
        _ session: WCSession,
        didReceive file: WCSessionFile
    ) {
        let url = file.fileURL
        guard let data = try? Data(contentsOf: url),
              let json = String(data: data, encoding: .utf8) else { return }
        WatchWorkoutInbox.shared.ingest(json: json)
    }
}
