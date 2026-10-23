import Foundation

/// Swift ports of `shared/treadmill` parsers for CoreBluetooth notification handling.
/// Keep in sync with Kotlin fixtures in `:shared:treadmill` tests.
enum TreadmillProtocolKind: String, Sendable {
    case ftms = "FTMS"
    case rsc = "RSC"
    case iconsole = "ICONSOLE"
    case none = "NONE"
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

enum FtmsTreadmillParser {
    static func parse(_ data: Data) -> TreadmillParsedMetrics? {
        guard data.count >= 2 else { return nil }
        var i = 0
        func u8() -> Int? {
            guard i < data.count else { return nil }
            defer { i += 1 }
            return Int(data[i])
        }
        func u16() -> Int? {
            guard let lo = u8(), let hi = u8() else { return nil }
            return lo | (hi << 8)
        }
        func s16() -> Int? {
            guard let u = u16() else { return nil }
            return u & 0x8000 != 0 ? u - 0x10000 : u
        }
        func u24() -> Int64? {
            guard let b0 = u8(), let b1 = u8(), let b2 = u8() else { return nil }
            return Int64(b0) | (Int64(b1) << 8) | (Int64(b2) << 16)
        }

        guard let flags = u16() else { return nil }
        var metrics = TreadmillParsedMetrics(protocolKind: .ftms)
        if flags & 0x0001 == 0 {
            guard let speed = u16() else { return nil }
            metrics.speedKmh = Double(speed) / 100.0
        }
        if flags & 0x0002 != 0 { _ = u16() }
        if flags & 0x0004 != 0 {
            guard let dist = u24() else { return nil }
            metrics.distanceMetres = Double(dist)
        }
        if flags & 0x0008 != 0 {
            guard let incline = s16(), let _ = s16() else { return nil }
            metrics.inclinePercent = Double(incline) / 10.0
        }
        if flags & 0x0010 != 0 { _ = u16(); _ = u16() }
        if flags & 0x0020 != 0 {
            guard let pace = u8() else { return nil }
            if pace > 0 { metrics.paceMinPerKm = 10.0 / Double(pace) }
        }
        if flags & 0x0040 != 0 { _ = u8() }
        if flags & 0x0080 != 0 {
            guard let total = u16(), let _ = u16(), let _ = u8() else { return nil }
            metrics.energyKcal = Double(total)
        }
        if flags & 0x0100 != 0 {
            guard let hr = u8() else { return nil }
            metrics.machineHeartRateBpm = hr
        }
        if flags & 0x0200 != 0 { _ = u8() }
        if flags & 0x0400 != 0 {
            guard let elapsed = u16() else { return nil }
            metrics.elapsedSeconds = elapsed
        }
        if flags & 0x0800 != 0 { _ = u16() }
        if flags & 0x1000 != 0 { _ = s16(); _ = s16() }
        if flags & 0x2000 != 0 {
            guard let steps = u24() else { return nil }
            metrics.steps = steps
        }
        if metrics.paceMinPerKm == nil, let speed = metrics.speedKmh, speed > 0 {
            metrics.paceMinPerKm = 60.0 / speed
        }
        return metrics
    }

    static func requestControl() -> Data { Data([0x00]) }
    static func setTargetSpeedKmh(_ speed: Double) -> Data {
        let units = max(0, min(0xFFFF, Int(speed * 100.0)))
        return Data([0x02, UInt8(units & 0xFF), UInt8((units >> 8) & 0xFF)])
    }
    static func setTargetInclinePercent(_ incline: Double) -> Data {
        var units = Int(incline * 10.0)
        units = max(-0x8000, min(0x7FFF, units))
        let asU = units & 0xFFFF
        return Data([0x03, UInt8(asU & 0xFF), UInt8((asU >> 8) & 0xFF)])
    }
}

enum RscMeasurementSwiftParser {
    static func parse(_ data: Data) -> TreadmillParsedMetrics? {
        guard data.count >= 4 else { return nil }
        var i = 0
        func u8() -> Int { defer { i += 1 }; return Int(data[i]) }
        func u16() -> Int { let lo = u8(); let hi = u8(); return lo | (hi << 8) }
        func u32() -> Int64 {
            let b0 = Int64(u8()); let b1 = Int64(u8()); let b2 = Int64(u8()); let b3 = Int64(u8())
            return b0 | (b1 << 8) | (b2 << 16) | (b3 << 24)
        }
        let flags = u8()
        let speedRaw = u16()
        let cadence = u8()
        if flags & 0x01 != 0 {
            guard i + 2 <= data.count else { return nil }
            _ = u16()
        }
        var distance: Double?
        if flags & 0x02 != 0 {
            guard i + 4 <= data.count else { return nil }
            distance = Double(u32()) / 10.0
        }
        let speedKmh = (Double(speedRaw) / 256.0) * 3.6
        return TreadmillParsedMetrics(
            protocolKind: .rsc,
            speedKmh: speedKmh,
            paceMinPerKm: speedKmh > 0 ? 60.0 / speedKmh : nil,
            distanceMetres: distance,
            cadenceSpm: cadence
        )
    }
}

enum IConsoleSwiftParser {
    static let uartTx = "49535343-1E4D-4BD9-BA61-23C647249616"
    static let uartRx = "49535343-8841-43F4-A8D4-ECBE34729BB3"

    static func checksum(_ bytes: [UInt8]) -> UInt8 {
        UInt8(bytes.reduce(0) { ($0 + Int($1)) & 0xFF })
    }

    static func withChecksum(_ bytes: [UInt8]) -> Data {
        Data(bytes + [checksum(bytes)])
    }

    static func ping() -> Data { withChecksum([0xF0, 0xA0, 0x01, 0x01]) }
    static func read() -> Data { withChecksum([0xF0, 0xA2, 0x01, 0x01]) }
    static func start() -> Data { withChecksum([0xF0, 0xA5, 0x01, 0x01, 0x02]) }
    static func stop() -> Data { withChecksum([0xF0, 0xA5, 0x01, 0x01, 0x04]) }
    static func setLevel(_ level: Int) -> Data {
        withChecksum([0xF0, 0xA6, 0x01, 0x01, UInt8(level + 1)])
    }

    static func parseTelemetry(_ data: Data) -> TreadmillParsedMetrics? {
        guard data.count == 21, data[0] == 0xF0, data[1] == 0xB2 else { return nil }
        let bytes = [UInt8](data)
        guard checksum(Array(bytes.dropLast())) == bytes.last else { return nil }
        func biased(_ high: UInt8, _ low: UInt8) -> Int {
            100 * (Int(high) - 1) + (Int(low) - 1)
        }
        let minutes = max(0, Int(bytes[4]) - 1)
        let seconds = min(59, max(0, Int(bytes[5]) - 1))
        let speed = Double(biased(bytes[6], bytes[7])) / 10.0
        let rpm = biased(bytes[8], bytes[9])
        let distanceKm = Double(biased(bytes[10], bytes[11])) / 10.0
        let kcal = biased(bytes[12], bytes[13])
        let hr = biased(bytes[14], bytes[15])
        return TreadmillParsedMetrics(
            protocolKind: .iconsole,
            speedKmh: speed,
            paceMinPerKm: speed > 0 ? 60.0 / speed : nil,
            distanceMetres: distanceKm * 1000.0,
            elapsedSeconds: minutes * 60 + seconds,
            machineHeartRateBpm: hr > 0 ? hr : nil,
            energyKcal: Double(kcal),
            cadenceSpm: rpm
        )
    }
}

enum TreadmillSessionNotesSwift {
    static let prefix = "omnigart.treadmill"

    static func encode(
        protocolKind: TreadmillProtocolKind,
        deviceName: String?,
        distanceMetres: Double?,
        averageSpeedKmh: Double?,
        averageInclinePercent: Double?,
        machineAverageHeartRateBpm: Double?,
        energyKcal: Double?,
        steps: Int64?,
        usedWatchEstimatedDistance: Bool,
        calibrationFactor: Double?
    ) -> String {
        [
            prefix,
            "proto=\(protocolKind.rawValue)",
            "dist=\(distanceMetres.map(String.init(describing:)) ?? "")",
            "spd=\(averageSpeedKmh.map(String.init(describing:)) ?? "")",
            "inc=\(averageInclinePercent.map(String.init(describing:)) ?? "")",
            "mhr=\(machineAverageHeartRateBpm.map(String.init(describing:)) ?? "")",
            "kcal=\(energyKcal.map(String.init(describing:)) ?? "")",
            "steps=\(steps.map(String.init(describing:)) ?? "")",
            "est=\(usedWatchEstimatedDistance ? "1" : "0")",
            "cal=\(calibrationFactor.map(String.init(describing:)) ?? "")",
            "dev=\((deviceName ?? "").replacingOccurrences(of: "|", with: "/"))"
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
