package com.omnigart.wear.workout

import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.omnigart.shared.health.store.device.PhoneWorkoutKindIds
import com.omnigart.shared.health.store.device.PhoneWorkoutSelection
import com.omnigart.shared.health.store.device.PhoneWorkoutSelectionCodec
import java.nio.charset.StandardCharsets
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicReference

/**
 * Receives phone → Wear launch payloads on [PhoneWorkoutSelection.LAUNCH_PATH] and notifies
 * listeners so [MainActivity] can start the matching Health Services exercise.
 */
class WearPhoneWorkoutLaunchListener(private val messageClient: MessageClient) : MessageClient.OnMessageReceivedListener {
    private val listeners = CopyOnWriteArrayList<(PhoneWorkoutSelection) -> Unit>()
    private val lastSelection = AtomicReference<PhoneWorkoutSelection?>(null)

    fun start() {
        messageClient.addListener(this)
    }

    fun stop() {
        messageClient.removeListener(this)
    }

    fun addListener(listener: (PhoneWorkoutSelection) -> Unit) {
        listeners.add(listener)
        lastSelection.get()?.let(listener)
    }

    fun removeListener(listener: (PhoneWorkoutSelection) -> Unit) {
        listeners.remove(listener)
    }

    override fun onMessageReceived(event: MessageEvent) {
        if (event.path != PhoneWorkoutSelection.LAUNCH_PATH) return
        val json = event.data.toString(StandardCharsets.UTF_8)
        val selection = PhoneWorkoutSelectionCodec.decodeOrNull(json) ?: return
        lastSelection.set(selection)
        listeners.forEach { it(selection) }
    }

    companion object {
        fun wearModeFor(selection: PhoneWorkoutSelection): WearWorkoutMode = when (selection.kind) {
            PhoneWorkoutKindIds.TRADITIONAL_STRENGTH,
            PhoneWorkoutKindIds.FUNCTIONAL_STRENGTH,
            PhoneWorkoutKindIds.GUIDED_STRENGTH -> WearWorkoutMode.STRENGTH
            else -> WearWorkoutMode.RUNNING
        }
    }
}
