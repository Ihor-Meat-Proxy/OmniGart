package com.omnigart.treadmill.diagnostics

import kotlin.test.Test
import kotlin.test.assertTrue

class TreadmillDiagnosticsLogTest {

    @Test
    fun textAndJsonExportIncludeRawHex() {
        val log = TreadmillDiagnosticsLog()
        log.protocolGuess = "FTMS"
        log.upsertDevice(
            id = "AA:BB",
            name = "Desk Treadmill",
            serviceUuids = listOf("00001826-0000-1000-8000-00805f9b34fb")
        )
        log.service("00001826-0000-1000-8000-00805f9b34fb")
        log.characteristic(
            "00001826-0000-1000-8000-00805f9b34fb",
            "00002acd-0000-1000-8000-00805f9b34fb",
            "NOTIFY"
        )
        log.appendNotify(
            deviceId = "AA:BB",
            deviceName = "Desk Treadmill",
            serviceUuid = "00001826-0000-1000-8000-00805f9b34fb",
            characteristicUuid = "00002acd-0000-1000-8000-00805f9b34fb",
            payload = byteArrayOf(0x00, 0x00, 0xE8.toByte(), 0x03)
        )

        val text = log.exportText()
        assertTrue(text.contains("OmniGart treadmill diagnostics"))
        assertTrue(text.contains("RAW"))

        val json = log.exportJson()
        assertTrue(json.contains("omnigart.treadmill.diagnostics.v1"))
        assertTrue(json.contains("FTMS"))
        assertTrue(json.contains("00e803") || json.contains("e803"))
        assertTrue(json.contains("Desk Treadmill"))
        assertTrue(log.entryCount() >= 1)
    }
}
