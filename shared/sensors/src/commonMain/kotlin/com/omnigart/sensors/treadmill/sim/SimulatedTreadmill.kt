package com.omnigart.sensors.treadmill.sim

import com.omnigart.sensors.treadmill.DefaultTreadmillIntervalPresets
import com.omnigart.sensors.treadmill.TreadmillInclineRange
import com.omnigart.sensors.treadmill.TreadmillMachineState
import com.omnigart.sensors.treadmill.TreadmillProtocolKind
import com.omnigart.sensors.treadmill.TreadmillSample
import com.omnigart.sensors.treadmill.TreadmillSpeedRange
import com.omnigart.sensors.treadmill.ftms.FtmsControlPoint
import com.omnigart.sensors.treadmill.ftms.FtmsTreadmillDataParser
import com.omnigart.sensors.treadmill.ftms.FtmsUuids
import kotlin.math.roundToInt

/**
 * In-process FTMS treadmill for iOS / Android simulators (no real BLE).
 *
 * Advances distance from target speed; exposes synthetic 0x2ACD notifications and
 * honours Control Point start/stop/speed/incline after [requestControl].
 */
class SimulatedTreadmill(private val tickSeconds: Double = 1.0) {
    var controlGranted: Boolean = false
        private set
    var running: Boolean = false
        private set
    var targetSpeedKmh: Double = 8.0
    var targetInclinePercent: Double = 1.0
    var elapsedSeconds: Int = 0
        private set
    var distanceMetres: Double = 0.0
        private set
    var totalEnergyKcal: Double = 0.0
        private set
    var heartRateBpm: Int = 120
    var steps: Int = 0
        private set

    val speedRange = TreadmillSpeedRange(minKmh = 1.0, maxKmh = 20.0, incrementKmh = 0.1)
    val inclineRange = TreadmillInclineRange(minPercent = 0.0, maxPercent = 15.0, incrementPercent = 0.5)

    val deviceId: String = "sim-treadmill-ftms"
    val deviceName: String = "OmniGart Simulated Treadmill"

    fun resetSession() {
        controlGranted = false
        running = false
        elapsedSeconds = 0
        distanceMetres = 0.0
        totalEnergyKcal = 0.0
        steps = 0
        targetSpeedKmh = 8.0
        targetInclinePercent = 1.0
    }

    fun handleControlPoint(bytes: ByteArray): ByteArray {
        if (bytes.isEmpty()) {
            return response(0x00, CONTROL_FAIL)
        }
        val op = bytes[0].toInt() and 0xFF
        when (op) {
            FtmsControlPoint.OP_REQUEST_CONTROL -> {
                controlGranted = true
                return response(op, CONTROL_OK)
            }
            FtmsControlPoint.OP_RESET -> {
                if (!controlGranted) return response(op, CONTROL_DENIED)
                resetSession()
                controlGranted = true
                return response(op, CONTROL_OK)
            }
            FtmsControlPoint.OP_START_OR_RESUME -> {
                if (!controlGranted) return response(op, CONTROL_DENIED)
                running = true
                return response(op, CONTROL_OK)
            }
            FtmsControlPoint.OP_STOP_OR_PAUSE -> {
                if (!controlGranted) return response(op, CONTROL_DENIED)
                running = false
                return response(op, CONTROL_OK)
            }
            FtmsControlPoint.OP_SET_TARGET_SPEED -> {
                if (!controlGranted) return response(op, CONTROL_DENIED)
                if (bytes.size < 3) return response(op, CONTROL_BAD_PARAM)
                val units = (bytes[1].toInt() and 0xFF) or ((bytes[2].toInt() and 0xFF) shl 8)
                targetSpeedKmh =
                    (units / 100.0).coerceIn(speedRange.minKmh, speedRange.maxKmh)
                return response(op, CONTROL_OK)
            }
            FtmsControlPoint.OP_SET_TARGET_INCLINATION -> {
                if (!controlGranted) return response(op, CONTROL_DENIED)
                if (bytes.size < 3) return response(op, CONTROL_BAD_PARAM)
                val units = (bytes[1].toInt() and 0xFF) or ((bytes[2].toInt() and 0xFF) shl 8)
                val signed = if (units >= 0x8000) units - 0x10000 else units
                targetInclinePercent =
                    (signed / 10.0).coerceIn(inclineRange.minPercent, inclineRange.maxPercent)
                return response(op, CONTROL_OK)
            }
            else -> return response(op, CONTROL_UNSUPPORTED)
        }
    }

    fun applyPreset(presetId: String): Boolean {
        val preset = DefaultTreadmillIntervalPresets.firstOrNull { it.id == presetId } ?: return false
        preset.targetSpeedKmh?.let { targetSpeedKmh = it }
        preset.targetInclinePercent?.let { targetInclinePercent = it }
        return true
    }

    /** Advance simulation by one tick and return a synthetic FTMS treadmill-data payload. */
    fun tick(): ByteArray {
        if (running) {
            elapsedSeconds += tickSeconds.roundToInt().coerceAtLeast(1)
            val metresPerTick = targetSpeedKmh * 1000.0 / 3600.0 * tickSeconds
            distanceMetres += metresPerTick
            totalEnergyKcal += (targetSpeedKmh * 1.05) * (tickSeconds / 60.0)
            steps += (targetSpeedKmh * 1.8 * tickSeconds).roundToInt().coerceAtLeast(0)
            heartRateBpm = (110 + targetSpeedKmh * 4 + targetInclinePercent).roundToInt().coerceIn(90, 185)
        }
        return encodeTreadmillData(currentSample())
    }

    fun currentSample(): TreadmillSample = TreadmillSample(
        speedKmh = if (running) targetSpeedKmh else 0.0,
        inclinePercent = targetInclinePercent,
        distanceMetres = distanceMetres,
        elapsedSeconds = elapsedSeconds,
        totalEnergyKcal = totalEnergyKcal,
        heartRateBpm = heartRateBpm,
        steps = steps,
        protocol = TreadmillProtocolKind.Simulated,
        machineState =
        when {
            running -> TreadmillMachineState.Running
            elapsedSeconds > 0 -> TreadmillMachineState.Stopped
            else -> TreadmillMachineState.Idle
        }
    )

    fun parseOwnNotification(bytes: ByteArray): TreadmillSample = FtmsTreadmillDataParser.parse(bytes).getOrThrow().copy(
        protocol = TreadmillProtocolKind.Simulated,
        machineState = currentSample().machineState,
        steps = steps
    )

    companion object {
        const val CONTROL_OK = 0x01
        const val CONTROL_UNSUPPORTED = 0x02
        const val CONTROL_BAD_PARAM = 0x03
        const val CONTROL_FAIL = 0x04
        const val CONTROL_DENIED = 0x05

        val treadmillDataUuid: String = FtmsUuids.TREADMILL_DATA

        fun response(requestOpcode: Int, result: Int): ByteArray = byteArrayOf(
            FtmsControlPoint.OP_RESPONSE.toByte(),
            requestOpcode.toByte(),
            result.toByte()
        )

        fun encodeTreadmillData(sample: TreadmillSample): ByteArray {
            // bit0 clear → speed; bit2 distance; bit3 incline; bit7 energy; bit8 HR; bit10 elapsed
            val f =
                (1 shl 2) or
                    (1 shl 3) or
                    (1 shl 7) or
                    (1 shl 8) or
                    (1 shl 10)
            val speed = ((sample.speedKmh ?: 0.0) * 100.0).roundToInt().coerceIn(0, 0xFFFF)
            val distance = (sample.distanceMetres ?: 0.0).roundToInt().coerceIn(0, 0xFFFFFF)
            val incline = (((sample.inclinePercent ?: 0.0) * 10.0).roundToInt()).and(0xFFFF)
            val ramp = 0x7FFF // N/A
            val totalKcal = (sample.totalEnergyKcal ?: 0.0).roundToInt().coerceIn(0, 0xFFFE)
            val hr = (sample.heartRateBpm ?: 0).coerceIn(0, 0xFE)
            val elapsed = (sample.elapsedSeconds ?: 0).coerceIn(0, 0xFFFF)

            return byteArrayOf(
                (f and 0xFF).toByte(),
                ((f shr 8) and 0xFF).toByte(),
                (speed and 0xFF).toByte(),
                ((speed shr 8) and 0xFF).toByte(),
                (distance and 0xFF).toByte(),
                ((distance shr 8) and 0xFF).toByte(),
                ((distance shr 16) and 0xFF).toByte(),
                (incline and 0xFF).toByte(),
                ((incline shr 8) and 0xFF).toByte(),
                (ramp and 0xFF).toByte(),
                ((ramp shr 8) and 0xFF).toByte(),
                (totalKcal and 0xFF).toByte(),
                ((totalKcal shr 8) and 0xFF).toByte(),
                0xFF.toByte(), // energy/hour N/A
                0xFF.toByte(),
                0xFF.toByte(), // energy/min N/A
                hr.toByte(),
                (elapsed and 0xFF).toByte(),
                ((elapsed shr 8) and 0xFF).toByte()
            )
        }
    }
}
