package com.omnigart.sensors.ble

/**
 * Platform BLE central for the standard Heart Rate Service (0x180D).
 *
 * Covers scan/pair, measurement notifications, body sensor location, battery,
 * reconnection, and platform background modes (implemented in adapters).
 */
interface BleHeartRateClient {
    /** Scan for peripherals advertising the Heart Rate Service. */
    fun startScan(onFound: (BleHrPeripheral) -> Unit)

    fun stopScan()

    suspend fun connect(peripheralId: String)

    suspend fun disconnect(peripheralId: String)

    /** Enable 0x2A37 notifications; parsed measurements delivered to [onMeasurement]. */
    fun startNotifications(
        peripheralId: String,
        onMeasurement: (HeartRateMeasurement) -> Unit,
    )

    fun stopNotifications(peripheralId: String)

    suspend fun readBodySensorLocation(peripheralId: String): BodySensorLocation?

    suspend fun readBatteryLevel(peripheralId: String): BatteryLevel?

    /**
     * Ask the platform to reconnect when the peripheral is next seen.
     * iOS: CBCentralManager connect with options / state restoration.
     * Android: autoConnect=true or Companion Device Manager.
     */
    fun enableAutoReconnect(peripheralId: String, enabled: Boolean)
}

data class BleHrPeripheral(
    val id: String,
    val name: String?,
    val rssi: Int?,
    val isPolar: Boolean = false,
)

enum class BleConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    DISCONNECTING,
}
