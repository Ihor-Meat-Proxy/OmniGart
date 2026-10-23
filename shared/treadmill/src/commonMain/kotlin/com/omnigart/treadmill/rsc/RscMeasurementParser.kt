package com.omnigart.treadmill.rsc

/**
 * Parser for RSC Measurement characteristic `0x2A53` (Running Speed and Cadence Service `0x1814`).
 *
 * Spec: Bluetooth SIG Running Speed and Cadence Service 1.0.
 */
object RscMeasurementParser {

    data class RscMeasurement(
        val flags: Int,
        val instantaneousSpeedMps: Double,
        val instantaneousCadenceSpm: Int,
        val instantaneousStrideLengthCm: Int? = null,
        val totalDistanceMetres: Double? = null,
        val isRunning: Boolean
    ) {
        val instantaneousSpeedKmh: Double get() = instantaneousSpeedMps * 3.6
    }

    fun parse(bytes: ByteArray): Result<RscMeasurement> = runCatching {
        require(bytes.size >= 4) { "RSC Measurement too short" }
        var i = 0
        fun u8(): Int = (bytes[i++].toInt() and 0xFF)
        fun u16(): Int {
            val lo = u8()
            val hi = u8()
            return lo or (hi shl 8)
        }
        fun u32(): Long {
            val b0 = u8().toLong()
            val b1 = u8().toLong()
            val b2 = u8().toLong()
            val b3 = u8().toLong()
            return b0 or (b1 shl 8) or (b2 shl 16) or (b3 shl 24)
        }

        val flags = u8()
        val speedRaw = u16()
        val cadence = u8()
        val stride =
            if ((flags and 0x01) != 0) {
                require(i + 2 <= bytes.size) { "truncated stride length" }
                u16()
            } else {
                null
            }
        val distanceDm =
            if ((flags and 0x02) != 0) {
                require(i + 4 <= bytes.size) { "truncated total distance" }
                u32()
            } else {
                null
            }
        val isRunning = (flags and 0x04) != 0

        RscMeasurement(
            flags = flags,
            instantaneousSpeedMps = speedRaw / 256.0,
            instantaneousCadenceSpm = cadence,
            instantaneousStrideLengthCm = stride,
            totalDistanceMetres = distanceDm?.let { it / 10.0 },
            isRunning = isRunning
        )
    }
}
