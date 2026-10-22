package com.omnigart.sensors.ble

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Byte fixtures for Bluetooth SIG Heart Rate Measurement (0x2A37).
 *
 * Flag bits: HR format | contact detected | contact supported | energy | RR | reserved
 */
class HeartRateMeasurementParserTest {

    @Test
    fun uint8HrOnly() {
        // flags=0x00, HR=72
        val m = HeartRateMeasurementParser.parse(bytes(0x00, 72))
        assertEquals(72, m.heartRateBpm)
        assertFalse(m.heartRateIsUint16)
        assertFalse(m.sensorContactSupported)
        assertFalse(m.sensorContactDetected)
        assertNull(m.energyExpendedKj)
        assertTrue(m.rrIntervalTicks.isEmpty())
    }

    @Test
    fun uint16HrOnly() {
        // flags=0x01, HR=300 (0x012C LE → 2C 01)
        val m = HeartRateMeasurementParser.parse(bytes(0x01, 0x2C, 0x01))
        assertEquals(300, m.heartRateBpm)
        assertTrue(m.heartRateIsUint16)
    }

    @Test
    fun sensorContactDetected() {
        // flags: contact supported (bit2) + detected (bit1) = 0x06, HR=80
        val m = HeartRateMeasurementParser.parse(bytes(0x06, 80))
        assertTrue(m.sensorContactSupported)
        assertTrue(m.sensorContactDetected)
        assertEquals(80, m.heartRateBpm)
    }

    @Test
    fun sensorContactSupportedButNotDetected() {
        // flags: contact supported only = 0x04
        val m = HeartRateMeasurementParser.parse(bytes(0x04, 65))
        assertTrue(m.sensorContactSupported)
        assertFalse(m.sensorContactDetected)
    }

    @Test
    fun energyExpendedPresent() {
        // flags=0x08, HR=90, energy=1234 kJ (0x04D2 LE → D2 04)
        val m = HeartRateMeasurementParser.parse(bytes(0x08, 90, 0xD2, 0x04))
        assertEquals(90, m.heartRateBpm)
        assertEquals(1234, m.energyExpendedKj)
    }

    @Test
    fun singleRrInterval() {
        // flags=0x10 (RR), HR=60, RR=1024 ticks = 1.000 s
        val m = HeartRateMeasurementParser.parse(bytes(0x10, 60, 0x00, 0x04))
        assertEquals(listOf(1024), m.rrIntervalTicks)
        assertEquals(1.0, m.rrIntervalsSeconds.single(), absoluteTolerance = 1e-9)
        assertEquals(1000.0, m.rrIntervalsMs.single(), absoluteTolerance = 1e-6)
    }

    @Test
    fun multipleRrIntervals() {
        // flags=0x10, HR=75, RR ticks: 800, 820, 810
        // 800=0x0320 → 20 03; 820=0x0334 → 34 03; 810=0x032A → 2A 03
        val m = HeartRateMeasurementParser.parse(
            bytes(0x10, 75, 0x20, 0x03, 0x34, 0x03, 0x2A, 0x03),
        )
        assertEquals(listOf(800, 820, 810), m.rrIntervalTicks)
        assertEquals(3, m.rrIntervalsSeconds.size)
        assertEquals(800.0 / 1024.0, m.rrIntervalsSeconds[0], absoluteTolerance = 1e-12)
    }

    @Test
    fun allFlagsCombined() {
        // uint16 HR + contact supported+detected + energy + RR
        // flags = 0x01|0x02|0x04|0x08|0x10 = 0x1F
        // HR=256 → 00 01; energy=10 → 0A 00; RR=1024 → 00 04
        val m = HeartRateMeasurementParser.parse(
            bytes(0x1F, 0x00, 0x01, 0x0A, 0x00, 0x00, 0x04),
        )
        assertEquals(256, m.heartRateBpm)
        assertTrue(m.heartRateIsUint16)
        assertTrue(m.sensorContactSupported)
        assertTrue(m.sensorContactDetected)
        assertEquals(10, m.energyExpendedKj)
        assertEquals(listOf(1024), m.rrIntervalTicks)
    }

    @Test
    fun emptyPayloadFails() {
        assertFailsWith<IllegalArgumentException> {
            HeartRateMeasurementParser.parse(byteArrayOf())
        }
    }

    @Test
    fun truncatedUint16HrFails() {
        assertFailsWith<IllegalArgumentException> {
            HeartRateMeasurementParser.parse(bytes(0x01, 0x2C))
        }
    }

    private fun bytes(vararg values: Int): ByteArray =
        values.map { it.toByte() }.toByteArray()
}
