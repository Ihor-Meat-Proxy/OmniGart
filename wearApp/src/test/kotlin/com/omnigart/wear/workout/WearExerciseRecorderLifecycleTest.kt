package com.omnigart.wear.workout

import com.omnigart.wear.MainActivity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w227dp-h227dp-small-notlong-round-watch")
class WearExerciseRecorderLifecycleTest {
    @Test
    fun activityDestroyCancelsRecorderScope() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val recorder = controller.get().recorderForTest()
        assertTrue(recorder.isLifecycleActive())
        controller.destroy()
        assertFalse(recorder.isLifecycleActive())
    }
}
