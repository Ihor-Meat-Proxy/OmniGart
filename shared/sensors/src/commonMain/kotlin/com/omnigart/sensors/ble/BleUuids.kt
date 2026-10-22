package com.omnigart.sensors.ble

/**
 * Bluetooth SIG assigned UUIDs used by the standard Heart Rate Profile.
 *
 * Short (16-bit) form shown; full 128-bit UUID =
 * `0000XXXX-0000-1000-8000-00805F9B34FB`.
 */
object BleUuids {
    const val HEART_RATE_SERVICE = "180D"
    const val HEART_RATE_MEASUREMENT = "2A37"
    const val BODY_SENSOR_LOCATION = "2A38"
    const val HEART_RATE_CONTROL_POINT = "2A39"

    const val BATTERY_SERVICE = "180F"
    const val BATTERY_LEVEL = "2A19"

    const val DEVICE_INFORMATION_SERVICE = "180A"

    fun toFullUuid(short16: String): String =
        "0000${short16.lowercase()}-0000-1000-8000-00805f9b34fb"
}
