package com.omnigart.treadmill.sim

import com.omnigart.treadmill.ftms.FtmsControlPoint
import com.omnigart.treadmill.ftms.FtmsTreadmillDataParser
import com.omnigart.treadmill.model.TreadmillProtocol
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SimulatedTreadmillTest {

    @Test
    fun controlPointRequiresArmBeforeStart() {
        val sim = SimulatedTreadmill()
        val denied = sim.handleControlPoint(FtmsControlPoint.startOrResume())
        assertEquals(
            FtmsControlPoint.ResultCode.CONTROL_NOT_PERMITTED.code,
            denied[2].toInt() and 0xFF
        )
        assertFalse(sim.running)

        val grant = sim.handleControlPoint(FtmsControlPoint.requestControl())
        assertEquals(FtmsControlPoint.ResultCode.SUCCESS.code, grant[2].toInt() and 0xFF)
        val start = sim.handleControlPoint(FtmsControlPoint.startOrResume())
        assertEquals(FtmsControlPoint.ResultCode.SUCCESS.code, start[2].toInt() and 0xFF)
        assertTrue(sim.running)
    }

    @Test
    fun tickAdvancesDistanceAndRoundTripsFtmsParser() {
        val sim = SimulatedTreadmill()
        sim.handleControlPoint(FtmsControlPoint.requestControl())
        sim.handleControlPoint(FtmsControlPoint.startOrResume())
        sim.handleControlPoint(FtmsControlPoint.setTargetSpeedKmh(10.0))

        val bytes = sim.tick()
        val parsed = FtmsTreadmillDataParser.parse(bytes).getOrThrow()
        assertEquals(10.0, parsed.instantaneousSpeedKmh)
        assertTrue((parsed.totalDistanceMetres ?: 0L) > 0L)
        assertTrue((parsed.elapsedTimeSeconds ?: 0) >= 1)

        val metrics = sim.parseOwnNotification(bytes)
        assertEquals(TreadmillProtocol.SIMULATED, metrics.protocol)
        assertTrue(metrics.connected)
        assertEquals(10.0, metrics.speedKmh)
    }

    @Test
    fun applyProgramSetsTargetsFromCatalog() {
        val sim = SimulatedTreadmill()
        assertTrue(sim.applyProgram("easy_run"))
        assertEquals(7.0, sim.targetSpeedKmh)
        assertEquals(0.0, sim.targetInclinePercent)
    }

    @Test
    fun stopClearsRunningFlag() {
        val sim = SimulatedTreadmill()
        sim.handleControlPoint(FtmsControlPoint.requestControl())
        sim.handleControlPoint(FtmsControlPoint.startOrResume())
        sim.handleControlPoint(FtmsControlPoint.stopOrPause(FtmsControlPoint.StopPause.STOP))
        assertFalse(sim.running)
        val idle = sim.tick()
        val parsed = FtmsTreadmillDataParser.parse(idle).getOrThrow()
        assertEquals(0.0, parsed.instantaneousSpeedKmh)
    }
}
