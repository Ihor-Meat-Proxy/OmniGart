package com.omnigart.sensors.ble

import com.omnigart.sensors.model.SensorContactStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HeartRateMeasurementParserTest {

    @Test
    fun uint8HrOnly() {
        val m = parse(0x00, 0x48)
        assertEquals(72, m.heartRateBpm)
        assertEquals(SensorContactStatus.NotSupported, m.sensorContact)
        assertEquals(null, m.energyExpendedKcal)
        assertTrue(m.rrIntervalsMs.isEmpty())
        assertEquals(false, m.heartRateUint16)
    }

    @Test
    fun uint16HrOnly() {
        val m = parse(0x01, 0x2C, 0x01) // 300 bpm
        assertEquals(300, m.heartRateBpm)
        assertEquals(true, m.heartRateUint16)
    }

    @Test
    fun sensorContactSupportedNotDetected() {
        // bits 1–2: supported (bit2=1), not detected (bit1=0) → flags 0x04
        val m = parse(0x04, 0x50)
        assertEquals(SensorContactStatus.SupportedNotDetected, m.sensorContact)
        assertEquals(80, m.heartRateBpm)
    }

    @Test
    fun sensorContactSupportedDetected() {
        // bits 1–2: supported + detected → 0x06
        val m = parse(0x06, 0x50)
        assertEquals(SensorContactStatus.SupportedDetected, m.sensorContact)
    }

    @Test
    fun energyExpendedPresent() {
        // flag bit3 → 0x08; EE = 0x1234
        val m = parse(0x08, 0x60, 0x34, 0x12)
        assertEquals(96, m.heartRateBpm)
        assertEquals(0x1234, m.energyExpendedKcal)
    }

    @Test
    fun singleRrInterval() {
        // flag bit4 → 0x10; RR = 1024 units = 1000 ms
        val m = parse(0x10, 0x3C, 0x00, 0x04)
        assertEquals(60, m.heartRateBpm)
        assertEquals(1, m.rrIntervalsMs.size)
        assertEquals(1000.0, m.rrIntervalsMs[0], 1e-9)
    }

    @Test
    fun multipleRrIntervals() {
        // Three RR values: 800, 810, 790 units → ms = units * 1000/1024
        val m = parse(
            0x10, 0x4B,
            0x20, 0x03, // 800
            0x2A, 0x03, // 810
            0x16, 0x03, // 790
        )
        assertEquals(3, m.rrIntervalsMs.size)
        assertEquals(HeartRateMeasurementParser.rrUnitsToMillis(800), m.rrIntervalsMs[0], 1e-9)
        assertEquals(HeartRateMeasurementParser.rrUnitsToMillis(810), m.rrIntervalsMs[1], 1e-9)
        assertEquals(HeartRateMeasurementParser.rrUnitsToMillis(790), m.rrIntervalsMs[2], 1e-9)
    }

    @Test
    fun allFlagsUint8() {
        // uint8 HR + contact detected + EE + RR
        // flags = 0x1E = 0b00011110
        val m = parse(
            0x1E,
            0x55, // 85 bpm
            0x10, 0x00, // EE 16
            0x00, 0x04, // RR 1024 → 1000 ms
            0x00, 0x04,
        )
        assertEquals(85, m.heartRateBpm)
        assertEquals(SensorContactStatus.SupportedDetected, m.sensorContact)
        assertEquals(16, m.energyExpendedKcal)
        assertEquals(2, m.rrIntervalsMs.size)
        assertEquals(false, m.heartRateUint16)
    }

    @Test
    fun allFlagsUint16() {
        // flags = 0x1F
        val m = parse(
            0x1F,
            0x90, 0x01, // 400 bpm
            0xE8, 0x03, // EE 1000
            0x00, 0x04,
        )
        assertEquals(400, m.heartRateBpm)
        assertEquals(true, m.heartRateUint16)
        assertEquals(1000, m.energyExpendedKcal)
        assertEquals(listOf(1000.0), m.rrIntervalsMs)
        assertEquals(SensorContactStatus.SupportedDetected, m.sensorContact)
    }

    @Test
    fun contactSupportedBitCombinationsMatrix() {
        // Exhaustive bits 1–2 with uint8 HR
        data class Case(val flags: Int, val expected: SensorContactStatus)
        val cases = listOf(
            Case(0x00, SensorContactStatus.NotSupported),
            Case(0x02, SensorContactStatus.NotSupported), // detected bit ignored if unsupported
            Case(0x04, SensorContactStatus.SupportedNotDetected),
            Case(0x06, SensorContactStatus.SupportedDetected),
        )
        for (c in cases) {
            val m = parse(c.flags, 0x40)
            assertEquals(c.expected, m.sensorContact, "flags=${c.flags}")
        }
    }

    @Test
    fun rrConversionExactVectors() {
        assertEquals(0.0, HeartRateMeasurementParser.rrUnitsToMillis(0), 0.0)
        assertEquals(1000.0, HeartRateMeasurementParser.rrUnitsToMillis(1024), 1e-12)
        assertEquals(500.0, HeartRateMeasurementParser.rrUnitsToMillis(512), 1e-12)
        // 854 units ≈ 833.984375 ms (common ~72 bpm RR)
        assertEquals(854 * 1000.0 / 1024.0, HeartRateMeasurementParser.rrUnitsToMillis(854), 1e-12)
    }

    @Test
    fun emptyPacketFails() {
        assertTrue(HeartRateMeasurementParser.parse(byteArrayOf()).isFailure)
    }

    @Test
    fun truncatedUint16HrFails() {
        assertTrue(HeartRateMeasurementParser.parse(byteArrayOf(0x01, 0x2C)).isFailure)
    }

    @Test
    fun truncatedUint8HrFails() {
        assertTrue(HeartRateMeasurementParser.parse(byteArrayOf(0x00)).isFailure)
    }

    @Test
    fun truncatedEnergyFails() {
        assertTrue(HeartRateMeasurementParser.parse(byteArrayOf(0x08, 0x50, 0x01)).isFailure)
    }

    @Test
    fun oddRrTrailingFails() {
        assertTrue(
            HeartRateMeasurementParser.parse(byteArrayOf(0x10, 0x50, 0x00, 0x04, 0x01)).isFailure
        )
    }

    @Test
    fun unexpectedTrailingWithoutRrFails() {
        assertTrue(
            HeartRateMeasurementParser.parse(byteArrayOf(0x00, 0x50, 0x01)).isFailure
        )
    }

    @Test
    fun bodySensorLocationCodes() {
        assertEquals(BodySensorLocation.Chest, BodySensorLocation.fromByte(1))
        assertEquals(BodySensorLocation.Wrist, BodySensorLocation.fromByte(2))
        assertEquals(null, BodySensorLocation.fromByte(99))
    }

    @Test
    fun batteryLevel() {
        assertEquals(87, BatteryLevelParser.parse(byteArrayOf(87.toByte())).getOrThrow())
        assertTrue(BatteryLevelParser.parse(byteArrayOf()).isFailure)
        assertTrue(BatteryLevelParser.parse(byteArrayOf(101)).isFailure)
    }

    @Test
    fun deviceInformationUtf8() {
        val info = DeviceInformationParser.merge(
            manufacturerName = "Polar Electro Oy".encodeToByteArray(),
            modelNumber = "H10".encodeToByteArray(),
        )
        assertEquals("Polar Electro Oy", info.manufacturerName)
        assertEquals("H10", info.modelNumber)
    }

    @Test
    fun bleUuidShortForm() {
        assertEquals(BleUuids.HEART_RATE_SERVICE, BleUuids.shortUuid16(0x180D))
        assertEquals(BleUuids.HEART_RATE_MEASUREMENT, BleUuids.shortUuid16(0x2A37))
    }

    private fun parse(vararg bytes: Int) =
        HeartRateMeasurementParser.parse(bytes.map { it.toByte() }.toByteArray()).getOrThrow()
}
