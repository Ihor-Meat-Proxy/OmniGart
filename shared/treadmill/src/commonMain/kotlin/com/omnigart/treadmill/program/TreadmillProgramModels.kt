package com.omnigart.treadmill.program

/**
 * Built-in treadmill run / walk programs.
 *
 * Steps prescribe duration or distance plus optional speed, incline, pace, or HR zone.
 * When FTMS (or FitShow) remote control is armed, the session auto-writes speed/incline;
 * otherwise the UI shows the same targets as prompts.
 */
enum class TreadmillActivityMode {
    RUN,
    WALK
}

enum class TreadmillStepGoalKind {
    DURATION_SECONDS,
    DISTANCE_METRES
}

data class TreadmillProgramStep(
    val id: String,
    /** i18n key for the step label. */
    val titleKey: String,
    val goalKind: TreadmillStepGoalKind,
    val goalValue: Double,
    val targetSpeedKmh: Double? = null,
    val targetInclinePercent: Double? = null,
    /** Pace in minutes per kilometre when speed is not set. */
    val targetPaceMinPerKm: Double? = null,
    /** 1–5 zone index using the user's zone model. */
    val targetHrZone: Int? = null
)

data class TreadmillProgram(
    val id: String,
    val titleKey: String,
    val bodyKey: String,
    val mode: TreadmillActivityMode,
    val steps: List<TreadmillProgramStep>
) {
    init {
        require(id.isNotBlank())
        require(steps.isNotEmpty())
    }
}

/** Hint produced each tick for UI and optional BLE control writes. */
data class TreadmillProgramPrompt(
    val stepIndex: Int,
    val step: TreadmillProgramStep,
    val remainingGoalValue: Double,
    val completed: Boolean,
    val autoSpeedKmh: Double?,
    val autoInclinePercent: Double?,
    /** Human-readable prompt when remote control is off or unavailable. */
    val promptKey: String
)
