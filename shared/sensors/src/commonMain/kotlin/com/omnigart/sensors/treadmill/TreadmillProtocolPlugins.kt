package com.omnigart.sensors.treadmill

import com.omnigart.sensors.treadmill.fitshow.FitShowCodec
import com.omnigart.sensors.treadmill.fitshow.FitShowStatusParser
import com.omnigart.sensors.treadmill.ftms.FtmsMachineStatusParser
import com.omnigart.sensors.treadmill.ftms.FtmsTreadmillDataParser
import com.omnigart.sensors.treadmill.ftms.FtmsUuids
import com.omnigart.sensors.treadmill.iconsole.IConsoleCodec
import com.omnigart.sensors.treadmill.iconsole.IConsoleTelemetryParser

object FtmsProtocolPlugin : TreadmillProtocolPlugin {
    override val kind: TreadmillProtocolKind = TreadmillProtocolKind.Ftms
    override val priority: Int = 0

    override fun matches(serviceUuids: Collection<String>): Boolean = serviceUuids.containsUuid("1826") ||
        serviceUuids.any { it.contains("1826") }

    override fun parseNotification(characteristicUuid: String, payload: ByteArray): TreadmillSample? {
        val uuid = characteristicUuid.lowercase()
        return when {
            uuid.contains("2acd") || uuid == FtmsUuids.TREADMILL_DATA ->
                FtmsTreadmillDataParser.parse(payload).getOrNull()
            uuid.contains("2ada") || uuid == FtmsUuids.FITNESS_MACHINE_STATUS ->
                FtmsMachineStatusParser.parse(payload)
                    .map { FtmsMachineStatusParser.toSamplePatch(it) }
                    .getOrNull()
            else -> null
        }
    }
}

object FitShowProtocolPlugin : TreadmillProtocolPlugin {
    override val kind: TreadmillProtocolKind = TreadmillProtocolKind.FitShow
    override val priority: Int = 10

    override fun matches(serviceUuids: Collection<String>): Boolean = serviceUuids.any {
        val u = it.lowercase()
        u.contains("fff0") || u == FitShowCodec.SERVICE
    }

    override fun parseNotification(characteristicUuid: String, payload: ByteArray): TreadmillSample? {
        val uuid = characteristicUuid.lowercase()
        if (!uuid.contains("fff1") && uuid != FitShowCodec.CHAR_NOTIFY) {
            // Some builds notify on FFF1 only; still try decode if payload looks framed.
            if (payload.isEmpty() || (payload[0].toInt() and 0xFF) != FitShowCodec.STX) {
                return null
            }
        }
        return FitShowStatusParser.parseNotification(payload).getOrNull()
    }
}

object IConsoleProtocolPlugin : TreadmillProtocolPlugin {
    override val kind: TreadmillProtocolKind = TreadmillProtocolKind.IConsole
    override val priority: Int = 20

    override fun matches(serviceUuids: Collection<String>): Boolean = serviceUuids.any {
        val u = it.lowercase()
        u.startsWith("49535343") || u == IConsoleCodec.UART_SERVICE.lowercase()
    }

    override fun parseNotification(characteristicUuid: String, payload: ByteArray): TreadmillSample? {
        if (payload.isEmpty() || (payload[0].toInt() and 0xFF) != IConsoleCodec.HEADER) {
            return null
        }
        return IConsoleTelemetryParser.parsePacket(payload).getOrNull()
    }
}
