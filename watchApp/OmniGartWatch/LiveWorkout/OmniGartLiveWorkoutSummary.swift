import Foundation

struct OmniGartLiveWorkoutSummary: Equatable, Sendable {
    var kind: OmniGartWorkoutKind
    var elapsed: TimeInterval
    var activeEnergyKcal: Double
    var basalEnergyKcal: Double
    var averageHeartRateBpm: Double
    var maxHeartRateBpm: Double
    var heartRateSamples: [Zone2HeartRateSample]
    var segmentCount: Int
    var startedAt: Date
    var endedAt: Date
    /// Strength set logs for device-transfer schema v2 (empty for non-strength).
    var sets: [OmniGartLoggedStrengthSet] = []

    var totalEnergyKcal: Double { activeEnergyKcal + basalEnergyKcal }
}
