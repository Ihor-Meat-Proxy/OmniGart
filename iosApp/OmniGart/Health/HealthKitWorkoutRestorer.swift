import Foundation
import HealthKit
import Shared

/// User-initiated restore of a local workout that was removed from Apple Health.
/// Append-only: exist-check → insert with bumped `HKMetadataKeySyncVersion` → never delete/update.
enum HealthKitWorkoutRestorer {
    enum Outcome: Sendable {
        case restored(UUID)
        case alreadyPresent
        case skipped
        case failed(String)
    }

    static func restore(localId: String, core: OmniGartCore) async -> Outcome {
        guard HKHealthStore.isHealthDataAvailable() else { return .skipped }
        guard core.localHealth.isRemovedFromPlatform(localId: localId) else { return .skipped }

        do {
            try await HealthKitWorkoutWriter.requestWorkoutAuthorization()
        } catch {
            return .failed(String(localized: "health_restore_permission_needed"))
        }

        guard core.localHealth.prepareRestore(localId: localId) else { return .skipped }
        let syncId = core.localHealth.restoreSyncIdentifier(localId: localId) ?? ""
        let syncVersion = Int(core.localHealth.restoreSyncVersion(localId: localId))
        guard !syncId.isEmpty else { return .skipped }

        do {
            if try await HealthKitWorkoutWriter.workoutExists(syncIdentifier: syncId) {
                core.localHealth.reconcileAlreadyOnPlatform(
                    localId: localId,
                    platformRecordId: nil
                )
                return .alreadyPresent
            }

            let payload = loadRestorePayload(localId: localId, core: core)
            let saved = try await HealthKitWorkoutWriter.save(
                options: .init(
                    activityType: activityType(from: payload.activityName),
                    start: payload.start,
                    end: payload.end,
                    energy: .init(
                        activeKcal: payload.activeKcal,
                        basalKcal: payload.basalKcal,
                        isEstimate: false
                    ),
                    syncIdentifier: syncId,
                    heartRate: payload.heartRate,
                    pauseResumeDates: payload.pauses,
                    segments: payload.segments,
                    indoor: true,
                    syncVersion: syncVersion
                )
            )
            guard let saved else { return .skipped }
            core.localHealth.completeRestore(
                localId: localId,
                platformRecordId: saved.workoutUUID.uuidString
            )
            return .restored(saved.workoutUUID)
        } catch {
            return .failed(error.localizedDescription)
        }
    }

    static func restoreAll(core: OmniGartCore) async -> (ok: Int, failed: Int) {
        var ok = 0
        var failed = 0
        for id in core.localHealth.removedFromPlatformIds() {
            switch await restore(localId: id as String, core: core) {
            case .restored, .alreadyPresent:
                ok += 1
            case .skipped:
                break
            case .failed:
                failed += 1
            }
        }
        return (ok, failed)
    }

    private struct RestorePayload {
        var activityName: String
        var start: Date
        var end: Date
        var activeKcal: Double
        var basalKcal: Double
        var heartRate: [HealthKitWorkoutWriter.HeartRatePoint]
        var pauses: [(pause: Date, resume: Date?)]
        var segments: [HealthKitWorkoutWriter.Segment]
    }

    private static func loadRestorePayload(localId: String, core: OmniGartCore) -> RestorePayload {
        let activityName = core.localHealth.workoutActivityTypeName(localId: localId) ?? "OTHER"
        let startMs = core.localHealth.workoutStartEpochMs(localId: localId)
        let endMs = core.localHealth.workoutEndEpochMs(localId: localId)
        let start = Date(timeIntervalSince1970: Double(startMs) / 1000.0)
        let end = Date(timeIntervalSince1970: Double(endMs) / 1000.0)
        let active = core.localHealth.workoutActiveEnergyKcal(localId: localId)?.doubleValue ?? 0
        let basal = core.localHealth.workoutBasalEnergyKcal(localId: localId)?.doubleValue ?? 0
        let heartRate = loadHeartRate(localId: localId, core: core)
        let events = loadEvents(localId: localId, core: core, startMs: startMs)
        return RestorePayload(
            activityName: activityName,
            start: start,
            end: end,
            activeKcal: active,
            basalKcal: basal,
            heartRate: heartRate,
            pauses: events.pauses,
            segments: events.segments
        )
    }

    private static func loadHeartRate(
        localId: String,
        core: OmniGartCore
    ) -> [HealthKitWorkoutWriter.HeartRatePoint] {
        let hrEpoch = core.localHealth.workoutHeartRateEpochMs(localId: localId)
        let hrBpm = core.localHealth.workoutHeartRateBpm(localId: localId)
        let hrCount = min(Int(hrEpoch.size), Int(hrBpm.size))
        var heartRate: [HealthKitWorkoutWriter.HeartRatePoint] = []
        for index in 0..<hrCount {
            let idx = Int32(index)
            heartRate.append(
                HealthKitWorkoutWriter.HeartRatePoint(
                    date: Date(timeIntervalSince1970: Double(hrEpoch.get(index: idx)) / 1000.0),
                    bpm: hrBpm.get(index: idx)
                )
            )
        }
        return heartRate
    }

    private static func loadEvents(
        localId: String,
        core: OmniGartCore,
        startMs: Int64
    ) -> (pauses: [(pause: Date, resume: Date?)], segments: [HealthKitWorkoutWriter.Segment]) {
        let eventTypes = core.localHealth.workoutEventTypesList(localId: localId)
        let eventStarts = core.localHealth.workoutEventStartEpochMs(localId: localId)
        let eventEnds = core.localHealth.workoutEventEndEpochMs(localId: localId)
        let eventLabels = core.localHealth.workoutEventLabelsList(localId: localId)
        var pauses: [(pause: Date, resume: Date?)] = []
        var segments: [HealthKitWorkoutWriter.Segment] = []
        var pendingPause: Date?
        for index in 0..<eventTypes.count {
            let type = eventTypes[index] as String
            let idx = Int32(index)
            let startEpoch = index < Int(eventStarts.size) ? eventStarts.get(index: idx) : startMs
            let endEpoch = index < Int(eventEnds.size) ? eventEnds.get(index: idx) : startMs
            let eventStart = Date(timeIntervalSince1970: Double(startEpoch) / 1000.0)
            let eventEnd = Date(timeIntervalSince1970: Double(endEpoch) / 1000.0)
            let label = (eventLabels[safe: index] as String?) ?? ""
            switch type {
            case "PAUSE":
                pendingPause = eventStart
            case "RESUME":
                if let pauseStart = pendingPause {
                    pauses.append((pause: pauseStart, resume: eventStart))
                    pendingPause = nil
                }
            case "SEGMENT", "LAP", "MARKER":
                segments.append(
                    HealthKitWorkoutWriter.Segment(
                        start: eventStart,
                        end: max(eventEnd, eventStart),
                        label: label.isEmpty ? type : label
                    )
                )
            default:
                break
            }
        }
        if let pauseStart = pendingPause {
            pauses.append((pause: pauseStart, resume: nil))
        }
        return (pauses, segments)
    }

    private static func activityType(from name: String) -> HKWorkoutActivityType {
        switch name.uppercased() {
        case "STRENGTH": return .traditionalStrengthTraining
        case "RUNNING": return .running
        case "CYCLING": return .cycling
        case "SWIMMING": return .swimming
        case "WALKING": return .walking
        case "HIIT": return .highIntensityIntervalTraining
        case "YOGA": return .yoga
        case "PILATES": return .pilates
        default: return .other
        }
    }
}

private extension Array {
    subscript(safe index: Int) -> Element? {
        indices.contains(index) ? self[index] : nil
    }
}
