package com.omnigart.treadmill.ftms

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FtmsControlPointTest {

    @Test
    fun requestControl() {
        assertEquals(listOf(0x00), FtmsControlPoint.requestControl().map { it.toInt() and 0xFF })
    }

    @Test
    fun setTargetSpeedTwoKmh() {
        // 2.00 km/h → 200 = 0x00C8 LE → 02 C8 00
        val cmd = FtmsControlPoint.setTargetSpeedKmh(2.0)
        assertEquals(listOf(0x02, 0xC8, 0x00), cmd.map { it.toInt() and 0xFF })
    }

    @Test
    fun setTargetInclineTwoPercent() {
        // 2.0% → 20 = 0x0014 → 03 14 00
        val cmd = FtmsControlPoint.setTargetInclinePercent(2.0)
        assertEquals(listOf(0x03, 0x14, 0x00), cmd.map { it.toInt() and 0xFF })
    }

    @Test
    fun setTargetInclineNegative() {
        val cmd = FtmsControlPoint.setTargetInclinePercent(-1.5)
        // -15 = 0xFFF1
        assertEquals(listOf(0x03, 0xF1, 0xFF), cmd.map { it.toInt() and 0xFF })
    }

    @Test
    fun stopPause() {
        assertEquals(
            listOf(0x08, 0x01),
            FtmsControlPoint.stopOrPause(FtmsControlPoint.StopPause.STOP).map { it.toInt() and 0xFF }
        )
        assertEquals(
            listOf(0x08, 0x02),
            FtmsControlPoint.stopOrPause(FtmsControlPoint.StopPause.PAUSE).map { it.toInt() and 0xFF }
        )
    }

    @Test
    fun parseSuccessResponse() {
        val r = FtmsControlPoint.parseResponse(byteArrayOf(0x80.toByte(), 0x02, 0x01)).getOrThrow()
        assertEquals(0x02, r.requestOpCode)
        assertEquals(FtmsControlPoint.ResultCode.SUCCESS, r.result)
    }

    @Test
    fun parseShortFails() {
        assertTrue(FtmsControlPoint.parseResponse(byteArrayOf(0x80.toByte())).isFailure)
    }
}
