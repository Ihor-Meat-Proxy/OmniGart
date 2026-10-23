package com.omnigart.sensors.treadmill.iconsole

import com.omnigart.sensors.ble.BleByteReaders
import com.omnigart.sensors.ble.BleParseException
import com.omnigart.sensors.treadmill.TreadmillMachineState
import com.omnigart.sensors.treadmill.TreadmillProtocolKind
import com.omnigart.sensors.treadmill.TreadmillSample

/**
 * iConsole READ reply parser (response OP typically 0xB2).
 *
 * Field layout follows published bike-centric reverse engineering. Speed and
 * distance units are applied the same way for treadmill trials; mark samples
 * provisional until a live capture validates offsets.
 */
object IConsoleTelemetryParser {
    private const val OP_READ_RESPONSE = 0xB2

    fun parsePacket(bytes: ByteArray): Result<TreadmillSample> = runCatching {
        val (op, body) = IConsoleCodec.decode(bytes).getOrThrow()
        // Response OP is often 0xB0 | (request low nibble) → 0xB2 for READ.
        if (op != OP_READ_RESPONSE && op != IConsoleCodec.OP_READ) {
            // Some firmwares echo with high nibble 0xB*.
            if ((op and 0xF0) != 0xB0) {
                throw BleParseException("Unexpected iConsole OP 0x${op.toString(16)}")
            }
        }
        parseBody(body)
    }

    fun parseBody(body: ByteArray): TreadmillSample {
        // Full READ reply is often ~21 bytes including header; body after OP may be ~19.
        // Accept either full packet body (starting at time fields) or raw 18+ data bytes.
        val data =
            if (body.size >= 18 && (body[0].toInt() and 0xFF) == 0xF0) {
                // Accidentally passed full packet without decode — strip header/op/cs.
                IConsoleCodec.decode(body).getOrThrow().second
            } else {
                body
            }
        BleByteReaders.requireRemaining(data, 0, 16, "iConsole telemetry")

        fun plus1(offset: Int): Int = IConsoleCodec.decodePlusOne(BleByteReaders.uint8(data, offset))

        val seconds =
            plus1(0) * 86400 + plus1(1) * 3600 + plus1(2) * 60 + plus1(3)
        val speedKmh = (100 * plus1(4) + plus1(5)) / 10.0
        val rpmOrCadence = (100 * plus1(6) + plus1(7)) / 10.0
        val distanceKm = (100 * plus1(8) + plus1(9)) / 10.0
        val kcal = (100 * plus1(10) + plus1(11)) / 10.0
        val hr = (100 * plus1(12) + plus1(13))
        val level = if (data.size > 16) plus1(16).toDouble() else null

        return TreadmillSample(
            speedKmh = speedKmh.coerceAtLeast(0.0),
            inclinePercent = level, // provisional: bikes use level; treadmills may map incline
            distanceMetres = (distanceKm * 1000.0).coerceAtLeast(0.0),
            elapsedSeconds = seconds.coerceAtLeast(0),
            totalEnergyKcal = kcal.coerceAtLeast(0.0),
            heartRateBpm = hr.takeIf { it in 1..220 },
            cadenceSpm = rpmOrCadence.takeIf { it > 0 },
            protocol = TreadmillProtocolKind.IConsole,
            machineState =
            if (speedKmh > 0.05) {
                TreadmillMachineState.Running
            } else {
                TreadmillMachineState.Idle
            }
        )
    }
}
