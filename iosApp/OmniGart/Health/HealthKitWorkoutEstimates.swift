import Foundation

extension HealthKitWorkoutWriter {
    /// Rough 1-minute HR recovery: peak near end minus HR ~60s after end (when samples exist).
    static func estimateHeartRateRecoveryBpm(samples: [HeartRatePoint], workoutEnd: Date) -> Double? {
        let nearEnd = samples.filter {
            $0.date >= workoutEnd.addingTimeInterval(-90) && $0.date <= workoutEnd
        }
        let after = samples.filter {
            $0.date > workoutEnd && $0.date <= workoutEnd.addingTimeInterval(75)
        }
        guard let peak = nearEnd.map(\.bpm).max(), let recovered = after.last?.bpm else {
            return nil
        }
        let delta = peak - recovered
        return delta > 0 ? delta : nil
    }

    /// Wellness effort score 0–10 from average HR / estimated max (Tanaka-style 208 − 0.7·age).
    static func estimateEffortScore(averageBpm: Double?, ageYears: Int) -> Double? {
        guard let averageBpm, averageBpm > 0, ageYears > 0 else { return nil }
        let hrMax = 208.0 - 0.7 * Double(ageYears)
        guard hrMax > 0 else { return nil }
        return min(10, max(0, (averageBpm / hrMax) * 10.0))
    }
}
