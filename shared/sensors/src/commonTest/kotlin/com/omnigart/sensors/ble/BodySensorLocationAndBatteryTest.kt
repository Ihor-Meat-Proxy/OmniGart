package com.omnigart.sensors.ble

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BodySensorLocationAndBatteryTest {

    @Test
    fun parsesKnownLocations() {
        assertEquals(BodySensorLocation.CHEST, BodySensorLocation.parse(byteArrayOf(1)))
        assertEquals(BodySensorLocation.WRIST, BodySensorLocation.parse(byteArrayOf(2)))
        assertEquals(BodySensorLocation.FOOT, BodySensorLocation.parse(byteArrayOf(6)))
    }

    @Test
    fun unknownLocationMapsToOther() {
        assertEquals(BodySensorLocation.OTHER, BodySensorLocation.parse(byteArrayOf(99)))
    }

    @Test
    fun batteryLevelValid() {
        assertEquals(87, BatteryLevel.parse(byteArrayOf(87)).percent)
        assertEquals(0, BatteryLevel.parse(byteArrayOf(0)).percent)
        assertEquals(100, BatteryLevel.parse(byteArrayOf(100)).percent)
    }

    @Test
    fun batteryLevelOutOfRangeFails() {
        assertFailsWith<IllegalArgumentException> {
            BatteryLevel.parse(byteArrayOf(101.toByte()))
        }
    }

    @Test
    fun bleUuidExpansion() {
        assertEquals(
            "0000180d-0000-1000-8000-00805f9b34fb",
            BleUuids.toFullUuid(BleUuids.HEART_RATE_SERVICE),
        )
        assertEquals(
            "00002a37-0000-1000-8000-00805f9b34fb",
            BleUuids.toFullUuid(BleUuids.HEART_RATE_MEASUREMENT),
        )
    }
}
