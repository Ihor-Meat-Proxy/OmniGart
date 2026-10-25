package com.omnigart.shared.health.store

import com.omnigart.shared.health.WorkoutActivityType
import com.omnigart.shared.health.store.export.HealthExportService
import com.omnigart.shared.health.store.export.OmniGartHealthExportDocument
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.serialization.json.Json

class HealthDataRoundTripTest {
    private val store = InMemoryLocalHealthStore()
    private val export = HealthExportService(store)
    private val engine = HealthSyncEngine(store)

    private fun sampleWorkout(id: String = "w1"): LocalHealthWorkout {
        val start = Instant.parse("2026-10-20T07:00:00Z")
        val end = Instant.parse("2026-10-20T07:45:00Z")
        return LocalHealthWorkout(
            id = id,
            identity = HealthSyncIdentity(HealthDataMapper.syncIdentifierFor(id), 1),
            activityType = WorkoutActivityType.RUNNING,
            startAt = start,
            endAt = end,
            activeEnergyKcal = 412.5,
            basalEnergyKcal = 55.25,
            distanceMetres = 8500.0,
            steps = 9200L,
            averageHeartRateBpm = 148.0,
            maxHeartRateBpm = 172.0,
            averageCadenceSpm = 168.0,
            planId = "plan-zone2-1",
            sessionId = "session-1",
            origin = HealthRecordOrigin.OMNIGART,
            sourceBundle = HealthDataMapper.SOURCE_BUNDLE_OMNIGART,
            sourceName = "OmniGart",
            updatedAt = end,
            heartRateSeries =
            listOf(
                LocalHeartRatePoint(Instant.parse("2026-10-20T07:01:00Z"), 120.0),
                LocalHeartRatePoint(Instant.parse("2026-10-20T07:10:00Z"), 145.0),
                LocalHeartRatePoint(Instant.parse("2026-10-20T07:30:00Z"), 160.0)
            ),
            samples =
            listOf(
                LocalQuantitySample(
                    id = "$id-dist-1",
                    type = HealthSampleType.DISTANCE,
                    recordedAt = start,
                    endAt = end,
                    value = 8500.0,
                    unit = "m"
                )
            ),
            route =
            listOf(
                LocalRoutePoint(
                    id = "$id-r0",
                    recordedAt = start,
                    latitude = 50.4501,
                    longitude = 30.5234,
                    altitudeMetres = 180.0,
                    pointIndex = 0
                ),
                LocalRoutePoint(
                    id = "$id-r1",
                    recordedAt = end,
                    latitude = 50.4600,
                    longitude = 30.5400,
                    altitudeMetres = 185.0,
                    pointIndex = 1
                )
            ),
            events =
            listOf(
                LocalWorkoutEvent(
                    id = "$id-e0",
                    type = HealthEventType.LAP,
                    startAt = Instant.parse("2026-10-20T07:20:00Z"),
                    endAt = Instant.parse("2026-10-20T07:20:00Z"),
                    label = "lap-1",
                    eventIndex = 0
                )
            ),
            sets =
            listOf(
                LocalWorkoutSet(
                    id = "$id-s0",
                    exerciseKey = "run.easy",
                    setIndex = 0,
                    durationSeconds = 2700,
                    distanceMetres = 8500.0
                )
            )
        )
    }

    @Test
    fun jsonExportImport_preservesFullWorkout() {
        val original = engine.recordLocally(sampleWorkout())
        val artifact = export.exportJson(listOf(original))
        assertNotNull(artifact.utf8Content)

        val fresh = InMemoryLocalHealthStore()
        val importer = HealthExportService(fresh)
        val count = importer.importJson(checkNotNull(artifact.utf8Content))
        assertEquals(1, count)

        val restored = fresh.getWorkout(original.id)
        assertNotNull(restored)
        assertWorkoutsEqual(original.copy(origin = HealthRecordOrigin.OMNIGART_JSON_IMPORT), restored)
    }

    @Test
    fun mapper_energyAndDistanceRoundTrip() {
        val kcal = 321.456
        val metres = 1234.567
        assertEquals(
            HealthDataMapper.roundEnergyKcal(kcal),
            HealthDataMapper.roundEnergyKcal(
                HealthDataMapper.joulesToKcal(HealthDataMapper.kcalToJoules(kcal))
            )
        )
        assertEquals(
            HealthDataMapper.roundDistanceMetres(metres),
            HealthDataMapper.roundDistanceMetres(
                HealthDataMapper.kilometresToMetres(HealthDataMapper.metresToKilometres(metres))
            )
        )
    }

    @Test
    fun activityType_mapsBothPlatforms() {
        WorkoutActivityType.entries.forEach { type ->
            val hk = HealthDataMapper.activityToHealthKitRaw(type)
            assertEquals(type, HealthDataMapper.activityFromHealthKitRaw(hk))
            val hc = HealthDataMapper.activityToHealthConnectType(type)
            assertEquals(type, HealthDataMapper.activityFromHealthConnectType(hc))
        }
    }

    @Test
    fun platformImport_mergesRichestHeartRate_andRecordsConflicts() {
        val local =
            engine.recordLocally(
                sampleWorkout().copy(
                    activeEnergyKcal = 400.0,
                    heartRateSeries =
                    listOf(
                        LocalHeartRatePoint(Instant.parse("2026-10-20T07:01:00Z"), 120.0),
                        LocalHeartRatePoint(Instant.parse("2026-10-20T07:10:00Z"), 145.0)
                    )
                )
            )
        val fromPlatform =
            local.copy(
                activeEnergyKcal = 450.0,
                origin = HealthRecordOrigin.APPLE_HEALTH,
                heartRateSeries =
                local.heartRateSeries +
                    LocalHeartRatePoint(Instant.parse("2026-10-20T07:30:00Z"), 160.0)
            )
        val merged = engine.importFromPlatform(fromPlatform, HealthSyncPlatform.APPLE_HEALTH)
        assertEquals(3, merged.heartRateSeries.size)
        assertEquals(400.0, merged.activeEnergyKcal)
        assertTrue(store.listOpenConflicts().isNotEmpty())
        assertEquals("omnigart", store.listOpenConflicts().first().preferredSource)
    }

    @Test
    fun mayWriteToPlatform_rejectsForeignOrigin() {
        val own = sampleWorkout()
        assertTrue(HealthDataMapper.mayWriteToPlatform(own))
        assertFalse(
            HealthDataMapper.mayWriteToPlatform(
                own.copy(origin = HealthRecordOrigin.APPLE_HEALTH, sourceBundle = "com.apple.Health")
            )
        )
    }

    @Test
    fun csvGpxTcx_exportNonEmpty() {
        engine.recordLocally(sampleWorkout())
        val artifacts = export.exportAll()
        assertTrue(artifacts.any { it.format.name.startsWith("CSV") && it.utf8Content!!.contains("w1") })
        assertTrue(artifacts.any { it.format.name == "GPX" && it.utf8Content!!.contains("trkpt") })
        val tcx = artifacts.first { it.format.name == "TCX" }
        assertTrue(tcx.utf8Content!!.contains("Activity"))
        // Required TCX schema namespace URL (IANA / Training Center Database) stays in XML.
        assertTrue(
            tcx.utf8Content!!.contains(
                "http://www.garmin.com/xmlschemas/TrainingCenterDatabase/v2"
            )
        )
        assertEquals("application/vnd.garmin.tcx+xml", tcx.mimeType)
        val fitNote = artifacts.first { it.format.name == "FIT_UNAVAILABLE" }
        assertEquals(
            "FIT export is not available: no AGPL-compatible FOSS FIT writer " +
                "is included. Use TCX/GPX/JSON instead.",
            fitNote.utf8Content
        )
        assertEquals("FIT unavailable (AGPL / FOSS library gap)", fitNote.note)
    }

    @Test
    fun softDelete_andSyncIdentifierLookup() {
        val w = engine.recordLocally(sampleWorkout())
        assertNotNull(store.getBySyncIdentifier(w.identity.syncIdentifier))
        store.softDeleteWorkout(w.id)
        assertEquals(null, store.getWorkout(w.id))
    }

    @Test
    fun exportDocument_schemaIdStable() {
        val json =
            Json { ignoreUnknownKeys = true }.decodeFromString<OmniGartHealthExportDocument>(
                export.exportJson(listOf(sampleWorkout())).utf8Content!!
            )
        assertEquals(OmniGartHealthExportDocument.SCHEMA_ID, json.schema)
        assertEquals(OmniGartHealthExportDocument.SCHEMA_VERSION, json.schemaVersion)
    }

    private fun assertWorkoutsEqual(expected: LocalHealthWorkout, actual: LocalHealthWorkout) {
        assertEquals(expected.id, actual.id)
        assertEquals(expected.identity.syncIdentifier, actual.identity.syncIdentifier)
        assertEquals(expected.identity.syncVersion, actual.identity.syncVersion)
        assertEquals(expected.activityType, actual.activityType)
        assertEquals(expected.startAt, actual.startAt)
        assertEquals(expected.endAt, actual.endAt)
        assertEquals(expected.activeEnergyKcal, actual.activeEnergyKcal)
        assertEquals(expected.basalEnergyKcal, actual.basalEnergyKcal)
        assertEquals(expected.distanceMetres, actual.distanceMetres)
        assertEquals(expected.steps, actual.steps)
        assertEquals(expected.heartRateSeries, actual.heartRateSeries)
        assertEquals(expected.route, actual.route)
        assertEquals(expected.events, actual.events)
        assertEquals(expected.sets, actual.sets)
        assertEquals(expected.planId, actual.planId)
        assertEquals(expected.origin, actual.origin)
    }
}
