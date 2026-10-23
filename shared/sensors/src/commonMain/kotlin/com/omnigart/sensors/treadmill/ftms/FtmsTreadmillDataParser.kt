package com.omnigart.sensors.treadmill.ftms

import com.omnigart.sensors.ble.BleByteReaders
import com.omnigart.sensors.ble.BleParseException
import com.omnigart.sensors.treadmill.TreadmillMachineState
import com.omnigart.sensors.treadmill.TreadmillProtocolKind
import com.omnigart.sensors.treadmill.TreadmillSample

/**
 * Parser for FTMS Treadmill Data (0x2ACD).
 *
 * Flags are uint16 LE. When bit 0 (More Data) is **clear**, Instantaneous Speed is present.
 * When bit 0 is **set**, this packet is a continuation without speed (merge with prior sample).
 *
 * Field order after flags follows Fitness Machine Service 1.0 §4.5.
 */
object FtmsTreadmillDataParser {
    private const val FLAG_MORE_DATA = 1 shl 0
    private const val FLAG_AVERAGE_SPEED = 1 shl 1
    private const val FLAG_TOTAL_DISTANCE = 1 shl 2
    private const val FLAG_INCLINATION = 1 shl 3
    private const val FLAG_ELEVATION_GAIN = 1 shl 4
    private const val FLAG_INSTANT_PACE = 1 shl 5
    private const val FLAG_AVERAGE_PACE = 1 shl 6
    private const val FLAG_EXPENDED_ENERGY = 1 shl 7
    private const val FLAG_HEART_RATE = 1 shl 8
    private const val FLAG_MET = 1 shl 9
    private const val FLAG_ELAPSED_TIME = 1 shl 10
    private const val FLAG_REMAINING_TIME = 1 shl 11
    private const val FLAG_FORCE_POWER = 1 shl 12

    private const val SINT16_NA = 0x7FFF
    private const val UINT16_NA = 0xFFFF
    private const val UINT8_NA = 0xFF

    fun parse(bytes: ByteArray): Result<TreadmillSample> = runCatching { parseOrThrow(bytes) }

    private fun parseOrThrow(bytes: ByteArray): TreadmillSample {
        BleByteReaders.requireRemaining(bytes, 0, 2, "flags")
        val flags = BleByteReaders.uint16Le(bytes, 0)
        var offset = 2

        var speedKmh: Double? = null
        if ((flags and FLAG_MORE_DATA) == 0) {
            BleByteReaders.requireRemaining(bytes, offset, 2, "instantaneous speed")
            speedKmh = BleByteReaders.uint16Le(bytes, offset) / 100.0
            offset += 2
        }

        var averageSpeedKmh: Double? = null
        if ((flags and FLAG_AVERAGE_SPEED) != 0) {
            BleByteReaders.requireRemaining(bytes, offset, 2, "average speed")
            averageSpeedKmh = BleByteReaders.uint16Le(bytes, offset) / 100.0
            offset += 2
        }

        var distanceMetres: Double? = null
        if ((flags and FLAG_TOTAL_DISTANCE) != 0) {
            BleByteReaders.requireRemaining(bytes, offset, 3, "total distance")
            distanceMetres = BleByteReaders.uint24Le(bytes, offset).toDouble()
            offset += 3
        }

        var inclinePercent: Double? = null
        var rampAngleDegrees: Double? = null
        if ((flags and FLAG_INCLINATION) != 0) {
            BleByteReaders.requireRemaining(bytes, offset, 4, "inclination")
            val inclineRaw = BleByteReaders.sint16Le(bytes, offset)
            val rampRaw = BleByteReaders.sint16Le(bytes, offset + 2)
            if (inclineRaw != SINT16_NA) inclinePercent = inclineRaw / 10.0
            if (rampRaw != SINT16_NA) rampAngleDegrees = rampRaw / 10.0
            offset += 4
        }

        var positiveElevationMetres: Double? = null
        var negativeElevationMetres: Double? = null
        if ((flags and FLAG_ELEVATION_GAIN) != 0) {
            BleByteReaders.requireRemaining(bytes, offset, 4, "elevation gain")
            positiveElevationMetres = BleByteReaders.uint16Le(bytes, offset) / 10.0
            negativeElevationMetres = BleByteReaders.uint16Le(bytes, offset + 2) / 10.0
            offset += 4
        }

        var instantaneousPaceMinPerKm: Double? = null
        if ((flags and FLAG_INSTANT_PACE) != 0) {
            BleByteReaders.requireRemaining(bytes, offset, 1, "instant pace")
            val pace = BleByteReaders.uint8(bytes, offset)
            if (pace != UINT8_NA) instantaneousPaceMinPerKm = pace / 10.0
            offset += 1
        }

        var averagePaceMinPerKm: Double? = null
        if ((flags and FLAG_AVERAGE_PACE) != 0) {
            BleByteReaders.requireRemaining(bytes, offset, 1, "average pace")
            val pace = BleByteReaders.uint8(bytes, offset)
            if (pace != UINT8_NA) averagePaceMinPerKm = pace / 10.0
            offset += 1
        }

        var totalEnergyKcal: Double? = null
        var energyPerHourKcal: Double? = null
        var energyPerMinuteKcal: Double? = null
        if ((flags and FLAG_EXPENDED_ENERGY) != 0) {
            BleByteReaders.requireRemaining(bytes, offset, 5, "expended energy")
            val total = BleByteReaders.uint16Le(bytes, offset)
            val perHour = BleByteReaders.uint16Le(bytes, offset + 2)
            val perMin = BleByteReaders.uint8(bytes, offset + 4)
            if (total != UINT16_NA) totalEnergyKcal = total.toDouble()
            if (perHour != UINT16_NA) energyPerHourKcal = perHour.toDouble()
            if (perMin != UINT8_NA) energyPerMinuteKcal = perMin.toDouble()
            offset += 5
        }

        var heartRateBpm: Int? = null
        if ((flags and FLAG_HEART_RATE) != 0) {
            BleByteReaders.requireRemaining(bytes, offset, 1, "heart rate")
            val hr = BleByteReaders.uint8(bytes, offset)
            if (hr != UINT8_NA) heartRateBpm = hr
            offset += 1
        }

        var met: Double? = null
        if ((flags and FLAG_MET) != 0) {
            BleByteReaders.requireRemaining(bytes, offset, 1, "MET")
            val raw = BleByteReaders.uint8(bytes, offset)
            if (raw != UINT8_NA) met = raw / 10.0
            offset += 1
        }

        var elapsedSeconds: Int? = null
        if ((flags and FLAG_ELAPSED_TIME) != 0) {
            BleByteReaders.requireRemaining(bytes, offset, 2, "elapsed time")
            elapsedSeconds = BleByteReaders.uint16Le(bytes, offset)
            offset += 2
        }

        var remainingSeconds: Int? = null
        if ((flags and FLAG_REMAINING_TIME) != 0) {
            BleByteReaders.requireRemaining(bytes, offset, 2, "remaining time")
            remainingSeconds = BleByteReaders.uint16Le(bytes, offset)
            offset += 2
        }

        var forceOnBeltNewtons: Int? = null
        var powerWatts: Int? = null
        if ((flags and FLAG_FORCE_POWER) != 0) {
            BleByteReaders.requireRemaining(bytes, offset, 4, "force/power")
            forceOnBeltNewtons = BleByteReaders.sint16Le(bytes, offset)
            powerWatts = BleByteReaders.sint16Le(bytes, offset + 2)
            offset += 4
        }

        if (offset > bytes.size) {
            throw BleParseException("Parser advanced past packet end")
        }

        return TreadmillSample(
            speedKmh = speedKmh,
            averageSpeedKmh = averageSpeedKmh,
            inclinePercent = inclinePercent,
            rampAngleDegrees = rampAngleDegrees,
            distanceMetres = distanceMetres,
            elapsedSeconds = elapsedSeconds,
            remainingSeconds = remainingSeconds,
            totalEnergyKcal = totalEnergyKcal,
            energyPerHourKcal = energyPerHourKcal,
            energyPerMinuteKcal = energyPerMinuteKcal,
            heartRateBpm = heartRateBpm,
            instantaneousPaceMinPerKm = instantaneousPaceMinPerKm,
            averagePaceMinPerKm = averagePaceMinPerKm,
            met = met,
            positiveElevationMetres = positiveElevationMetres,
            negativeElevationMetres = negativeElevationMetres,
            forceOnBeltNewtons = forceOnBeltNewtons,
            powerWatts = powerWatts,
            protocol = TreadmillProtocolKind.Ftms,
            machineState = TreadmillMachineState.Unknown,
            rawFlags = flags
        )
    }

    /** Merge a More-Data continuation into [base], preferring newer non-null fields. */
    fun merge(base: TreadmillSample, more: TreadmillSample): TreadmillSample = base.copy(
        speedKmh = more.speedKmh ?: base.speedKmh,
        averageSpeedKmh = more.averageSpeedKmh ?: base.averageSpeedKmh,
        inclinePercent = more.inclinePercent ?: base.inclinePercent,
        rampAngleDegrees = more.rampAngleDegrees ?: base.rampAngleDegrees,
        distanceMetres = more.distanceMetres ?: base.distanceMetres,
        elapsedSeconds = more.elapsedSeconds ?: base.elapsedSeconds,
        remainingSeconds = more.remainingSeconds ?: base.remainingSeconds,
        totalEnergyKcal = more.totalEnergyKcal ?: base.totalEnergyKcal,
        energyPerHourKcal = more.energyPerHourKcal ?: base.energyPerHourKcal,
        energyPerMinuteKcal = more.energyPerMinuteKcal ?: base.energyPerMinuteKcal,
        heartRateBpm = more.heartRateBpm ?: base.heartRateBpm,
        steps = more.steps ?: base.steps,
        instantaneousPaceMinPerKm =
        more.instantaneousPaceMinPerKm ?: base.instantaneousPaceMinPerKm,
        averagePaceMinPerKm = more.averagePaceMinPerKm ?: base.averagePaceMinPerKm,
        met = more.met ?: base.met,
        positiveElevationMetres = more.positiveElevationMetres ?: base.positiveElevationMetres,
        negativeElevationMetres = more.negativeElevationMetres ?: base.negativeElevationMetres,
        forceOnBeltNewtons = more.forceOnBeltNewtons ?: base.forceOnBeltNewtons,
        powerWatts = more.powerWatts ?: base.powerWatts,
        cadenceSpm = more.cadenceSpm ?: base.cadenceSpm,
        machineState =
        if (more.machineState != TreadmillMachineState.Unknown) {
            more.machineState
        } else {
            base.machineState
        },
        rawFlags = more.rawFlags ?: base.rawFlags
    )
}
