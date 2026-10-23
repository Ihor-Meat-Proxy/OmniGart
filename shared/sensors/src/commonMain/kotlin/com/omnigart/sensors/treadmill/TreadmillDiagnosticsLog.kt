package com.omnigart.sensors.treadmill

import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Raw BLE capture for the Treadmill diagnostics screen (Settings › Developer).
 * Export as JSON so a real treadmill protocol can be reverse-verified later.
 */
data class TreadmillDiagnosticsEntry(
    val timestampEpochMs: Long,
    val deviceId: String,
    val deviceName: String?,
    val serviceUuid: String?,
    val characteristicUuid: String,
    val hex: String,
    val note: String? = null
)

data class TreadmillDiagnosticsDevice(
    val id: String,
    val name: String?,
    val rssi: Int?,
    val serviceUuids: List<String>,
    val characteristics: List<TreadmillDiagnosticsCharacteristic>
)

data class TreadmillDiagnosticsCharacteristic(val serviceUuid: String, val characteristicUuid: String, val properties: List<String>)

class TreadmillDiagnosticsLog(private val clock: Clock = Clock.System) {
    private val entries = mutableListOf<TreadmillDiagnosticsEntry>()
    private val devices = mutableMapOf<String, TreadmillDiagnosticsDevice>()
    var protocolGuess: TreadmillProtocolKind? = null

    fun upsertDevice(device: TreadmillDiagnosticsDevice) {
        devices[device.id] = device
    }

    fun append(
        deviceId: String,
        deviceName: String?,
        serviceUuid: String?,
        characteristicUuid: String,
        payload: ByteArray,
        note: String? = null,
        at: Instant = clock.now()
    ) {
        entries +=
            TreadmillDiagnosticsEntry(
                timestampEpochMs = at.toEpochMilliseconds(),
                deviceId = deviceId,
                deviceName = deviceName,
                serviceUuid = serviceUuid,
                characteristicUuid = characteristicUuid,
                hex = payload.toHex(),
                note = note
            )
    }

    fun clear() {
        entries.clear()
        devices.clear()
        protocolGuess = null
    }

    fun snapshot(): List<TreadmillDiagnosticsEntry> = entries.toList()

    fun devicesSnapshot(): List<TreadmillDiagnosticsDevice> = devices.values.toList()

    fun toJson(pretty: Boolean = true): String {
        val nl = if (pretty) "\n" else ""
        val indent = if (pretty) "  " else ""
        val sb = StringBuilder()
        sb.append("{").append(nl)
        sb.append(indent).append("\"schema\":\"omnigart.treadmill.diagnostics.v1\",").append(nl)
        sb.append(indent).append("\"capturedAtEpochMs\":").append(clock.now().toEpochMilliseconds())
            .append(",").append(nl)
        sb.append(indent).append("\"protocolGuess\":")
            .append(protocolGuess?.name?.let { "\"$it\"" } ?: "null").append(",").append(nl)
        sb.append(indent).append("\"devices\":[")
        val deviceList = devices.values.toList()
        deviceList.forEachIndexed { i, d ->
            if (i > 0) sb.append(",")
            sb.append(nl).append(indent).append(indent)
            sb.append(deviceJson(d))
        }
        if (deviceList.isNotEmpty()) sb.append(nl).append(indent)
        sb.append("],").append(nl)
        sb.append(indent).append("\"entries\":[")
        entries.forEachIndexed { i, e ->
            if (i > 0) sb.append(",")
            sb.append(nl).append(indent).append(indent)
            sb.append(entryJson(e))
        }
        if (entries.isNotEmpty()) sb.append(nl).append(indent)
        sb.append("]").append(nl)
        sb.append("}")
        return sb.toString()
    }

    private fun deviceJson(d: TreadmillDiagnosticsDevice): String {
        val chars =
            d.characteristics.joinToString(",") { c ->
                """{"serviceUuid":${jsonStr(c.serviceUuid)},"characteristicUuid":${jsonStr(c.characteristicUuid)},"properties":[${
                    c.properties.joinToString(",") { jsonStr(it) }
                }]}"""
            }
        return """{"id":${jsonStr(d.id)},"name":${jsonStrOrNull(d.name)},"rssi":${d.rssi ?: "null"},"serviceUuids":[${
            d.serviceUuids.joinToString(",") { jsonStr(it) }
        }],"characteristics":[$chars]}"""
    }

    private fun entryJson(e: TreadmillDiagnosticsEntry): String =
        """{"timestampEpochMs":${e.timestampEpochMs},"deviceId":${jsonStr(e.deviceId)},"deviceName":${
            jsonStrOrNull(e.deviceName)
        },"serviceUuid":${jsonStrOrNull(e.serviceUuid)},"characteristicUuid":${
            jsonStr(e.characteristicUuid)
        },"hex":${jsonStr(e.hex)},"note":${jsonStrOrNull(e.note)}}"""

    private fun jsonStr(value: String): String = buildString {
        append('"')
        value.forEach { ch ->
            when (ch) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(ch)
            }
        }
        append('"')
    }

    private fun jsonStrOrNull(value: String?): String = value?.let { jsonStr(it) } ?: "null"
}

fun ByteArray.toHex(): String = joinToString(separator = "") { b ->
    val v = b.toInt() and 0xFF
    val hex = v.toString(16)
    if (hex.length == 1) "0$hex" else hex
}
