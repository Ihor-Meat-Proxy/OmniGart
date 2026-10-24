import Foundation
import HealthKit
import Shared
import WatchConnectivity

/// Starts a companion Apple Watch workout when possible; otherwise callers use iPhone fallback.
///
/// Flow: encode exact [PhoneWorkoutSelection] over WCSession, then
/// `HKHealthStore.startWatchApp(with:)` so the watch `handle(_:)` path opens the matching kind.
/// The watch session calls `startMirroringToCompanionDevice` so the phone can show live metrics
/// and pause / end without writing a second HealthKit workout.
enum HealthKitWatchLauncher {
    enum LaunchOutcome: Sendable, Equatable {
        case watchStarted
        /// No paired watch, app not installed, unreachable, or HealthKit failure — phone-only path.
        case watchUnavailable(String)
        /// Settings toggle off — skip watch launch without treating it as an error.
        case skippedByPreference
    }

    /// UserDefaults key shared with `StartWorkoutsOnWatchSettingsToggle` (default on).
    static let preferenceKey = "omnigart.workout.start_on_watch.enabled"

    static var isStartOnWatchEnabled: Bool {
        if UserDefaults.standard.object(forKey: preferenceKey) == nil {
            return true
        }
        return UserDefaults.standard.bool(forKey: preferenceKey)
    }

    static func startOnWatch(
        selection: PhoneWorkoutSelection,
        respectPreference: Bool = true
    ) async -> LaunchOutcome {
        if respectPreference, !isStartOnWatchEnabled {
            return .skippedByPreference
        }
        guard HKHealthStore.isHealthDataAvailable() else {
            return .watchUnavailable("health_unavailable")
        }
        guard WCSession.isSupported() else {
            return .watchUnavailable("watch_unsupported")
        }
        let session = WCSession.default
        if session.activationState != .activated {
            try? await Task.sleep(nanoseconds: 200_000_000)
        }
        if session.activationState == .activated {
            if !session.isPaired {
                return .watchUnavailable("watch_not_paired")
            }
            if !session.isWatchAppInstalled {
                return .watchUnavailable("watch_app_not_installed")
            }
        }

        await MainActor.run {
            PhoneWatchConnectivityHub.shared.sendWorkoutSelection(selection)
        }

        let configuration = makeConfiguration(for: selection)
        let store = HKHealthStore()
        do {
            try await HealthKitWorkoutWriter.requestWorkoutAuthorization()
            try await withCheckedThrowingContinuation { (continuation: CheckedContinuation<Void, Error>) in
                store.startWatchApp(with: configuration) { success, error in
                    if let error {
                        continuation.resume(throwing: error)
                    } else if success {
                        continuation.resume()
                    } else {
                        continuation.resume(
                            throwing: NSError(
                                domain: "OmniGart.HealthKitWatchLauncher",
                                code: 1,
                                userInfo: [
                                    NSLocalizedDescriptionKey: "Watch app did not start"
                                ]
                            )
                        )
                    }
                }
            }
            return .watchStarted
        } catch {
            return .watchUnavailable(error.localizedDescription)
        }
    }

    /// Convenience for call sites that only know the HealthKit activity type.
    static func startOnWatch(
        activityType: HKWorkoutActivityType,
        indoor: Bool = true,
        kindOverride: String? = nil,
        programId: String? = nil,
        routineId: String? = nil,
        equipmentIds: [String] = [],
        sessionKey: String? = nil,
        respectPreference: Bool = true
    ) async -> LaunchOutcome {
        let kind = kindOverride ?? kindId(for: activityType, indoor: indoor)
        let selection = PhoneWorkoutSelectionFactory.shared.create(
            kind: kind,
            programId: programId,
            routineId: routineId,
            dayIndex: programId != nil ? 0 : Int32(-1),
            equipmentIds: KotlinPrimitiveArrays.stringArray(equipmentIds),
            sessionKey: sessionKey,
            watchIsRecorder: true,
            durationSeconds: Int32(-1),
            distanceMetres: -1.0,
            zoneLowerBpm: Int32(-1),
            zoneUpperBpm: Int32(-1),
            targetSets: Int32(-1),
            targetReps: Int32(-1),
            targetWeightKg: -1.0
        )
        return await startOnWatch(selection: selection, respectPreference: respectPreference)
    }

    static func makeConfiguration(for selection: PhoneWorkoutSelection) -> HKWorkoutConfiguration {
        let config = HKWorkoutConfiguration()
        let family = PhoneWorkoutKindIds.shared.platformStrengthFamily(kind: selection.kind)
        switch family {
        case PhoneWorkoutKindIds.shared.TRADITIONAL_STRENGTH:
            config.activityType = .traditionalStrengthTraining
            config.locationType = .indoor
        case PhoneWorkoutKindIds.shared.FUNCTIONAL_STRENGTH:
            config.activityType = .functionalStrengthTraining
            config.locationType = .indoor
        case PhoneWorkoutKindIds.shared.YOGA:
            config.activityType = .yoga
            config.locationType = .indoor
        case PhoneWorkoutKindIds.shared.PILATES:
            config.activityType = .pilates
            config.locationType = .indoor
        case PhoneWorkoutKindIds.shared.ZONE2_RUN:
            config.activityType = .running
            config.locationType = .outdoor
        case PhoneWorkoutKindIds.shared.TREADMILL_RUN:
            config.activityType = .running
            config.locationType = .indoor
        case PhoneWorkoutKindIds.shared.TREADMILL_WALK:
            config.activityType = .walking
            config.locationType = .indoor
        default:
            config.activityType = .other
            config.locationType = .indoor
        }
        return config
    }

    private static func kindId(for activityType: HKWorkoutActivityType, indoor: Bool) -> String {
        switch activityType {
        case .traditionalStrengthTraining:
            return PhoneWorkoutKindIds.shared.TRADITIONAL_STRENGTH
        case .functionalStrengthTraining:
            return PhoneWorkoutKindIds.shared.FUNCTIONAL_STRENGTH
        case .yoga:
            return PhoneWorkoutKindIds.shared.YOGA
        case .pilates:
            return PhoneWorkoutKindIds.shared.PILATES
        case .running:
            return indoor
                ? PhoneWorkoutKindIds.shared.TREADMILL_RUN
                : PhoneWorkoutKindIds.shared.ZONE2_RUN
        case .walking:
            return PhoneWorkoutKindIds.shared.TREADMILL_WALK
        default:
            return PhoneWorkoutKindIds.shared.TRADITIONAL_STRENGTH
        }
    }
}
