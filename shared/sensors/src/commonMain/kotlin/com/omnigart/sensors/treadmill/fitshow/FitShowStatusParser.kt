package com.omnigart.sensors.treadmill.fitshow

import com.omnigart.sensors.ble.BleByteReaders
import com.omnigart.sensors.ble.BleParseException
import com.omnigart.sensors.treadmill.TreadmillMachineState
import com.omnigart.sensors.treadmill.TreadmillProtocolKind
import com.omnigart.sensors.treadmill.TreadmillSample

/**
 * FitShow status (`0x51`) payload parser.
 *
 * Classic ≤25 km/h machines use a single-byte speed field (0.1 km/h).
 * When the device reports a max speed above 25 km/h, speed becomes uint16 LE —
 * pass [speedAsWord]=true after reading the speed-range reply.
 */
object FitShowStatusParser {
    fun parseStatusPayload(payload: ByteArray, speedAsWord: Boolean = false): Result<TreadmillSample> = runCatching {
        if (payload.isEmpty()) throw BleParseException("Empty FitShow status")
        var offset = 0
        val stateByte = BleByteReaders.uint8(payload, offset++)
        val state = mapState(stateByte)

        val speedKmh: Double
        if (speedAsWord) {
            BleByteReaders.requireRemaining(payload, offset, 2, "FitShow speed word")
            speedKmh = BleByteReaders.uint16Le(payload, offset) / 10.0
            offset += 2
        } else {
            BleByteReaders.requireRemaining(payload, offset, 1, "FitShow speed byte")
            speedKmh = BleByteReaders.uint8(payload, offset) / 10.0
            offset += 1
        }

        BleByteReaders.requireRemaining(payload, offset, 1, "FitShow incline")
        val incline = BleByteReaders.uint8(payload, offset).toDouble()
        offset += 1

        BleByteReaders.requireRemaining(payload, offset, 2, "FitShow time")
        val elapsed = BleByteReaders.uint16Le(payload, offset)
        offset += 2

        BleByteReaders.requireRemaining(payload, offset, 2, "FitShow distance")
        val distanceKm = BleByteReaders.uint16Le(payload, offset) / 1000.0
        offset += 2

        BleByteReaders.requireRemaining(payload, offset, 2, "FitShow kcal")
        val kcal = BleByteReaders.uint16Le(payload, offset) / 10.0
        offset += 2

        var steps: Int? = null
        if (payload.size >= offset + 2) {
            steps = BleByteReaders.uint16Le(payload, offset)
            offset += 2
        }

        var hr: Int? = null
        if (payload.size >= offset + 1) {
            val raw = BleByteReaders.uint8(payload, offset)
            if (raw in 1..220) hr = raw
        }

        TreadmillSample(
            speedKmh = speedKmh,
            inclinePercent = incline,
            distanceMetres = distanceKm * 1000.0,
            elapsedSeconds = elapsed,
            totalEnergyKcal = kcal,
            heartRateBpm = hr,
            steps = steps,
            protocol = TreadmillProtocolKind.FitShow,
            machineState = state
        )
    }

    fun parseNotification(bytes: ByteArray, speedAsWord: Boolean = false): Result<TreadmillSample> {
        val frame = FitShowCodec.decode(bytes).getOrElse { return Result.failure(it) }
        if (frame.cmd != FitShowCodec.CMD_STATUS) {
            return Result.failure(BleParseException("Not a FitShow status frame (cmd=${frame.cmd})"))
        }
        return parseStatusPayload(frame.payload, speedAsWord)
    }

    fun buildPollStatus(): ByteArray = FitShowCodec.encode(FitShowCodec.CMD_STATUS)

    fun buildStart(): ByteArray = FitShowCodec.encode(FitShowCodec.CMD_CONTROL, FitShowCodec.SCMD_START)

    fun buildStop(): ByteArray = FitShowCodec.encode(FitShowCodec.CMD_CONTROL, FitShowCodec.SCMD_STOP)

    fun buildPause(): ByteArray = FitShowCodec.encode(FitShowCodec.CMD_CONTROL, FitShowCodec.SCMD_PAUSE)

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

    private fun mapState(value: Int): TreadmillMachineState = when (value) {
        0 -> TreadmillMachineState.Idle
        1 -> TreadmillMachineState.Stopped
        2 -> TreadmillMachineState.Starting
        3 -> TreadmillMachineState.Running
        4 -> TreadmillMachineState.Stopping
        5 -> TreadmillMachineState.Error
        10 -> TreadmillMachineState.Paused
        else -> TreadmillMachineState.Unknown
    }
}
