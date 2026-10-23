package com.omnigart.sensors.ble

/**
 * Battery Level characteristic (0x2A19): uint8 percent 0–100.
 */
object BatteryLevelParser {
    fun parse(bytes: ByteArray): Result<Int> {
        if (bytes.isEmpty()) {
            return Result.failure(IllegalArgumentException("Empty battery level packet"))
        }
        val level = bytes[0].toInt() and 0xFF
        if (level > 100) {
            return Result.failure(IllegalArgumentException("Battery level out of range: $level"))
        }
        return Result.success(level)
    }
}
