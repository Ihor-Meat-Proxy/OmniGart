package com.omnigart.sensors.api

/**
 * Declarative permission / Info.plist requirements for BLE HR sensors.
 * Platform modules apply these; app shells must still declare them in manifests.
 */
object PlatformPermissions {
    object Android {
        /** API 31+ */
        const val BLUETOOTH_SCAN = "android.permission.BLUETOOTH_SCAN"
        const val BLUETOOTH_CONNECT = "android.permission.BLUETOOTH_CONNECT"

        /** Legacy ≤ API 30 */
        const val BLUETOOTH = "android.permission.BLUETOOTH"
        const val BLUETOOTH_ADMIN = "android.permission.BLUETOOTH_ADMIN"
        const val ACCESS_FINE_LOCATION = "android.permission.ACCESS_FINE_LOCATION"

        /**
         * BLUETOOTH_SCAN must use `android:usesPermissionFlags="neverForLocation"`
         * when the app does not derive physical location from BLE scans.
         */
        const val NEVER_FOR_LOCATION_FLAG = "neverForLocation"
    }

    object Apple {
        const val USAGE_DESCRIPTION_KEY = "NSBluetoothAlwaysUsageDescription"
        const val USAGE_DESCRIPTION_DEFAULT =
            "OmniGart connects to Bluetooth heart-rate sensors during workouts."

        /** UIBackgroundModes value for phone workouts keeping central role alive. */
        const val BACKGROUND_MODE_BLUETOOTH_CENTRAL = "bluetooth-central"

        /**
         * watchOS: CoreBluetooth central is available, but HealthKit workout builders
         * use the Apple Watch optical HR sensor. External BLE straps can feed a custom
         * UI / Shared pipeline; they are not a drop-in HealthKit HR source replacement.
         * See docs/SENSORS.md.
         */
        const val WATCHOS_EXTERNAL_HR_LIMITATION =
            "External BLE HR straps are not substituted into HealthKit workout HR on watchOS."
    }

    object WearOs {
        /**
         * Wear OS can act as BLE central and consume standard HR profile straps during
         * an exercise session more freely than watchOS HealthKit, subject to battery and
         * companion-phone routing. Prefer on-watch optical HR when the strap is unreachable.
         */
        const val NOTE =
            "Wear OS supports BLE central HR straps; validate OEM battery restrictions."
    }
}
