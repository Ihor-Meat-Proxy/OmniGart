package com.omnigart.treadmill.ftms

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FtmsTreadmillDataParserTest {

    @Test
    fun speedOnlyWhenMoreDataBitClear() {
        // flags 0x0000 → instantaneous speed present; 1.00 km/h = 100
        val data = parse(0x00, 0x00, 0x64, 0x00)
        assertEquals(1.0, data.instantaneousSpeedKmh!!, 1e-9)
        assertNull(data.totalDistanceMetres)
    }

    @Test
    fun speedDistanceInclineEnergyHrElapsed() {
        // flags: bit2 distance, bit3 incline, bit7 energy, bit8 HR, bit10 elapsed
        // More Data bit0 = 0 → speed present
        // flags = 0x058C = bit2|bit3|bit7|bit8|bit10
        val flagsLo = 0x8C
        val flagsHi = 0x05
        val bytes =
            bytes(
                flagsLo, flagsHi,
                // speed 10.00 km/h = 1000
                0xE8, 0x03,
                // distance 1500 m
                0xDC, 0x05, 0x00,
                // incline 1.5% = 15, ramp 0
                0x0F, 0x00, 0x00, 0x00,
                // energy 120, 400/h, 5/min
                0x78, 0x00, 0x90, 0x01, 0x05,
                // HR 142
                0x8E,
                // elapsed 600 s
                0x58, 0x02
            )
        val data = FtmsTreadmillDataParser.parse(bytes).getOrThrow()
        assertEquals(10.0, data.instantaneousSpeedKmh!!, 1e-9)
        assertEquals(1500L, data.totalDistanceMetres)
        assertEquals(1.5, data.inclinationPercent!!, 1e-9)
        assertEquals(120, data.totalEnergyKcal)
        assertEquals(142, data.heartRateBpm)
        assertEquals(600, data.elapsedTimeSeconds)
    }

    @Test
    fun negativeIncline() {
        // flags bit3 only + speed: 0x0008
        val data =
            parse(
                0x08, 0x00,
                0xC8, 0x00, // 2.00 km/h
                0xEC, 0xFF, // -2.0% = -20 as sint16 LE
                0x00, 0x00
            )
        assertEquals(2.0, data.instantaneousSpeedKmh!!, 1e-9)
        assertEquals(-2.0, data.inclinationPercent!!, 1e-9)
    }

    @Test
    fun stepsFlagBit13() {
        // flags: speed + steps → bit13 = 0x2000, bit0 clear
        val data =
            parse(
                0x00, 0x20,
                0xF4, 0x01, // 5.00 km/h
                0xE8, 0x03, 0x00 // 1000 steps
            )
        assertEquals(5.0, data.instantaneousSpeedKmh!!, 1e-9)
        assertEquals(1000L, data.steps)
    }

    @Test
    fun truncatedFails() {
        assertTrue(FtmsTreadmillDataParser.parse(byteArrayOf(0x00)).isFailure)
        assertTrue(FtmsTreadmillDataParser.parse(byteArrayOf(0x04, 0x00, 0x01)).isFailure)
    }

    @Test
    fun recordedFixtureSpeed10Incline1() {
        // Captured-style: flags 0x000C (distance+incline), speed 12.0, dist 500, incline 1.0%
        val data =
            parse(
                0x0C, 0x00,
                0xB0, 0x04, // 1200 → 12.00
                0xF4, 0x01, 0x00, // 500 m
                0x0A, 0x00, // 1.0%
                0x00, 0x00
            )
        assertEquals(12.0, data.instantaneousSpeedKmh!!, 1e-9)
        assertEquals(500L, data.totalDistanceMetres)
        assertEquals(1.0, data.inclinationPercent!!, 1e-9)
    }

    private fun parse(vararg values: Int) =
        FtmsTreadmillDataParser.parse(bytes(*values)).getOrThrow()

    private fun bytes(vararg values: Int): ByteArray =
        values.map { it.toByte() }.toByteArray()
}
