import Foundation
import SwiftUI

/// Equipment toggles for guided strength; ids come from [WatchConnectivityHub].
@MainActor
final class WatchEquipmentSync: ObservableObject {
    static let shared = WatchEquipmentSync()

    @Published var enabledIds: Set<String> = []
    @Published var catalogueIds: [String] = []

    func activateIfNeeded() {
        WatchConnectivityHub.shared.activate()
        let hubIds = WatchConnectivityHub.shared.enabledEquipmentIds
        if !hubIds.isEmpty {
            enabledIds = hubIds
        }
    }
}
