import Foundation
import HealthKit

extension HealthKitWorkoutWriter {
    struct EnergyInput: Sendable {
        var activeKcal: Double
        var basalKcal: Double
        /// When true, metadata marks energy as a wellness estimate (iPhone-only path).
        var isEstimate: Bool = false
    }

    struct HeartRatePoint: Sendable {
        var date: Date
        var bpm: Double
    }

    struct Segment: Sendable {
        var start: Date
        var end: Date
        var label: String
    }

    struct StrengthMetadata: Sendable {
        var totalSets: Int
        var totalReps: Int
        var totalWeightKg: Double
    }

    struct SaveOptions: Sendable {
        var activityType: HKWorkoutActivityType
        var start: Date
        var end: Date
        var energy: EnergyInput
        var heartRate: [HeartRatePoint] = []
        var pauseResumeDates: [(pause: Date, resume: Date?)] = []
        var segments: [Segment] = []
        var strength: StrengthMetadata?
        var indoor: Bool = true
        var saveMindfulMinutes: Bool = false
        var estimatedEffortScore: Double?
        var heartRateRecoveryBpm: Double?
    }

    struct Result: Sendable {
        var workoutUUID: UUID
        var activeKcal: Double
        var basalKcal: Double
        var averageHeartRateBpm: Double?
        var maxHeartRateBpm: Double?
        var energyIsEstimate: Bool
        var effortScore: Double?
        var heartRateRecoveryBpm: Double?
    }
}
