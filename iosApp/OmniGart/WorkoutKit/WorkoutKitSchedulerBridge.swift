import Foundation
import HealthKit
#if canImport(WorkoutKit)
import WorkoutKit
#endif

/// Schedules OmniGart plans into the system Workout app via WorkoutKit.
/// Soft-fails when the feature flag / Personal Team gate / OS support is missing.
@MainActor
final class WorkoutKitSchedulerBridge {
    static let shared = WorkoutKitSchedulerBridge()

    /// Mirrors `ScheduledWorkoutValidator.MAX_SCHEDULED_WORKOUTS` in shared Kotlin.
    static let maxScheduledWorkouts = 50

    struct ScheduleOutcome: Sendable {
        let ok: Bool
        let scheduledCount: Int
        let failureCode: String?
        let detail: String?
    }

    func isSupported() -> Bool {
        guard OmniGartCapabilities.workoutKitSchedulingCompiledIn else { return false }
        if #available(iOS 17.0, *) {
            return true
        }
        return false
    }

    func schedule(planJSON: [String: Any]) async -> ScheduleOutcome {
        guard isSupported() else {
            return ScheduleOutcome(
                ok: false,
                scheduledCount: 0,
                failureCode: "UNSUPPORTED_ON_PLATFORM",
                detail: "WorkoutKit gate off or OS too old"
            )
        }
        do {
            let draft = try WorkoutKitPlanBuilder.draft(from: planJSON)
#if canImport(WorkoutKit)
            if #available(iOS 17.0, *) {
                // Hop off MainActor: WorkoutScheduler is not Sendable (Xcode 26.2 / 27.2).
                return try await Self.scheduleAvailable(draft: draft)
            }
#endif
            return ScheduleOutcome(
                ok: false,
                scheduledCount: 0,
                failureCode: "UNSUPPORTED_ON_PLATFORM",
                detail: "WorkoutKit module unavailable"
            )
        } catch let error as WorkoutKitPlanBuilder.BuilderError {
            let code: String
            switch error {
            case .strengthUnsupported: code = "STRENGTH_NOT_SUPPORTED"
            case .invalidPlan: code = "INVALID_PLAN"
            case .workoutKitUnavailable: code = "UNSUPPORTED_ON_PLATFORM"
            }
            return ScheduleOutcome(ok: false, scheduledCount: 0, failureCode: code, detail: error.localizedDescription)
        } catch {
            return ScheduleOutcome(
                ok: false,
                scheduledCount: 0,
                failureCode: "UNKNOWN",
                detail: error.localizedDescription
            )
        }
    }

#if canImport(WorkoutKit)
    /// All `WorkoutScheduler` use stays in this nonisolated helper so the non-Sendable
    /// type never crosses out of a MainActor context (Swift 6 / Xcode 26.2 and 27.2).
    @available(iOS 17.0, *)
    nonisolated private static func scheduleAvailable(
        draft: WorkoutKitPlanBuilder.Draft
    ) async throws -> ScheduleOutcome {
        let custom = try WorkoutKitPlanBuilder.makeCustomWorkout(from: draft)
        let plan = WorkoutPlan(.custom(custom))
        let date = draft.scheduledStart ?? Date().addingTimeInterval(3600)
        let components = Calendar.current.dateComponents(
            [.year, .month, .day, .hour, .minute],
            from: date
        )

        let existingCount = await scheduledWorkoutCount()
        if existingCount >= maxScheduledWorkouts {
            return ScheduleOutcome(
                ok: false,
                scheduledCount: existingCount,
                failureCode: "SCHEDULE_CAP_REACHED",
                detail: "At WorkoutScheduler cap (\(maxScheduledWorkouts))"
            )
        }

        let scheduler = WorkoutScheduler.shared
        // Xcode 26.2: non-throwing; ignore authorization state for soft-fail scheduling.
        _ = await scheduler.requestAuthorization()
        await scheduler.schedule(plan, at: components)
        let afterCount = await scheduledWorkoutCount()
        return ScheduleOutcome(
            ok: true,
            scheduledCount: afterCount,
            failureCode: nil,
            detail: nil
        )
    }

    @available(iOS 17.0, *)
    nonisolated private static func scheduledWorkoutCount() async -> Int {
        await WorkoutScheduler.shared.scheduledWorkouts.count
    }
#endif
}
