import Foundation
import Shared

func strengthCatalog(_ key: String) -> String {
    StringCatalog.shared.t(key: key, locale: AppLocale.tag)
}

/// Field overrides applied to an existing `WorkoutSet` when it is re-saved.
struct StrengthSetPatch {
    var isWarmup: Bool?
    var isDropSet: Bool?
    var isFailure: Bool?
    var rpe: KotlinDouble?
    var note: String?
    var completed: Bool?
    var reps: KotlinInt?
    var weightKg: KotlinDouble?
}

struct StrengthNewSet {
    var exerciseId: String
    var reps: Int32
    var weightKg: KotlinDouble
    var rir: KotlinDouble?
    var unilateral: Bool
    var restSeconds: Int
}

/// Pure helpers for strength set tables, labels, and persistence payloads.
enum StrengthSetOps {
    static func exerciseOrder(_ session: Shared.Session) -> [String] {
        var seen: [String] = []
        for set in session.sets where !seen.contains(set.exerciseId) {
            seen.append(set.exerciseId)
        }
        return seen
    }

    static func setsFor(_ session: Shared.Session, _ exerciseId: String) -> [WorkoutSet] {
        session.sets.filter { $0.exerciseId == exerciseId }.sorted { $0.setIndex < $1.setIndex }
    }

    static func setLabel(_ sets: [WorkoutSet], setId: String) -> String {
        guard let target = sets.first(where: { $0.id == setId }) else { return "—" }
        if target.isWarmup { return "W" }
        var workingNumber = 0
        for set in sets where !set.isWarmup {
            workingNumber += 1
            if set.id == setId { return "\(workingNumber)" }
        }
        return "—"
    }

    static func previousSet(_ session: Shared.Session, exerciseId: String, before: Int) -> WorkoutSet? {
        session.sets
            .filter {
                $0.exerciseId == exerciseId && Int($0.setIndex) < before && $0.completed && !$0.isWarmup
            }
            .max(by: { $0.setIndex < $1.setIndex })
    }

    static func previousDisplay(_ previous: WorkoutSet?, unit: MassUnit) -> String? {
        guard let previous, let reps = previous.reps else { return nil }
        guard let kg = previous.weightKg else { return "\(reps.intValue) reps" }
        let display = MassUnits.shared.kgToDisplay(kg: kg.doubleValue, unit: unit)
        let unitLabel = unit == .lb ? "lb" : "kg"
        return String(format: "%.0f%@ × %d", display, unitLabel, reps.intValue)
    }

    static func displayWeightText(set: WorkoutSet, previous: WorkoutSet?, unit: MassUnit) -> String {
        let kg = set.weightKg?.doubleValue
            ?? set.targetWeightKg?.doubleValue
            ?? previous?.weightKg?.doubleValue
            ?? 0
        let display = MassUnits.shared.kgToDisplay(kg: kg, unit: unit)
        return String(format: display.truncatingRemainder(dividingBy: 1) == 0 ? "%.0f" : "%.1f", display)
    }

    static func displayRepsText(set: WorkoutSet, previous: WorkoutSet?) -> String {
        "\(set.reps?.intValue ?? previous?.reps?.intValue ?? 8)"
    }

    static func apply(_ patch: StrengthSetPatch, to set: WorkoutSet) -> WorkoutSet {
        WorkoutSet(
            id: set.id,
            exerciseId: set.exerciseId,
            setIndex: set.setIndex,
            reps: patch.reps ?? set.reps,
            weightKg: patch.weightKg ?? set.weightKg,
            durationSeconds: set.durationSeconds,
            distanceMetres: set.distanceMetres,
            rir: set.rir,
            rpe: patch.rpe ?? set.rpe,
            completed: patch.completed ?? set.completed,
            isWarmup: patch.isWarmup ?? set.isWarmup,
            isDropSet: patch.isDropSet ?? set.isDropSet,
            isFailure: patch.isFailure ?? set.isFailure,
            assistanceKg: set.assistanceKg,
            loggedPerHand: set.loggedPerHand,
            note: patch.note ?? set.note,
            restSeconds: set.restSeconds,
            targetWeightKg: set.targetWeightKg,
            targetReps: set.targetReps
        )
    }

    static func completionPatch(
        set: WorkoutSet,
        weightText: String?,
        repsText: String?,
        unit: MassUnit
    ) -> StrengthSetPatch {
        let displayWeight = Double(weightText ?? displayWeightText(set: set, previous: nil, unit: unit)) ?? 0
        let kg = MassUnits.shared.displayToKg(value: displayWeight, unit: unit)
        let reps = Int32(repsText ?? "\(set.reps?.intValue ?? 0)") ?? 0
        return StrengthSetPatch(
            completed: true,
            reps: KotlinInt(value: reps),
            weightKg: KotlinDouble(value: kg)
        )
    }

    static func add(_ new: StrengthNewSet, sessionId: String, core: OmniGartCore) {
        _ = core.strengthLogging.addSet(
            sessionId: sessionId,
            exerciseId: new.exerciseId,
            reps: new.reps,
            weightKg: new.weightKg,
            rir: new.rir,
            isWarmup: false,
            assistanceKg: nil,
            loggedPerHand: new.unilateral,
            completed: false,
            rpe: nil,
            isDropSet: false,
            isFailure: false,
            note: nil,
            restSeconds: KotlinInt(value: Int32(new.restSeconds)),
            targetWeightKg: new.weightKg,
            targetReps: KotlinInt(value: new.reps)
        )
    }

    static func dateFromEpochMillis(_ millis: Int64) -> Date {
        Date(timeIntervalSince1970: TimeInterval(millis) / 1000.0)
    }

    static func formatDuration(_ seconds: TimeInterval) -> String {
        let total = max(0, Int(seconds.rounded()))
        let hours = total / 3600
        let minutes = (total % 3600) / 60
        let secs = total % 60
        if hours > 0 {
            return String(format: "%d:%02d:%02d", hours, minutes, secs)
        }
        return String(format: "%d:%02d", minutes, secs)
    }
}
