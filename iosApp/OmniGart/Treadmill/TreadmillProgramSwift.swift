import Foundation

/// Swift mirror of `TreadmillProgramCatalog` / runner for phone UI (keep in sync with Kotlin).
enum TreadmillProgramSwift {
    struct Step {
        let titleKey: String
        let durationSeconds: Double
        let speedKmh: Double?
        let inclinePercent: Double?
        let hrZone: Int?
    }

    struct Program {
        let id: String
        let titleKey: String
        let mode: TreadmillSessionKindSwift
        let steps: [Step]
    }

    struct Prompt {
        let stepIndex: Int
        let step: Step
        let promptKey: String
        let autoSpeedKmh: Double?
        let autoInclinePercent: Double?
        let completed: Bool
    }

    static let all: [Program] = [
        Program(
            id: "easy_run",
            titleKey: "treadmill_program_easy_run_title",
            mode: .run,
            steps: [
                Step(titleKey: "treadmill_step_warmup", durationSeconds: 300, speedKmh: 7, inclinePercent: 0, hrZone: 2),
                Step(titleKey: "treadmill_step_easy", durationSeconds: 1200, speedKmh: 9, inclinePercent: 0, hrZone: 2),
                Step(titleKey: "treadmill_step_cooldown", durationSeconds: 300, speedKmh: 6, inclinePercent: 0, hrZone: 1)
            ]
        ),
        Program(
            id: "intervals",
            titleKey: "treadmill_program_intervals_title",
            mode: .run,
            steps: intervalSteps()
        ),
        Program(
            id: "hill_incline",
            titleKey: "treadmill_program_hill_title",
            mode: .run,
            steps: [
                Step(titleKey: "treadmill_step_warmup", durationSeconds: 240, speedKmh: 7, inclinePercent: 0, hrZone: 2),
                Step(titleKey: "treadmill_step_hill", durationSeconds: 120, speedKmh: 8, inclinePercent: 4, hrZone: 3),
                Step(titleKey: "treadmill_step_recover", durationSeconds: 90, speedKmh: 7, inclinePercent: 1, hrZone: 2),
                Step(titleKey: "treadmill_step_hill", durationSeconds: 120, speedKmh: 8, inclinePercent: 6, hrZone: 3),
                Step(titleKey: "treadmill_step_recover", durationSeconds: 90, speedKmh: 7, inclinePercent: 1, hrZone: 2),
                Step(titleKey: "treadmill_step_hill", durationSeconds: 120, speedKmh: 8, inclinePercent: 8, hrZone: 4),
                Step(titleKey: "treadmill_step_cooldown", durationSeconds: 300, speedKmh: 6, inclinePercent: 0, hrZone: 1)
            ]
        ),
        Program(
            id: "tempo",
            titleKey: "treadmill_program_tempo_title",
            mode: .run,
            steps: [
                Step(titleKey: "treadmill_step_warmup", durationSeconds: 300, speedKmh: 7.5, inclinePercent: 0, hrZone: 2),
                Step(titleKey: "treadmill_step_tempo", durationSeconds: 1200, speedKmh: 11, inclinePercent: 0, hrZone: 3),
                Step(titleKey: "treadmill_step_cooldown", durationSeconds: 300, speedKmh: 6.5, inclinePercent: 0, hrZone: 1)
            ]
        ),
        Program(
            id: "fartlek",
            titleKey: "treadmill_program_fartlek_title",
            mode: .run,
            steps: fartlekSteps()
        ),
        Program(
            id: "progression",
            titleKey: "treadmill_program_progression_title",
            mode: .run,
            steps: [
                Step(titleKey: "treadmill_step_warmup", durationSeconds: 300, speedKmh: 7, inclinePercent: 0, hrZone: 2),
                Step(titleKey: "treadmill_step_easy", durationSeconds: 600, speedKmh: 9, inclinePercent: 0, hrZone: 2),
                Step(titleKey: "treadmill_step_build", durationSeconds: 600, speedKmh: 10.5, inclinePercent: 0, hrZone: 3),
                Step(titleKey: "treadmill_step_tempo", durationSeconds: 600, speedKmh: 11.5, inclinePercent: 0, hrZone: 3),
                Step(titleKey: "treadmill_step_cooldown", durationSeconds: 300, speedKmh: 6.5, inclinePercent: 0, hrZone: 1)
            ]
        ),
        Program(
            id: "walk_run_beginner",
            titleKey: "treadmill_program_walk_run_title",
            mode: .run,
            steps: walkRunSteps()
        ),
        Program(
            id: "recovery_walk",
            titleKey: "treadmill_program_recovery_walk_title",
            mode: .walk,
            steps: [
                Step(titleKey: "treadmill_step_warmup_walk", durationSeconds: 180, speedKmh: 4, inclinePercent: 0, hrZone: 1),
                Step(titleKey: "treadmill_step_easy_walk", durationSeconds: 1200, speedKmh: 5, inclinePercent: 1, hrZone: 1),
                Step(titleKey: "treadmill_step_cooldown_walk", durationSeconds: 180, speedKmh: 3.5, inclinePercent: 0, hrZone: 1)
            ]
        )
    ]

    static func programs(for mode: TreadmillSessionKindSwift) -> [Program] {
        all.filter { $0.mode == mode }
    }

    static func byId(_ id: String) -> Program? { all.first { $0.id == id } }

    static func prompt(
        program: Program,
        elapsedSeconds: Double,
        allowRemoteControl: Bool,
        controlAvailable: Bool
    ) -> Prompt {
        var consumed = 0.0
        for (index, step) in program.steps.enumerated() {
            let into = elapsedSeconds - consumed
            consumed += step.durationSeconds
            if into < step.durationSeconds {
                let canAuto = allowRemoteControl && controlAvailable
                return Prompt(
                    stepIndex: index,
                    step: step,
                    promptKey: canAuto ? "treadmill_program_auto_step" : "treadmill_program_prompt_step",
                    autoSpeedKmh: canAuto ? step.speedKmh : nil,
                    autoInclinePercent: canAuto ? step.inclinePercent : nil,
                    completed: false
                )
            }
        }
        let last = program.steps[program.steps.count - 1]
        return Prompt(
            stepIndex: program.steps.count - 1,
            step: last,
            promptKey: "treadmill_program_complete",
            autoSpeedKmh: nil,
            autoInclinePercent: nil,
            completed: true
        )
    }

    private static func intervalSteps() -> [Step] {
        var steps = [
            Step(titleKey: "treadmill_step_warmup", durationSeconds: 300, speedKmh: 7, inclinePercent: 0, hrZone: 2)
        ]
        for _ in 0..<4 {
            steps.append(
                Step(titleKey: "treadmill_step_hard", durationSeconds: 90, speedKmh: 12, inclinePercent: 0, hrZone: 4)
            )
            steps.append(
                Step(titleKey: "treadmill_step_recover", durationSeconds: 90, speedKmh: 7, inclinePercent: 0, hrZone: 2)
            )
        }
        steps.append(
            Step(titleKey: "treadmill_step_cooldown", durationSeconds: 300, speedKmh: 6, inclinePercent: 0, hrZone: 1)
        )
        return steps
    }

    private static func fartlekSteps() -> [Step] {
        [
            Step(titleKey: "treadmill_step_warmup", durationSeconds: 300, speedKmh: 7, inclinePercent: 0, hrZone: 2),
            Step(titleKey: "treadmill_step_surge", durationSeconds: 60, speedKmh: 13, inclinePercent: 0, hrZone: 4),
            Step(titleKey: "treadmill_step_easy", durationSeconds: 120, speedKmh: 8.5, inclinePercent: 0, hrZone: 2),
            Step(titleKey: "treadmill_step_surge", durationSeconds: 45, speedKmh: 14, inclinePercent: 0, hrZone: 5),
            Step(titleKey: "treadmill_step_easy", durationSeconds: 150, speedKmh: 8, inclinePercent: 0, hrZone: 2),
            Step(titleKey: "treadmill_step_surge", durationSeconds: 90, speedKmh: 12.5, inclinePercent: 1, hrZone: 4),
            Step(titleKey: "treadmill_step_easy", durationSeconds: 180, speedKmh: 8, inclinePercent: 0, hrZone: 2),
            Step(titleKey: "treadmill_step_cooldown", durationSeconds: 300, speedKmh: 6, inclinePercent: 0, hrZone: 1)
        ]
    }

    private static func walkRunSteps() -> [Step] {
        var steps = [
            Step(titleKey: "treadmill_step_warmup_walk", durationSeconds: 300, speedKmh: 4.5, inclinePercent: 0, hrZone: 1)
        ]
        for _ in 0..<4 {
            steps.append(
                Step(titleKey: "treadmill_step_jog", durationSeconds: 60, speedKmh: 7.5, inclinePercent: 0, hrZone: 2)
            )
            steps.append(
                Step(titleKey: "treadmill_step_walk", durationSeconds: 90, speedKmh: 4.5, inclinePercent: 0, hrZone: 1)
            )
        }
        steps.append(
            Step(
                titleKey: "treadmill_step_cooldown_walk",
                durationSeconds: 300,
                speedKmh: 4,
                inclinePercent: 0,
                hrZone: 1
            )
        )
        return steps
    }
}

enum TreadmillHrAlertSwift {
    static func evaluate(
        bpm: Int?,
        hrMax: Int,
        targetZone: Int?,
        alertsEnabled: Bool,
        secondsSinceLastSample: Double?
    ) -> String? {
        guard alertsEnabled else { return nil }
        if let secondsSinceLastSample, secondsSinceLastSample >= 12 {
            return "treadmill_hr_alert_dropout"
        }
        guard let bpm else { return nil }
        let highLimit = Int(Double(hrMax) * 0.95)
        if bpm >= highLimit {
            return "treadmill_hr_alert_high"
        }
        if let targetZone {
            let lower = Double(hrMax) * (0.5 + 0.1 * Double(targetZone - 1))
            let upper = Double(hrMax) * (0.5 + 0.1 * Double(targetZone))
            if Double(bpm) > upper { return "treadmill_hr_alert_above_zone" }
            if Double(bpm) < lower { return "treadmill_hr_alert_below_zone" }
        }
        return nil
    }
}
