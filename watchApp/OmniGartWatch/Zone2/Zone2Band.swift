import Foundation

/// Watch-side Zone 2 band. Keep cut-points aligned with
/// `com.omnigart.activity.hr.Zone2` (shared activity-algorithms).
struct Zone2Band: Equatable, Sendable {
    enum Method: String, Sendable {
        case hrrKarvonen
        case percentHrMax
        case lthr
    }

    var method: Method
    var lowerBpm: Int
    var upperBpm: Int

    static func karvonen(hrMax: Int, hrRest: Int) -> Zone2Band {
        // 60–70% HRR
        let reserve = Double(hrMax - hrRest)
        let lo = hrRest + Int((0.60 * reserve).rounded())
        let hi = hrRest + Int((0.70 * reserve).rounded())
        return Zone2Band(method: .hrrKarvonen, lowerBpm: lo, upperBpm: max(hi, lo + 1))
    }

    func contains(_ bpm: Int, hysteresisBpm: Int = 0) -> Bool {
        let pad = max(0, hysteresisBpm)
        return bpm >= (lowerBpm - pad) && bpm <= (upperBpm + pad)
    }
}

enum Zone2ZoneState: Equatable, Sendable {
    case below
    case inTarget
    case above
}
