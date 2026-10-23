import Foundation
import HealthKit
#if canImport(WorkoutKit)
import WorkoutKit
#endif

/// Builds WorkoutKit `CustomWorkout` values from OmniGart JSON plan dictionaries.
/// Strength / set-rep blocks are rejected (no WorkoutKit equivalent).
enum WorkoutKitPlanBuilder {
    enum BuilderError: Error, LocalizedError {
        case workoutKitUnavailable
        case strengthUnsupported
        case invalidPlan(String)

        var errorDescription: String? {
            switch self {
            case .workoutKitUnavailable:
                return "WorkoutKit is not available on this OS or build"
            case .strengthUnsupported:
                return "Strength set/rep workouts cannot be scheduled via WorkoutKit"
            case .invalidPlan(let detail):
                return detail
            }
        }
    }

    struct DraftBlock {
        let kind: String
        let displayName: String
        let goalKind: String
        let goalValue: Double?
        let alertKind: String?
        let alertLower: Double?
        let alertUpper: Double?
        let repeatCount: Int
    }

    struct Draft {
        let id: String
        let activity: String
        let displayName: String
        let blocks: [DraftBlock]
        let scheduledStart: Date?
    }

    static func draft(from json: [String: Any]) throws -> Draft {
        guard let id = json["id"] as? String, !id.isEmpty else {
            throw BuilderError.invalidPlan("missing id")
        }
        let activity = (json["activityType"] as? String) ?? "RUNNING"
        if activity == "STRENGTH" {
            throw BuilderError.strengthUnsupported
        }
        let displayName = (json["displayNameKey"] as? String) ?? id
        guard let rawBlocks = json["blocks"] as? [[String: Any]], !rawBlocks.isEmpty else {
            throw BuilderError.invalidPlan("blocks required")
        }
        let blocks: [DraftBlock] = try rawBlocks.map { block in
            let kind = (block["kind"] as? String) ?? "WORK"
            if kind == "STRENGTH_UNSUPPORTED" {
                throw BuilderError.strengthUnsupported
            }
            let goal = block["goal"] as? [String: Any]
            let alerts = (block["alerts"] as? [[String: Any]])?.first
            return DraftBlock(
                kind: kind,
                displayName: (block["displayNameKey"] as? String) ?? kind,
                goalKind: (goal?["kind"] as? String) ?? "OPEN",
                goalValue: goal?["value"] as? Double,
                alertKind: alerts?["kind"] as? String,
                alertLower: alerts?["lower"] as? Double,
                alertUpper: alerts?["upper"] as? Double,
                repeatCount: max(1, (block["repeatCount"] as? Int) ?? 1)
            )
        }
        let startMillis = json["scheduledStartEpochMillis"] as? Double
        let scheduledStart = startMillis.map { Date(timeIntervalSince1970: $0 / 1000.0) }
        return Draft(
            id: id,
            activity: activity,
            displayName: displayName,
            blocks: blocks,
            scheduledStart: scheduledStart
        )
    }

#if canImport(WorkoutKit)
    @available(iOS 17.0, watchOS 10.0, *)
    static func makeCustomWorkout(from draft: Draft) throws -> CustomWorkout {
        guard OmniGartCapabilities.workoutKitSchedulingAvailable else {
            throw BuilderError.workoutKitUnavailable
        }
        let activityType = hkActivity(draft.activity)
        var warmup: WorkoutStep?
        var cooldown: WorkoutStep?
        var blocks: [IntervalBlock] = []

        for block in draft.blocks {
            let step = try makeStep(block)
            switch block.kind {
            case "WARM_UP":
                warmup = step
            case "COOL_DOWN":
                cooldown = step
            case "WORK", "RECOVERY":
                let purpose: IntervalStep.Purpose = block.kind == "RECOVERY" ? .recovery : .work
                let interval = IntervalStep(purpose: purpose, step: step)
                blocks.append(IntervalBlock(steps: [interval], iterations: block.repeatCount))
            default:
                let interval = IntervalStep(purpose: .work, step: step)
                blocks.append(IntervalBlock(steps: [interval], iterations: block.repeatCount))
            }
        }

        return CustomWorkout(
            activity: activityType,
            location: .outdoor,
            displayName: draft.displayName,
            warmup: warmup,
            blocks: blocks,
            cooldown: cooldown
        )
    }

    @available(iOS 17.0, watchOS 10.0, *)
    private static func makeStep(_ block: DraftBlock) throws -> WorkoutStep {
        let goal: WorkoutGoal = {
            switch block.goalKind {
            case "TIME_SECONDS":
                let seconds = block.goalValue ?? 0
                return .time(seconds, .seconds)
            case "DISTANCE_METRES":
                let metres = block.goalValue ?? 0
                return .distance(metres, .meters)
            default:
                return .open
            }
        }()

        // Heart-rate band alerts are attached when the OS API accepts a bpm range.
        // Zone 2 PR wires explicit bands; builder keeps goal-only steps if alert helpers differ by SDK.
        if block.alertKind == "HEART_RATE_ZONE",
           let lower = block.alertLower,
           let upper = block.alertUpper,
           lower <= upper {
            let alert = HeartRateRangeAlert(target: lower...upper)
            return WorkoutStep(goal: goal, alert: alert)
        }
        return WorkoutStep(goal: goal)
    }

    private static func hkActivity(_ raw: String) -> HKWorkoutActivityType {
        switch raw {
        case "RUNNING": return .running
        case "CYCLING": return .cycling
        case "SWIMMING": return .swimming
        case "WALKING": return .walking
        case "HIIT": return .highIntensityIntervalTraining
        case "YOGA": return .yoga
        default: return .other
        }
    }
#endif
}
