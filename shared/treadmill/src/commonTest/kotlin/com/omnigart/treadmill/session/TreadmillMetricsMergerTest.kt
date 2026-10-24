package com.omnigart.treadmill.session

import com.omnigart.treadmill.ftms.FtmsTreadmillDataParser
import com.omnigart.treadmill.model.DistanceCalibration
import com.omnigart.treadmill.model.TreadmillLiveMetrics
import com.omnigart.treadmill.model.TreadmillProtocol
import com.omnigart.treadmill.rsc.RscMeasurementParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TreadmillMetricsMergerTest {

    @Test
    fun prefersWatchHeartRate() {
        val ftms =
            FtmsTreadmillDataParser.TreadmillData(
                flags = 0,
                instantaneousSpeedKmh = 10.0,
                heartRateBpm = 150
            )
        val m = TreadmillMetricsMerger.fromFtms(ftms, watchHr = 140, hrMax = 190)
        assertEquals(140, m.displayHeartRateBpm)
        assertEquals(140, m.watchHeartRateBpm)
        assertEquals(150, m.machineHeartRateBpm)
        assertEquals(TreadmillProtocol.FTMS, m.protocol)
    }

    @Test
    fun fallsBackToMachineHeartRate() {
        val ftms =
            FtmsTreadmillDataParser.TreadmillData(
                flags = 0,
                instantaneousSpeedKmh = 9.0,
                heartRateBpm = 155
            )
        val m = TreadmillMetricsMerger.fromFtms(ftms, watchHr = null, hrMax = 190)
        assertEquals(155, m.displayHeartRateBpm)
    }

    @Test
    fun paceFromSpeed() {
        assertEquals(6.0, TreadmillLiveMetrics.paceFromSpeedKmh(10.0)!!, 1e-9)
        assertNull(TreadmillLiveMetrics.paceFromSpeedKmh(0.0))
    }

    @Test
    fun zones() {
        assertEquals(2, TreadmillMetricsMerger.heartRateZone(120, 190))
        assertEquals(5, TreadmillMetricsMerger.heartRateZone(180, 190))
    }

    @Test
    fun rscMerge() {
        val rsc =
            RscMeasurementParser.RscMeasurement(
                flags = 0,
                instantaneousSpeedMps = 3.0,
                instantaneousCadenceSpm = 170,
                isRunning = true
            )
        val m = TreadmillMetricsMerger.fromRsc(rsc, watchHr = 130, hrMax = 185)
        assertEquals(TreadmillProtocol.RSC, m.protocol)
        assertEquals(10.8, m.speedKmh!!, 0.05)
    }

    @Test
    fun distanceCalibration() {
        val cal = DistanceCalibration(watchEstimatedMetres = 1000.0, consoleReportedMetres = 1100.0)
        assertEquals(1.1, cal.factor, 1e-9)
        assertEquals(2200.0, cal.apply(2000.0), 1e-9)
    }
}
