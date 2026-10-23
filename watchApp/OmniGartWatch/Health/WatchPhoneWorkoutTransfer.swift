import Foundation
import WatchConnectivity

/// Queues finished watch workouts to the iPhone via WCSession (reliable delivery).
/// Payload is OmniGart device-transfer JSON; the phone stores locally and must not re-write HealthKit.
@MainActor
final class WatchPhoneWorkoutTransfer: NSObject, WCSessionDelegate {
    static let shared = WatchPhoneWorkoutTransfer()

    private let payloadKey = "omnigart.workout.transfer.json"
    private var activated = false

    func activate() {
        guard WCSession.isSupported() else { return }
        let session = WCSession.default
        session.delegate = self
        session.activate()
    }

    /// Queue a transfer. Prefer `transferUserInfo` (queued when phone unreachable); fall back to file for large payloads.
    func enqueue(jsonPayload: String) {
        guard WCSession.isSupported() else { return }
        let session = WCSession.default
        if !activated {
            session.delegate = self
            session.activate()
        }
        let data = Data(jsonPayload.utf8)
        // Keep userInfo for compact workouts; use transferFile when HR series is large.
        if data.count < 50_000 {
            session.transferUserInfo([payloadKey: jsonPayload])
        } else {
            let url = FileManager.default.temporaryDirectory
                .appendingPathComponent("omnigart-watch-transfer-\(UUID().uuidString).json")
            do {
                try data.write(to: url, options: .atomic)
                session.transferFile(url, metadata: [payloadKey: "file"])
            } catch {
                // Last resort: still try userInfo (may truncate on extreme payloads).
                session.transferUserInfo([payloadKey: jsonPayload])
            }
        }
    }

    nonisolated func session(
        _ session: WCSession,
        activationDidCompleteWith activationState: WCSessionActivationState,
        error: Error?
    ) {
        Task { @MainActor in
            activated = activationState == .activated
        }
    }
}

/// Builds device-transfer JSON from a finished watch workout and enqueues WCSession delivery.
enum WatchWorkoutTransferBuilder {
    static func activityTypeName(for kind: OmniGartWorkoutKind) -> String {
        switch kind {
        case .traditionalStrength, .functionalStrength: return "STRENGTH"
        case .yoga: return "YOGA"
        case .pilates: return "PILATES"
        case .zone2Run, .treadmillRun: return "RUNNING"
        }
    }

    static func enqueueLiveSummary(_ summary: OmniGartLiveWorkoutSummary, localId: String, syncIdentifier: String) {
        let startMs = Int64(summary.startedAt.timeIntervalSince1970 * 1000)
        let json = makeJSON(
            localId: localId,
            syncIdentifier: syncIdentifier,
            activityTypeName: activityTypeName(for: summary.kind),
            startEpochMs: startMs,
            endEpochMs: Int64(summary.endedAt.timeIntervalSince1970 * 1000),
            activeEnergyKcal: summary.activeEnergyKcal,
            basalEnergyKcal: summary.basalEnergyKcal,
            distanceMetres: nil,
            averageHeartRateBpm: summary.averageHeartRateBpm,
            maxHeartRateBpm: summary.maxHeartRateBpm,
            averageCadenceSpm: nil,
            samples: summary.heartRateSamples,
            sessionStart: summary.startedAt
        )
        WatchPhoneWorkoutTransfer.shared.enqueue(jsonPayload: json)
    }

    static func enqueueZone2Summary(
        _ summary: Zone2Summary,
        localId: String,
        syncIdentifier: String,
        startedAt: Date,
        endedAt: Date
    ) {
        let startMs = Int64(startedAt.timeIntervalSince1970 * 1000)
        let json = makeJSON(
            localId: localId,
            syncIdentifier: syncIdentifier,
            activityTypeName: "RUNNING",
            startEpochMs: startMs,
            endEpochMs: Int64(endedAt.timeIntervalSince1970 * 1000),
            activeEnergyKcal: summary.activeEnergyKcal,
            basalEnergyKcal: summary.basalEnergyKcal,
            distanceMetres: summary.distanceMetres,
            averageHeartRateBpm: summary.averageHeartRateBpm,
            maxHeartRateBpm: summary.maxHeartRateBpm,
            averageCadenceSpm: summary.averageCadenceSpm,
            samples: summary.heartRateSamples,
            sessionStart: startedAt
        )
        WatchPhoneWorkoutTransfer.shared.enqueue(jsonPayload: json)
    }

    private static func makeJSON(
        localId: String,
        syncIdentifier: String,
        activityTypeName: String,
        startEpochMs: Int64,
        endEpochMs: Int64,
        activeEnergyKcal: Double,
        basalEnergyKcal: Double,
        distanceMetres: Double?,
        averageHeartRateBpm: Double,
        maxHeartRateBpm: Double,
        averageCadenceSpm: Double?,
        samples: [Zone2HeartRateSample],
        sessionStart: Date
    ) -> String {
        func esc(_ s: String) -> String {
            s.replacingOccurrences(of: "\\", with: "\\\\")
                .replacingOccurrences(of: "\"", with: "\\\"")
        }
        let hrEpoch = samples.map { sample -> Int64 in
            startEpochMs + Int64(sample.offsetSeconds * 1000)
        }
        let hrBpm = samples.map { Double($0.bpm) }
        let hrEpochJson = hrEpoch.map(String.init).joined(separator: ",")
        let hrBpmJson = hrBpm.map { String(format: "%.1f", $0) }.joined(separator: ",")
        var fields: [String] = [
            "\"schema\":\"omnigart.device.workout.transfer\"",
            "\"schemaVersion\":1",
            "\"localId\":\"\(esc(localId))\"",
            "\"syncIdentifier\":\"\(esc(syncIdentifier))\"",
            "\"syncVersion\":1",
            "\"activityTypeName\":\"\(esc(activityTypeName))\"",
            "\"startEpochMs\":\(startEpochMs)",
            "\"endEpochMs\":\(endEpochMs)",
            "\"activeEnergyKcal\":\(activeEnergyKcal)",
            "\"basalEnergyKcal\":\(basalEnergyKcal)",
            "\"averageHeartRateBpm\":\(averageHeartRateBpm)",
            "\"maxHeartRateBpm\":\(maxHeartRateBpm)",
            "\"heartRateEpochMs\":[\(hrEpochJson)]",
            "\"heartRateBpm\":[\(hrBpmJson)]",
            "\"sourceDevice\":\"APPLE_WATCH\"",
            "\"sourceBundle\":\"com.omnigart\"",
            "\"sourceName\":\"OmniGart\""
        ]
        if let distanceMetres {
            fields.append("\"distanceMetres\":\(distanceMetres)")
        }
        if let averageCadenceSpm {
            fields.append("\"averageCadenceSpm\":\(averageCadenceSpm)")
        }
        _ = sessionStart // used via startEpochMs; keep signature for call-site clarity
        return "{" + fields.joined(separator: ",") + "}"
    }
}
