package com.omnigart.body

import com.omnigart.body.models.ProgressPhotoMeta
import com.omnigart.body.trends.Ewma
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class EwmaAndPhotosTest {

    @Test
    fun ewmaSmoothsSeries() {
        val points =
            listOf(
                Ewma.Point(1, 80.0),
                Ewma.Point(2, 82.0),
                Ewma.Point(3, 81.0),
            )
        val smoothed = Ewma.smooth(points, alpha = 0.5)
        assertEquals(3, smoothed.size)
        assertEquals(80.0, smoothed[0].smoothed)
        assertEquals(0.5 * 82.0 + 0.5 * 80.0, smoothed[1].smoothed, absoluteTolerance = 1e-9)
        assertTrue(smoothed[2].smoothed in 80.5..82.0)
    }

    @Test
    fun ewmaNextStreams() {
        val a = Ewma.next(null, 70.0)
        assertEquals(70.0, a)
        val b = Ewma.next(a, 80.0, alpha = 0.25)
        assertEquals(0.25 * 80.0 + 0.75 * 70.0, b, absoluteTolerance = 1e-9)
    }

    @Test
    fun progressPhotosRequireLocalOnly() {
        val ok =
            ProgressPhotoMeta(
                id = "p1",
                capturedAtEpochMs = 1L,
                localUri = "file:///private/photos/p1.jpg",
            )
        assertTrue(ok.localOnly)
        assertFailsWith<IllegalArgumentException> {
            ProgressPhotoMeta(
                id = "p2",
                capturedAtEpochMs = 1L,
                localUri = "file:///x",
                localOnly = false,
            )
        }
    }
}
