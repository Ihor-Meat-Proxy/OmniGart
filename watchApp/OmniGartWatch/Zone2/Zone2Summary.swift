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

    /// Compact `m:ss` / `h:mm:ss` for watch glances.
    static func formatDuration(_ interval: TimeInterval) -> String {
        let total = max(0, Int(interval.rounded()))
        let h = total / 3600
        let m = (total % 3600) / 60
        let s = total % 60
        if h > 0 {
            return String(format: "%d:%02d:%02d", h, m, s)
        }
        return String(format: "%d:%02d", m, s)
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

    /// Downsample densely collected HR for Charts / sparkline (keeps endpoints).
    static func downsample(
        _ samples: [Zone2HeartRateSample],
        maxPoints: Int = 60
    ) -> [Zone2HeartRateSample] {
        guard maxPoints > 2, samples.count > maxPoints else { return samples }
        var out: [Zone2HeartRateSample] = []
        out.reserveCapacity(maxPoints)
        let lastIndex = samples.count - 1
        for i in 0..<maxPoints {
            let idx = i == maxPoints - 1
                ? lastIndex
                : (i * lastIndex) / (maxPoints - 1)
            out.append(samples[idx])
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
