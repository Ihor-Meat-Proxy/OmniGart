package com.omnigart.sensors.ble

/**
 * Android mirror of shared KMP `HeartRateMeasurementParser`.
 * Prefer calling into `:shared:sensors` once the android KMP target is wired;
 * this copy keeps the library standalone-buildable.
 */
data class ParsedHeartRateMeasurement(
    val heartRateBpm: Int,
    val heartRateIsUint16: Boolean,
    val sensorContactSupported: Boolean,
    val sensorContactDetected: Boolean,
    val energyExpendedKj: Int?,
    val rrIntervalTicks: List<Int>,
    val rrIntervalsSeconds: List<Double>,
)

object HeartRateMeasurementParser {
    private const val FLAG_HR_UINT16 = 0x01
    private const val FLAG_CONTACT_DETECTED = 0x02
    private const val FLAG_CONTACT_SUPPORTED = 0x04
    private const val FLAG_ENERGY_EXPENDED = 0x08
    private const val FLAG_RR_INTERVALS = 0x10
    private const val TICKS_PER_SECOND = 1024.0

    fun parse(payload: ByteArray): ParsedHeartRateMeasurement {
        require(payload.isNotEmpty()) { "empty HR measurement" }
        val flags = payload[0].toInt() and 0xFF
        var offset = 1
        val hrIsUint16 = flags and FLAG_HR_UINT16 != 0
        val contactSupported = flags and FLAG_CONTACT_SUPPORTED != 0
        val contactDetected = contactSupported && flags and FLAG_CONTACT_DETECTED != 0
        val energyPresent = flags and FLAG_ENERGY_EXPENDED != 0
        val rrPresent = flags and FLAG_RR_INTERVALS != 0

        val hr = if (hrIsUint16) {
            require(payload.size >= offset + 2)
            readU16(payload, offset).also { offset += 2 }
        } else {
            require(payload.size >= offset + 1)
            (payload[offset].toInt() and 0xFF).also { offset += 1 }
        }

        val energy = if (energyPresent) {
            require(payload.size >= offset + 2)
            readU16(payload, offset).also { offset += 2 }
        } else {
            null
        }

        val rrTicks = mutableListOf<Int>()
        if (rrPresent) {
            while (offset + 1 < payload.size) {
                rrTicks += readU16(payload, offset)
                offset += 2
            }
        }

        return ParsedHeartRateMeasurement(
            heartRateBpm = hr,
            heartRateIsUint16 = hrIsUint16,
            sensorContactSupported = contactSupported,
            sensorContactDetected = contactDetected,
            energyExpendedKj = energy,
            rrIntervalTicks = rrTicks,
            rrIntervalsSeconds = rrTicks.map { it / TICKS_PER_SECOND },
        )
    }

    private fun readU16(bytes: ByteArray, offset: Int): Int {
        val lo = bytes[offset].toInt() and 0xFF
        val hi = bytes[offset + 1].toInt() and 0xFF
        return lo or (hi shl 8)
    }
}
