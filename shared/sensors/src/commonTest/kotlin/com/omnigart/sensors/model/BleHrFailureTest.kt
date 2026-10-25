package com.omnigart.sensors.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BleHrFailureTest {
    @Test
    fun resultSuccess_hasNoFailure() {
        val result = BleHrResult.success()
        assertTrue(result.isSuccess)
        assertNull(result.failureOrNull())
        assertIs<BleHrResult.Success>(result)
    }

    @Test
    fun expectedFailures_carryStableMessages() {
        assertEquals(
            "Missing BLE permissions: android.permission.BLUETOOTH_SCAN",
            BleHrFailure.MissingPermissions(listOf("android.permission.BLUETOOTH_SCAN")).message,
        )
        assertEquals("Bluetooth adapter disabled", BleHrFailure.AdapterDisabled.message)
        assertEquals("Bluetooth LE scanner unavailable", BleHrFailure.ScannerUnavailable.message)
        assertEquals("Unknown device AA:BB", BleHrFailure.DeviceNotFound("AA:BB").message)
        assertEquals("Scan failed: 3", BleHrFailure.ScanFailed(3).message)
        assertEquals(
            "GATT discoverServices failed: status=133",
            BleHrFailure.GattError(133, "discoverServices").message,
        )
        assertEquals("Reconnect attempts exhausted", BleHrFailure.ReconnectExhausted.message)
    }

    @Test
    fun failureResult_surfacesTypedReason() {
        val failure = BleHrFailure.AdapterDisabled
        val result = BleHrResult.failure(failure)
        assertFalse(result.isSuccess)
        assertEquals(failure, result.failureOrNull())
        assertIs<BleHrResult.Failure>(result)
    }
}
