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

/// Inputs for the device-transfer JSON of a finished watch workout.
struct WatchWorkoutTransferPayload {
    var localId: String
    var syncIdentifier: String
    var activityTypeName: String
    var startEpochMs: Int64
    var endEpochMs: Int64
    var activeEnergyKcal: Double
    var basalEnergyKcal: Double
    var distanceMetres: Double?
    var averageHeartRateBpm: Double
    var maxHeartRateBpm: Double
    var averageCadenceSpm: Double?
    var samples: [Zone2HeartRateSample]
    var sets: [OmniGartLoggedStrengthSet] = []
}

/// Builds device-transfer JSON from a finished watch workout and enqueues WCSession delivery.
enum WatchWorkoutTransferBuilder {
    static func activityTypeName(for kind: OmniGartWorkoutKind) -> String {
        switch kind {
        case .traditionalStrength, .functionalStrength: return "STRENGTH"
        case .yoga: return "YOGA"
        case .pilates: return "PILATES"
        case .zone2Run: return "RUNNING"
        }
    }

    static func enqueueLiveSummary(_ summary: OmniGartLiveWorkoutSummary, localId: String, syncIdentifier: String) {
        enqueueLiveSummary(summary, localId: localId, syncIdentifier: syncIdentifier, sets: summary.sets)
    }

    static func enqueueLiveSummary(
        _ summary: OmniGartLiveWorkoutSummary,
        localId: String,
        syncIdentifier: String,
        sets: [OmniGartLoggedStrengthSet]
    ) {
        let json = makeJSON(
            WatchWorkoutTransferPayload(
                localId: localId,
                syncIdentifier: syncIdentifier,
                activityTypeName: activityTypeName(for: summary.kind),
                startEpochMs: Int64(summary.startedAt.timeIntervalSince1970 * 1000),
                endEpochMs: Int64(summary.endedAt.timeIntervalSince1970 * 1000),
                activeEnergyKcal: summary.activeEnergyKcal,
                basalEnergyKcal: summary.basalEnergyKcal,
                distanceMetres: nil,
                averageHeartRateBpm: summary.averageHeartRateBpm,
                maxHeartRateBpm: summary.maxHeartRateBpm,
                averageCadenceSpm: nil,
                samples: summary.heartRateSamples,
                sets: sets
            )
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
        let json = makeJSON(
            WatchWorkoutTransferPayload(
                localId: localId,
                syncIdentifier: syncIdentifier,
                activityTypeName: "RUNNING",
                startEpochMs: Int64(startedAt.timeIntervalSince1970 * 1000),
                endEpochMs: Int64(endedAt.timeIntervalSince1970 * 1000),
                activeEnergyKcal: summary.activeEnergyKcal,
                basalEnergyKcal: summary.basalEnergyKcal,
                distanceMetres: summary.distanceMetres,
                averageHeartRateBpm: summary.averageHeartRateBpm,
                maxHeartRateBpm: summary.maxHeartRateBpm,
                averageCadenceSpm: summary.averageCadenceSpm,
                samples: summary.heartRateSamples,
                sets: []
            )
        )
        WatchPhoneWorkoutTransfer.shared.enqueue(jsonPayload: json)
    }

    static func makeJSON(_ payload: WatchWorkoutTransferPayload) -> String {
        var fields = baseFields(payload)
        if let distanceMetres = payload.distanceMetres {
            fields.append("\"distanceMetres\":\(distanceMetres)")
        }
        if let averageCadenceSpm = payload.averageCadenceSpm {
            fields.append("\"averageCadenceSpm\":\(averageCadenceSpm)")
        }
        fields.append("\"sets\":[\(setsJSON(payload.sets))]")
        return "{" + fields.joined(separator: ",") + "}"
    }

    private static func escape(_ text: String) -> String {
        text.replacingOccurrences(of: "\\", with: "\\\\")
            .replacingOccurrences(of: "\"", with: "\\\"")
    }

    private static func baseFields(_ payload: WatchWorkoutTransferPayload) -> [String] {
        let hrEpoch = payload.samples.map { sample -> Int64 in
            payload.startEpochMs + Int64(sample.offsetSeconds * 1000)
        }
        let hrBpm = payload.samples.map { Double($0.bpm) }
        let hrEpochJson = hrEpoch.map(String.init).joined(separator: ",")
        let hrBpmJson = hrBpm.map { String(format: "%.1f", $0) }.joined(separator: ",")
        return [
            "\"schema\":\"omnigart.device.workout.transfer\"",
            "\"schemaVersion\":2",
            "\"localId\":\"\(escape(payload.localId))\"",
            "\"syncIdentifier\":\"\(escape(payload.syncIdentifier))\"",
            "\"syncVersion\":1",
            "\"activityTypeName\":\"\(escape(payload.activityTypeName))\"",
            "\"startEpochMs\":\(payload.startEpochMs)",
            "\"endEpochMs\":\(payload.endEpochMs)",
            "\"activeEnergyKcal\":\(payload.activeEnergyKcal)",
            "\"basalEnergyKcal\":\(payload.basalEnergyKcal)",
            "\"averageHeartRateBpm\":\(payload.averageHeartRateBpm)",
            "\"maxHeartRateBpm\":\(payload.maxHeartRateBpm)",
            "\"heartRateEpochMs\":[\(hrEpochJson)]",
            "\"heartRateBpm\":[\(hrBpmJson)]",
            "\"sourceDevice\":\"APPLE_WATCH\"",
            "\"sourceBundle\":\"com.omnigart\"",
            "\"sourceName\":\"OmniGart\""
        ]
    }

    private static func setsJSON(_ sets: [OmniGartLoggedStrengthSet]) -> String {
        sets.map { set -> String in
            var parts: [String] = [
                "\"id\":\"\(escape(set.id))\"",
                "\"exerciseKey\":\"\(escape(set.exerciseId))\"",
                "\"setIndex\":\(set.setNumber)",
                "\"reps\":\(set.reps)",
                "\"loadKg\":\(set.weightKg)",
                "\"isWarmup\":\(set.isWarmup ? "true" : "false")"
            ]
            if let note = set.target, !note.isEmpty {
                parts.append("\"note\":\"\(escape(note))\"")
            }
            return "{" + parts.joined(separator: ",") + "}"
        }.joined(separator: ",")
    }
}
