package com.omnigart.treadmill.ftms

/**
 * Parser for FTMS Treadmill Data characteristic `0x2ACD`.
 *
 * Spec: Bluetooth SIG Fitness Machine Service — Treadmill Data.
 * Little-endian. Flag bit 0 ("More Data") inverted: when bit0 == 0, Instantaneous Speed is present.
 */
object FtmsTreadmillDataParser {

    data class TreadmillData(
        val flags: Int,
        val instantaneousSpeedKmh: Double? = null,
        val averageSpeedKmh: Double? = null,
        val totalDistanceMetres: Long? = null,
        val inclinationPercent: Double? = null,
        val rampAngleDegrees: Double? = null,
        val positiveElevationGainMetres: Double? = null,
        val negativeElevationGainMetres: Double? = null,
        val instantaneousPaceMinPerKm: Double? = null,
        val averagePaceMinPerKm: Double? = null,
        val totalEnergyKcal: Int? = null,
        val energyPerHourKcal: Int? = null,
        val energyPerMinuteKcal: Int? = null,
        val heartRateBpm: Int? = null,
        val metabolicEquivalent: Double? = null,
        val elapsedTimeSeconds: Int? = null,
        val remainingTimeSeconds: Int? = null,
        val forceOnBeltNewtons: Int? = null,
        val powerOutputWatts: Int? = null,
        val steps: Long? = null
    )

    fun parse(bytes: ByteArray): Result<TreadmillData> = runCatching {
        require(bytes.size >= 2) { "Treadmill Data too short for flags" }
        val cursor = Cursor(bytes)
        val flags = cursor.u16()
        val instantaneousSpeed =
            if ((flags and 0x0001) == 0) cursor.u16().toDouble() / 100.0 else null
        val averageSpeed =
            if ((flags and 0x0002) != 0) cursor.u16().toDouble() / 100.0 else null
        val totalDistance =
            if ((flags and 0x0004) != 0) cursor.u24() else null
        val inclination =
            if ((flags and 0x0008) != 0) cursor.s16().toDouble() / 10.0 else null
        val rampAngle =
            if ((flags and 0x0008) != 0) cursor.s16().toDouble() / 10.0 else null
        val positiveElevation =
            if ((flags and 0x0010) != 0) cursor.u16().toDouble() / 10.0 else null
        val negativeElevation =
            if ((flags and 0x0010) != 0) cursor.u16().toDouble() / 10.0 else null
        val instantaneousPace =
            if ((flags and 0x0020) != 0) {
                val raw = cursor.u8()
                // Characteristic unit is 0.1 km/min → min/km = 10 / raw when raw > 0
                if (raw > 0) 10.0 / raw.toDouble() else null
            } else {
                null
            }
        val averagePace =
            if ((flags and 0x0040) != 0) {
                val raw = cursor.u8()
                if (raw > 0) 10.0 / raw.toDouble() else null
            } else {
                null
            }
        val totalEnergy =
            if ((flags and 0x0080) != 0) cursor.u16() else null
        val energyPerHour =
            if ((flags and 0x0080) != 0) cursor.u16() else null
        val energyPerMinute =
            if ((flags and 0x0080) != 0) cursor.u8() else null
        val heartRate =
            if ((flags and 0x0100) != 0) cursor.u8() else null
        val met =
            if ((flags and 0x0200) != 0) cursor.u8().toDouble() / 10.0 else null
        val elapsed =
            if ((flags and 0x0400) != 0) cursor.u16() else null
        val remaining =
            if ((flags and 0x0800) != 0) cursor.u16() else null
        val force =
            if ((flags and 0x1000) != 0) cursor.s16() else null
        val power =
            if ((flags and 0x1000) != 0) cursor.s16() else null
        val steps =
            if ((flags and 0x2000) != 0) cursor.u24() else null

        TreadmillData(
            flags = flags,
            instantaneousSpeedKmh = instantaneousSpeed,
            averageSpeedKmh = averageSpeed,
            totalDistanceMetres = totalDistance,
            inclinationPercent = inclination,
            rampAngleDegrees = rampAngle,
            positiveElevationGainMetres = positiveElevation,
            negativeElevationGainMetres = negativeElevation,
            instantaneousPaceMinPerKm = instantaneousPace,
            averagePaceMinPerKm = averagePace,
            totalEnergyKcal = totalEnergy,
            energyPerHourKcal = energyPerHour,
            energyPerMinuteKcal = energyPerMinute,
            heartRateBpm = heartRate,
            metabolicEquivalent = met,
            elapsedTimeSeconds = elapsed,
            remainingTimeSeconds = remaining,
            forceOnBeltNewtons = force,
            powerOutputWatts = power,
            steps = steps
        )
    }

    private class Cursor(private val bytes: ByteArray) {
        private var i = 0

        fun u8(): Int {
            require(i < bytes.size) { "truncated u8 at $i" }
            return bytes[i++].toInt() and 0xFF
        }

        fun u16(): Int {
            val lo = u8()
            val hi = u8()
            return lo or (hi shl 8)
        }

        fun s16(): Int {
            val u = u16()
            return if (u and 0x8000 != 0) u - 0x10000 else u
        }

        fun u24(): Long {
            val b0 = u8().toLong()
            val b1 = u8().toLong()
            val b2 = u8().toLong()
            return b0 or (b1 shl 8) or (b2 shl 16)
        }
    }
}
