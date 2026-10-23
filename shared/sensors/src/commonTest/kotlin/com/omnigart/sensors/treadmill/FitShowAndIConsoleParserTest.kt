package com.omnigart.sensors.treadmill

import com.omnigart.sensors.treadmill.fitshow.FitShowCodec
import com.omnigart.sensors.treadmill.fitshow.FitShowStatusParser
import com.omnigart.sensors.treadmill.iconsole.IConsoleCodec
import com.omnigart.sensors.treadmill.iconsole.IConsoleTelemetryParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FitShowAndIConsoleParserTest {

    @Test
    fun fitShowFrameRoundTripAndFcs() {
        val frame = FitShowCodec.encode(FitShowCodec.CMD_STATUS)
        assertEquals(0x02, frame[0].toInt() and 0xFF)
        assertEquals(0x03, frame.last().toInt() and 0xFF)
        val decoded = FitShowCodec.decode(frame).getOrThrow()
        assertEquals(FitShowCodec.CMD_STATUS, decoded.cmd)
    }

    @Test
    fun fitShowRejectsBadFcs() {
        val good = FitShowCodec.encode(FitShowCodec.CMD_STATUS)
        val bad = good.copyOf()
        bad[bad.size - 2] = (bad[bad.size - 2].toInt() xor 0xFF).toByte()
        assertTrue(FitShowCodec.decode(bad).isFailure)
    }

    @Test
    fun fitShowStatusRunningFixture() {
        // state=3 running, speed=50 (5.0 km/h), incline=0, time=60s, dist=100 (0.100 km),
        // kcal=1000 (100.0), steps=0, hr=0
        val payload =
            byteArrayOf(
                0x03, // running
                0x32, // 5.0 km/h
                0x00, // incline
                0x3C, 0x00, // 60 s
                0x64, 0x00, // 0.100 km
                0xE8.toByte(), 0x03, // 100.0 kcal (1000 / 10)
                0x00, 0x00, // steps
                0x00 // hr
            )
        val sample = FitShowStatusParser.parseStatusPayload(payload).getOrThrow()
        assertEquals(5.0, sample.speedKmh!!, 1e-9)
        assertEquals(0.0, sample.inclinePercent!!, 1e-9)
        assertEquals(60, sample.elapsedSeconds)
        assertEquals(100.0, sample.distanceMetres!!, 1e-9)
        assertEquals(100.0, sample.totalEnergyKcal!!, 1e-9)
        assertEquals(TreadmillMachineState.Running, sample.machineState)
        assertEquals(TreadmillProtocolKind.FitShow, sample.protocol)

        val framed = FitShowCodec.encode(FitShowCodec.CMD_STATUS, payload)
        val fromNotify = FitShowStatusParser.parseNotification(framed).getOrThrow()
        assertEquals(5.0, fromNotify.speedKmh!!, 1e-9)
    }

    @Test
    fun fitShowHighSpeedWordMode() {
        val payload =
            byteArrayOf(
                0x03,
                0x2C, 0x01, // 300 → 30.0 km/h
                0x02,
                0x0A, 0x00,
                0x00, 0x00,
                0x00, 0x00,
                0x00, 0x00,
                0x00
            )
        val sample = FitShowStatusParser.parseStatusPayload(payload, speedAsWord = true).getOrThrow()
        assertEquals(30.0, sample.speedKmh!!, 1e-9)
        assertEquals(2.0, sample.inclinePercent!!, 1e-9)
    }

    @Test
    fun fitShowControlCommands() {
        val start = FitShowStatusParser.buildStart()
        assertEquals(FitShowCodec.CMD_CONTROL, start[1].toInt() and 0xFF)
        assertEquals(FitShowCodec.SCMD_START, start[2].toInt() and 0xFF)
        assertTrue(FitShowCodec.decode(start).isSuccess)

        val set = FitShowStatusParser.buildSetTarget(8.5, 3.0)
        assertTrue(FitShowCodec.decode(set).isSuccess)
    }

    @Test
    fun fitShowDetector() {
        assertEquals(
            TreadmillProtocolKind.FitShow,
            TreadmillProtocolDetector.detect(listOf(FitShowCodec.SERVICE))
        )
    }

    @Test
    fun iConsoleChecksumAndPing() {
        val ping = IConsoleCodec.ping()
        assertEquals(0xF0, ping[0].toInt() and 0xFF)
        assertEquals(0xA0, ping[1].toInt() and 0xFF)
        assertTrue(IConsoleCodec.decode(ping).isSuccess)
    }

    @Test
    fun iConsoleTelemetryFixture() {
        // Body fields use value+1 encoding. Build a READ response packet.
        fun enc(v: Int) = (v + 1).toByte()
        val body =
            byteArrayOf(
                enc(0), enc(0), enc(1), enc(30), // 0d 0h 1m 30s → 90 s
                enc(0), enc(85), // speed 8.5
                enc(0), enc(0), // cadence
                enc(0), enc(12), // distance 1.2 km → units /10
                enc(1), enc(20), // kcal 12.0? (100*1+20)/10 = 12.0
                enc(1), enc(40), // hr 140
                enc(0), enc(0), // watt
                enc(2), // level/incline 2
                0x00
            )
        val packet = IConsoleCodec.encode(0xB2, body)
        val sample = IConsoleTelemetryParser.parsePacket(packet).getOrThrow()
        assertEquals(90, sample.elapsedSeconds)
        assertEquals(8.5, sample.speedKmh!!, 1e-9)
        assertEquals(1200.0, sample.distanceMetres!!, 1e-9)
        assertEquals(140, sample.heartRateBpm)
        assertEquals(2.0, sample.inclinePercent!!, 1e-9)
        assertEquals(TreadmillProtocolKind.IConsole, sample.protocol)
    }

    @Test
    fun iConsoleDetector() {
        assertEquals(
            TreadmillProtocolKind.IConsole,
            TreadmillProtocolDetector.detect(listOf(IConsoleCodec.UART_SERVICE))
        )
    }

    @Test
    fun ftmsWinsOverFitShowWhenBothPresent() {
        val kind =
            TreadmillProtocolDetector.detect(
                listOf(FitShowCodec.SERVICE, "00001826-0000-1000-8000-00805f9b34fb")
            )
        assertEquals(TreadmillProtocolKind.Ftms, kind)
    }
}
