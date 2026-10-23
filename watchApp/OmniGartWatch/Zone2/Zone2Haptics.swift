import Foundation
import WatchKit

/// Zone-enter / leave haptics with debounce so the wrist is not buzzed constantly.
@MainActor
final class Zone2HapticsController {
    struct Settings: Equatable, Sendable {
        var enabled: Bool = true
        /// Seconds that must elapse before another zone-transition haptic.
        var debounceSeconds: TimeInterval = 8
        /// Extra bpm padding when deciding leave/enter to reduce flicker.
        var hysteresisBpm: Int = 3
    }

    private(set) var settings: Settings
    private var lastState: Zone2ZoneState = .below
    private var lastHapticAt: Date = .distantPast
    private let device: WKInterfaceDevice

    init(settings: Settings = Settings(), device: WKInterfaceDevice = .current()) {
        self.settings = settings
        self.device = device
    }

    func updateSettings(_ settings: Settings) {
        self.settings = settings
    }

    /// Classify bpm relative to band (without hysteresis).
    func state(bpm: Int, band: Zone2Band) -> Zone2ZoneState {
        if bpm < band.lowerBpm { return .below }
        if bpm > band.upperBpm { return .above }
        return .inTarget
    }

    /// Call on each HR sample; fires at most one haptic per debounce window.
    func process(bpm: Int, band: Zone2Band, now: Date = Date()) {
        guard settings.enabled else {
            lastState = state(bpm: bpm, band: band)
            return
        }
        let padded = Zone2Band(
            method: band.method,
            lowerBpm: band.lowerBpm,
            upperBpm: band.upperBpm
        )
        let next: Zone2ZoneState
        if lastState == .inTarget {
            // Require leaving beyond hysteresis before counting as leave.
            if bpm < band.lowerBpm - settings.hysteresisBpm {
                next = .below
            } else if bpm > band.upperBpm + settings.hysteresisBpm {
                next = .above
            } else {
                next = .inTarget
            }
        } else {
            // Enter only when inside the core band (not merely hysteresis pad).
            next = state(bpm: bpm, band: padded)
        }

        defer { lastState = next }
        guard next != lastState else { return }
        guard now.timeIntervalSince(lastHapticAt) >= settings.debounceSeconds else { return }

        switch (lastState, next) {
        case (_, .inTarget):
            device.play(.directionUp)
        case (.inTarget, .above):
            device.play(.notification)
        case (.inTarget, .below):
            device.play(.directionDown)
        default:
            break
        }
        lastHapticAt = now
    }
}
