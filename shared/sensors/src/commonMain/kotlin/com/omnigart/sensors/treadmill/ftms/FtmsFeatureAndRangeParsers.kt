package com.omnigart.sensors.treadmill.ftms

import com.omnigart.sensors.ble.BleByteReaders
import com.omnigart.sensors.treadmill.FitnessMachineFeatures
import com.omnigart.sensors.treadmill.TreadmillInclineRange
import com.omnigart.sensors.treadmill.TreadmillSpeedRange

/** Fitness Machine Feature (0x2ACC) — 4 byte machine features + 4 byte target setting. */
object FtmsFeatureParser {
    fun parse(bytes: ByteArray): Result<FitnessMachineFeatures> = runCatching {
        BleByteReaders.requireRemaining(bytes, 0, 8, "fitness machine feature")
        val machine = BleByteReaders.uint32Le(bytes, 0)
        val target = BleByteReaders.uint32Le(bytes, 4)
        FitnessMachineFeatures(
            averageSpeedSupported = bit(machine, 0),
            cadenceSupported = bit(machine, 1),
            totalDistanceSupported = bit(machine, 2),
            inclinationSupported = bit(machine, 3),
            elevationGainSupported = bit(machine, 4),
            paceSupported = bit(machine, 5),
            stepCountSupported = bit(machine, 6),
            resistanceLevelSupported = bit(machine, 7),
            strideCountSupported = bit(machine, 8),
            expendedEnergySupported = bit(machine, 9),
            heartRateMeasurementSupported = bit(machine, 10),
            metabolicEquivalentSupported = bit(machine, 11),
            elapsedTimeSupported = bit(machine, 12),
            remainingTimeSupported = bit(machine, 13),
            powerMeasurementSupported = bit(machine, 14),
            forceOnBeltAndPowerOutputSupported = bit(machine, 15),
            userDataRetentionSupported = bit(machine, 16),
            speedTargetSettingSupported = bit(target, 0),
            inclinationTargetSettingSupported = bit(target, 1),
            resistanceTargetSettingSupported = bit(target, 2),
            powerTargetSettingSupported = bit(target, 3),
            heartRateTargetSettingSupported = bit(target, 4),
            targetedExpendedEnergyConfigurationSupported = bit(target, 5),
            targetedStepNumberConfigurationSupported = bit(target, 6),
            targetedStrideNumberConfigurationSupported = bit(target, 7),
            targetedDistanceConfigurationSupported = bit(target, 8),
            targetedTrainingTimeConfigurationSupported = bit(target, 9),
            targetedTimeInTwoHeartRateZonesConfigurationSupported = bit(target, 10),
            targetedTimeInThreeHeartRateZonesConfigurationSupported = bit(target, 11),
            targetedTimeInFiveHeartRateZonesConfigurationSupported = bit(target, 12),
            indoorBikeSimulationControlSupported = bit(target, 13),
            wheelCircumferenceConfigurationSupported = bit(target, 14),
            spinDownControlSupported = bit(target, 15),
            targetedCadenceConfigurationSupported = bit(target, 16),
            rawMachineFeatures = machine,
            rawTargetSettingFeatures = target
        )
    }

    private fun bit(value: Long, index: Int): Boolean = ((value shr index) and 1L) == 1L
}

/** Supported Speed Range (0x2AD4) — min, max, increment as uint16 LE in 0.01 km/h. */
object FtmsSupportedSpeedRangeParser {
    fun parse(bytes: ByteArray): Result<TreadmillSpeedRange> = runCatching {
        BleByteReaders.requireRemaining(bytes, 0, 6, "supported speed range")
        TreadmillSpeedRange(
            minKmh = BleByteReaders.uint16Le(bytes, 0) / 100.0,
            maxKmh = BleByteReaders.uint16Le(bytes, 2) / 100.0,
            incrementKmh = BleByteReaders.uint16Le(bytes, 4) / 100.0
        )
    }
}

/**
 * Supported Inclination Range (0x2AD5) — min/max sint16 LE 0.1 %, increment uint16 LE 0.1 %.
 */
object FtmsSupportedInclinationRangeParser {
    fun parse(bytes: ByteArray): Result<TreadmillInclineRange> = runCatching {
        BleByteReaders.requireRemaining(bytes, 0, 6, "supported inclination range")
        TreadmillInclineRange(
            minPercent = BleByteReaders.sint16Le(bytes, 0) / 10.0,
            maxPercent = BleByteReaders.sint16Le(bytes, 2) / 10.0,
            incrementPercent = BleByteReaders.uint16Le(bytes, 4) / 10.0
        )
    }
}
