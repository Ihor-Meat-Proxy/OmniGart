package com.omnigart.wear.workout

import android.content.Context
import android.net.Uri
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.omnigart.shared.OmniGartCore
import com.omnigart.shared.health.store.device.DeviceWorkoutTransferDocument
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch

/**
 * Wear → phone Data Layer sender with ack-driven local drop.
 *
 * Puts UTF-8 JSON under `/omnigart/workout/transfer/<syncId>`.
 * On [DeviceWorkoutTransferDocument.ACK_PATH] message, soft-deletes the Wear copy.
 */
class WearPhoneWorkoutTransfer(
    context: Context,
    private val core: OmniGartCore,
    private val dataClient: DataClient = Wearable.getDataClient(context.applicationContext),
    private val messageClient: MessageClient = Wearable.getMessageClient(context.applicationContext)
) : MessageClient.OnMessageReceivedListener {
    private val io = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val scope = CoroutineScope(SupervisorJob() + io)
    private val pendingLocalIds = ConcurrentHashMap<String, String>()

    fun start() {
        messageClient.addListener(this)
    }

    fun stop() {
        messageClient.removeListener(this)
    }

    /**
     * Remember pending ack and put a Data Layer item. Retries are safe: phone ingest is
     * idempotent by sync id.
     */
    fun enqueue(localId: String, syncIdentifier: String, jsonPayload: String) {
        pendingLocalIds[syncIdentifier] = localId
        scope.launch {
            val request =
                PutDataMapRequest.create(
                    DeviceWorkoutTransferDocument.DATA_PATH_PREFIX + syncIdentifier
                ).apply {
                    dataMap.putString(DeviceWorkoutTransferDocument.PAYLOAD_KEY, jsonPayload)
                    dataMap.putLong("updatedAtMs", System.currentTimeMillis())
                }.asPutDataRequest()
                    .setUrgent()
            Tasks.await(dataClient.putDataItem(request))
        }
    }

    override fun onMessageReceived(event: MessageEvent) {
        if (event.path != DeviceWorkoutTransferDocument.ACK_PATH) return
        val syncId = event.data.toString(StandardCharsets.UTF_8)
        val localId = pendingLocalIds.remove(syncId) ?: core.localHealth.getBySyncIdentifier(syncId)
        if (localId != null) {
            core.localHealth.softDeleteWorkout(localId)
        }
        scope.launch {
            runCatching {
                Tasks.await(
                    dataClient.deleteDataItems(
                        Uri.parse(
                            "wear://*" + DeviceWorkoutTransferDocument.DATA_PATH_PREFIX + syncId
                        )
                    )
                )
            }
        }
    }

    fun pendingCount(): Int = pendingLocalIds.size

    companion object {
        /** Pure ack handling for unit tests (no Wearable SDK). */
        fun applyAck(syncIdentifier: String, pending: MutableMap<String, String>, softDelete: (String) -> Unit) {
            val localId = pending.remove(syncIdentifier) ?: return
            softDelete(localId)
        }
    }
}
