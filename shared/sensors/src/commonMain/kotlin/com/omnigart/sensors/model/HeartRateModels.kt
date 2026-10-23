package com.omnigart.sensors.model

import kotlinx.datetime.Instant

/**
 * Parsed Heart Rate Measurement (0x2A37) payload.
 * Clean type for the health layer (HealthKit / Health Connect writers).
 */
data class HeartRateMeasurement(
    val heartRateBpm: Int,
    val sensorContact: SensorContactStatus,
    val energyExpendedKcal: Int?,
    val rrIntervalsMs: List<Double>,
    val flags: Int,
    val heartRateUint16: Boolean
)

enum class SensorContactStatus {
    NotSupported,
    SupportedNotDetected,
    SupportedDetected
}

/**
 * Time-stamped HR sample suitable for health-store export.
 */
data class HeartRateSample(
    val timestamp: Instant,
    val bpm: Int,
    val sensorContact: SensorContactStatus = SensorContactStatus.NotSupported,
    val sourceDeviceId: String? = null,
    val quality: Double? = null
)

/**
 * Single RR interval in milliseconds (from BLE 1/1024 s units or Polar PPI).
 */
data class RrInterval(val durationMs: Double, val timestamp: Instant? = null)

data class DeviceInformation(
    val manufacturerName: String? = null,
    val modelNumber: String? = null,
    val serialNumber: String? = null,
    val hardwareRevision: String? = null,
    val firmwareRevision: String? = null,
    val softwareRevision: String? = null
)

enum class SensorKind {
    StandardHeartRateProfile,
    PolarH10,
    PolarVeritySense,
    PolarOther,
    Unknown
}

data class SensorDevice(
    val id: String,
    val name: String?,
    val kind: SensorKind = SensorKind.Unknown,
    val rssi: Int? = null,
    val advertisedServiceUuids: List<String> = emptyList()
)

enum class SensorConnectionState {
    Disconnected,
    Scanning,
    Connecting,
    Connected,
    Ready,
    Reconnecting,
    Failed
}

data class SensorSessionState(
    val device: SensorDevice?,
    val connection: SensorConnectionState,
    val batteryPercent: Int? = null,
    val bodySensorLocation: String? = null,
    val deviceInformation: DeviceInformation? = null,
    val lastError: String? = null
)

/**
 * Continuous HR/RR series aligned for a workout window.
 * Health layer consumes [samples] / [rrIntervalsMs] without BLE details.
 */
data class WorkoutHeartRateSeries(
    val workoutId: String,
    val deviceId: String?,
    val source: HeartRateSource,
    val samples: List<HeartRateSample>,
    val rrIntervalsMs: List<Double>,
    val startedAt: Instant,
    val endedAt: Instant
)

enum class HeartRateSource {
    LiveBle,
    PolarOfflineRecording,
    Merged
}

data class AccSample(val timestamp: Instant, val xMg: Int, val yMg: Int, val zMg: Int)

data class EcgSample(val timestamp: Instant, val microVolts: Int)
