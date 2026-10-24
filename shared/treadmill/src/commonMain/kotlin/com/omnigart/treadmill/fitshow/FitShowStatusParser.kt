package com.omnigart.treadmill.fitshow

/**
 * FitShow status (`0x51`) payload parser.
 *
 * Classic ≤25 km/h machines use a single-byte speed field (0.1 km/h).
 * When the device reports a max speed above 25 km/h, speed becomes uint16 LE —
 * pass [speedAsWord]=true after reading the speed-range reply.
 */
object FitShowStatusParser {
    enum class MachineState {
        Idle,
        Stopped,
        Starting,
        Running,
        Pausing,
        Stopping,
        Error,
        Paused,
        Unknown
    }

    data class Status(
        val state: MachineState,
        val speedKmh: Double,
        val inclinePercent: Double,
        val elapsedSeconds: Int,
        val distanceMetres: Double,
        val energyKcal: Double,
        val steps: Int? = null,
        val heartRateBpm: Int? = null
    )

    fun parseStatusPayload(payload: ByteArray, speedAsWord: Boolean = false): Result<Status> =
        runCatching {
            require(payload.isNotEmpty()) { "Empty FitShow status" }
            var offset = 0
            val state = mapState(u8(payload, offset++))

            val speedKmh: Double
            if (speedAsWord) {
                requireRemaining(payload, offset, 2)
                speedKmh = u16Le(payload, offset) / 10.0
                offset += 2
            } else {
                requireRemaining(payload, offset, 1)
                speedKmh = u8(payload, offset) / 10.0
                offset += 1
            }

            requireRemaining(payload, offset, 1)
            val incline = u8(payload, offset).toDouble()
            offset += 1

            requireRemaining(payload, offset, 2)
            val elapsed = u16Le(payload, offset)
            offset += 2

            requireRemaining(payload, offset, 2)
            val distanceKm = u16Le(payload, offset) / 1000.0
            offset += 2

            requireRemaining(payload, offset, 2)
            val kcal = u16Le(payload, offset) / 10.0
            offset += 2

            var steps: Int? = null
            if (payload.size >= offset + 2) {
                steps = u16Le(payload, offset)
                offset += 2
            }

            var hr: Int? = null
            if (payload.size >= offset + 1) {
                val raw = u8(payload, offset)
                if (raw in 1..220) hr = raw
            }

            Status(
                state = state,
                speedKmh = speedKmh,
                inclinePercent = incline,
                elapsedSeconds = elapsed,
                distanceMetres = distanceKm * 1000.0,
                energyKcal = kcal,
                steps = steps,
                heartRateBpm = hr
            )
        }

    fun parseNotification(bytes: ByteArray, speedAsWord: Boolean = false): Result<Status> {
        val frame = FitShowCodec.decode(bytes).getOrElse { return Result.failure(it) }
        if (frame.cmd != FitShowCodec.CMD_STATUS) {
            return Result.failure(IllegalArgumentException("Not a FitShow status frame (cmd=${frame.cmd})"))
        }
        return parseStatusPayload(frame.payload, speedAsWord)
    }

    fun buildPollStatus(): ByteArray = FitShowCodec.encode(FitShowCodec.CMD_STATUS)

    fun buildStart(): ByteArray =
        FitShowCodec.encode(FitShowCodec.CMD_CONTROL, FitShowCodec.SCMD_START)

    fun buildStop(): ByteArray =
        FitShowCodec.encode(FitShowCodec.CMD_CONTROL, FitShowCodec.SCMD_STOP)

    fun buildPause(): ByteArray =
        FitShowCodec.encode(FitShowCodec.CMD_CONTROL, FitShowCodec.SCMD_PAUSE)

    /** Speed as 0.1 km/h byte (classic) + incline byte. */
    fun buildSetTarget(speedKmh: Double, inclinePercent: Double): ByteArray {
        val speedByte = (speedKmh * 10.0).toInt().coerceIn(0, 255)
        val inclineByte = inclinePercent.toInt().coerceIn(0, 40)
        return FitShowCodec.encode(
            FitShowCodec.CMD_CONTROL,
            FitShowCodec.SCMD_SET_TARGET,
            byteArrayOf(speedByte.toByte(), inclineByte.toByte())
        )
    }

    private fun mapState(value: Int): MachineState =
        when (value) {
            0 -> MachineState.Idle
            1 -> MachineState.Stopped
            2 -> MachineState.Starting
            3 -> MachineState.Running
            4 -> MachineState.Stopping
            5 -> MachineState.Error
            10 -> MachineState.Paused
            else -> MachineState.Unknown
        }

    private fun u8(bytes: ByteArray, offset: Int): Int = bytes[offset].toInt() and 0xFF

    private fun u16Le(bytes: ByteArray, offset: Int): Int =
        u8(bytes, offset) or (u8(bytes, offset + 1) shl 8)

    private fun requireRemaining(bytes: ByteArray, offset: Int, need: Int) {
        require(bytes.size >= offset + need) { "FitShow status truncated" }
    }
}
