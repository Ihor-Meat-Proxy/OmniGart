package com.omnigart.treadmill.ftms

/**
 * FTMS Fitness Machine Control Point (`0x2AD9`) encode / decode.
 *
 * Remote speed and incline writes must stay behind a user opt-in.
 */
object FtmsControlPoint {

    enum class OpCode(val code: Int) {
        REQUEST_CONTROL(0x00),
        RESET(0x01),
        SET_TARGET_SPEED(0x02),
        SET_TARGET_INCLINE(0x03),
        START_OR_RESUME(0x07),
        STOP_OR_PAUSE(0x08),
        RESPONSE(0x80);

        companion object {
            fun from(code: Int): OpCode? = entries.firstOrNull { it.code == code }
        }
    }

    enum class ResultCode(val code: Int) {
        SUCCESS(0x01),
        NOT_SUPPORTED(0x02),
        INCORRECT_PARAMETER(0x03),
        OPERATION_FAILED(0x04),
        CONTROL_NOT_PERMITTED(0x05);

        companion object {
            fun from(code: Int): ResultCode? = entries.firstOrNull { it.code == code }
        }
    }

    enum class StopPause(val code: Int) {
        STOP(0x01),
        PAUSE(0x02)
    }

    data class Response(
        val requestOpCode: Int,
        val result: ResultCode?,
        val rawResult: Int
    )

    fun requestControl(): ByteArray = byteArrayOf(OpCode.REQUEST_CONTROL.code.toByte())

    fun reset(): ByteArray = byteArrayOf(OpCode.RESET.code.toByte())

    fun startOrResume(): ByteArray = byteArrayOf(OpCode.START_OR_RESUME.code.toByte())

    fun stopOrPause(action: StopPause): ByteArray =
        byteArrayOf(OpCode.STOP_OR_PAUSE.code.toByte(), action.code.toByte())

    /** Target speed in km/h (resolution 0.01). */
    fun setTargetSpeedKmh(speedKmh: Double): ByteArray {
        require(speedKmh >= 0.0) { "speed must be >= 0" }
        val units = (speedKmh * 100.0).toInt().coerceIn(0, 0xFFFF)
        return byteArrayOf(
            OpCode.SET_TARGET_SPEED.code.toByte(),
            (units and 0xFF).toByte(),
            ((units shr 8) and 0xFF).toByte()
        )
    }

    /** Target incline in percent (resolution 0.1). */
    fun setTargetInclinePercent(inclinePercent: Double): ByteArray {
        val units = (inclinePercent * 10.0).toInt().coerceIn(-0x8000, 0x7FFF)
        val asU = units and 0xFFFF
        return byteArrayOf(
            OpCode.SET_TARGET_INCLINE.code.toByte(),
            (asU and 0xFF).toByte(),
            ((asU shr 8) and 0xFF).toByte()
        )
    }

    fun parseResponse(bytes: ByteArray): Result<Response> = runCatching {
        require(bytes.size >= 3) { "Control Point response too short" }
        require((bytes[0].toInt() and 0xFF) == OpCode.RESPONSE.code) {
            "Expected response opcode 0x80"
        }
        val request = bytes[1].toInt() and 0xFF
        val raw = bytes[2].toInt() and 0xFF
        Response(requestOpCode = request, result = ResultCode.from(raw), rawResult = raw)
    }
}
