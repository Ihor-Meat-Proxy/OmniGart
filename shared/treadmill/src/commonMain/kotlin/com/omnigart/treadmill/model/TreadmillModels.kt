package com.omnigart.treadmill.model

import kotlin.math.round

/** Which wire protocol produced the latest sample. */
enum class TreadmillProtocol {
    FTMS,
    RSC,
    ICONSOLE,
    NONE
}

/** Remembered treadmill for auto-reconnect. */
data class RememberedTreadmill(
    val id: String,
    val name: String?,
    val protocol: TreadmillProtocol,
    val lastConnectedEpochMs: Long
)

/**
 * Live metrics shown during a Treadmill run.
 *
 * Heart rate preference: [watchHeartRateBpm] first, then [machineHeartRateBpm].
 */
data class TreadmillLiveMetrics(
    val protocol: TreadmillProtocol = TreadmillProtocol.NONE,
    val connected: Boolean = false,
    val speedKmh: Double? = null,
    val paceMinPerKm: Double? = null,
    val inclinePercent: Double? = null,
    val distanceMetres: Double? = null,
    val elapsedSeconds: Int? = null,
    val watchHeartRateBpm: Int? = null,
    val machineHeartRateBpm: Int? = null,
    val energyKcal: Double? = null,
    val steps: Long? = null,
    val cadenceSpm: Int? = null,
    val powerWatts: Double? = null,
    val heartRateZone: Int? = null
) {
    /** Display HR: watch optical preferred, machine second. */
    val displayHeartRateBpm: Int?
        get() = watchHeartRateBpm ?: machineHeartRateBpm

    companion object {
        fun paceFromSpeedKmh(speedKmh: Double?): Double? {
            if (speedKmh == null || speedKmh <= 0.0) return null
            return round((60.0 / speedKmh) * 100.0) / 100.0
        }
    }
}

/** User opt-in for remote speed / incline writes. Default off. */
data class TreadmillControlPreferences(
    val allowRemoteControl: Boolean = false
)

/**
 * Snapshot persisted with the local workout (also feeds GPX/TCX extensions).
 */
data class TreadmillWorkoutSummary(
    val protocol: TreadmillProtocol,
    val deviceName: String?,
    val distanceMetres: Double?,
    val averageSpeedKmh: Double?,
    val averageInclinePercent: Double?,
    val machineAverageHeartRateBpm: Double?,
    val energyKcal: Double?,
    val steps: Long?,
    val usedWatchEstimatedDistance: Boolean,
    val calibrationFactor: Double? = null
)

/** Optional post-run scale when distance came from the watch only. */
data class DistanceCalibration(
    val watchEstimatedMetres: Double,
    val consoleReportedMetres: Double
) {
    init {
        require(watchEstimatedMetres > 0.0)
        require(consoleReportedMetres > 0.0)
    }

    val factor: Double get() = consoleReportedMetres / watchEstimatedMetres

    fun apply(estimatedMetres: Double): Double = estimatedMetres * factor
}
