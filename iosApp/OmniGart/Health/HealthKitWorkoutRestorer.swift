import Foundation
import HealthKit
import Shared

/// User-initiated restore of a local workout that was removed from Apple Health.
/// Append-only: exist-check → insert with bumped `HKMetadataKeySyncVersion` → never delete/update.
@MainActor
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
            return try await performRestore(
                localId: localId,
                core: core,
                syncId: syncId,
                syncVersion: syncVersion
            )
        } catch {
            return .failed(error.localizedDescription)
        }
    }

    @MainActor
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

    private static func performRestore(
        localId: String,
        core: OmniGartCore,
        syncId: String,
        syncVersion: Int
    ) async throws -> Outcome {
        let snapshot = loadSnapshot(localId: localId, core: core)
        let heartRate = loadHeartRate(localId: localId, core: core)
        let timeline = loadTimelineEvents(
            localId: localId,
            core: core,
            fallbackStartMs: snapshot.startMs
        )
        let saved = try await HealthKitWorkoutWriter.save(
            options: .init(
                activityType: activityType(from: snapshot.activityName),
                start: snapshot.start,
                end: snapshot.end,
                energy: .init(
                    activeKcal: snapshot.activeKcal,
                    basalKcal: snapshot.basalKcal,
                    isEstimate: false
                ),
                syncIdentifier: syncId,
                heartRate: heartRate,
                pauseResumeDates: timeline.pauses,
                segments: timeline.segments,
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

    private struct WorkoutSnapshot {
        var activityName: String
        var startMs: Int64
        var start: Date
        var end: Date
        var activeKcal: Double
        var basalKcal: Double
    }

    private static func loadSnapshot(localId: String, core: OmniGartCore) -> WorkoutSnapshot {
        let activityName = core.localHealth.workoutActivityTypeName(localId: localId) ?? "OTHER"
        let startMs = core.localHealth.workoutStartEpochMs(localId: localId)
        let endMs = core.localHealth.workoutEndEpochMs(localId: localId)
        return WorkoutSnapshot(
            activityName: activityName,
            startMs: startMs,
            start: Date(timeIntervalSince1970: Double(startMs) / 1000.0),
            end: Date(timeIntervalSince1970: Double(endMs) / 1000.0),
            activeKcal: core.localHealth.workoutActiveEnergyKcal(localId: localId)?.doubleValue ?? 0,
            basalKcal: core.localHealth.workoutBasalEnergyKcal(localId: localId)?.doubleValue ?? 0
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
        for sampleIndex in 0..<hrCount {
            let listIndex = Int32(sampleIndex)
            heartRate.append(
                HealthKitWorkoutWriter.HeartRatePoint(
                    date: Date(
                        timeIntervalSince1970: Double(hrEpoch.get(index: listIndex)) / 1000.0
                    ),
                    bpm: hrBpm.get(index: listIndex)
                )
            )
        }
        return heartRate
    }

    private struct TimelineEvents {
        var pauses: [(pause: Date, resume: Date?)]
        var segments: [HealthKitWorkoutWriter.Segment]
    }

    private struct TimelineAccumulator {
        var pauses: [(pause: Date, resume: Date?)] = []
        var segments: [HealthKitWorkoutWriter.Segment] = []
        var pendingPause: Date?
    }

    private static func loadTimelineEvents(
        localId: String,
        core: OmniGartCore,
        fallbackStartMs: Int64
    ) -> TimelineEvents {
        let eventTypes = core.localHealth.workoutEventTypesList(localId: localId)
        let eventStarts = core.localHealth.workoutEventStartEpochMs(localId: localId)
        let eventEnds = core.localHealth.workoutEventEndEpochMs(localId: localId)
        let eventLabels = core.localHealth.workoutEventLabelsList(localId: localId)
        var state = TimelineAccumulator()
        for eventIndex in 0..<eventTypes.count {
            let listIndex = Int32(eventIndex)
            let startEpoch = eventIndex < Int(eventStarts.size)
                ? eventStarts.get(index: listIndex)
                : fallbackStartMs
            let endEpoch = eventIndex < Int(eventEnds.size)
                ? eventEnds.get(index: listIndex)
                : fallbackStartMs
            applyTimelineEvent(
                type: eventTypes[eventIndex] as String,
                eventStart: Date(timeIntervalSince1970: Double(startEpoch) / 1000.0),
                eventEnd: Date(timeIntervalSince1970: Double(endEpoch) / 1000.0),
                label: (eventLabels[safe: eventIndex] as String?) ?? "",
                state: &state
            )
        }
        if let pauseStart = state.pendingPause {
            state.pauses.append((pause: pauseStart, resume: nil))
        }
        return TimelineEvents(pauses: state.pauses, segments: state.segments)
    }

    private static func applyTimelineEvent(
        type: String,
        eventStart: Date,
        eventEnd: Date,
        label: String,
        state: inout TimelineAccumulator
    ) {
        switch type {
        case "PAUSE":
            state.pendingPause = eventStart
        case "RESUME":
            if let pauseStart = state.pendingPause {
                state.pauses.append((pause: pauseStart, resume: eventStart))
                state.pendingPause = nil
            }
        case "SEGMENT", "LAP", "MARKER":
            state.segments.append(
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
