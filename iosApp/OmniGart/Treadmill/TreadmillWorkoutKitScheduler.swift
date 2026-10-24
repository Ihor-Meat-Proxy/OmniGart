import Foundation
import Shared

/// Schedules a treadmill program as an indoor WorkoutKit custom workout.
enum TreadmillWorkoutKitScheduler {
    static func planJSON(programId: String, hrMax: Int, scheduledStart: Date? = nil) -> [String: Any]? {
        guard let program = TreadmillProgramSwift.byId(programId) else { return nil }
        let activity = program.mode == .walk ? "WALKING" : "RUNNING"
        let blocks: [[String: Any]] = program.steps.map { step in
            var entry: [String: Any] = [
                "kind": "WORK",
                "displayNameKey": step.titleKey,
                "goal": [
                    "kind": "TIME_SECONDS",
                    "value": step.durationSeconds
                ],
                "repeatCount": 1
            ]
            if let zone = step.hrZone {
                let lower = Double(hrMax) * (0.5 + 0.1 * Double(zone - 1))
                let upper = Double(hrMax) * (0.5 + 0.1 * Double(zone))
                entry["alerts"] = [[
                    "kind": "HEART_RATE_ZONE",
                    "lower": lower,
                    "upper": upper
                ]]
            }
            return entry
        }
        var json: [String: Any] = [
            "id": "treadmill.\(program.id)",
            "activityType": activity,
            "displayNameKey": program.titleKey,
            "location": "indoor",
            "blocks": blocks
        ]
        if let scheduledStart {
            json["scheduledStartEpochMillis"] = scheduledStart.timeIntervalSince1970 * 1000.0
        }
        return json
    }

    @MainActor
    static func schedule(
        programId: String,
        hrMax: Int,
        hrRest: Int? = nil
    ) async -> WorkoutKitSchedulerBridge.ScheduleOutcome {
        _ = hrRest
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
                detail: "OMNIGART_ENABLE_WORKOUTKIT gate off"
            )
        }
        guard let json = planJSON(programId: programId, hrMax: hrMax) else {
            return WorkoutKitSchedulerBridge.ScheduleOutcome(
                ok: false,
                scheduledCount: 0,
                failureCode: "INVALID_PLAN",
                detail: "Unknown treadmill program"
            )
        }
        return await WorkoutKitSchedulerBridge.shared.schedule(planJSON: json)
    }
}
