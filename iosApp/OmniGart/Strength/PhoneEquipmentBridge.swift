import Foundation
import Shared

/// Publishes the phone equipment catalogue to the paired watch via the shared WCSession hub.
/// Does not touch HealthKit session recording.
@MainActor
enum PhoneEquipmentBridge {
    static let shared = PhoneEquipmentBridgeProxy()

    final class PhoneEquipmentBridgeProxy {
        func publish(from profile: UserEquipmentProfile) {
            PhoneWatchConnectivityHub.shared.publishEquipment(from: profile)
        }
    }
}
