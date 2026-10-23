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
            return try await writeRestoredWorkout(
                localId: localId,
                core: core,
                syncId: syncId,
                syncVersion: syncVersion
            )
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

    private static func writeRestoredWorkout(
        localId: String,
        core: OmniGartCore,
        syncId: String,
        syncVersion: Int
    ) async throws -> Outcome {
        let activityName = core.localHealth.workoutActivityTypeName(localId: localId) ?? "OTHER"
        let startMs = core.localHealth.workoutStartEpochMs(localId: localId)
        let endMs = core.localHealth.workoutEndEpochMs(localId: localId)
        let start = Date(timeIntervalSince1970: Double(startMs) / 1000.0)
        let end = Date(timeIntervalSince1970: Double(endMs) / 1000.0)
        let active = core.localHealth.workoutActiveEnergyKcal(localId: localId)?.doubleValue ?? 0
        let basal = core.localHealth.workoutBasalEnergyKcal(localId: localId)?.doubleValue ?? 0
        let heartRate = loadHeartRate(localId: localId, core: core)
        let events = loadEvents(localId: localId, core: core, fallbackStartMs: startMs)
        let saved = try await HealthKitWorkoutWriter.save(
            options: .init(
                activityType: activityType(from: activityName),
                start: start,
                end: end,
                energy: .init(activeKcal: active, basalKcal: basal, isEstimate: false),
                syncIdentifier: syncId,
                heartRate: heartRate,
                pauseResumeDates: events.pauses,
                segments: events.segments,
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
        fallbackStartMs: Int64
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
            let startEpoch = index < Int(eventStarts.size) ? eventStarts.get(index: idx) : fallbackStartMs
            let endEpoch = index < Int(eventEnds.size) ? eventEnds.get(index: idx) : fallbackStartMs
            let startDate = Date(timeIntervalSince1970: Double(startEpoch) / 1000.0)
            let endDate = Date(timeIntervalSince1970: Double(endEpoch) / 1000.0)
            let label = (eventLabels[safe: index] as String?) ?? ""
            switch type {
            case "PAUSE":
                pendingPause = startDate
            case "RESUME":
                if let pauseDate = pendingPause {
                    pauses.append((pause: pauseDate, resume: startDate))
                    pendingPause = nil
                }
            case "SEGMENT", "LAP", "MARKER":
                segments.append(
                    HealthKitWorkoutWriter.Segment(
                        start: startDate,
                        end: max(endDate, startDate),
                        label: label.isEmpty ? type : label
                    )
                )
            default:
                break
            }
        }
        if let pauseDate = pendingPause {
            pauses.append((pause: pauseDate, resume: nil))
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
