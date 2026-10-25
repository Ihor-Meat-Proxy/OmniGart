package com.omnigart.android.health

import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import com.omnigart.shared.OmniGartCore
import com.omnigart.shared.content.AssetContentStore
import com.omnigart.shared.data.DatabaseDriverFactory
import com.omnigart.shared.health.HealthWorkout
import com.omnigart.shared.health.HeartRateSample
import com.omnigart.shared.health.SdkHealthConnectGateway
import com.omnigart.shared.health.WorkoutActivityType
import com.omnigart.shared.health.store.device.DeviceTransferIngestResult
import com.omnigart.shared.health.store.device.DeviceWorkoutTransferCodec
import com.omnigart.shared.health.store.device.DeviceWorkoutTransferDocument
import com.omnigart.shared.health.store.device.shouldAckWearAfterIngest
import java.nio.charset.StandardCharsets
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Phone-side Wear Data Layer inbox.
 *
 * Flow: decode transfer → local store (idempotent by sync id) → append-only Health Connect
 * `insertRecords` once (dedupe by clientRecordId) → ack Wear so it can drop its copy.
 */
class WearWorkoutInboxService : WearableListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        val events = dataEvents.map { it }
        dataEvents.release()
        scope.launch {
            val core =
                OmniGartCore(
                    DatabaseDriverFactory(applicationContext),
                    contentStore = AssetContentStore(applicationContext.assets)
                )
            events
                .filter { it.type == DataEvent.TYPE_CHANGED }
                .mapNotNull { event ->
                    val path = event.dataItem.uri.path ?: return@mapNotNull null
                    if (!path.startsWith(DeviceWorkoutTransferDocument.DATA_PATH_PREFIX)) {
                        return@mapNotNull null
                    }
                    DataMapItem.fromDataItem(event.dataItem)
                        .dataMap
                        .getString(DeviceWorkoutTransferDocument.PAYLOAD_KEY)
                }.forEach { json -> handlePayload(core, json) }
        }
    }

    private suspend fun handlePayload(core: OmniGartCore, json: String) {
        val resultName = core.localHealth.ingestDeviceTransferJson(json)
        if (resultName == DeviceTransferIngestResult.REJECTED.name) return
        val doc = runCatching { DeviceWorkoutTransferCodec.decode(json) }.getOrNull() ?: return
        writeHealthConnectOnce(core, doc)
        sendAck(doc.syncIdentifier)
    }

    private suspend fun writeHealthConnectOnce(core: OmniGartCore, doc: DeviceWorkoutTransferDocument) {
        val localId = core.localHealth.getBySyncIdentifier(doc.syncIdentifier) ?: doc.localId
        val start = Instant.fromEpochMilliseconds(doc.startEpochMs)
        val end = Instant.fromEpochMilliseconds(doc.endEpochMs)
        val gateway = SdkHealthConnectGateway(applicationContext)
        // Append-only: exist-check clientRecordId from EPOCH, then insertRecords at most once.
        if (gateway.clientRecordIdExists(doc.syncIdentifier)) {
            core.localHealth.markPlatformLinked(localId, doc.syncIdentifier)
            return
        }
        val activity =
            runCatching { WorkoutActivityType.valueOf(doc.activityTypeName) }
                .getOrDefault(WorkoutActivityType.OTHER)
        val hr =
            doc.heartRateEpochMs.indices.map { i ->
                HeartRateSample(
                    beatsPerMinute = doc.heartRateBpm[i],
                    recordedAt = Instant.fromEpochMilliseconds(doc.heartRateEpochMs[i])
                )
            }
        val hw =
            HealthWorkout(
                id = doc.localId,
                activityType = activity,
                start = start,
                end = end,
                energyKcal = doc.activeEnergyKcal,
                basalEnergyKcal = doc.basalEnergyKcal,
                distanceMetres = doc.distanceMetres,
                averageHeartRateBpm = doc.averageHeartRateBpm,
                heartRateSamples = hr,
                sourceName = doc.sourceName,
                clientRecordVersion = SdkHealthConnectGateway.HEALTH_CONNECT_CLIENT_RECORD_VERSION
            )
        gateway.writeWorkouts(listOf(hw))
        core.localHealth.markPlatformLinked(localId, doc.syncIdentifier)
    }

    private fun sendAck(syncIdentifier: String) {
        val messageClient: MessageClient = Wearable.getMessageClient(this)
        val nodes = Tasks.await(Wearable.getNodeClient(this).connectedNodes)
        val payload = syncIdentifier.toByteArray(StandardCharsets.UTF_8)
        for (node in nodes) {
            Tasks.await(
                messageClient.sendMessage(node.id, DeviceWorkoutTransferDocument.ACK_PATH, payload)
            )
        }
    }

    companion object {
        /** Test helper: ingest outcomes that should ack Wear. */
        fun shouldAckAfterIngest(resultName: String): Boolean = shouldAckWearAfterIngest(resultName)
    }
}
