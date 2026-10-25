import Foundation
import HealthKit
import Shared
import WatchKit

/// Holds a workout kind / full selection requested from the iPhone via `startWatchApp` + WCSession.
@MainActor
final class PendingWatchWorkout: ObservableObject {
    static let shared = PendingWatchWorkout()

    @Published var kind: OmniGartWorkoutKind?
    @Published var selection: PhoneWorkoutSelection?
    /// When true, open guided strength programme UI instead of free live workout.
    @Published var openGuidedStrength: Bool = false

    func apply(selection: PhoneWorkoutSelection) {
        self.selection = selection
        if selection.kind == PhoneWorkoutKindIds.shared.GUIDED_STRENGTH {
            openGuidedStrength = true
            kind = nil
            return
        }
        openGuidedStrength = false
        kind = OmniGartWorkoutKind.from(selectionKind: selection.kind)
            ?? OmniGartWorkoutKind.from(
                configuration: HealthKitWatchConfigurationBridge.makeConfiguration(kind: selection.kind)
            )
    }

    func apply(configuration: HKWorkoutConfiguration) {
        // Prefer richer WCSession selection when already received.
        if selection != nil, kind != nil { return }
        kind = OmniGartWorkoutKind.from(configuration: configuration)
        openGuidedStrength = false
    }

    func clearNavigation() {
        kind = nil
        openGuidedStrength = false
    }
}

/// Maps selection kind strings to HK configurations without importing iOS-only types.
enum HealthKitWatchConfigurationBridge {
    static func makeConfiguration(kind: String) -> HKWorkoutConfiguration {
        let config = HKWorkoutConfiguration()
        let family = PhoneWorkoutKindIds.shared.platformStrengthFamily(kind: kind)
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
}

/// Receives `startWatchApp` / mirrored workout configurations from the iPhone.
final class WatchAppDelegate: NSObject, WKApplicationDelegate {
    func applicationDidFinishLaunching() {
        Task { @MainActor in
            WatchConnectivityHub.shared.activate()
            WatchPhoneWorkoutTransfer.shared.activate()
        }
    }

    func handle(_ workoutConfiguration: HKWorkoutConfiguration) {
        Task { @MainActor in
            PendingWatchWorkout.shared.apply(configuration: workoutConfiguration)
        }
    }
}
