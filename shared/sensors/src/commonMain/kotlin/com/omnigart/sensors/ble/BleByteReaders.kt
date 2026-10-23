package com.omnigart.sensors.ble

/** Little-endian BLE field readers shared by HR and treadmill parsers. */
internal object BleByteReaders {
    fun uint8(bytes: ByteArray, offset: Int): Int = bytes[offset].toInt() and 0xFF

    fun uint16Le(bytes: ByteArray, offset: Int): Int {
        val lo = bytes[offset].toInt() and 0xFF
        val hi = bytes[offset + 1].toInt() and 0xFF
        return lo or (hi shl 8)
    }

    fun sint16Le(bytes: ByteArray, offset: Int): Int {
        val u = uint16Le(bytes, offset)
        return if (u >= 0x8000) u - 0x10000 else u
    }

    fun uint24Le(bytes: ByteArray, offset: Int): Int {
        val b0 = bytes[offset].toInt() and 0xFF
        val b1 = bytes[offset + 1].toInt() and 0xFF
        val b2 = bytes[offset + 2].toInt() and 0xFF
        return b0 or (b1 shl 8) or (b2 shl 16)
    }

    fun uint32Le(bytes: ByteArray, offset: Int): Long {
        val b0 = bytes[offset].toLong() and 0xFF
        val b1 = bytes[offset + 1].toLong() and 0xFF
        val b2 = bytes[offset + 2].toLong() and 0xFF
        val b3 = bytes[offset + 3].toLong() and 0xFF
        return b0 or (b1 shl 8) or (b2 shl 16) or (b3 shl 24)
    }

    fun requireRemaining(bytes: ByteArray, offset: Int, need: Int, label: String) {
        if (bytes.size < offset + need) {
            throw BleParseException("Truncated $label (need $need at $offset, have ${bytes.size})")
        }
    }
}

class BleParseException(message: String) : Exception(message)
