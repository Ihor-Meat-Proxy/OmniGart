package com.omnigart.sensors.treadmill

/**
 * Live treadmill telemetry normalised across FTMS, FitShow-style, and iConsole parsers.
 *
 * Units are SI-friendly for the health store: km/h, percent grade, metres, seconds, kcal, bpm.
 */
data class TreadmillSample(
    val speedKmh: Double? = null,
    val averageSpeedKmh: Double? = null,
    val inclinePercent: Double? = null,
    val rampAngleDegrees: Double? = null,
    val distanceMetres: Double? = null,
    val elapsedSeconds: Int? = null,
    val remainingSeconds: Int? = null,
    val totalEnergyKcal: Double? = null,
    val energyPerHourKcal: Double? = null,
    val energyPerMinuteKcal: Double? = null,
    val heartRateBpm: Int? = null,
    val steps: Int? = null,
    val instantaneousPaceMinPerKm: Double? = null,
    val averagePaceMinPerKm: Double? = null,
    val met: Double? = null,
    val positiveElevationMetres: Double? = null,
    val negativeElevationMetres: Double? = null,
    val forceOnBeltNewtons: Int? = null,
    val powerWatts: Int? = null,
    val cadenceSpm: Double? = null,
    val protocol: TreadmillProtocolKind = TreadmillProtocolKind.Unknown,
    val machineState: TreadmillMachineState = TreadmillMachineState.Unknown,
    val rawFlags: Int? = null
)

enum class TreadmillProtocolKind {
    Ftms,
    FitShow,
    IConsole,
    Simulated,
    Unknown
}

enum class TreadmillMachineState {
    Unknown,
    Idle,
    Stopped,
    Paused,
    Running,
    Starting,
    Stopping,
    SafetyKeyRemoved,
    Error
}

data class TreadmillSpeedRange(val minKmh: Double, val maxKmh: Double, val incrementKmh: Double)

data class TreadmillInclineRange(val minPercent: Double, val maxPercent: Double, val incrementPercent: Double)

data class FitnessMachineFeatures(
    val averageSpeedSupported: Boolean = false,
    val cadenceSupported: Boolean = false,
    val totalDistanceSupported: Boolean = false,
    val inclinationSupported: Boolean = false,
    val elevationGainSupported: Boolean = false,
    val paceSupported: Boolean = false,
    val stepCountSupported: Boolean = false,
    val resistanceLevelSupported: Boolean = false,
    val strideCountSupported: Boolean = false,
    val expendedEnergySupported: Boolean = false,
    val heartRateMeasurementSupported: Boolean = false,
    val metabolicEquivalentSupported: Boolean = false,
    val elapsedTimeSupported: Boolean = false,
    val remainingTimeSupported: Boolean = false,
    val powerMeasurementSupported: Boolean = false,
    val forceOnBeltAndPowerOutputSupported: Boolean = false,
    val userDataRetentionSupported: Boolean = false,
    val speedTargetSettingSupported: Boolean = false,
    val inclinationTargetSettingSupported: Boolean = false,
    val resistanceTargetSettingSupported: Boolean = false,
    val powerTargetSettingSupported: Boolean = false,
    val heartRateTargetSettingSupported: Boolean = false,
    val targetedExpendedEnergyConfigurationSupported: Boolean = false,
    val targetedStepNumberConfigurationSupported: Boolean = false,
    val targetedStrideNumberConfigurationSupported: Boolean = false,
    val targetedDistanceConfigurationSupported: Boolean = false,
    val targetedTrainingTimeConfigurationSupported: Boolean = false,
    val targetedTimeInTwoHeartRateZonesConfigurationSupported: Boolean = false,
    val targetedTimeInThreeHeartRateZonesConfigurationSupported: Boolean = false,
    val targetedTimeInFiveHeartRateZonesConfigurationSupported: Boolean = false,
    val indoorBikeSimulationControlSupported: Boolean = false,
    val wheelCircumferenceConfigurationSupported: Boolean = false,
    val spinDownControlSupported: Boolean = false,
    val targetedCadenceConfigurationSupported: Boolean = false,
    val rawMachineFeatures: Long = 0,
    val rawTargetSettingFeatures: Long = 0
)

data class TreadmillControlResult(val requestOpcode: Int, val resultCode: Int, val success: Boolean) {
    companion object {
        const val RESULT_SUCCESS = 0x01
        const val RESULT_OP_CODE_NOT_SUPPORTED = 0x02
        const val RESULT_INVALID_PARAMETER = 0x03
        const val RESULT_OPERATION_FAILED = 0x04
        const val RESULT_CONTROL_NOT_PERMITTED = 0x05
    }
}

/**
 * Interval preset applied only after an explicit user action (never auto-written
 * to the Fitness Machine Control Point).
 */
data class TreadmillIntervalPreset(
    val id: String,
    val labelKey: String,
    val targetSpeedKmh: Double?,
    val targetInclinePercent: Double?,
    val durationSeconds: Int?
)

val DefaultTreadmillIntervalPresets: List<TreadmillIntervalPreset> =
    listOf(
        TreadmillIntervalPreset("easy", "treadmill_preset_easy", 5.0, 0.0, 300),
        TreadmillIntervalPreset("steady", "treadmill_preset_steady", 8.0, 1.0, 300),
        TreadmillIntervalPreset("tempo", "treadmill_preset_tempo", 10.0, 1.5, 180),
        TreadmillIntervalPreset("hill", "treadmill_preset_hill", 6.0, 6.0, 120),
        TreadmillIntervalPreset("walk", "treadmill_preset_walk", 4.0, 2.0, 300)
    )

fun TreadmillSample.paceMinPerKmFromSpeed(): Double? {
    val speed = speedKmh ?: return instantaneousPaceMinPerKm
    if (speed <= 0.0) return null
    return 60.0 / speed
}
