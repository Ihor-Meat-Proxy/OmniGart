package com.omnigart.wear.workout

import com.omnigart.shared.health.store.device.PhoneWorkoutKindIds
import com.omnigart.shared.health.store.device.PhoneWorkoutSelection
import com.omnigart.shared.health.store.device.PhoneWorkoutSelectionCodec
import kotlin.test.Test
import kotlin.test.assertEquals

class WearPhoneWorkoutLaunchListenerTest {
    @Test
    fun wearMode_mapsStrengthKinds() {
        val guided =
            PhoneWorkoutSelection(kind = PhoneWorkoutKindIds.GUIDED_STRENGTH, watchIsRecorder = true)
        assertEquals(WearWorkoutMode.STRENGTH, WearPhoneWorkoutLaunchListener.wearModeFor(guided))
        val run = PhoneWorkoutSelection(kind = PhoneWorkoutKindIds.ZONE2_RUN, watchIsRecorder = true)
        assertEquals(WearWorkoutMode.RUNNING, WearPhoneWorkoutLaunchListener.wearModeFor(run))
    }

    @Test
    fun selection_codec_roundTrip_forLaunchPath() {
        val original =
            PhoneWorkoutSelection(
                kind = PhoneWorkoutKindIds.TREADMILL_WALK,
                routineId = "walk-easy",
                equipmentIds = listOf("treadmill"),
                sessionKey = "wear-1",
                watchIsRecorder = true
            )
        val decoded = PhoneWorkoutSelectionCodec.decode(PhoneWorkoutSelectionCodec.encode(original))
        assertEquals(original, decoded)
        assertEquals("/omnigart/workout/launch", PhoneWorkoutSelection.LAUNCH_PATH)
    }
}
