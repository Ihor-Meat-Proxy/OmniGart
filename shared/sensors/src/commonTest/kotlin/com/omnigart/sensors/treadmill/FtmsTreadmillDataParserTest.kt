package com.omnigart.sensors.treadmill

import com.omnigart.sensors.treadmill.ftms.FtmsControlPoint
import com.omnigart.sensors.treadmill.ftms.FtmsFeatureParser
import com.omnigart.sensors.treadmill.ftms.FtmsMachineStatusParser
import com.omnigart.sensors.treadmill.ftms.FtmsSupportedInclinationRangeParser
import com.omnigart.sensors.treadmill.ftms.FtmsSupportedSpeedRangeParser
import com.omnigart.sensors.treadmill.ftms.FtmsTreadmillDataParser
import com.omnigart.sensors.treadmill.sim.SimulatedTreadmill
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FtmsTreadmillDataParserTest {

    @Test
    fun speedOnlyFlagsClear() {
        // flags 0x0000, speed 10.00 km/h = 1000 = 0x03E8
        val sample = parse(0x00, 0x00, 0xE8, 0x03)
        assertEquals(10.0, sample.speedKmh!!, 1e-9)
        assertNull(sample.distanceMetres)
        assertEquals(TreadmillProtocolKind.Ftms, sample.protocol)
    }

    @Test
    fun fullFlagsSpeedDistanceInclineEnergyHrTime() {
        // Recorded-style fixture: 8.50 km/h, 1234 m, 2.5%, 120 kcal, 142 bpm, 600 s
        val flags =
            (1 shl 2) or (1 shl 3) or (1 shl 7) or (1 shl 8) or (1 shl 10)
        val bytes =
            bytes(
                flags and 0xFF,
                (flags shr 8) and 0xFF,
                0x52, 0x03, // 850 → 8.50 km/h
                0xD2, 0x04, 0x00, // 1234 m
                0x19, 0x00, // incline 25 → 2.5%
                0xFF, 0x7F, // ramp N/A
                0x78, 0x00, // 120 kcal
                0xFF, 0xFF, // energy/h N/A
                0xFF, // energy/min N/A
                0x8E, // 142 bpm
                0x58, 0x02 // 600 s
            )
        val sample = FtmsTreadmillDataParser.parse(bytes).getOrThrow()
        assertEquals(8.5, sample.speedKmh!!, 1e-9)
        assertEquals(1234.0, sample.distanceMetres!!, 1e-9)
        assertEquals(2.5, sample.inclinePercent!!, 1e-9)
        assertEquals(120.0, sample.totalEnergyKcal!!, 1e-9)
        assertEquals(142, sample.heartRateBpm)
        assertEquals(600, sample.elapsedSeconds)
    }

    @Test
    fun moreDataOmitsSpeed() {
        val flags = 1 // More Data set
        val bytes = bytes(flags, 0x00, 0x64, 0x00, 0x00) // distance 100 m only (bit2 not set — truncated ok)
        // With only More Data and no other bits, packet is just flags.
        val emptyMore = FtmsTreadmillDataParser.parse(bytes(0x01, 0x00)).getOrThrow()
        assertNull(emptyMore.speedKmh)

        val withDistance =
            FtmsTreadmillDataParser.parse(
                bytes(0x05, 0x00, 0x64, 0x00, 0x00) // more+distance, 100 m
            ).getOrThrow()
        assertNull(withDistance.speedKmh)
        assertEquals(100.0, withDistance.distanceMetres!!, 1e-9)

        val merged =
            FtmsTreadmillDataParser.merge(
                parse(0x00, 0x00, 0xE8, 0x03),
                withDistance
            )
        assertEquals(10.0, merged.speedKmh!!, 1e-9)
        assertEquals(100.0, merged.distanceMetres!!, 1e-9)
    }

    @Test
    fun controlPointBuildersAndResponse() {
        assertEquals(byteArrayOf(0x00).toList(), FtmsControlPoint.requestControl().toList())
        assertEquals(byteArrayOf(0x07).toList(), FtmsControlPoint.startOrResume().toList())
        assertEquals(byteArrayOf(0x08, 0x01).toList(), FtmsControlPoint.stop().toList())

        val speed = FtmsControlPoint.setTargetSpeed(10.5)
        assertEquals(0x02, speed[0].toInt() and 0xFF)
        assertEquals(1050, (speed[1].toInt() and 0xFF) or ((speed[2].toInt() and 0xFF) shl 8))

        val incline = FtmsControlPoint.setTargetInclination(3.5)
        assertEquals(0x03, incline[0].toInt() and 0xFF)
        assertEquals(35, (incline[1].toInt() and 0xFF) or ((incline[2].toInt() and 0xFF) shl 8))

        val ok = FtmsControlPoint.parseResponse(byteArrayOf(0x80.toByte(), 0x07, 0x01)).getOrThrow()
        assertTrue(ok.success)
        assertEquals(0x07, ok.requestOpcode)

        val denied = FtmsControlPoint.parseResponse(byteArrayOf(0x80.toByte(), 0x02, 0x05)).getOrThrow()
        assertFalse(denied.success)
    }

    @Test
    fun featureAndRanges() {
        // machine: distance+incline+energy+HR+elapsed (bits 2,3,9,10,12)
        val machine = (1L shl 2) or (1L shl 3) or (1L shl 9) or (1L shl 10) or (1L shl 12)
        // target: speed + incline
        val target = (1L shl 0) or (1L shl 1)
        val featureBytes = ByteArray(8)
        writeU32(featureBytes, 0, machine)
        writeU32(featureBytes, 4, target)
        val features = FtmsFeatureParser.parse(featureBytes).getOrThrow()
        assertTrue(features.totalDistanceSupported)
        assertTrue(features.inclinationSupported)
        assertTrue(features.speedTargetSettingSupported)
        assertTrue(features.inclinationTargetSettingSupported)

        val speedRange =
            FtmsSupportedSpeedRangeParser.parse(
                bytes(0x64, 0x00, 0xD0, 0x07, 0x0A, 0x00) // 1.00–20.00, step 0.10
            ).getOrThrow()
        assertEquals(1.0, speedRange.minKmh, 1e-9)
        assertEquals(20.0, speedRange.maxKmh, 1e-9)
        assertEquals(0.1, speedRange.incrementKmh, 1e-9)

        val inclineRange =
            FtmsSupportedInclinationRangeParser.parse(
                bytes(0x00, 0x00, 0x96, 0x00, 0x05, 0x00) // 0–15.0, step 0.5
            ).getOrThrow()
        assertEquals(0.0, inclineRange.minPercent, 1e-9)
        assertEquals(15.0, inclineRange.maxPercent, 1e-9)
        assertEquals(0.5, inclineRange.incrementPercent, 1e-9)
    }

    @Test
    fun machineStatusEvents() {
        val started =
            FtmsMachineStatusParser.parse(byteArrayOf(0x04)).getOrThrow()
        assertEquals(TreadmillMachineState.Running, started.state)

        val paused =
            FtmsMachineStatusParser.parse(byteArrayOf(0x02, 0x02)).getOrThrow()
        assertEquals(TreadmillMachineState.Paused, paused.state)

        val safety =
            FtmsMachineStatusParser.parse(byteArrayOf(0x03)).getOrThrow()
        assertEquals(TreadmillMachineState.SafetyKeyRemoved, safety.state)
    }

    @Test
    fun protocolDetectorPrefersFtms() {
        val kind =
            TreadmillProtocolDetector.detect(
                listOf("0000180d-0000-1000-8000-00805f9b34fb", "00001826-0000-1000-8000-00805f9b34fb")
            )
        assertEquals(TreadmillProtocolKind.Ftms, kind)
    }

    @Test
    fun simulatedTreadmillRoundTrip() {
        val sim = SimulatedTreadmill()
        val grant = sim.handleControlPoint(FtmsControlPoint.requestControl())
        assertEquals(0x01, grant[2].toInt() and 0xFF)
        assertTrue(sim.controlGranted)
        sim.handleControlPoint(FtmsControlPoint.startOrResume())
        sim.handleControlPoint(FtmsControlPoint.setTargetSpeed(9.0))
        val payload = sim.tick()
        val sample = sim.parseOwnNotification(payload)
        assertEquals(TreadmillProtocolKind.Simulated, sample.protocol)
        assertNotNull(sample.speedKmh)
        assertTrue(sample.distanceMetres!! > 0.0)
        assertEquals(TreadmillMachineState.Running, sample.machineState)
    }

    @Test
    fun truncatedPacketFails() {
        assertTrue(FtmsTreadmillDataParser.parse(byteArrayOf(0x00)).isFailure)
        assertTrue(FtmsTreadmillDataParser.parse(byteArrayOf(0x00, 0x00, 0x01)).isFailure)
    }

    private fun parse(vararg values: Int) = FtmsTreadmillDataParser.parse(bytes(*values)).getOrThrow()

    private fun bytes(vararg values: Int): ByteArray = values.map { it.toByte() }.toByteArray()

    private fun writeU32(dest: ByteArray, offset: Int, value: Long) {
        dest[offset] = (value and 0xFF).toByte()
        dest[offset + 1] = ((value shr 8) and 0xFF).toByte()
        dest[offset + 2] = ((value shr 16) and 0xFF).toByte()
        dest[offset + 3] = ((value shr 24) and 0xFF).toByte()
    }
}
