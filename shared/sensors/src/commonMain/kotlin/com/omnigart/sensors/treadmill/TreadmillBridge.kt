package com.omnigart.sensors.treadmill

import com.omnigart.sensors.treadmill.fitshow.FitShowStatusParser
import com.omnigart.sensors.treadmill.ftms.FtmsControlPoint
import com.omnigart.sensors.treadmill.ftms.FtmsFeatureParser
import com.omnigart.sensors.treadmill.ftms.FtmsMachineStatusParser
import com.omnigart.sensors.treadmill.ftms.FtmsSupportedInclinationRangeParser
import com.omnigart.sensors.treadmill.ftms.FtmsSupportedSpeedRangeParser
import com.omnigart.sensors.treadmill.ftms.FtmsTreadmillDataParser
import com.omnigart.sensors.treadmill.iconsole.IConsoleTelemetryParser
import com.omnigart.sensors.treadmill.sim.SimulatedTreadmill

/**
 * Primitive-friendly façade for Apple/Android shells (mirrors [LocalHealthBridge] style).
 * Avoids constructing nested Kotlin data classes from Swift.
 */
@Suppress("TooManyFunctions")
object TreadmillBridge {
    private var simulator: SimulatedTreadmill? = null

    fun detectProtocol(serviceUuids: Array<String>): String = TreadmillProtocolDetector.detect(serviceUuids.toList()).name

    fun parseNotification(protocolName: String, characteristicUuid: String, payload: ByteArray): TreadmillSampleDto? {
        val kind =
            runCatching { TreadmillProtocolKind.valueOf(protocolName) }
                .getOrDefault(TreadmillProtocolKind.Unknown)
        val sample =
            TreadmillProtocolDetector.parse(kind, characteristicUuid, payload)
                ?: when (kind) {
                    TreadmillProtocolKind.Ftms ->
                        FtmsTreadmillDataParser.parse(payload).getOrNull()
                            ?: FtmsMachineStatusParser.parse(payload)
                                .map { FtmsMachineStatusParser.toSamplePatch(it) }
                                .getOrNull()
                    TreadmillProtocolKind.FitShow ->
                        FitShowStatusParser.parseNotification(payload).getOrNull()
                    TreadmillProtocolKind.IConsole ->
                        IConsoleTelemetryParser.parsePacket(payload).getOrNull()
                    else -> null
                }
        return sample?.toDto()
    }

    fun parseFtmsTreadmillData(payload: ByteArray): TreadmillSampleDto? = FtmsTreadmillDataParser.parse(payload).getOrNull()?.toDto()

    fun parseFtmsFeatures(payload: ByteArray): Boolean = FtmsFeatureParser.parse(payload).isSuccess

    fun parseFtmsSpeedRange(payload: ByteArray): DoubleArray? {
        val range = FtmsSupportedSpeedRangeParser.parse(payload).getOrNull() ?: return null
        return doubleArrayOf(range.minKmh, range.maxKmh, range.incrementKmh)
    }

    fun parseFtmsInclineRange(payload: ByteArray): DoubleArray? {
        val range = FtmsSupportedInclinationRangeParser.parse(payload).getOrNull() ?: return null
        return doubleArrayOf(range.minPercent, range.maxPercent, range.incrementPercent)
    }

    fun controlRequestControl(): ByteArray = FtmsControlPoint.requestControl()

    fun controlStart(): ByteArray = FtmsControlPoint.startOrResume()

    fun controlStop(): ByteArray = FtmsControlPoint.stop()

    fun controlPause(): ByteArray = FtmsControlPoint.pause()

    fun controlSetSpeed(kmh: Double): ByteArray = FtmsControlPoint.setTargetSpeed(kmh)

    fun controlSetIncline(percent: Double): ByteArray = FtmsControlPoint.setTargetInclination(percent)

    fun controlParseResponseSuccess(payload: ByteArray): Boolean = FtmsControlPoint.parseResponse(payload).getOrNull()?.success == true

    fun fitShowStart(): ByteArray = FitShowStatusParser.buildStart()

    fun fitShowStop(): ByteArray = FitShowStatusParser.buildStop()

    fun fitShowSetTarget(speedKmh: Double, inclinePercent: Double): ByteArray = FitShowStatusParser.buildSetTarget(speedKmh, inclinePercent)

    fun fitShowPoll(): ByteArray = FitShowStatusParser.buildPollStatus()

    fun presetIds(): Array<String> = DefaultTreadmillIntervalPresets.map { it.id }.toTypedArray()

    fun presetSpeedKmh(id: String): Double? = DefaultTreadmillIntervalPresets.firstOrNull { it.id == id }?.targetSpeedKmh

    fun presetInclinePercent(id: String): Double? = DefaultTreadmillIntervalPresets.firstOrNull { it.id == id }?.targetInclinePercent

    fun presetLabelKey(id: String): String? = DefaultTreadmillIntervalPresets.firstOrNull { it.id == id }?.labelKey

    fun simulatorReset() {
        simulator = SimulatedTreadmill().also { it.resetSession() }
    }

    fun simulatorEnsure(): SimulatedTreadmill = simulator ?: SimulatedTreadmill().also { simulator = it }

    fun simulatorControl(payload: ByteArray): ByteArray = simulatorEnsure().handleControlPoint(payload)

    fun simulatorTick(): TreadmillSampleDto {
        val bytes = simulatorEnsure().tick()
        return simulatorEnsure().parseOwnNotification(bytes).toDto()
    }

    fun simulatorApplyPreset(id: String): Boolean = simulatorEnsure().applyPreset(id)

    fun diagnosticsAppendJson(
        existingJson: String?,
        deviceId: String,
        deviceName: String?,
        serviceUuid: String?,
        characteristicUuid: String,
        payload: ByteArray
    ): String {
        val log = TreadmillDiagnosticsLog()
        // Single-entry export for incremental share; callers may concatenate.
        log.append(
            deviceId = deviceId,
            deviceName = deviceName,
            serviceUuid = serviceUuid,
            characteristicUuid = characteristicUuid,
            payload = payload
        )
        val next = log.toJson(pretty = true)
        return if (existingJson.isNullOrBlank()) next else existingJson
    }
}

data class TreadmillSampleDto(
    val speedKmh: Double = Double.NaN,
    val inclinePercent: Double = Double.NaN,
    val distanceMetres: Double = Double.NaN,
    val elapsedSeconds: Int = -1,
    val totalEnergyKcal: Double = Double.NaN,
    val heartRateBpm: Int = -1,
    val steps: Int = -1,
    val cadenceSpm: Double = Double.NaN,
    val protocolName: String = "Unknown",
    val machineStateName: String = "Unknown"
) {
    fun optionalSpeed(): Double? = speedKmh.takeUnless { it.isNaN() }

    fun optionalIncline(): Double? = inclinePercent.takeUnless { it.isNaN() }

    fun optionalDistance(): Double? = distanceMetres.takeUnless { it.isNaN() }

    fun optionalEnergy(): Double? = totalEnergyKcal.takeUnless { it.isNaN() }

    fun optionalCadence(): Double? = cadenceSpm.takeUnless { it.isNaN() }

    fun optionalElapsed(): Int? = elapsedSeconds.takeIf { it >= 0 }

    fun optionalHr(): Int? = heartRateBpm.takeIf { it > 0 }

    fun optionalSteps(): Int? = steps.takeIf { it >= 0 }
}

private fun TreadmillSample.toDto(): TreadmillSampleDto = TreadmillSampleDto(
    speedKmh = speedKmh ?: Double.NaN,
    inclinePercent = inclinePercent ?: Double.NaN,
    distanceMetres = distanceMetres ?: Double.NaN,
    elapsedSeconds = elapsedSeconds ?: -1,
    totalEnergyKcal = totalEnergyKcal ?: Double.NaN,
    heartRateBpm = heartRateBpm ?: -1,
    steps = steps ?: -1,
    cadenceSpm = cadenceSpm ?: Double.NaN,
    protocolName = protocol.name,
    machineStateName = machineState.name
)
