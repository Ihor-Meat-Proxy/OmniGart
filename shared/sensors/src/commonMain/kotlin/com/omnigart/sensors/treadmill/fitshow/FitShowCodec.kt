package com.omnigart.sensors.treadmill.fitshow

import com.omnigart.sensors.ble.BleParseException

/**
 * FitShow-style framed packets used by many budget treadmills (FFF0 service).
 *
 * Frame: `0x02 | CMD | [SCMD] | DATA… | FCS | 0x03`
 * FCS = XOR of every byte from CMD through last DATA byte (not including STX/ETX).
 *
 * Verified against published open protocol notes (MIT / AGPL-compatible sources —
 * see docs/TREADMILL_PROTOCOLS.md). Speculative where OEMs diverge.
 */
object FitShowCodec {
    const val STX: Int = 0x02
    const val ETX: Int = 0x03

    /** Service / characteristics (Bluetooth Base UUID form). */
    const val SERVICE = "0000fff0-0000-1000-8000-00805f9b34fb"
    const val CHAR_NOTIFY = "0000fff1-0000-1000-8000-00805f9b34fb"
    const val CHAR_WRITE = "0000fff2-0000-1000-8000-00805f9b34fb"

    const val CMD_INFO = 0x50
    const val CMD_STATUS = 0x51
    const val CMD_DATA = 0x52
    const val CMD_CONTROL = 0x53

    const val SCMD_MODEL = 0x00
    const val SCMD_SPEED_RANGE = 0x02
    const val SCMD_INCLINE_RANGE = 0x03
    const val SCMD_START = 0x01
    const val SCMD_SET_TARGET = 0x02
    const val SCMD_STOP = 0x03
    const val SCMD_PAUSE = 0x0A

    data class Frame(val cmd: Int, val payload: ByteArray) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Frame) return false
            return cmd == other.cmd && payload.contentEquals(other.payload)
        }

        override fun hashCode(): Int = 31 * cmd + payload.contentHashCode()
    }

    fun fcs(cmd: Int, payload: ByteArray): Int {
        var xor = cmd and 0xFF
        for (b in payload) {
            xor = xor xor (b.toInt() and 0xFF)
        }
        return xor and 0xFF
    }

    fun encode(cmd: Int, payload: ByteArray = byteArrayOf()): ByteArray {
        val checksum = fcs(cmd, payload)
        return ByteArray(3 + payload.size + 1).also { out ->
            out[0] = STX.toByte()
            out[1] = cmd.toByte()
            payload.copyInto(out, 2)
            out[2 + payload.size] = checksum.toByte()
            out[3 + payload.size] = ETX.toByte()
        }
    }

    fun encode(cmd: Int, scmd: Int, data: ByteArray = byteArrayOf()): ByteArray = encode(cmd, byteArrayOf(scmd.toByte()) + data)

    fun decode(bytes: ByteArray): Result<Frame> = runCatching {
        if (bytes.size < 4) throw BleParseException("FitShow frame too short")
        if ((bytes[0].toInt() and 0xFF) != STX) {
            throw BleParseException("FitShow missing STX")
        }
        if ((bytes[bytes.lastIndex].toInt() and 0xFF) != ETX) {
            throw BleParseException("FitShow missing ETX")
        }
        val cmd = bytes[1].toInt() and 0xFF
        val payload = bytes.copyOfRange(2, bytes.size - 2)
        val expected = fcs(cmd, payload)
        val actual = bytes[bytes.size - 2].toInt() and 0xFF
        if (expected != actual) {
            throw BleParseException("FitShow FCS mismatch (expected $expected got $actual)")
        }
        Frame(cmd, payload)
    }
}
