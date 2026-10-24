package com.omnigart.treadmill.sim

import com.omnigart.treadmill.ble.TreadmillBleUuids
import com.omnigart.treadmill.ftms.FtmsControlPoint
import com.omnigart.treadmill.ftms.FtmsTreadmillDataParser
import com.omnigart.treadmill.model.TreadmillLiveMetrics
import com.omnigart.treadmill.model.TreadmillProtocol
import com.omnigart.treadmill.program.TreadmillProgramCatalog
import com.omnigart.treadmill.session.TreadmillMetricsMerger
import kotlin.math.roundToInt

/**
 * In-process FTMS treadmill for phone/emulator testing without real BLE.
 *
 * Advances distance from target speed; exposes synthetic 0x2ACD notifications and
 * honours Control Point start/stop/speed/incline after [requestControl].
 */
class SimulatedTreadmill(
    private val tickSeconds: Double = 1.0,
    private val hrMax: Int = 190
) {
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
    var steps: Long = 0
        private set

    val speedMinKmh: Double = 1.0
    val speedMaxKmh: Double = 20.0
    val inclineMinPercent: Double = 0.0
    val inclineMaxPercent: Double = 15.0

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
        heartRateBpm = 120
    }

    fun handleControlPoint(bytes: ByteArray): ByteArray {
        if (bytes.isEmpty()) {
            return response(0x00, FtmsControlPoint.ResultCode.OPERATION_FAILED.code)
        }
        val op = bytes[0].toInt() and 0xFF
        when (op) {
            FtmsControlPoint.OpCode.REQUEST_CONTROL.code -> {
                controlGranted = true
                return response(op, FtmsControlPoint.ResultCode.SUCCESS.code)
            }
            FtmsControlPoint.OpCode.RESET.code -> {
                if (!controlGranted) {
                    return response(op, FtmsControlPoint.ResultCode.CONTROL_NOT_PERMITTED.code)
                }
                resetSession()
                controlGranted = true
                return response(op, FtmsControlPoint.ResultCode.SUCCESS.code)
            }
            FtmsControlPoint.OpCode.START_OR_RESUME.code -> {
                if (!controlGranted) {
                    return response(op, FtmsControlPoint.ResultCode.CONTROL_NOT_PERMITTED.code)
                }
                running = true
                return response(op, FtmsControlPoint.ResultCode.SUCCESS.code)
            }
            FtmsControlPoint.OpCode.STOP_OR_PAUSE.code -> {
                if (!controlGranted) {
                    return response(op, FtmsControlPoint.ResultCode.CONTROL_NOT_PERMITTED.code)
                }
                running = false
                return response(op, FtmsControlPoint.ResultCode.SUCCESS.code)
            }
            FtmsControlPoint.OpCode.SET_TARGET_SPEED.code -> {
                if (!controlGranted) {
                    return response(op, FtmsControlPoint.ResultCode.CONTROL_NOT_PERMITTED.code)
                }
                if (bytes.size < 3) {
                    return response(op, FtmsControlPoint.ResultCode.INCORRECT_PARAMETER.code)
                }
                val units = (bytes[1].toInt() and 0xFF) or ((bytes[2].toInt() and 0xFF) shl 8)
                targetSpeedKmh = (units / 100.0).coerceIn(speedMinKmh, speedMaxKmh)
                return response(op, FtmsControlPoint.ResultCode.SUCCESS.code)
            }
            FtmsControlPoint.OpCode.SET_TARGET_INCLINE.code -> {
                if (!controlGranted) {
                    return response(op, FtmsControlPoint.ResultCode.CONTROL_NOT_PERMITTED.code)
                }
                if (bytes.size < 3) {
                    return response(op, FtmsControlPoint.ResultCode.INCORRECT_PARAMETER.code)
                }
                val units = (bytes[1].toInt() and 0xFF) or ((bytes[2].toInt() and 0xFF) shl 8)
                val signed = if (units >= 0x8000) units - 0x10000 else units
                targetInclinePercent =
                    (signed / 10.0).coerceIn(inclineMinPercent, inclineMaxPercent)
                return response(op, FtmsControlPoint.ResultCode.SUCCESS.code)
            }
            else -> return response(op, FtmsControlPoint.ResultCode.NOT_SUPPORTED.code)
        }
    }

    /** Apply the first step targets from a built-in program id. */
    fun applyProgram(programId: String): Boolean {
        val program = TreadmillProgramCatalog.byId(programId) ?: return false
        val step =
            program.steps.firstOrNull { it.targetSpeedKmh != null }
                ?: program.steps.firstOrNull()
                ?: return false
        step.targetSpeedKmh?.let { targetSpeedKmh = it.coerceIn(speedMinKmh, speedMaxKmh) }
        step.targetInclinePercent?.let {
            targetInclinePercent = it.coerceIn(inclineMinPercent, inclineMaxPercent)
        }
        return true
    }

    /** Advance simulation by one tick and return a synthetic FTMS treadmill-data payload. */
    fun tick(): ByteArray {
        if (running) {
            elapsedSeconds += tickSeconds.roundToInt().coerceAtLeast(1)
            val metresPerTick = targetSpeedKmh * 1000.0 / 3600.0 * tickSeconds
            distanceMetres += metresPerTick
            totalEnergyKcal += (targetSpeedKmh * 1.05) * (tickSeconds / 60.0)
            steps += (targetSpeedKmh * 1.8 * tickSeconds).roundToInt().coerceAtLeast(0).toLong()
            heartRateBpm =
                (110 + targetSpeedKmh * 4 + targetInclinePercent).roundToInt().coerceIn(90, 185)
        }
        return encodeTreadmillData(currentFtmsData())
    }

    fun currentMetrics(watchHr: Int? = null): TreadmillLiveMetrics {
        val parsed = FtmsTreadmillDataParser.parse(encodeTreadmillData(currentFtmsData())).getOrThrow()
        return TreadmillMetricsMerger.fromFtms(parsed, watchHr, hrMax).copy(
            protocol = TreadmillProtocol.SIMULATED,
            connected = true
        )
    }

    fun parseOwnNotification(bytes: ByteArray, watchHr: Int? = null): TreadmillLiveMetrics {
        val parsed = FtmsTreadmillDataParser.parse(bytes).getOrThrow()
        return TreadmillMetricsMerger.fromFtms(parsed, watchHr, hrMax).copy(
            protocol = TreadmillProtocol.SIMULATED,
            connected = true,
            steps = steps
        )
    }

    /** Encode the current sample without advancing the clock. */
    fun encodeCurrentNotification(): ByteArray = encodeTreadmillData(currentFtmsData())

    private fun currentFtmsData(): FtmsTreadmillDataParser.TreadmillData =
        FtmsTreadmillDataParser.TreadmillData(
            flags = 0,
            instantaneousSpeedKmh = if (running) targetSpeedKmh else 0.0,
            totalDistanceMetres = distanceMetres.roundToInt().toLong().coerceAtLeast(0),
            inclinationPercent = targetInclinePercent,
            totalEnergyKcal = totalEnergyKcal.roundToInt().coerceAtLeast(0),
            heartRateBpm = heartRateBpm,
            elapsedTimeSeconds = elapsedSeconds,
            steps = steps
        )

    companion object {
        val treadmillDataUuid: String = TreadmillBleUuids.TREADMILL_DATA

        fun response(requestOpcode: Int, result: Int): ByteArray =
            byteArrayOf(
                FtmsControlPoint.OpCode.RESPONSE.code.toByte(),
                requestOpcode.toByte(),
                result.toByte()
            )

        fun encodeTreadmillData(sample: FtmsTreadmillDataParser.TreadmillData): ByteArray {
            // bit0 clear → speed; bit2 distance; bit3 incline; bit7 energy; bit8 HR; bit10 elapsed; bit13 steps
            val f =
                (1 shl 2) or
                    (1 shl 3) or
                    (1 shl 7) or
                    (1 shl 8) or
                    (1 shl 10) or
                    (1 shl 13)
            val speed = ((sample.instantaneousSpeedKmh ?: 0.0) * 100.0).roundToInt().coerceIn(0, 0xFFFF)
            val distance = (sample.totalDistanceMetres ?: 0L).toInt().coerceIn(0, 0xFFFFFF)
            val incline = (((sample.inclinationPercent ?: 0.0) * 10.0).roundToInt()).and(0xFFFF)
            val ramp = 0x7FFF // N/A
            val totalKcal = (sample.totalEnergyKcal ?: 0).coerceIn(0, 0xFFFE)
            val hr = (sample.heartRateBpm ?: 0).coerceIn(0, 0xFE)
            val elapsed = (sample.elapsedTimeSeconds ?: 0).coerceIn(0, 0xFFFF)
            val stepCount = (sample.steps ?: 0L).toInt().coerceIn(0, 0xFFFFFF)

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
                ((elapsed shr 8) and 0xFF).toByte(),
                (stepCount and 0xFF).toByte(),
                ((stepCount shr 8) and 0xFF).toByte(),
                ((stepCount shr 16) and 0xFF).toByte()
            )
        }
    }
}
