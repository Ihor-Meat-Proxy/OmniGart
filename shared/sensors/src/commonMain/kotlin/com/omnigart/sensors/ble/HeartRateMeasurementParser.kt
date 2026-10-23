package com.omnigart.sensors.ble

import com.omnigart.sensors.model.HeartRateMeasurement
import com.omnigart.sensors.model.SensorContactStatus

/**
 * Spec-compliant parser for Heart Rate Measurement (0x2A37).
 *
 * Flag bit layout (Bluetooth Heart Rate Service 1.0):
 * - bit 0: Heart Rate Value Format (0 = uint8, 1 = uint16)
 * - bits 1–2: Sensor Contact Status
 * - bit 3: Energy Expended Status (1 = present, uint16 kcal)
 * - bit 4: RR-Interval (1 = present, one or more uint16 in 1/1024 s units)
 * - bits 5–7: Reserved
 *
 * RR intervals are converted to milliseconds: `rrMs = rrUnits * 1000 / 1024`.
 */
object HeartRateMeasurementParser {
    private const val FLAG_HR_UINT16 = 0x01
    private const val FLAG_SENSOR_CONTACT_SUPPORTED = 0x04
    private const val FLAG_SENSOR_CONTACT_DETECTED = 0x02
    private const val FLAG_ENERGY_EXPENDED = 0x08
    private const val FLAG_RR_INTERVALS = 0x10

    /** Conversion from BLE 1/1024 s ticks to milliseconds. */
    fun rrUnitsToMillis(units: Int): Double = units * 1000.0 / 1024.0

    fun parse(bytes: ByteArray): Result<HeartRateMeasurement> {
        if (bytes.isEmpty()) {
            return Result.failure(HeartRateParseException("Empty packet"))
        }

        val flags = bytes[0].toInt() and 0xFF
        var offset = 1

        val heartRateBpm: Int
        val hrUint16 = (flags and FLAG_HR_UINT16) != 0
        if (hrUint16) {
            if (bytes.size < offset + 2) {
                return Result.failure(HeartRateParseException("Truncated uint16 heart rate"))
            }
            heartRateBpm = readUint16Le(bytes, offset)
            offset += 2
        } else {
            if (bytes.size < offset + 1) {
                return Result.failure(HeartRateParseException("Truncated uint8 heart rate"))
            }
            heartRateBpm = bytes[offset].toInt() and 0xFF
            offset += 1
        }

        val contactSupported = (flags and FLAG_SENSOR_CONTACT_SUPPORTED) != 0
        val contactDetected = (flags and FLAG_SENSOR_CONTACT_DETECTED) != 0
        val sensorContact = when {
            !contactSupported -> SensorContactStatus.NotSupported
            contactDetected -> SensorContactStatus.SupportedDetected
            else -> SensorContactStatus.SupportedNotDetected
        }

        val energyExpendedKcal: Int?
        if ((flags and FLAG_ENERGY_EXPENDED) != 0) {
            if (bytes.size < offset + 2) {
                return Result.failure(HeartRateParseException("Truncated energy expended"))
            }
            energyExpendedKcal = readUint16Le(bytes, offset)
            offset += 2
        } else {
            energyExpendedKcal = null
        }

        val rrIntervalsMs = mutableListOf<Double>()
        if ((flags and FLAG_RR_INTERVALS) != 0) {
            val remaining = bytes.size - offset
            if (remaining % 2 != 0) {
                return Result.failure(HeartRateParseException("Odd trailing bytes for RR intervals"))
            }
            var i = offset
            while (i + 1 < bytes.size) {
                val units = readUint16Le(bytes, i)
                rrIntervalsMs += rrUnitsToMillis(units)
                i += 2
            }
            offset = bytes.size
        } else if (offset < bytes.size) {
            return Result.failure(
                HeartRateParseException("Unexpected trailing bytes without RR flag (${bytes.size - offset})")
            )
        }

        return Result.success(
            HeartRateMeasurement(
                heartRateBpm = heartRateBpm,
                sensorContact = sensorContact,
                energyExpendedKcal = energyExpendedKcal,
                rrIntervalsMs = rrIntervalsMs,
                flags = flags,
                heartRateUint16 = hrUint16,
            )
        )
    }

    private fun readUint16Le(bytes: ByteArray, offset: Int): Int {
        val lo = bytes[offset].toInt() and 0xFF
        val hi = bytes[offset + 1].toInt() and 0xFF
        return lo or (hi shl 8)
    }
}

class HeartRateParseException(message: String) : Exception(message)
