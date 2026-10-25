package com.omnigart.sensors.android

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import com.omnigart.sensors.android.ble.StandardBleHeartRateManager
import com.omnigart.sensors.model.BleHrFailure
import com.omnigart.sensors.model.BleHrResult
import com.omnigart.sensors.model.SensorConnectionState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.annotation.Config

/**
 * Expected BLE failures must return [BleHrResult.Failure] and update session state —
 * never throw once the sensors UI is enabled.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StandardBleHeartRateManagerFailureTest {
    @Test
    fun startScan_whenAdapterDisabled_returnsTypedFailure() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        disableBluetooth(context)
        val manager = StandardBleHeartRateManager(context)
        try {
            val result = manager.startScan()
            assertFalse(result.isSuccess)
            assertTrue(result.failureOrNull() is BleHrFailure.AdapterDisabled)
            assertEquals(SensorConnectionState.Failed, manager.sessionState.value.connection)
            assertEquals(
                BleHrFailure.AdapterDisabled,
                manager.sessionState.value.lastFailure,
            )
            assertEquals(
                BleHrFailure.AdapterDisabled.message,
                manager.sessionState.value.lastError,
            )
        } finally {
            manager.close()
        }
    }

    @Test
    fun connect_whenAdapterDisabled_returnsTypedFailure() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        disableBluetooth(context)
        val manager = StandardBleHeartRateManager(context)
        try {
            val result = manager.connect("AA:BB:CC:DD:EE:FF")
            assertTrue(result is BleHrResult.Failure)
            assertTrue(result.failureOrNull() is BleHrFailure.AdapterDisabled)
            assertEquals(SensorConnectionState.Failed, manager.sessionState.value.connection)
        } finally {
            manager.close()
        }
    }

    @Test
    fun connect_withInvalidAddress_returnsTypedFailureNotThrow() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        enableBluetooth(context)
        val manager = StandardBleHeartRateManager(context)
        try {
            val result = manager.connect("not-a-mac")
            assertTrue(result is BleHrResult.Failure)
            assertNotNull(result.failureOrNull())
            assertEquals(SensorConnectionState.Failed, manager.sessionState.value.connection)
        } finally {
            manager.close()
        }
    }

    private fun disableBluetooth(context: Context) {
        val adapter =
            (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
                ?: BluetoothAdapter.getDefaultAdapter()
        Shadows.shadowOf(adapter).setEnabled(false)
    }

    private fun enableBluetooth(context: Context) {
        val adapter =
            (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
                ?: BluetoothAdapter.getDefaultAdapter()
        Shadows.shadowOf(adapter).setEnabled(true)
    }
}
