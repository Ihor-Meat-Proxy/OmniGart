package com.omnigart.sensors.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import java.util.UUID

/**
 * Standard Heart Rate Service (0x180D) scanner / GATT client with auto-reconnect.
 */
class BleHeartRateScanner(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val adapter: BluetoothAdapter? =
        (appContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter

    var onDeviceFound: ((BluetoothDevice, Int) -> Unit)? = null
    var onMeasurement: ((ParsedHeartRateMeasurement, BluetoothDevice) -> Unit)? = null
    var onConnectionState: ((BluetoothDevice, Int) -> Unit)? = null
    var onBattery: ((Int, BluetoothDevice) -> Unit)? = null

    private var gatt: BluetoothGatt? = null
    private var autoReconnectAddress: String? = null

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            onDeviceFound?.invoke(result.device, result.rssi)
        }
    }

    @SuppressLint("MissingPermission")
    fun startScan() {
        val scanner = adapter?.bluetoothLeScanner ?: return
        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(HRS_SERVICE))
            .build()
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        scanner.startScan(listOf(filter), settings, scanCallback)
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        adapter?.bluetoothLeScanner?.stopScan(scanCallback)
    }

    @SuppressLint("MissingPermission")
    fun connect(device: BluetoothDevice, autoConnect: Boolean = false) {
        gatt?.close()
        gatt = device.connectGatt(appContext, autoConnect, gattCallback, BluetoothDevice.TRANSPORT_LE)
        if (autoConnect) autoReconnectAddress = device.address
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        autoReconnectAddress = null
        gatt?.disconnect()
        gatt?.close()
        gatt = null
    }

    fun enableAutoReconnect(address: String?, enabled: Boolean) {
        autoReconnectAddress = if (enabled) address else null
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            onConnectionState?.invoke(g.device, newState)
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> g.discoverServices()
                BluetoothProfile.STATE_DISCONNECTED -> {
                    if (autoReconnectAddress == g.device.address) {
                        g.connect()
                    }
                }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            val hrs = g.getService(HRS_SERVICE) ?: return
            val measurement = hrs.getCharacteristic(HR_MEASUREMENT) ?: return
            g.setCharacteristicNotification(measurement, true)
            val cccd = measurement.getDescriptor(CCCD)
            if (cccd != null) {
                cccd.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                g.writeDescriptor(cccd)
            }
            hrs.getCharacteristic(BODY_SENSOR_LOCATION)?.let { g.readCharacteristic(it) }
            g.getService(BATTERY_SERVICE)
                ?.getCharacteristic(BATTERY_LEVEL)
                ?.let { g.readCharacteristic(it) }
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(
            g: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
        ) {
            if (characteristic.uuid == HR_MEASUREMENT) {
                val value = characteristic.value ?: return
                onMeasurement?.invoke(HeartRateMeasurementParser.parse(value), g.device)
            }
        }

        override fun onCharacteristicChanged(
            g: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
        ) {
            if (characteristic.uuid == HR_MEASUREMENT) {
                onMeasurement?.invoke(HeartRateMeasurementParser.parse(value), g.device)
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicRead(
            g: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int,
        ) {
            handleRead(g, characteristic, characteristic.value)
        }

        override fun onCharacteristicRead(
            g: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int,
        ) {
            handleRead(g, characteristic, value)
        }

        private fun handleRead(
            g: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray?,
        ) {
            if (value == null || value.isEmpty()) return
            when (characteristic.uuid) {
                BATTERY_LEVEL -> onBattery?.invoke(value[0].toInt() and 0xFF, g.device)
                else -> Unit
            }
        }
    }

    companion object {
        val HRS_SERVICE: UUID = uuid16("180D")
        val HR_MEASUREMENT: UUID = uuid16("2A37")
        val BODY_SENSOR_LOCATION: UUID = uuid16("2A38")
        val BATTERY_SERVICE: UUID = uuid16("180F")
        val BATTERY_LEVEL: UUID = uuid16("2A19")
        val CCCD: UUID = uuid16("2902")

        private fun uuid16(short: String): UUID =
            UUID.fromString("0000${short.lowercase()}-0000-1000-8000-00805f9b34fb")
    }
}
