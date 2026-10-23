package com.omnigart.treadmill.ble

/**
 * Bluetooth SIG and vendor UUIDs used for treadmill discovery.
 *
 * FTMS / RSC: Bluetooth assigned numbers.
 * iConsole BLE UART: Microchip / ISSC transparent UART (RN4870 / BM70 family).
 */
object TreadmillBleUuids {
    const val BLUETOOTH_BASE_UUID_SUFFIX = "-0000-1000-8000-00805f9b34fb"

    /** Fitness Machine Service — 0x1826 */
    const val FITNESS_MACHINE_SERVICE = "00001826$BLUETOOTH_BASE_UUID_SUFFIX"

    /** Fitness Machine Feature — 0x2ACC */
    const val FITNESS_MACHINE_FEATURE = "00002acc$BLUETOOTH_BASE_UUID_SUFFIX"

    /** Treadmill Data — 0x2ACD */
    const val TREADMILL_DATA = "00002acd$BLUETOOTH_BASE_UUID_SUFFIX"

    /** Training Status — 0x2AD3 */
    const val TRAINING_STATUS = "00002ad3$BLUETOOTH_BASE_UUID_SUFFIX"

    /** Fitness Machine Control Point — 0x2AD9 */
    const val FITNESS_MACHINE_CONTROL_POINT = "00002ad9$BLUETOOTH_BASE_UUID_SUFFIX"

    /** Fitness Machine Status — 0x2ADA */
    const val FITNESS_MACHINE_STATUS = "00002ada$BLUETOOTH_BASE_UUID_SUFFIX"

    /** Running Speed and Cadence Service — 0x1814 */
    const val RSC_SERVICE = "00001814$BLUETOOTH_BASE_UUID_SUFFIX"

    /** RSC Measurement — 0x2A53 */
    const val RSC_MEASUREMENT = "00002a53$BLUETOOTH_BASE_UUID_SUFFIX"

    /** RSC Feature — 0x2A54 */
    const val RSC_FEATURE = "00002a54$BLUETOOTH_BASE_UUID_SUFFIX"

    /** Client Characteristic Configuration Descriptor — 0x2902 */
    const val CCCD = "00002902$BLUETOOTH_BASE_UUID_SUFFIX"

    /** FitShow-style service / notify / write (budget treadmills). */
    const val FITSHOW_SERVICE = "0000fff0$BLUETOOTH_BASE_UUID_SUFFIX"
    const val FITSHOW_NOTIFY = "0000fff1$BLUETOOTH_BASE_UUID_SUFFIX"
    const val FITSHOW_WRITE = "0000fff2$BLUETOOTH_BASE_UUID_SUFFIX"

    /** Standard Heart Rate Service — chest strap overlay during treadmill sessions. */
    const val HEART_RATE_SERVICE = "0000180d$BLUETOOTH_BASE_UUID_SUFFIX"
    const val HEART_RATE_MEASUREMENT = "00002a37$BLUETOOTH_BASE_UUID_SUFFIX"

    /**
     * Microchip / ISSC transparent UART TX (device → central notify).
     * Documented for iConsole BLE modules (RN4870 / BM70).
     */
    const val ICONSOLE_UART_TX = "49535343-1e4d-4bd9-ba61-23c647249616"

    /** Microchip / ISSC transparent UART RX (central → device write). */
    const val ICONSOLE_UART_RX = "49535343-8841-43f4-a8d4-ecbe34729bb3"

    fun shortUuid16(value: Int): String {
        require(value in 0..0xFFFF) { "16-bit UUID out of range: $value" }
        return "%08x%s".format(value, BLUETOOTH_BASE_UUID_SUFFIX)
    }

    fun normalize(uuid: String): String = uuid.lowercase()
}
