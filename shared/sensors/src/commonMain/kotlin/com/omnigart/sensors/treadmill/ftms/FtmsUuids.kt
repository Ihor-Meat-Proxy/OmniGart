package com.omnigart.sensors.treadmill.ftms

import com.omnigart.sensors.ble.BleUuids

/**
 * Bluetooth SIG Fitness Machine Service (FTMS) assigned numbers.
 * Spec: Fitness Machine Service 1.0 / GATT Specification Supplement.
 */
object FtmsUuids {
    /** Fitness Machine Service — 0x1826 */
    const val FITNESS_MACHINE_SERVICE = "00001826${BleUuids.BLUETOOTH_BASE_UUID_SUFFIX}"

    /** Fitness Machine Feature — 0x2ACC */
    const val FITNESS_MACHINE_FEATURE = "00002acc${BleUuids.BLUETOOTH_BASE_UUID_SUFFIX}"

    /** Treadmill Data — 0x2ACD */
    const val TREADMILL_DATA = "00002acd${BleUuids.BLUETOOTH_BASE_UUID_SUFFIX}"

    /** Training Status — 0x2AD3 */
    const val TRAINING_STATUS = "00002ad3${BleUuids.BLUETOOTH_BASE_UUID_SUFFIX}"

    /** Supported Speed Range — 0x2AD4 */
    const val SUPPORTED_SPEED_RANGE = "00002ad4${BleUuids.BLUETOOTH_BASE_UUID_SUFFIX}"

    /** Supported Inclination Range — 0x2AD5 */
    const val SUPPORTED_INCLINATION_RANGE = "00002ad5${BleUuids.BLUETOOTH_BASE_UUID_SUFFIX}"

    /** Fitness Machine Control Point — 0x2AD9 */
    const val FITNESS_MACHINE_CONTROL_POINT = "00002ad9${BleUuids.BLUETOOTH_BASE_UUID_SUFFIX}"

    /** Fitness Machine Status — 0x2ADA */
    const val FITNESS_MACHINE_STATUS = "00002ada${BleUuids.BLUETOOTH_BASE_UUID_SUFFIX}"
}
