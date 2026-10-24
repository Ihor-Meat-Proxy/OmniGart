package com.omnigart.treadmill.rsc

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RscMeasurementParserTest {

    @Test
    fun speedAndCadenceOnly() {
        // flags 0, speed 3.00 m/s = 768 = 0x0300, cadence 180
        val m = parse(0x00, 0x00, 0x03, 0xB4)
        assertEquals(3.0, m.instantaneousSpeedMps, 1e-9)
        assertEquals(10.8, m.instantaneousSpeedKmh, 1e-9)
        assertEquals(180, m.instantaneousCadenceSpm)
        assertEquals(false, m.isRunning)
    }

    @Test
    fun withStrideAndDistanceRunning() {
        // flags 0x07 = stride + distance + running
        // speed 2.5 m/s = 640 = 0x0280, cadence 160, stride 120 cm, distance 4250.0 m = 42500 dm
        val m =
            parse(
                0x07,
                0x80, 0x02,
                0xA0,
                0x78, 0x00,
                0x04, 0xA6, 0x00, 0x00 // 42500 dm LE
            )
        assertEquals(2.5, m.instantaneousSpeedMps, 1e-9)
        assertEquals(160, m.instantaneousCadenceSpm)
        assertEquals(120, m.instantaneousStrideLengthCm)
        assertEquals(4250.0, m.totalDistanceMetres!!, 1e-9)
        assertEquals(true, m.isRunning)
    }

    @Test
    fun truncatedFails() {
        assertTrue(RscMeasurementParser.parse(byteArrayOf(0x00, 0x01)).isFailure)
        assertTrue(RscMeasurementParser.parse(byteArrayOf(0x01, 0x00, 0x01, 0x50)).isFailure)
    }

    private fun parse(vararg values: Int) =
        RscMeasurementParser.parse(values.map { it.toByte() }.toByteArray()).getOrThrow()
}
