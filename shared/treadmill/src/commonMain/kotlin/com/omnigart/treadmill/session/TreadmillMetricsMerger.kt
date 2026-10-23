package com.omnigart.treadmill.session

import com.omnigart.treadmill.fitshow.FitShowStatusParser
import com.omnigart.treadmill.ftms.FtmsTreadmillDataParser
import com.omnigart.treadmill.iconsole.IConsoleProtocol
import com.omnigart.treadmill.model.TreadmillLiveMetrics
import com.omnigart.treadmill.model.TreadmillProtocol
import com.omnigart.treadmill.rsc.RscMeasurementParser
import kotlin.math.roundToInt

/**
 * Merges FTMS / RSC / iConsole samples with watch HR into live UI metrics.
 * Heart-rate zone uses simple 5-zone model from % of estimated max HR.
 */
object TreadmillMetricsMerger {

    fun heartRateZone(bpm: Int?, hrMax: Int): Int? {
        if (bpm == null || hrMax <= 0) return null
        val pct = bpm.toDouble() / hrMax.toDouble()
        return when {
            pct < 0.60 -> 1
            pct < 0.70 -> 2
            pct < 0.80 -> 3
            pct < 0.90 -> 4
            else -> 5
        }
    }

    fun fromFtms(
        data: FtmsTreadmillDataParser.TreadmillData,
        watchHr: Int?,
        hrMax: Int
    ): TreadmillLiveMetrics {
        val speed = data.instantaneousSpeedKmh
        val displayHr = watchHr ?: data.heartRateBpm
        return TreadmillLiveMetrics(
            protocol = TreadmillProtocol.FTMS,
            connected = true,
            speedKmh = speed,
            paceMinPerKm = data.instantaneousPaceMinPerKm
                ?: TreadmillLiveMetrics.paceFromSpeedKmh(speed),
            inclinePercent = data.inclinationPercent,
            distanceMetres = data.totalDistanceMetres?.toDouble(),
            elapsedSeconds = data.elapsedTimeSeconds,
            watchHeartRateBpm = watchHr,
            machineHeartRateBpm = data.heartRateBpm,
            energyKcal = data.totalEnergyKcal?.toDouble(),
            steps = data.steps,
            powerWatts = data.powerOutputWatts?.toDouble(),
            heartRateZone = heartRateZone(displayHr, hrMax)
        )
    }

    fun fromRsc(
        data: RscMeasurementParser.RscMeasurement,
        watchHr: Int?,
        hrMax: Int,
        inclinePercent: Double? = null
    ): TreadmillLiveMetrics {
        val speed = data.instantaneousSpeedKmh
        return TreadmillLiveMetrics(
            protocol = TreadmillProtocol.RSC,
            connected = true,
            speedKmh = round1(speed),
            paceMinPerKm = TreadmillLiveMetrics.paceFromSpeedKmh(speed),
            inclinePercent = inclinePercent,
            distanceMetres = data.totalDistanceMetres,
            watchHeartRateBpm = watchHr,
            machineHeartRateBpm = null,
            cadenceSpm = data.instantaneousCadenceSpm,
            heartRateZone = heartRateZone(watchHr, hrMax)
        )
    }

    fun fromIConsole(
        data: IConsoleProtocol.Telemetry,
        watchHr: Int?,
        hrMax: Int
    ): TreadmillLiveMetrics {
        val displayHr = watchHr ?: data.heartRateBpm.takeIf { it > 0 }
        return TreadmillLiveMetrics(
            protocol = TreadmillProtocol.ICONSOLE,
            connected = true,
            speedKmh = data.speedKmh,
            paceMinPerKm = TreadmillLiveMetrics.paceFromSpeedKmh(data.speedKmh),
            inclinePercent = null,
            distanceMetres = data.distanceMetres,
            elapsedSeconds = data.elapsedSeconds,
            watchHeartRateBpm = watchHr,
            machineHeartRateBpm = data.heartRateBpm.takeIf { it > 0 },
            energyKcal = data.energyKcal.toDouble(),
            cadenceSpm = data.cadenceRpm,
            powerWatts = data.powerWatts,
            heartRateZone = heartRateZone(displayHr, hrMax)
        )
    }

    fun fromFitShow(
        data: FitShowStatusParser.Status,
        watchHr: Int?,
        hrMax: Int
    ): TreadmillLiveMetrics {
        val displayHr = watchHr ?: data.heartRateBpm
        return TreadmillLiveMetrics(
            protocol = TreadmillProtocol.FITSHOW,
            connected = true,
            speedKmh = data.speedKmh,
            paceMinPerKm = TreadmillLiveMetrics.paceFromSpeedKmh(data.speedKmh),
            inclinePercent = data.inclinePercent,
            distanceMetres = data.distanceMetres,
            elapsedSeconds = data.elapsedSeconds,
            watchHeartRateBpm = watchHr,
            machineHeartRateBpm = data.heartRateBpm,
            energyKcal = data.energyKcal,
            steps = data.steps?.toLong(),
            heartRateZone = heartRateZone(displayHr, hrMax)
        )
    }

    fun disconnected(watchHr: Int?, hrMax: Int, estimatedDistanceMetres: Double?): TreadmillLiveMetrics =
        TreadmillLiveMetrics(
            protocol = TreadmillProtocol.NONE,
            connected = false,
            distanceMetres = estimatedDistanceMetres,
            watchHeartRateBpm = watchHr,
            heartRateZone = heartRateZone(watchHr, hrMax)
        )

    private fun round1(v: Double): Double = (v * 10.0).roundToInt() / 10.0
}
