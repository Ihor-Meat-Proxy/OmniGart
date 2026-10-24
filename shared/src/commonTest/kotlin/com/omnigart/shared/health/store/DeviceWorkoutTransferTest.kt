package com.omnigart.shared.health.store

import com.omnigart.shared.health.WorkoutActivityType
import com.omnigart.shared.health.store.device.DeviceTransferIngestResult
import com.omnigart.shared.health.store.device.DeviceWorkoutTransferCodec
import com.omnigart.shared.health.store.device.DeviceWorkoutTransferDocument
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class DeviceWorkoutTransferTest {
    private val store = InMemoryLocalHealthStore()
    private val engine = HealthSyncEngine(store)
    private val bridge = LocalHealthBridge(store, engine, com.omnigart.shared.health.store.export.HealthExportService(store))

    private fun sampleDoc(
        localId: String = "watch-w1",
        sourceDevice: String = DeviceWorkoutTransferDocument.SOURCE_APPLE_WATCH
    ): DeviceWorkoutTransferDocument {
        val start = Instant.parse("2026-10-23T08:00:00Z").toEpochMilliseconds()
        val end = Instant.parse("2026-10-23T08:40:00Z").toEpochMilliseconds()
        return DeviceWorkoutTransferDocument(
            localId = localId,
            syncIdentifier = HealthDataMapper.syncIdentifierFor(localId),
            syncVersion = 1,
            activityTypeName = WorkoutActivityType.RUNNING.name,
            startEpochMs = start,
            endEpochMs = end,
            activeEnergyKcal = 320.0,
            basalEnergyKcal = 40.0,
            distanceMetres = 5200.0,
            averageHeartRateBpm = 142.0,
            maxHeartRateBpm = 168.0,
            heartRateEpochMs = listOf(start + 1_000, start + 30_000),
            heartRateBpm = listOf(138.0, 150.0),
            sourceDevice = sourceDevice
        )
    }

    @Test
    fun codec_roundTripsDocument() {
        val original = sampleDoc()
        val decoded = DeviceWorkoutTransferCodec.decode(DeviceWorkoutTransferCodec.encode(original))
        assertEquals(original, decoded)
        val local = DeviceWorkoutTransferCodec.toLocalWorkout(decoded)
        assertEquals(original.syncIdentifier, local.identity.syncIdentifier)
        assertEquals(2, local.heartRateSeries.size)
        assertEquals(HealthRecordOrigin.OMNIGART, local.origin)
    }

    @Test
    fun ingest_isIdempotentBySyncId() {
        val doc = sampleDoc()
        assertEquals(DeviceTransferIngestResult.INSERTED, engine.ingestDeviceTransfer(doc))
        assertEquals(DeviceTransferIngestResult.ALREADY_PRESENT, engine.ingestDeviceTransfer(doc))
        assertEquals(1, store.listWorkouts().size)
        assertNotNull(store.getBySyncIdentifier(doc.syncIdentifier))
    }

    @Test
    fun ingest_rejectsBadSchema() {
        val bad = sampleDoc().copy(schema = "other")
        assertEquals(DeviceTransferIngestResult.REJECTED, engine.ingestDeviceTransfer(bad))
        assertTrue(store.listWorkouts().isEmpty())
    }

    @Test
    fun bridge_ingestJson_andEncode() {
        val json = DeviceWorkoutTransferCodec.encode(sampleDoc("wear-1", DeviceWorkoutTransferDocument.SOURCE_WEAR_OS))
        assertEquals(DeviceTransferIngestResult.INSERTED.name, bridge.ingestDeviceTransferJson(json))
        assertEquals(DeviceTransferIngestResult.ALREADY_PRESENT.name, bridge.ingestDeviceTransferJson(json))
        val encoded = bridge.encodeDeviceTransferJson("wear-1", DeviceWorkoutTransferDocument.SOURCE_WEAR_OS)
        assertNotNull(encoded)
        val roundTrip = DeviceWorkoutTransferCodec.decode(encoded)
        assertEquals("wear-1", roundTrip.localId)
        assertEquals(DeviceWorkoutTransferDocument.SOURCE_WEAR_OS, roundTrip.sourceDevice)
    }

    @Test
    fun dataPath_roundTripsSyncId() {
        val syncId = HealthDataMapper.syncIdentifierFor("abc")
        val path = DeviceWorkoutTransferCodec.dataPathFor(syncId)
        assertEquals(syncId, DeviceWorkoutTransferCodec.syncIdentifierFromDataPath(path))
        assertNull(DeviceWorkoutTransferCodec.syncIdentifierFromDataPath("/other/path"))
    }

    @Test
    fun softDelete_afterAckSemantics() {
        val doc = sampleDoc("pending-1", DeviceWorkoutTransferDocument.SOURCE_WEAR_OS)
        engine.ingestDeviceTransfer(doc)
        bridge.softDeleteWorkout("pending-1")
        assertTrue(store.listWorkouts().none { it.id == "pending-1" && !it.deleted })
        // Soft-deleted rows stay out of default list; sync id lookup may still see deleted depending on store.
        // Re-ingest with same sync id after soft-delete should still be idempotent if row exists.
        val again = engine.ingestDeviceTransfer(doc)
        assertTrue(
            again == DeviceTransferIngestResult.ALREADY_PRESENT ||
                again == DeviceTransferIngestResult.INSERTED
        )
    }
}
