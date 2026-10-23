import Foundation

/// One heart-rate sample collected during a Zone 2 session (for post-run chart).
struct Zone2HeartRateSample: Equatable, Sendable {
    let offsetSeconds: TimeInterval
    let bpm: Int
}

/// Immutable post-run snapshot shown after `Zone2WorkoutSession.stop()` succeeds.
struct Zone2Summary: Equatable, Sendable {
    var timeInZone: TimeInterval
    var distanceMetres: Double
    var averageCadenceSpm: Double
    var elapsed: TimeInterval
    var heartRateSamples: [Zone2HeartRateSample]
    var activeEnergyKcal: Double
    var basalEnergyKcal: Double
    var averageHeartRateBpm: Double
    var maxHeartRateBpm: Double

    var totalEnergyKcal: Double { activeEnergyKcal + basalEnergyKcal }

    /// Compact `m:ss` / `h:mm:ss` for watch glances.
    static func formatDuration(_ interval: TimeInterval) -> String {
        let total = max(0, Int(interval.rounded()))
        let hours = total / 3600
        let minutes = (total % 3600) / 60
        let seconds = total % 60
        if hours > 0 {
            return String(format: "%d:%02d:%02d", hours, minutes, seconds)
        }
        return String(format: "%d:%02d", minutes, seconds)
    }

    /// Metres below 1 km as whole metres; otherwise km with two decimals.
    static func formatDistance(_ metres: Double) -> String {
        if metres >= 1000 {
            return String(format: "%.2f km", metres / 1000)
        }
        return String(format: "%.0f m", max(0, metres))
    }

    /// Whole steps/min for glanceable summary.
    static func formatCadence(_ spm: Double) -> String {
        String(format: "%.0f", max(0, spm))
    }

    /// Whole kilocalories for live + summary glances.
    static func formatEnergyKcal(_ kcal: Double) -> String {
        String(format: "%.0f", max(0, kcal.rounded()))
    }

    /// Downsample densely collected HR for Charts / sparkline (keeps endpoints).
    static func downsample(
        _ samples: [Zone2HeartRateSample],
        maxPoints: Int = 60
    ) -> [Zone2HeartRateSample] {
        guard maxPoints > 2, samples.count > maxPoints else { return samples }
        var out: [Zone2HeartRateSample] = []
        out.reserveCapacity(maxPoints)
        let lastIndex = samples.count - 1
        for pointIndex in 0..<maxPoints {
            let sampleIndex = pointIndex == maxPoints - 1
                ? lastIndex
                : (pointIndex * lastIndex) / (maxPoints - 1)
            out.append(samples[sampleIndex])
        }
        return out
    }

    /// Mean of positive cadence readings; zero when none were recorded.
    static func averageCadence(samples: [Double]) -> Double {
        let positive = samples.filter { $0 > 0 }
        guard !positive.isEmpty else { return 0 }
        return positive.reduce(0, +) / Double(positive.count)
    }
}
