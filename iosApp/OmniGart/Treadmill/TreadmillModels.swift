import Foundation

/// Swift ports of `shared/treadmill` models for CoreBluetooth + UI.
enum TreadmillProtocolKind: String, Sendable {
    case ftms = "FTMS"
    case fitshow = "FITSHOW"
    case rsc = "RSC"
    case iconsole = "ICONSOLE"
    case none = "NONE"
}

enum TreadmillSessionKindSwift: String, CaseIterable, Identifiable, Sendable {
    case run
    case walk

    var id: String { rawValue }

    var activityTypeName: String {
        switch self {
        case .run: return "RUNNING"
        case .walk: return "WALKING"
        }
    }

    var titleKey: String {
        switch self {
        case .run: return "treadmill_run_title"
        case .walk: return "treadmill_walk_title"
        }
    }
}

struct TreadmillParsedMetrics: Equatable, Sendable {
    var protocolKind: TreadmillProtocolKind = .none
    var speedKmh: Double?
    var paceMinPerKm: Double?
    var inclinePercent: Double?
    var distanceMetres: Double?
    var elapsedSeconds: Int?
    var machineHeartRateBpm: Int?
    var energyKcal: Double?
    var steps: Int64?
    var cadenceSpm: Int?
}

struct TreadmillLiveSnapshot: Equatable {
    var protocolKind: TreadmillProtocolKind = .none
    var connected: Bool = false
    var speedKmh: Double?
    var paceMinPerKm: Double?
    var inclinePercent: Double?
    var distanceMetres: Double?
    var elapsedSeconds: Int?
    var heartRateBpm: Int?
    var energyKcal: Double?
    var steps: Int64?
    var zone: Int?
}

struct TreadmillSessionNotesFields: Sendable {
    var protocolKind: TreadmillProtocolKind
    var deviceName: String?
    var distanceMetres: Double?
    var averageSpeedKmh: Double?
    var averageInclinePercent: Double?
    var machineAverageHeartRateBpm: Double?
    var energyKcal: Double?
    var steps: Int64?
    var usedWatchEstimatedDistance: Bool
    var calibrationFactor: Double?
}

enum TreadmillSessionNotesSwift {
    static let prefix = "omnigart.treadmill"

    static func encode(_ fields: TreadmillSessionNotesFields) -> String {
        [
            prefix,
            "proto=\(fields.protocolKind.rawValue)",
            "dist=\(fields.distanceMetres.map(String.init(describing:)) ?? "")",
            "spd=\(fields.averageSpeedKmh.map(String.init(describing:)) ?? "")",
            "inc=\(fields.averageInclinePercent.map(String.init(describing:)) ?? "")",
            "mhr=\(fields.machineAverageHeartRateBpm.map(String.init(describing:)) ?? "")",
            "kcal=\(fields.energyKcal.map(String.init(describing:)) ?? "")",
            "steps=\(fields.steps.map(String.init(describing:)) ?? "")",
            "est=\(fields.usedWatchEstimatedDistance ? "1" : "0")",
            "cal=\(fields.calibrationFactor.map(String.init(describing:)) ?? "")",
            "dev=\((fields.deviceName ?? "").replacingOccurrences(of: "|", with: "/"))"
        ].joined(separator: "|")
    }
}

enum TreadmillHeartRateZone {
    static func zone(bpm: Int?, hrMax: Int) -> Int? {
        guard let bpm, hrMax > 0 else { return nil }
        let pct = Double(bpm) / Double(hrMax)
        switch pct {
        case ..<0.60: return 1
        case ..<0.70: return 2
        case ..<0.80: return 3
        case ..<0.90: return 4
        default: return 5
        }
    }
}
