package com.omnigart.treadmill.fitshow

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FitShowParserTest {
    @Test
    fun frameRoundTripAndFcs() {
        val frame = FitShowCodec.encode(FitShowCodec.CMD_STATUS)
        assertEquals(0x02, frame[0].toInt() and 0xFF)
        assertEquals(0x03, frame.last().toInt() and 0xFF)
        val decoded = FitShowCodec.decode(frame).getOrThrow()
        assertEquals(FitShowCodec.CMD_STATUS, decoded.cmd)
    }

    @Test
    fun rejectsBadFcs() {
        val good = FitShowCodec.encode(FitShowCodec.CMD_STATUS)
        val bad = good.copyOf()
        bad[bad.size - 2] = (bad[bad.size - 2].toInt() xor 0xFF).toByte()
        assertTrue(FitShowCodec.decode(bad).isFailure)
    }

    @Test
    fun statusRunningFixture() {
        val payload =
            byteArrayOf(
                0x03,
                0x32,
                0x00,
                0x3C, 0x00,
                0x64, 0x00,
                0xE8.toByte(), 0x03,
                0x00, 0x00,
                0x00
            )
        val sample = FitShowStatusParser.parseStatusPayload(payload).getOrThrow()
        assertEquals(5.0, sample.speedKmh, 1e-9)
        assertEquals(0.0, sample.inclinePercent, 1e-9)
        assertEquals(60, sample.elapsedSeconds)
        assertEquals(100.0, sample.distanceMetres, 1e-9)
        assertEquals(100.0, sample.energyKcal, 1e-9)
        assertEquals(FitShowStatusParser.MachineState.Running, sample.state)

        val framed = FitShowCodec.encode(FitShowCodec.CMD_STATUS, payload)
        val fromNotify = FitShowStatusParser.parseNotification(framed).getOrThrow()
        assertEquals(5.0, fromNotify.speedKmh, 1e-9)
    }

    @Test
    fun highSpeedWordMode() {
        val payload =
            byteArrayOf(
                0x03,
                0x2C, 0x01,
                0x02,
                0x0A, 0x00,
                0x00, 0x00,
                0x00, 0x00,
                0x00, 0x00,
                0x00
            )
        val sample = FitShowStatusParser.parseStatusPayload(payload, speedAsWord = true).getOrThrow()
        assertEquals(30.0, sample.speedKmh, 1e-9)
        assertEquals(2.0, sample.inclinePercent, 1e-9)
    }

    @Test
    fun controlCommands() {
        val start = FitShowStatusParser.buildStart()
        assertEquals(FitShowCodec.CMD_CONTROL, start[1].toInt() and 0xFF)
        assertEquals(FitShowCodec.SCMD_START, start[2].toInt() and 0xFF)
        assertTrue(FitShowCodec.decode(start).isSuccess)
        assertTrue(FitShowCodec.decode(FitShowStatusParser.buildSetTarget(8.5, 3.0)).isSuccess)
    }
}
