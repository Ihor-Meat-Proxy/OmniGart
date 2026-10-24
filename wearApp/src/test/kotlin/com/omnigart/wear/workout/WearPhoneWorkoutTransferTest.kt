package com.omnigart.wear.workout

import com.omnigart.shared.health.store.HealthDataMapper
import com.omnigart.shared.health.store.HealthSyncEngine
import com.omnigart.shared.health.store.InMemoryLocalHealthStore
import com.omnigart.shared.health.store.LocalHealthBridge
import com.omnigart.shared.health.store.device.DeviceTransferIngestResult
import com.omnigart.shared.health.store.device.DeviceWorkoutTransferCodec
import com.omnigart.shared.health.store.device.DeviceWorkoutTransferDocument
import com.omnigart.shared.health.store.device.shouldAckWearAfterIngest
import com.omnigart.shared.health.store.export.HealthExportService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Wear → phone transfer contract without Wearable / Health Services hardware.
 */
class WearPhoneWorkoutTransferTest {
    @Test
    fun ackDropsPendingLocalCopy() {
        val pending = mutableMapOf("omnigart.workout.w1" to "w1")
        val deleted = mutableListOf<String>()
        WearPhoneWorkoutTransfer.applyAck("omnigart.workout.w1", pending) { deleted += it }
        assertTrue(pending.isEmpty())
        assertEquals(listOf("w1"), deleted)
    }

    @Test
    fun phoneIngestThenAck_isIdempotent() {
        val store = InMemoryLocalHealthStore()
        val engine = HealthSyncEngine(store)
        val bridge = LocalHealthBridge(store, engine, HealthExportService(store))
        val start = 1_700_000_000_000L
        val doc =
            DeviceWorkoutTransferDocument(
                localId = "wear-test-1",
                syncIdentifier = HealthDataMapper.syncIdentifierFor("wear-test-1"),
                activityTypeName = "RUNNING",
                startEpochMs = start,
                endEpochMs = start + 600_000,
                activeEnergyKcal = 200.0,
                heartRateEpochMs = listOf(start + 1_000),
                heartRateBpm = listOf(130.0),
                sourceDevice = DeviceWorkoutTransferDocument.SOURCE_WEAR_OS
            )
        val json = DeviceWorkoutTransferCodec.encode(doc)
        assertEquals(DeviceTransferIngestResult.INSERTED.name, bridge.ingestDeviceTransferJson(json))
        assertTrue(shouldAckWearAfterIngest(DeviceTransferIngestResult.INSERTED.name))
        assertEquals(
            DeviceTransferIngestResult.ALREADY_PRESENT.name,
            bridge.ingestDeviceTransferJson(json)
        )
        assertTrue(shouldAckWearAfterIngest(DeviceTransferIngestResult.ALREADY_PRESENT.name))
        assertFalse(shouldAckWearAfterIngest(DeviceTransferIngestResult.REJECTED.name))
    }
}
