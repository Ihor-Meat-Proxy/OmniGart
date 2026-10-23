import Foundation

enum FtmsTreadmillParser {
    static func parse(_ data: Data) -> TreadmillParsedMetrics? {
        guard data.count >= 2 else { return nil }
        var cursor = ByteCursor(data)
        guard let flags = cursor.readU16() else { return nil }
        var metrics = TreadmillParsedMetrics(protocolKind: .ftms)
        if flags & 0x0001 == 0 {
            guard let speed = cursor.readU16() else { return nil }
            metrics.speedKmh = Double(speed) / 100.0
        }
        if !applyOptionalFields(flags: flags, cursor: &cursor, into: &metrics) {
            return nil
        }
        if metrics.paceMinPerKm == nil, let speed = metrics.speedKmh, speed > 0 {
            metrics.paceMinPerKm = 60.0 / speed
        }
        return metrics
    }

    // swiftlint:disable:next cyclomatic_complexity
    private static func applyOptionalFields(
        flags: Int,
        cursor: inout ByteCursor,
        into metrics: inout TreadmillParsedMetrics
    ) -> Bool {
        if flags & 0x0002 != 0 { guard cursor.readU16() != nil else { return false } }
        if flags & 0x0004 != 0 {
            guard let dist = cursor.readU24() else { return false }
            metrics.distanceMetres = Double(dist)
        }
        if flags & 0x0008 != 0 {
            guard let incline = cursor.readS16(), cursor.readS16() != nil else { return false }
            metrics.inclinePercent = Double(incline) / 10.0
        }
        if flags & 0x0010 != 0 {
            guard cursor.readU16() != nil, cursor.readU16() != nil else { return false }
        }
        if flags & 0x0020 != 0 {
            guard let pace = cursor.readU8() else { return false }
            if pace > 0 { metrics.paceMinPerKm = 10.0 / Double(pace) }
        }
        if flags & 0x0040 != 0 { guard cursor.readU8() != nil else { return false } }
        if flags & 0x0080 != 0 {
            guard let total = cursor.readU16(),
                  cursor.readU16() != nil,
                  cursor.readU8() != nil else { return false }
            metrics.energyKcal = Double(total)
        }
        if flags & 0x0100 != 0 {
            guard let heartRate = cursor.readU8() else { return false }
            metrics.machineHeartRateBpm = heartRate
        }
        if flags & 0x0200 != 0 { guard cursor.readU8() != nil else { return false } }
        if flags & 0x0400 != 0 {
            guard let elapsed = cursor.readU16() else { return false }
            metrics.elapsedSeconds = elapsed
        }
        if flags & 0x0800 != 0 { guard cursor.readU16() != nil else { return false } }
        if flags & 0x1000 != 0 {
            guard cursor.readS16() != nil, cursor.readS16() != nil else { return false }
        }
        if flags & 0x2000 != 0 {
            guard let steps = cursor.readU24() else { return false }
            metrics.steps = steps
        }
        return true
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

struct ByteCursor {
    private let data: Data
    private var index: Int = 0

    init(_ data: Data) { self.data = data }

    mutating func readU8() -> Int? {
        guard index < data.count else { return nil }
        defer { index += 1 }
        return Int(data[index])
    }

    mutating func readU16() -> Int? {
        guard let low = readU8(), let high = readU8() else { return nil }
        return low | (high << 8)
    }

    mutating func readS16() -> Int? {
        guard let unsigned = readU16() else { return nil }
        return unsigned & 0x8000 != 0 ? unsigned - 0x10000 : unsigned
    }

    mutating func readU24() -> Int64? {
        guard let byte0 = readU8(), let byte1 = readU8(), let byte2 = readU8() else { return nil }
        return Int64(byte0) | (Int64(byte1) << 8) | (Int64(byte2) << 16)
    }

    mutating func readU32() -> Int64? {
        guard let byte0 = readU8(),
              let byte1 = readU8(),
              let byte2 = readU8(),
              let byte3 = readU8() else { return nil }
        return Int64(byte0) | (Int64(byte1) << 8) | (Int64(byte2) << 16) | (Int64(byte3) << 24)
    }
}
