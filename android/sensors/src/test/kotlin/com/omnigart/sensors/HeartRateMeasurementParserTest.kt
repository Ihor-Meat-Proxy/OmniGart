package com.omnigart.sensors

import com.omnigart.sensors.ble.HeartRateMeasurementParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HeartRateMeasurementParserTest {
    @Test
    fun uint8Hr() {
        val m = HeartRateMeasurementParser.parse(byteArrayOf(0x00, 72))
        assertEquals(72, m.heartRateBpm)
        assertFalse(m.heartRateIsUint16)
    }

    @Test
    fun uint16AndRr() {
        val m = HeartRateMeasurementParser.parse(
            byteArrayOf(0x11, 0x2C, 0x01, 0x00, 0x04),
        )
        assertEquals(300, m.heartRateBpm)
        assertTrue(m.heartRateIsUint16)
        assertEquals(listOf(1024), m.rrIntervalTicks)
        assertEquals(1.0, m.rrIntervalsSeconds.single(), 1e-9)
    }
}
