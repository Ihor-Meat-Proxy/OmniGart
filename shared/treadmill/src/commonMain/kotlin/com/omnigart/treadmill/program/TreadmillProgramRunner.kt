package com.omnigart.treadmill.program

/**
 * Advances through [TreadmillProgram] steps from elapsed time and optional distance.
 * Emits auto speed/incline when remote control is allowed, else prompt keys for the UI.
 */
object TreadmillProgramRunner {
    fun prompt(
        program: TreadmillProgram,
        elapsedSeconds: Double,
        distanceMetres: Double,
        allowRemoteControl: Boolean,
        controlAvailable: Boolean
    ): TreadmillProgramPrompt {
        var consumedSeconds = 0.0
        var consumedMetres = 0.0
        program.steps.forEachIndexed { index, step ->
            val stepSpan = step.goalValue
            val progress =
                when (step.goalKind) {
                    TreadmillStepGoalKind.DURATION_SECONDS -> {
                        val into = (elapsedSeconds - consumedSeconds).coerceAtLeast(0.0)
                        consumedSeconds += stepSpan
                        into
                    }
                    TreadmillStepGoalKind.DISTANCE_METRES -> {
                        val into = (distanceMetres - consumedMetres).coerceAtLeast(0.0)
                        consumedMetres += stepSpan
                        into
                    }
                }
            if (progress < stepSpan) {
                val remaining = stepSpan - progress
                val canAuto = allowRemoteControl && controlAvailable
                return TreadmillProgramPrompt(
                    stepIndex = index,
                    step = step,
                    remainingGoalValue = remaining,
                    completed = false,
                    autoSpeedKmh = step.targetSpeedKmh.takeIf { canAuto },
                    autoInclinePercent = step.targetInclinePercent.takeIf { canAuto },
                    promptKey =
                        if (canAuto) {
                            "treadmill_program_auto_step"
                        } else {
                            "treadmill_program_prompt_step"
                        }
                )
            }
        }
        val last = program.steps.last()
        return TreadmillProgramPrompt(
            stepIndex = program.steps.lastIndex,
            step = last,
            remainingGoalValue = 0.0,
            completed = true,
            autoSpeedKmh = null,
            autoInclinePercent = null,
            promptKey = "treadmill_program_complete"
        )
    }

    fun resolvedSpeedKmh(step: TreadmillProgramStep): Double? {
        step.targetSpeedKmh?.let { return it }
        val pace = step.targetPaceMinPerKm ?: return null
        if (pace <= 0.0) return null
        return 60.0 / pace
    }
}
