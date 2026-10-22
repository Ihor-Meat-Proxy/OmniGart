package com.omnigart.sensors.ble

/**
 * Battery Level characteristic (0x2A19) — uint8 percent 0–100.
 */
data class BatteryLevel(val percent: Int) {
    init {
        require(percent in 0..100) { "Battery level must be 0..100, was $percent" }
    }

    companion object {
        fun parse(payload: ByteArray): BatteryLevel {
            require(payload.isNotEmpty()) { "Battery Level characteristic is empty" }
            val value = payload[0].toUByte().toInt()
            require(value in 0..100) { "Invalid battery level byte: $value" }
            return BatteryLevel(value)
        }
    }
}
