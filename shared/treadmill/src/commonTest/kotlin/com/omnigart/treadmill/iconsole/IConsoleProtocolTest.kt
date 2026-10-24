package com.omnigart.treadmill.iconsole

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IConsoleProtocolTest {

    @Test
    fun pingChecksumMatchesDocumentedVector() {
        // Documented: F0 A0 01 01 92
        assertEquals(
            listOf(0xF0, 0xA0, 0x01, 0x01, 0x92),
            IConsoleProtocol.ping().map { it.toInt() and 0xFF }
        )
    }

    @Test
    fun readChecksumMatchesDocumentedVector() {
        assertEquals(
            listOf(0xF0, 0xA2, 0x01, 0x01, 0x94),
            IConsoleProtocol.read().map { it.toInt() and 0xFF }
        )
    }

    @Test
    fun startStopDocumentedVectors() {
        assertEquals(
            listOf(0xF0, 0xA5, 0x01, 0x01, 0x02, 0x99),
            IConsoleProtocol.start().map { it.toInt() and 0xFF }
        )
        assertEquals(
            listOf(0xF0, 0xA5, 0x01, 0x01, 0x04, 0x9B),
            IConsoleProtocol.stop().map { it.toInt() and 0xFF }
        )
    }

    @Test
    fun setLevelSeventeen() {
        // Documented: F0 A6 01 01 12 AA for level 17
        assertEquals(
            listOf(0xF0, 0xA6, 0x01, 0x01, 0x12, 0xAA),
            IConsoleProtocol.setLevel(17).map { it.toInt() and 0xFF }
        )
    }

    @Test
    fun parseDocumentedTelemetryComment() {
        // From haraldh/iconsole header comment:
        // f0:b2 01:01:04:06 03:11 01:3b 01:07 01:0d 01:01 0f:33 09 02 + checksum
        // T: 3:05  speed from 03:11 hex → 21.6 km/h  RPM 58  D:0.6  cal 12  HF 0  W 145.0  LVL 8
        val body =
            intArrayOf(
                0xF0, 0xB2,
                0x01, 0x01, 0x04, 0x06, // time → min=3 sec=5
                0x03, 0x11, // speed → 21.6 (100*(3-1)+(0x11-1))/10
                0x01, 0x3B, // rpm → 58
                0x01, 0x07, // distance → 0.6 km
                0x01, 0x0D, // cal → 12
                0x01, 0x01, // hf → 0
                0x0F, 0x33, // power → 145.0 W
                0x09, // level 8
                0x02 // status started
            )
        val csum = body.sum() and 0xFF
        val packet = (body.toList() + csum).map { it.toByte() }.toByteArray()
        val t = IConsoleProtocol.parseTelemetry(packet).getOrThrow()
        assertEquals(21.6, t.speedKmh, 1e-9)
        assertEquals(58, t.cadenceRpm)
        assertEquals(0.6, t.distanceKm, 1e-9)
        assertEquals(12, t.energyKcal)
        assertEquals(0, t.heartRateBpm)
        assertEquals(145.0, t.powerWatts, 1e-9)
        assertEquals(8, t.level)
        assertEquals(3, t.minutes)
        assertEquals(5, t.seconds)
        assertEquals(true, t.isRunning)
    }

    @Test
    fun badChecksumFails() {
        val packet = ByteArray(21) { 0x01 }
        packet[0] = 0xF0.toByte()
        packet[1] = 0xB2.toByte()
        packet[20] = 0x00
        assertTrue(IConsoleProtocol.parseTelemetry(packet).isFailure)
    }

    @Test
    fun expectedResponseOpcode() {
        assertEquals(0xB2, IConsoleProtocol.expectedResponseOpcode(0xA2))
        assertEquals(0xB5, IConsoleProtocol.expectedResponseOpcode(0xA5))
    }
}
