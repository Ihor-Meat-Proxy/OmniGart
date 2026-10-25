package com.omnigart.sensors.model

/**
 * Expected BLE heart-rate failures (permissions, adapter, scan, GATT).
 *
 * These are not programming errors — UI should show [message] and keep
 * running. Prefer returning [BleHrResult.Failure] over throwing.
 */
sealed class BleHrFailure {
    abstract val message: String

    data class MissingPermissions(
        val permissions: List<String>,
    ) : BleHrFailure() {
        override val message: String
            get() = "Missing BLE permissions: ${permissions.joinToString()}"
    }

    data object AdapterDisabled : BleHrFailure() {
        override val message: String get() = "Bluetooth adapter disabled"
    }

    data object ScannerUnavailable : BleHrFailure() {
        override val message: String get() = "Bluetooth LE scanner unavailable"
    }

    data class DeviceNotFound(
        val deviceId: String,
    ) : BleHrFailure() {
        override val message: String get() = "Unknown device $deviceId"
    }

    data class ScanFailed(
        val errorCode: Int,
    ) : BleHrFailure() {
        override val message: String get() = "Scan failed: $errorCode"
    }

    data class GattError(
        val status: Int,
        val operation: String,
    ) : BleHrFailure() {
        override val message: String get() = "GATT $operation failed: status=$status"
    }

    data object ReconnectExhausted : BleHrFailure() {
        override val message: String get() = "Reconnect attempts exhausted"
    }
}

/** Outcome of scan / connect that must not crash the sensors UI. */
sealed class BleHrResult {
    data object Success : BleHrResult()

    data class Failure(
        val failure: BleHrFailure,
    ) : BleHrResult()

    val isSuccess: Boolean get() = this is Success

    fun failureOrNull(): BleHrFailure? = (this as? Failure)?.failure

    companion object {
        fun success(): BleHrResult = Success

        fun failure(failure: BleHrFailure): BleHrResult = Failure(failure)
    }
}
