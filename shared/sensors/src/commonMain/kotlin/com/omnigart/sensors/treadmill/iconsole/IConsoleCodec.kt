package com.omnigart.sensors.treadmill.iconsole

import com.omnigart.sensors.ble.BleParseException

/**
 * iConsole+ UART packet codec (Microchip Transparent UART over BLE, or classic SPP).
 *
 * Packet: `0xF0 | OP | …body… | checksum` where checksum = (sum of prior bytes) & 0xFF.
 * Many numeric fields are stored as value+1 (decode: byte − 1).
 *
 * Based on published MIT-licensed reverse engineering (see docs/TREADMILL_PROTOCOLS.md).
 * Public captures are strongest for bikes; treadmill mapping is provisional until a
 * live Reebok iConsole capture confirms field layout.
 */
object IConsoleCodec {
    const val HEADER = 0xF0

    /** Microchip Transparent UART service characteristics. */
    const val UART_SERVICE = "49535343-fe7d-4ae5-8fa9-9fafd205e455"
    const val UART_TX_NOTIFY = "49535343-1e4d-4bd9-ba61-23c647249616"
    const val UART_RX_WRITE = "49535343-8841-43f4-a8d4-ecbe34729bb3"

    const val OP_PING = 0xA0
    const val OP_STATUS = 0xA1
    const val OP_READ = 0xA2
    const val OP_CONTROL = 0xA5
    const val OP_SET_LEVEL = 0xA6

    fun checksum(bytes: ByteArray): Int {
        var sum = 0
        for (b in bytes) {
            sum = (sum + (b.toInt() and 0xFF)) and 0xFF
        }
        return sum
    }

    fun encode(op: Int, body: ByteArray = byteArrayOf()): ByteArray {
        val withoutCs = ByteArray(2 + body.size)
        withoutCs[0] = HEADER.toByte()
        withoutCs[1] = op.toByte()
        body.copyInto(withoutCs, 2)
        return withoutCs + checksum(withoutCs).toByte()
    }

    fun decode(bytes: ByteArray): Result<Pair<Int, ByteArray>> = runCatching {
        if (bytes.size < 3) throw BleParseException("iConsole packet too short")
        if ((bytes[0].toInt() and 0xFF) != HEADER) {
            throw BleParseException("iConsole missing header 0xF0")
        }
        val bodyAndOp = bytes.copyOfRange(0, bytes.size - 1)
        val expected = checksum(bodyAndOp)
        val actual = bytes.last().toInt() and 0xFF
        if (expected != actual) {
            throw BleParseException("iConsole checksum mismatch")
        }
        val op = bytes[1].toInt() and 0xFF
        val body = if (bytes.size > 3) bytes.copyOfRange(2, bytes.size - 1) else byteArrayOf()
        op to body
    }

    fun ping(): ByteArray = encode(OP_PING, byteArrayOf(0x01, 0x01))

    fun initVersion(): ByteArray = encode(OP_PING, byteArrayOf(0x02, 0x02))

    fun status(): ByteArray = encode(OP_STATUS, byteArrayOf(0x01, 0x01))

    fun read(): ByteArray = encode(OP_READ, byteArrayOf(0x01, 0x01))

    fun start(): ByteArray = encode(OP_CONTROL, byteArrayOf(0x01, 0x01, 0x02))

    fun stop(): ByteArray = encode(OP_CONTROL, byteArrayOf(0x01, 0x01, 0x04))

    fun setLevel(level: Int): ByteArray {
        val encoded = (level + 1).coerceIn(1, 255)
        return encode(OP_SET_LEVEL, byteArrayOf(0x01, 0x01, encoded.toByte()))
    }

    fun decodePlusOne(value: Int): Int = (value and 0xFF) - 1
}
