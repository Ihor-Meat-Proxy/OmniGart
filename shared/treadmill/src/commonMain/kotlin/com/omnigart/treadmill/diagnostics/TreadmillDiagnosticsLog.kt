package com.omnigart.treadmill.diagnostics

/**
 * Append-only capture of GATT discovery and raw bytes for unknown console variants.
 * Export as UTF-8 text or JSON for support / protocol follow-up.
 */
class TreadmillDiagnosticsLog {
    private val lines = ArrayList<String>()
    private val devices = LinkedHashMap<String, DeviceSnapshot>()
    private val entries = ArrayList<RawEntry>()
    var protocolGuess: String? = null

    fun clear() {
        lines.clear()
        devices.clear()
        entries.clear()
        protocolGuess = null
    }

    fun info(message: String) {
        lines += "${timestamp()} INFO $message"
    }

    fun service(uuid: String, primary: Boolean = true) {
        lines += "${timestamp()} SERVICE ${if (primary) "primary" else "secondary"} $uuid"
    }

    fun characteristic(serviceUuid: String, charUuid: String, properties: String) {
        lines += "${timestamp()} CHAR svc=$serviceUuid char=$charUuid props=$properties"
        val deviceId = devices.keys.lastOrNull() ?: "unknown"
        val device =
            devices.getOrPut(deviceId) {
                DeviceSnapshot(
                    id = deviceId,
                    name = null,
                    serviceUuids = mutableListOf(),
                    characteristics = mutableListOf()
                )
            }
        if (serviceUuid !in device.serviceUuids) {
            device.serviceUuids += serviceUuid
        }
        device.characteristics +=
            CharacteristicSnapshot(
                serviceUuid = serviceUuid,
                characteristicUuid = charUuid,
                properties = properties.split(',').map { it.trim() }.filter { it.isNotEmpty() }
            )
    }

    fun upsertDevice(id: String, name: String?, serviceUuids: List<String> = emptyList()) {
        val existing =
            devices.getOrPut(id) {
                DeviceSnapshot(id = id, name = name, serviceUuids = mutableListOf(), characteristics = mutableListOf())
            }
        if (name != null) existing.name = name
        serviceUuids.forEach { uuid ->
            if (uuid !in existing.serviceUuids) existing.serviceUuids += uuid
        }
        info("device $id ${name.orEmpty()}")
    }

    fun rawBytes(source: String, bytes: ByteArray, note: String? = null) {
        val hex = bytes.toHexSpaced()
        lines += "${timestamp()} RAW $source $hex"
        val deviceId = devices.keys.lastOrNull() ?: "unknown"
        val device = devices[deviceId]
        entries +=
            RawEntry(
                deviceId = deviceId,
                deviceName = device?.name,
                serviceUuid = null,
                characteristicUuid = source,
                hex = bytes.toHexCompact(),
                note = note
            )
    }

    fun appendNotify(
        deviceId: String,
        deviceName: String?,
        serviceUuid: String?,
        characteristicUuid: String,
        payload: ByteArray,
        note: String? = null
    ) {
        upsertDevice(deviceId, deviceName, listOfNotNull(serviceUuid))
        rawBytes(characteristicUuid, payload, note)
        val last = entries.lastOrNull() ?: return
        entries[entries.lastIndex] =
            last.copy(
                deviceId = deviceId,
                deviceName = deviceName,
                serviceUuid = serviceUuid,
                characteristicUuid = characteristicUuid,
                note = note
            )
    }

    fun exportText(): String =
        buildString {
            appendLine("OmniGart treadmill diagnostics")
            appendLine("Protocol capture only — not a medical record.")
            appendLine("---")
            lines.forEach { appendLine(it) }
        }

    fun exportJson(pretty: Boolean = true): String {
        val nl = if (pretty) "\n" else ""
        val indent = if (pretty) "  " else ""
        val sb = StringBuilder()
        sb.append("{").append(nl)
        sb.append(indent).append("\"schema\":\"omnigart.treadmill.diagnostics.v1\",").append(nl)
        sb.append(indent).append("\"protocolGuess\":")
            .append(protocolGuess?.let { jsonStr(it) } ?: "null").append(",").append(nl)
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

    fun lineCount(): Int = lines.size

    fun entryCount(): Int = entries.size

    private fun timestamp(): String = "t=${lines.size}"

    private fun deviceJson(d: DeviceSnapshot): String {
        val chars =
            d.characteristics.joinToString(",") { c ->
                """{"serviceUuid":${jsonStr(c.serviceUuid)},"characteristicUuid":${jsonStr(c.characteristicUuid)},"properties":[${
                    c.properties.joinToString(",") { jsonStr(it) }
                }]}"""
            }
        return """{"id":${jsonStr(d.id)},"name":${jsonStrOrNull(d.name)},"serviceUuids":[${
            d.serviceUuids.joinToString(",") { jsonStr(it) }
        }],"characteristics":[$chars]}"""
    }

    private fun entryJson(e: RawEntry): String =
        """{"deviceId":${jsonStr(e.deviceId)},"deviceName":${jsonStrOrNull(e.deviceName)},"serviceUuid":${
            jsonStrOrNull(e.serviceUuid)
        },"characteristicUuid":${jsonStr(e.characteristicUuid)},"hex":${jsonStr(e.hex)},"note":${
            jsonStrOrNull(e.note)
        }}"""

    private fun jsonStr(value: String): String =
        buildString {
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

    private data class DeviceSnapshot(
        val id: String,
        var name: String?,
        val serviceUuids: MutableList<String>,
        val characteristics: MutableList<CharacteristicSnapshot>
    )

    private data class CharacteristicSnapshot(
        val serviceUuid: String,
        val characteristicUuid: String,
        val properties: List<String>
    )

    private data class RawEntry(
        val deviceId: String,
        val deviceName: String?,
        val serviceUuid: String?,
        val characteristicUuid: String,
        val hex: String,
        val note: String?
    )

    private fun ByteArray.toHexSpaced(): String =
        joinToString(separator = " ") { b ->
            (b.toInt() and 0xFF).toString(16).padStart(2, '0')
        }

    private fun ByteArray.toHexCompact(): String =
        joinToString(separator = "") { b ->
            (b.toInt() and 0xFF).toString(16).padStart(2, '0')
        }
}
