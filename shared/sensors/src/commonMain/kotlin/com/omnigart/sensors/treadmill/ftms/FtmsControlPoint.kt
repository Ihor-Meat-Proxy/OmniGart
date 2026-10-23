package com.omnigart.sensors.treadmill.ftms

import com.omnigart.sensors.ble.BleByteReaders
import com.omnigart.sensors.treadmill.TreadmillControlResult

/**
 * Fitness Machine Control Point (0x2AD9) builders and response parser.
 *
 * Control writes MUST be gated behind an explicit user action in the UI.
 * Always send [requestControl] and wait for a success indicate before other ops.
 */
object FtmsControlPoint {
    const val OP_REQUEST_CONTROL = 0x00
    const val OP_RESET = 0x01
    const val OP_SET_TARGET_SPEED = 0x02
    const val OP_SET_TARGET_INCLINATION = 0x03
    const val OP_START_OR_RESUME = 0x07
    const val OP_STOP_OR_PAUSE = 0x08
    const val OP_RESPONSE = 0x80

    const val STOP_PARAM_STOP = 0x01
    const val STOP_PARAM_PAUSE = 0x02

    fun requestControl(): ByteArray = byteArrayOf(OP_REQUEST_CONTROL.toByte())

    fun reset(): ByteArray = byteArrayOf(OP_RESET.toByte())

    fun startOrResume(): ByteArray = byteArrayOf(OP_START_OR_RESUME.toByte())

    fun stop(): ByteArray = byteArrayOf(OP_STOP_OR_PAUSE.toByte(), STOP_PARAM_STOP.toByte())

    fun pause(): ByteArray = byteArrayOf(OP_STOP_OR_PAUSE.toByte(), STOP_PARAM_PAUSE.toByte())

    /** Target speed in 0.01 km/h units (uint16 LE). */
    fun setTargetSpeed(kmh: Double): ByteArray {
        val units = (kmh * 100.0).toInt().coerceIn(0, 0xFFFF)
        return byteArrayOf(
            OP_SET_TARGET_SPEED.toByte(),
            (units and 0xFF).toByte(),
            ((units shr 8) and 0xFF).toByte()
        )
    }

    /** Target incline in 0.1 % units (sint16 LE). */
    fun setTargetInclination(percent: Double): ByteArray {
        val units = (percent * 10.0).toInt().coerceIn(-0x8000, 0x7FFF)
        val asU = units and 0xFFFF
        return byteArrayOf(
            OP_SET_TARGET_INCLINATION.toByte(),
            (asU and 0xFF).toByte(),
            ((asU shr 8) and 0xFF).toByte()
        )
    }

    fun parseResponse(bytes: ByteArray): Result<TreadmillControlResult> = runCatching {
        BleByteReaders.requireRemaining(bytes, 0, 3, "control point response")
        if (BleByteReaders.uint8(bytes, 0) != OP_RESPONSE) {
            error("Not a control point response (expected 0x80)")
        }
        val requestOpcode = BleByteReaders.uint8(bytes, 1)
        val resultCode = BleByteReaders.uint8(bytes, 2)
        TreadmillControlResult(
            requestOpcode = requestOpcode,
            resultCode = resultCode,
            success = resultCode == TreadmillControlResult.RESULT_SUCCESS
        )
    }
}
