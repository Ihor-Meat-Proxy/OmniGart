import Foundation
import Shared

/// Schedules a Zone 2 continuous run into the system Workout app via WorkoutKit.
///
/// Dual path with the in-app watch Zone 2 session: same Karvonen (or optional) band,
/// delivered as a `HEART_RATE_ZONE` alert on the work block.
///
/// Gates (all must pass before `WorkoutScheduler` is touched):
/// - `FeatureFlags.workoutKitSchedulingEnabled` (default **false**, dark until ready)
/// - `OmniGartCapabilities.workoutKitSchedulingCompiledIn` — set
///   `OMNIGART_ENABLE_WORKOUTKIT=0` on Personal Team if signing fails
/// - OS / WorkoutKit availability (`WorkoutKitSchedulerBridge.isSupported()`)
enum Zone2WorkoutKitScheduler {
    enum Method: String, Sendable {
        case hrrKarvonen
        case percentHrMax
        case lthr
    }

    /// Plan dictionary matching `Zone2WorkoutKitPlans.continuousRun` / `WorkoutKitPlanBuilder`.
    static func planJSON(
        id: String,
        hrMax: Int,
        hrRest: Int,
        workSeconds: Int,
        scheduledStart: Date? = nil,
        method: Method = .hrrKarvonen,
        lthrBpm: Int? = nil,
        displayNameKey: String = "running.session.zone2",
        warmUpSeconds: Int = 300,
        coolDownSeconds: Int = 300
    ) -> [String: Any] {
        let band = zone2Band(hrMax: hrMax, hrRest: hrRest, method: method, lthrBpm: lthrBpm)
        var json: [String: Any] = [
            "id": id,
            "activityType": "RUNNING",
            "displayNameKey": displayNameKey,
            "blocks": [
                [
                    "kind": "WARM_UP",
                    "displayNameKey": "\(displayNameKey).warmup",
                    "goal": ["kind": "TIME_SECONDS", "value": Double(warmUpSeconds)],
                    "repeatCount": 1
                ],
                [
                    "kind": "WORK",
                    "displayNameKey": "\(displayNameKey).work",
                    "goal": ["kind": "TIME_SECONDS", "value": Double(workSeconds)],
                    "alerts": [
                        [
                            "kind": "HEART_RATE_ZONE",
                            "lower": Double(band.lowerBpm),
                            "upper": Double(band.upperBpm)
                        ]
                    ],
                    "repeatCount": 1
                ],
                [
                    "kind": "COOL_DOWN",
                    "displayNameKey": "\(displayNameKey).cooldown",
                    "goal": ["kind": "TIME_SECONDS", "value": Double(coolDownSeconds)],
                    "repeatCount": 1
                ]
            ]
        ]
        if let scheduledStart {
            json["scheduledStartEpochMillis"] = scheduledStart.timeIntervalSince1970 * 1000.0
        }
        return json
    }

    /// Builds the plan and schedules when feature flags / Personal Team capability allow.
    @MainActor
    static func schedule(
        id: String,
        hrMax: Int,
        hrRest: Int,
        workSeconds: Int,
        scheduledStart: Date? = nil,
        method: Method = .hrrKarvonen,
        lthrBpm: Int? = nil
    ) async -> WorkoutKitSchedulerBridge.ScheduleOutcome {
        guard FeatureFlags.shared.workoutKitSchedulingEnabled else {
            return WorkoutKitSchedulerBridge.ScheduleOutcome(
                ok: false,
                scheduledCount: 0,
                failureCode: "FEATURE_DISABLED",
                detail: "workoutKitSchedulingEnabled is false"
            )
        }
        guard OmniGartCapabilities.workoutKitSchedulingCompiledIn else {
            return WorkoutKitSchedulerBridge.ScheduleOutcome(
                ok: false,
                scheduledCount: 0,
                failureCode: "UNSUPPORTED_ON_PLATFORM",
                detail: "OMNIGART_ENABLE_WORKOUTKIT gate off (Personal Team)"
            )
        }
        let json = planJSON(
            id: id,
            hrMax: hrMax,
            hrRest: hrRest,
            workSeconds: workSeconds,
            scheduledStart: scheduledStart,
            method: method,
            lthrBpm: lthrBpm
        )
        return await WorkoutKitSchedulerBridge.shared.schedule(planJSON: json)
    }

    /// Keep cut-points aligned with `com.omnigart.activity.hr.Zone2`.
    static func zone2Band(
        hrMax: Int,
        hrRest: Int,
        method: Method = .hrrKarvonen,
        lthrBpm: Int? = nil
    ) -> (lowerBpm: Int, upperBpm: Int) {
        switch method {
        case .hrrKarvonen:
            // 60–70% HRR (Karvonen); matches Zone2.DEFAULT_*_FRACTION
            let reserve = Double(hrMax - hrRest)
            let lo = hrRest + Int(0.60 * reserve)
            let hi = hrRest + Int(0.70 * reserve)
            return (lo, max(hi, lo + 1))
        case .percentHrMax:
            let lo = Int(Double(hrMax) * 0.60)
            let hi = Int(Double(hrMax) * 0.70)
            return (lo, max(hi, lo + 1))
        case .lthr:
            let lthr = lthrBpm ?? 0
            let lo = Int((Double(lthr) * 0.80).rounded())
            let hi = Int((Double(lthr) * 0.90).rounded())
            return (lo, max(hi, lo + 1))
        }
    }
}
