import Foundation

enum RscMeasurementSwiftParser {
    static func parse(_ data: Data) -> TreadmillParsedMetrics? {
        guard data.count >= 4 else { return nil }
        var cursor = ByteCursor(data)
        let flags = cursor.readU8() ?? 0
        guard let speedRaw = cursor.readU16(), let cadence = cursor.readU8() else { return nil }
        if flags & 0x01 != 0 {
            guard cursor.readU16() != nil else { return nil }
        }
        var distance: Double?
        if flags & 0x02 != 0 {
            guard let raw = cursor.readU32() else { return nil }
            distance = Double(raw) / 10.0
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
        let heartRate = biased(bytes[14], bytes[15])
        return TreadmillParsedMetrics(
            protocolKind: .iconsole,
            speedKmh: speed,
            paceMinPerKm: speed > 0 ? 60.0 / speed : nil,
            distanceMetres: distanceKm * 1000.0,
            elapsedSeconds: minutes * 60 + seconds,
            machineHeartRateBpm: heartRate > 0 ? heartRate : nil,
            energyKcal: Double(kcal),
            cadenceSpm: rpm
        )
    }
}

enum FitShowSwiftParser {
    static let service = "0000FFF0-0000-1000-8000-00805F9B34FB"
    static let notify = "0000FFF1-0000-1000-8000-00805F9B34FB"
    static let write = "0000FFF2-0000-1000-8000-00805F9B34FB"
    static let cmdStatus = 0x51
    static let cmdControl = 0x53

    static func fcs(cmd: Int, payload: [UInt8]) -> UInt8 {
        var xor = cmd & 0xFF
        for byte in payload { xor ^= Int(byte) }
        return UInt8(xor & 0xFF)
    }

    static func encode(cmd: Int, payload: [UInt8] = []) -> Data {
        var bytes: [UInt8] = [0x02, UInt8(cmd & 0xFF)]
        bytes.append(contentsOf: payload)
        bytes.append(fcs(cmd: cmd, payload: payload))
        bytes.append(0x03)
        return Data(bytes)
    }

    static func pollStatus() -> Data { encode(cmd: cmdStatus) }

    static func setTarget(speedKmh: Double, inclinePercent: Double) -> Data {
        let speedByte = UInt8(max(0, min(255, Int(speedKmh * 10.0))))
        let inclineByte = UInt8(max(0, min(40, Int(inclinePercent))))
        return encode(cmd: cmdControl, payload: [0x02, speedByte, inclineByte])
    }

    static func parseNotification(_ data: Data, speedAsWord: Bool = false) -> TreadmillParsedMetrics? {
        let bytes = [UInt8](data)
        guard bytes.count >= 4, bytes.first == 0x02, bytes.last == 0x03 else { return nil }
        let cmd = Int(bytes[1])
        let payload = Array(bytes[2..<(bytes.count - 2)])
        guard fcs(cmd: cmd, payload: payload) == bytes[bytes.count - 2] else { return nil }
        guard cmd == cmdStatus, !payload.isEmpty else { return nil }
        return parseStatus(payload, speedAsWord: speedAsWord)
    }

    private static func parseStatus(_ payload: [UInt8], speedAsWord: Bool) -> TreadmillParsedMetrics? {
        var index = 0
        func takeU8() -> Int? {
            guard index < payload.count else { return nil }
            defer { index += 1 }
            return Int(payload[index])
        }
        func takeU16() -> Int? {
            guard let low = takeU8(), let high = takeU8() else { return nil }
            return low | (high << 8)
        }
        _ = takeU8()
        let speed: Double
        if speedAsWord {
            guard let raw = takeU16() else { return nil }
            speed = Double(raw) / 10.0
        } else {
            guard let raw = takeU8() else { return nil }
            speed = Double(raw) / 10.0
        }
        guard let incline = takeU8(), let elapsed = takeU16(), let dist = takeU16(),
              let kcal = takeU16() else { return nil }
        let steps = takeU16().map(Int64.init)
        let heartRate = takeU8().flatMap { $0 > 0 && $0 <= 220 ? $0 : nil }
        return TreadmillParsedMetrics(
            protocolKind: .fitshow,
            speedKmh: speed,
            paceMinPerKm: speed > 0 ? 60.0 / speed : nil,
            inclinePercent: Double(incline),
            distanceMetres: Double(dist),
            elapsedSeconds: elapsed,
            machineHeartRateBpm: heartRate,
            energyKcal: Double(kcal) / 10.0,
            steps: steps
        )
    }
}
