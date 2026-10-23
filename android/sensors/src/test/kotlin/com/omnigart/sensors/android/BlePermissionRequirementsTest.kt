package com.omnigart.sensors.android

import com.omnigart.sensors.android.permission.BlePermissionRequirements
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BlePermissionRequirementsTest {
    @Test
    fun requiredPermissionsNonEmpty() {
        assertTrue(BlePermissionRequirements.requiredPermissions().isNotEmpty())
    }
}
