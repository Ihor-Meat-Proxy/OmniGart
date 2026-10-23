package com.omnigart.sensors.treadmill

/**
 * Plug-in contract for treadmill BLE protocol parsers.
 *
 * [TreadmillProtocolDetector] tries FTMS first, then FitShow-style (FFF0), then
 * iConsole UART. New protocols register as additional [TreadmillProtocolPlugin]s.
 */
interface TreadmillProtocolPlugin {
    val kind: TreadmillProtocolKind
    val priority: Int

    /** True when advertised or discovered service UUIDs match this protocol. */
    fun matches(serviceUuids: Collection<String>): Boolean

    fun parseNotification(characteristicUuid: String, payload: ByteArray): TreadmillSample?
}

object TreadmillProtocolRegistry {
    private val plugins: MutableList<TreadmillProtocolPlugin> =
        mutableListOf(
            FtmsProtocolPlugin,
            FitShowProtocolPlugin,
            IConsoleProtocolPlugin
        )

    fun all(): List<TreadmillProtocolPlugin> = plugins.sortedBy { it.priority }

    fun register(plugin: TreadmillProtocolPlugin) {
        plugins.removeAll { it.kind == plugin.kind }
        plugins += plugin
    }

    fun detect(serviceUuids: Collection<String>): TreadmillProtocolKind {
        val normalised = serviceUuids.map { it.lowercase() }
        return all().firstOrNull { it.matches(normalised) }?.kind
            ?: TreadmillProtocolKind.Unknown
    }

    fun plugin(kind: TreadmillProtocolKind): TreadmillProtocolPlugin? = all().firstOrNull { it.kind == kind }
}

object TreadmillProtocolDetector {
    /**
     * Prefer FTMS (0x1826). Fall back to FitShow FFF0, then Microchip UART used by
     * some iConsole consoles. Marketing names alone are never enough.
     */
    fun detect(serviceUuids: Collection<String>): TreadmillProtocolKind = TreadmillProtocolRegistry.detect(serviceUuids)

    fun parse(kind: TreadmillProtocolKind, characteristicUuid: String, payload: ByteArray): TreadmillSample? =
        TreadmillProtocolRegistry.plugin(kind)
            ?.parseNotification(characteristicUuid.lowercase(), payload)
}

internal fun String.asBleUuidLower(): String = lowercase()

internal fun Collection<String>.containsUuid(shortOrFull: String): Boolean {
    val target = shortOrFull.lowercase().replace("-", "")
    return any { candidate ->
        val u = candidate.lowercase().replace("-", "")
        u == target ||
            u.contains(target) ||
            (target.length == 4 && u.contains(target))
    }
}
