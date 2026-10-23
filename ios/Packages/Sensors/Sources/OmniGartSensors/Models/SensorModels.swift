import Foundation

/// Mirror of shared KMP sensor models for the Swift side.
/// Keep field names aligned with `com.omnigart.sensors` for future SKIE / binding.

public struct HeartRateSample: Sendable, Equatable {
    public let epochMillis: Int64
    public let bpm: Int
    public let source: HeartRateSource
    public let rrIntervalsMs: [Double]

    public init(
        epochMillis: Int64,
        bpm: Int,
        source: HeartRateSource,
        rrIntervalsMs: [Double] = []
    ) {
        self.epochMillis = epochMillis
        self.bpm = bpm
        self.source = source
        self.rrIntervalsMs = rrIntervalsMs
    }
}

public enum HeartRateSource: String, Sendable {
    case bleHrs
    case polarLive
    case polarOffline
    case watchWorkout
    case merged
}

public struct ParsedHeartRateMeasurement: Sendable, Equatable {
    public let heartRateBpm: Int
    public let heartRateIsUint16: Bool
    public let sensorContactSupported: Bool
    public let sensorContactDetected: Bool
    public let energyExpendedKj: Int?
    public let rrIntervalTicks: [Int]
    public let rrIntervalsSeconds: [Double]
}

public enum BodySensorLocation: Int, Sendable {
    case other = 0
    case chest = 1
    case wrist = 2
    case finger = 3
    case hand = 4
    case earLobe = 5
    case foot = 6
}

public struct PolarDeviceInfo: Sendable, Equatable {
    public let deviceId: String
    public let address: String?
    public let name: String?
    public let isH10: Bool
    public let batteryPercent: Int?

    public init(
        deviceId: String,
        address: String? = nil,
        name: String? = nil,
        isH10: Bool = false,
        batteryPercent: Int? = nil
    ) {
        self.deviceId = deviceId
        self.address = address
        self.name = name
        self.isH10 = isH10
        self.batteryPercent = batteryPercent
    }
}

public enum H10SampleType: String, Sendable {
    case hr
    case rr
}
