import Foundation

/// Heart-rate samples around a just-finished workout, plus the average/max over the workout window.
struct WorkoutHeartRateSummary {
    /// All samples fetched, including a short tail after `end` used for recovery estimation.
    var allSamples: [HealthKitWorkoutWriter.HeartRatePoint]
    /// Samples recorded no later than the workout end.
    var during: [HealthKitWorkoutWriter.HeartRatePoint]
    var averageBpm: Double?
    var maxBpm: Double?

    static func load(start: Date, end: Date) async -> WorkoutHeartRateSummary {
        let samples =
            (try? await HealthKitWorkoutWriter.fetchHeartRate(
                from: start,
                to: end.addingTimeInterval(90)
            )) ?? []
        let during = samples.filter { $0.date <= end }
        let positive = during.map(\.bpm).filter { $0 > 0 }
        return WorkoutHeartRateSummary(
            allSamples: samples,
            during: during,
            averageBpm: positive.isEmpty ? nil : positive.reduce(0, +) / Double(positive.count),
            maxBpm: positive.max()
        )
    }
}
