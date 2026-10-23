package com.omnigart.sensors.android

import com.omnigart.sensors.android.store.RememberedDevicesStore
import com.omnigart.sensors.model.SensorDevice
import com.omnigart.sensors.model.SensorKind
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class RememberedDevicesStoreTest {
    @Test
    fun rememberAndForgetRoundTrip() {
        val context = RuntimeEnvironment.getApplication()
        val store = RememberedDevicesStore(context)
        val device = SensorDevice("AA:BB:CC:DD:EE:FF", "Test Strap", SensorKind.StandardHeartRateProfile)
        store.remember(device)
        assertEquals(1, store.devices.value.size)
        assertEquals("Test Strap", store.devices.value.first().name)
        store.forget(device.id)
        assertEquals(0, store.devices.value.size)
    }
}
