package com.omnigart.sensors.ble

/**
 * Bluetooth SIG assigned numbers used by the Heart Rate Profile and related services.
 *
 * Spec: Bluetooth Core Specification Supplement (CSS) + Heart Rate Profile 1.0 /
 * Heart Rate Service 1.0. UUIDs are 16-bit shortened forms of the Bluetooth Base UUID.
 */
object BleUuids {
    const val BLUETOOTH_BASE_UUID_SUFFIX = "-0000-1000-8000-00805f9b34fb"

    /** Heart Rate Service — 0x180D */
    const val HEART_RATE_SERVICE = "0000180d$BLUETOOTH_BASE_UUID_SUFFIX"

    /** Heart Rate Measurement — 0x2A37 */
    const val HEART_RATE_MEASUREMENT = "00002a37$BLUETOOTH_BASE_UUID_SUFFIX"

    /** Body Sensor Location — 0x2A38 */
    const val BODY_SENSOR_LOCATION = "00002a38$BLUETOOTH_BASE_UUID_SUFFIX"

    /** Heart Rate Control Point — 0x2A39 */
    const val HEART_RATE_CONTROL_POINT = "00002a39$BLUETOOTH_BASE_UUID_SUFFIX"

    /** Battery Service — 0x180F */
    const val BATTERY_SERVICE = "0000180f$BLUETOOTH_BASE_UUID_SUFFIX"

    /** Battery Level — 0x2A19 */
    const val BATTERY_LEVEL = "00002a19$BLUETOOTH_BASE_UUID_SUFFIX"

    /** Device Information Service — 0x180A */
    const val DEVICE_INFORMATION_SERVICE = "0000180a$BLUETOOTH_BASE_UUID_SUFFIX"

    const val MANUFACTURER_NAME = "00002a29$BLUETOOTH_BASE_UUID_SUFFIX"
    const val MODEL_NUMBER = "00002a24$BLUETOOTH_BASE_UUID_SUFFIX"
    const val SERIAL_NUMBER = "00002a25$BLUETOOTH_BASE_UUID_SUFFIX"
    const val HARDWARE_REVISION = "00002a27$BLUETOOTH_BASE_UUID_SUFFIX"
    const val FIRMWARE_REVISION = "00002a26$BLUETOOTH_BASE_UUID_SUFFIX"
    const val SOFTWARE_REVISION = "00002a28$BLUETOOTH_BASE_UUID_SUFFIX"

    /** Client Characteristic Configuration Descriptor — 0x2902 */
    const val CCCD = "00002902$BLUETOOTH_BASE_UUID_SUFFIX"

    fun shortUuid16(value: Int): String {
        require(value in 0..0xFFFF) { "16-bit UUID out of range: $value" }
        return "%08x%s".format(value, BLUETOOTH_BASE_UUID_SUFFIX)
    }
}
