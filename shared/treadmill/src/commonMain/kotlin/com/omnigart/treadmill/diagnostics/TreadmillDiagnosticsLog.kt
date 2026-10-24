package com.omnigart.treadmill.diagnostics

/**
 * Append-only capture of GATT discovery and raw bytes for unknown console variants.
 * Export as UTF-8 text for support.
 */
class TreadmillDiagnosticsLog {
    private val lines = ArrayList<String>()

    fun clear() {
        lines.clear()
    }

    fun info(message: String) {
        lines += "${timestamp()} INFO $message"
    }

    fun service(uuid: String, primary: Boolean = true) {
        lines += "${timestamp()} SERVICE ${if (primary) "primary" else "secondary"} $uuid"
    }

    fun characteristic(serviceUuid: String, charUuid: String, properties: String) {
        lines += "${timestamp()} CHAR svc=$serviceUuid char=$charUuid props=$properties"
    }

    fun rawBytes(source: String, bytes: ByteArray) {
        lines += "${timestamp()} RAW $source ${bytes.toHex()}"
    }

    fun exportText(): String =
        buildString {
            appendLine("OmniGart treadmill diagnostics")
            appendLine("Protocol capture only — not a medical record.")
            appendLine("---")
            lines.forEach { appendLine(it) }
        }

    fun lineCount(): Int = lines.size

    private fun timestamp(): String = "t=${lines.size}"

    private fun ByteArray.toHex(): String =
        joinToString(separator = " ") { b ->
            (b.toInt() and 0xFF).toString(16).padStart(2, '0')
        }
}
