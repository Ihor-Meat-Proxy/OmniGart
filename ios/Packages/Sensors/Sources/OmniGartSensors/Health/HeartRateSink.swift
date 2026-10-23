import Foundation

/// Writes HR samples to HealthKit. No HealthKit import here — the Health adapter
/// PR supplies `HealthKitHeartRateSink`. This package only defines the contract.
public protocol HeartRateSink: AnyObject {
    func writeHeartRate(_ samples: [HeartRateSample]) async throws -> Int
}

public final class NoOpHeartRateSink: HeartRateSink {
    public init() {}
    public func writeHeartRate(_ samples: [HeartRateSample]) async throws -> Int { 0 }
}

public final class RecordingHeartRateSink: HeartRateSink {
    public private(set) var writes: [[HeartRateSample]] = []
    public init() {}
    public func writeHeartRate(_ samples: [HeartRateSample]) async throws -> Int {
        writes.append(samples)
        return samples.count
    }
}
