package com.omnigart.sensors.treadmill

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TreadmillBridgeAndDiagnosticsTest {

    @Test
    fun bridgeSimulatorAndControlRoundTrip() {
        TreadmillBridge.simulatorReset()
        val grant = TreadmillBridge.simulatorControl(TreadmillBridge.controlRequestControl())
        assertEquals(1, grant[2].toInt() and 0xFF)
        TreadmillBridge.simulatorControl(TreadmillBridge.controlStart())
        TreadmillBridge.simulatorApplyPreset("tempo")
        val sample = TreadmillBridge.simulatorTick()
        assertEquals("Simulated", sample.protocolName)
        assertTrue(sample.optionalSpeed() != null)
        assertTrue(sample.optionalDistance()!! >= 0.0)
    }

    @Test
    fun diagnosticsJsonExport() {
        val log = TreadmillDiagnosticsLog()
        log.protocolGuess = TreadmillProtocolKind.Ftms
        log.append(
            deviceId = "aa",
            deviceName = "Desk Treadmill",
            serviceUuid = "00001826-0000-1000-8000-00805f9b34fb",
            characteristicUuid = "00002acd-0000-1000-8000-00805f9b34fb",
            payload = byteArrayOf(0x00, 0x00, 0xE8.toByte(), 0x03)
        )
        val json = log.toJson()
        assertTrue(json.contains("omnigart.treadmill.diagnostics.v1"))
        assertTrue(json.contains("00e803") || json.contains("00e8") || json.contains("e803"))
        assertTrue(json.contains("Ftms"))
    }

    @Test
    fun hexHelper() {
        assertEquals("0001ff", byteArrayOf(0x00, 0x01, 0xFF.toByte()).toHex())
    }
}
