package com.omnigart.sensors.ble

/**
 * Parsed Heart Rate Measurement characteristic (0x2A37).
 *
 * Flags (byte 0) per Bluetooth SIG Heart Rate Service 1.0:
 * - bit 0: Heart Rate Value Format (0 = uint8 bpm, 1 = uint16 bpm little-endian)
 * - bits 1–2: Sensor Contact Status
 * - bit 3: Energy Expended Status (1 = uint16 kJ present)
 * - bit 4: RR-Interval bits present (each uint16 in units of 1/1024 s)
 * - bits 5–7: Reserved
 */
data class HeartRateMeasurement(
    val heartRateBpm: Int,
    val heartRateIsUint16: Boolean,
    val sensorContactSupported: Boolean,
    val sensorContactDetected: Boolean,
    val energyExpendedKj: Int?,
    /** RR intervals in seconds (converted from 1/1024 s ticks). */
    val rrIntervalsSeconds: List<Double>,
    /** Raw RR interval ticks in units of 1/1024 second. */
    val rrIntervalTicks: List<Int>,
) {
    /** RR intervals in milliseconds. */
    val rrIntervalsMs: List<Double>
        get() = rrIntervalsSeconds.map { it * 1000.0 }
}

object HeartRateMeasurementParser {
    private const val FLAG_HR_UINT16 = 0x01
    private const val FLAG_CONTACT_DETECTED = 0x02
    private const val FLAG_CONTACT_SUPPORTED = 0x04
    private const val FLAG_ENERGY_EXPENDED = 0x08
    private const val FLAG_RR_INTERVALS = 0x10

    private const val TICKS_PER_SECOND = 1024.0

    fun parse(payload: ByteArray): HeartRateMeasurement {
        require(payload.isNotEmpty()) { "Heart Rate Measurement characteristic is empty" }

        val flags = payload[0].toUByte().toInt()
        val hrIsUint16 = (flags and FLAG_HR_UINT16) != 0
        val contactSupported = (flags and FLAG_CONTACT_SUPPORTED) != 0
        val contactDetected = contactSupported && (flags and FLAG_CONTACT_DETECTED) != 0
        val energyPresent = (flags and FLAG_ENERGY_EXPENDED) != 0
        val rrPresent = (flags and FLAG_RR_INTERVALS) != 0

        var offset = 1

        val heartRateBpm = if (hrIsUint16) {
            require(payload.size >= offset + 2) { "Truncated uint16 heart rate" }
            readUint16Le(payload, offset).also { offset += 2 }
        } else {
            require(payload.size >= offset + 1) { "Truncated uint8 heart rate" }
            payload[offset].toUByte().toInt().also { offset += 1 }
        }

        val energyExpended = if (energyPresent) {
            require(payload.size >= offset + 2) { "Truncated energy expended" }
            readUint16Le(payload, offset).also { offset += 2 }
        } else {
            null
        }

        val rrTicks = mutableListOf<Int>()
        if (rrPresent) {
            while (offset + 1 < payload.size) {
                rrTicks += readUint16Le(payload, offset)
                offset += 2
            }
            // Odd trailing byte is a protocol violation; ignore rather than crash.
        }

        val rrSeconds = rrTicks.map { it / TICKS_PER_SECOND }

        return HeartRateMeasurement(
            heartRateBpm = heartRateBpm,
            heartRateIsUint16 = hrIsUint16,
            sensorContactSupported = contactSupported,
            sensorContactDetected = contactDetected,
            energyExpendedKj = energyExpended,
            rrIntervalsSeconds = rrSeconds,
            rrIntervalTicks = rrTicks,
        )
    }

    private fun readUint16Le(bytes: ByteArray, offset: Int): Int {
        val lo = bytes[offset].toUByte().toInt()
        val hi = bytes[offset + 1].toUByte().toInt()
        return lo or (hi shl 8)
    }
}
