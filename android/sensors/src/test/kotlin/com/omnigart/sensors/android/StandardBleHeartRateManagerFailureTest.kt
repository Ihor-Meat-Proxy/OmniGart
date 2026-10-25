package com.omnigart.sensors.android

import android.bluetooth.BluetoothManager
import android.content.Context
import com.omnigart.sensors.android.ble.StandardBleHeartRateManager
import com.omnigart.sensors.android.permission.BlePermissionRequirements
import com.omnigart.sensors.model.BleHrFailure
import com.omnigart.sensors.model.BleHrResult
import com.omnigart.sensors.model.SensorConnectionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
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
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StandardBleHeartRateManagerFailureTest {
    private val mainDispatcher = UnconfinedTestDispatcher()

    @Before
    fun installMainDispatcher() {
        // Manager hops to Dispatchers.Main; avoid Robolectric looper deadlocks under runBlocking.
        Dispatchers.setMain(mainDispatcher)
    }

    @After
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun startScan_whenAdapterDisabled_returnsTypedFailure() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        grantBlePermissions(context)
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
        grantBlePermissions(context)
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
    fun connect_withInvalidAddress_returnsDeviceNotFound() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        grantBlePermissions(context)
        enableBluetooth(context)
        val manager = StandardBleHeartRateManager(context)
        try {
            val result = manager.connect("not-a-mac")
            assertTrue(result is BleHrResult.Failure)
            assertTrue(result.failureOrNull() is BleHrFailure.DeviceNotFound)
            assertEquals(SensorConnectionState.Failed, manager.sessionState.value.connection)
        } finally {
            manager.close()
        }
    }

    @Test
    fun startScan_whenPermissionsMissing_returnsTypedFailure() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        // Fresh application context: do not grant BLE permissions.
        enableBluetooth(context)
        val manager = StandardBleHeartRateManager(context)
        try {
            val result = manager.startScan()
            assertTrue(result is BleHrResult.Failure)
            assertTrue(result.failureOrNull() is BleHrFailure.MissingPermissions)
            assertEquals(SensorConnectionState.Failed, manager.sessionState.value.connection)
        } finally {
            manager.close()
        }
    }

    private fun grantBlePermissions(context: Context) {
        val app = context.applicationContext as android.app.Application
        Shadows.shadowOf(app).grantPermissions(*BlePermissionRequirements.requiredPermissions())
    }

    private fun disableBluetooth(context: Context) {
        val adapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
        Shadows.shadowOf(adapter).setEnabled(false)
    }

    private fun enableBluetooth(context: Context) {
        val adapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
        Shadows.shadowOf(adapter).setEnabled(true)
    }
}
