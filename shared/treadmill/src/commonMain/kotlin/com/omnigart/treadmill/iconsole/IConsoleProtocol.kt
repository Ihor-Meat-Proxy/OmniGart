package com.omnigart.treadmill.iconsole

/**
 * Clean-room implementation of the iConsole proprietary serial framing.
 *
 * Protocol documentation references (not copied):
 * - haraldh/iconsole — Bluetooth Classic SPP reverse engineering (bike console)
 * - iconsole-plus (GPL-2.0) — BLE UART handshake / heartbeat notes
 * - Microchip RN4870 transparent UART characteristic UUIDs
 *
 * OmniGart code is AGPL-3.0. Treadmill incline/speed set opcodes are not fully
 * published for all Reebok/iConsole variants; use diagnostics captures to extend.
 */
object IConsoleProtocol {

    const val PACKET_START: Int = 0xF0

    /** Microchip / ISSC transparent UART (BLE). */
    const val BLE_UART_TX_UUID = "49535343-1e4d-4bd9-ba61-23c647249616"
    const val BLE_UART_RX_UUID = "49535343-8841-43f4-a8d4-ecbe34729bb3"

    data class Telemetry(
        val speedKmh: Double,
        val cadenceRpm: Int,
        val distanceKm: Double,
        val energyKcal: Int,
        val heartRateBpm: Int,
        val powerWatts: Double,
        val level: Int,
        val status: Int?,
        val minutes: Int,
        val seconds: Int
    ) {
        val distanceMetres: Double get() = distanceKm * 1000.0
        val elapsedSeconds: Int get() = minutes * 60 + seconds
        val isRunning: Boolean get() = status == 2
    }

    fun checksum(payloadWithoutChecksum: ByteArray): Int =
        payloadWithoutChecksum.fold(0) { acc, b -> (acc + (b.toInt() and 0xFF)) and 0xFF }

    fun withChecksum(bytesWithoutChecksum: ByteArray): ByteArray =
        bytesWithoutChecksum + checksum(bytesWithoutChecksum).toByte()

    fun verifyChecksum(packet: ByteArray): Boolean {
        if (packet.size < 2) return false
        val expected = checksum(packet.copyOfRange(0, packet.size - 1))
        return (packet.last().toInt() and 0xFF) == expected
    }

    fun ping(): ByteArray = withChecksum(byteArrayOf(0xF0.toByte(), 0xA0.toByte(), 0x01, 0x01))

    fun status(): ByteArray = withChecksum(byteArrayOf(0xF0.toByte(), 0xA1.toByte(), 0x01, 0x01))

    fun read(): ByteArray = withChecksum(byteArrayOf(0xF0.toByte(), 0xA2.toByte(), 0x01, 0x01))

    fun initA0(): ByteArray =
        withChecksum(byteArrayOf(0xF0.toByte(), 0xA0.toByte(), 0x02, 0x02))

    fun initA3(): ByteArray =
        withChecksum(byteArrayOf(0xF0.toByte(), 0xA3.toByte(), 0x01, 0x01, 0x01))

    fun initA4(): ByteArray =
        withChecksum(
            byteArrayOf(
                0xF0.toByte(), 0xA4.toByte(),
                0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01
            )
        )

    fun start(): ByteArray =
        withChecksum(byteArrayOf(0xF0.toByte(), 0xA5.toByte(), 0x01, 0x01, 0x02))

    fun stop(): ByteArray =
        withChecksum(byteArrayOf(0xF0.toByte(), 0xA5.toByte(), 0x01, 0x01, 0x04))

    /**
     * Bike-oriented level / resistance set. Some treadmill consoles reuse level for
     * incline steps; unknown variants need a diagnostics capture.
     */
    fun setLevel(level: Int): ByteArray {
        require(level in 0..31) { "level out of range: $level" }
        return withChecksum(
            byteArrayOf(0xF0.toByte(), 0xA6.toByte(), 0x01, 0x01, (level + 1).toByte())
        )
    }

    /**
     * Parse a 21-byte READ response (`F0 B2 …`).
     * Values use the offset-biased encoding documented by community RE.
     */
    fun parseTelemetry(packet: ByteArray): Result<Telemetry> = runCatching {
        require(packet.size == 21) { "Expected 21-byte READ response, got ${packet.size}" }
        require((packet[0].toInt() and 0xFF) == PACKET_START) { "Missing start byte" }
        require((packet[1].toInt() and 0xFF) == 0xB2) { "Expected response 0xB2" }
        require(verifyChecksum(packet)) { "Checksum mismatch" }

        fun biased(high: Int, low: Int): Int = 100 * (high - 1) + (low - 1)

        val minutes = (packet[4].toInt() and 0xFF) - 1
        val seconds = (packet[5].toInt() and 0xFF) - 1
        val speedRaw = biased(packet[6].toInt() and 0xFF, packet[7].toInt() and 0xFF)
        val rpm = biased(packet[8].toInt() and 0xFF, packet[9].toInt() and 0xFF)
        val distanceRaw = biased(packet[10].toInt() and 0xFF, packet[11].toInt() and 0xFF)
        val calories = biased(packet[12].toInt() and 0xFF, packet[13].toInt() and 0xFF)
        val hr = biased(packet[14].toInt() and 0xFF, packet[15].toInt() and 0xFF)
        val powerRaw = biased(packet[16].toInt() and 0xFF, packet[17].toInt() and 0xFF)
        val level = (packet[18].toInt() and 0xFF) - 1
        val status = packet[19].toInt() and 0xFF

        Telemetry(
            speedKmh = speedRaw / 10.0,
            cadenceRpm = rpm,
            distanceKm = distanceRaw / 10.0,
            energyKcal = calories,
            heartRateBpm = hr,
            powerWatts = powerRaw / 10.0,
            level = level,
            status = status,
            minutes = minutes.coerceAtLeast(0),
            seconds = seconds.coerceIn(0, 59)
        )
    }

    fun expectedResponseOpcode(requestSecondByte: Int): Int =
        0xB0 or (requestSecondByte and 0x0F)
}
