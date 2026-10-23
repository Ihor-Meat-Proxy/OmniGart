import Foundation

/// Parses Bluetooth SIG Heart Rate Measurement (0x2A37).
/// Logic mirrored from `shared/sensors` Kotlin parser — keep fixtures in sync.
public enum HeartRateMeasurementParser {
    private static let flagHrUint16 = 0x01
    private static let flagContactDetected = 0x02
    private static let flagContactSupported = 0x04
    private static let flagEnergyExpended = 0x08
    private static let flagRrIntervals = 0x10
    private static let ticksPerSecond = 1024.0

    public static func parse(_ data: Data) throws -> ParsedHeartRateMeasurement {
        guard !data.isEmpty else {
            throw SensorError.emptyCharacteristic
        }
        let bytes = [UInt8](data)
        let flags = Int(bytes[0])
        var offset = 1

        let hrIsUint16 = (flags & flagHrUint16) != 0
        let contactSupported = (flags & flagContactSupported) != 0
        let contactDetected = contactSupported && (flags & flagContactDetected) != 0
        let energyPresent = (flags & flagEnergyExpended) != 0
        let rrPresent = (flags & flagRrIntervals) != 0

        let heartRate: Int
        if hrIsUint16 {
            guard bytes.count >= offset + 2 else { throw SensorError.truncatedPayload }
            heartRate = Int(bytes[offset]) | (Int(bytes[offset + 1]) << 8)
            offset += 2
        } else {
            guard bytes.count >= offset + 1 else { throw SensorError.truncatedPayload }
            heartRate = Int(bytes[offset])
            offset += 1
        }

        var energy: Int?
        if energyPresent {
            guard bytes.count >= offset + 2 else { throw SensorError.truncatedPayload }
            energy = Int(bytes[offset]) | (Int(bytes[offset + 1]) << 8)
            offset += 2
        }

        var rrTicks: [Int] = []
        if rrPresent {
            while offset + 1 < bytes.count {
                let tick = Int(bytes[offset]) | (Int(bytes[offset + 1]) << 8)
                rrTicks.append(tick)
                offset += 2
            }
        }

        let rrSeconds = rrTicks.map { Double($0) / ticksPerSecond }

        return ParsedHeartRateMeasurement(
            heartRateBpm: heartRate,
            heartRateIsUint16: hrIsUint16,
            sensorContactSupported: contactSupported,
            sensorContactDetected: contactDetected,
            energyExpendedKj: energy,
            rrIntervalTicks: rrTicks,
            rrIntervalsSeconds: rrSeconds
        )
    }
}

public enum SensorError: Error, Sendable {
    case emptyCharacteristic
    case truncatedPayload
    case notConnected
    case unsupportedFeature
    case polarSdk(String)
}
