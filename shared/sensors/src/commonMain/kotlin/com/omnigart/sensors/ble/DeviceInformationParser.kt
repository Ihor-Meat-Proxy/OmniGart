package com.omnigart.sensors.ble

import com.omnigart.sensors.model.DeviceInformation

/**
 * Helpers for Device Information Service (0x180A) string characteristics (UTF-8).
 */
object DeviceInformationParser {
    fun utf8String(bytes: ByteArray): String = bytes.decodeToString().trimEnd('\u0000').trim()

    fun merge(
        existing: DeviceInformation = DeviceInformation(),
        manufacturerName: ByteArray? = null,
        modelNumber: ByteArray? = null,
        serialNumber: ByteArray? = null,
        hardwareRevision: ByteArray? = null,
        firmwareRevision: ByteArray? = null,
        softwareRevision: ByteArray? = null
    ): DeviceInformation = existing.copy(
        manufacturerName = manufacturerName?.let(::utf8String) ?: existing.manufacturerName,
        modelNumber = modelNumber?.let(::utf8String) ?: existing.modelNumber,
        serialNumber = serialNumber?.let(::utf8String) ?: existing.serialNumber,
        hardwareRevision = hardwareRevision?.let(::utf8String) ?: existing.hardwareRevision,
        firmwareRevision = firmwareRevision?.let(::utf8String) ?: existing.firmwareRevision,
        softwareRevision = softwareRevision?.let(::utf8String) ?: existing.softwareRevision
    )
}
