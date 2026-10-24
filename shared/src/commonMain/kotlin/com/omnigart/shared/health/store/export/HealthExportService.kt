package com.omnigart.shared.health.store.export

import com.omnigart.shared.health.store.HealthExportRange
import com.omnigart.shared.health.store.HealthSampleType
import com.omnigart.shared.health.store.LocalHealthStore
import com.omnigart.shared.health.store.LocalHealthWorkout
import kotlin.time.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * User-triggered local export/import. Files stay on-device; host shells present
 * share sheet / Files (iOS) or SAF (Android).
 */
class HealthExportService(
    private val store: LocalHealthStore,
    private val clock: Clock = Clock.System,
    private val json: Json =
        Json {
            prettyPrint = true
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
) {
    fun exportAll(range: HealthExportRange = HealthExportRange()): List<HealthExportArtifact> {
        val workouts = store.listWorkouts(range)
        return buildList {
            add(exportJson(workouts))
            add(exportWorkoutsCsv(workouts))
            add(exportHeartRateCsv(workouts))
            add(exportSamplesCsv(workouts))
            add(exportGpx(workouts))
            add(exportTcx(workouts))
            add(
                HealthExportArtifact(
                    format = HealthExportFormat.FIT_UNAVAILABLE,
                    fileName = "omnigart-fit-unavailable.txt",
                    mimeType = "text/plain",
                    utf8Content =
                    "FIT export is not available: no AGPL-compatible FOSS FIT writer " +
                        "is included. Use TCX/GPX/JSON instead.",
                    note = "FIT unavailable (AGPL / FOSS library gap)"
                )
            )
        }
    }

    fun exportJson(workouts: List<LocalHealthWorkout>): HealthExportArtifact {
        val doc =
            OmniGartHealthExportDocument(
                exportedAt = clock.now().toString(),
                workouts = workouts
            )
        return HealthExportArtifact(
            format = HealthExportFormat.JSON,
            fileName = "omnigart-health.json",
            mimeType = "application/json",
            utf8Content = json.encodeToString(doc)
        )
    }

    fun importJson(utf8: String): Int {
        val doc = json.decodeFromString<OmniGartHealthExportDocument>(utf8)
        require(doc.schema == OmniGartHealthExportDocument.SCHEMA_ID) {
            "Unsupported export schema: ${doc.schema}"
        }
        var count = 0
        doc.workouts.forEach { workout ->
            val stamped =
                workout.copy(
                    origin = com.omnigart.shared.health.store.HealthRecordOrigin.OMNIGART_JSON_IMPORT,
                    updatedAt = clock.now()
                )
            store.upsertWorkout(stamped)
            count++
        }
        return count
    }

    fun exportWorkoutsCsv(workouts: List<LocalHealthWorkout>): HealthExportArtifact {
        val header =
            listOf(
                "id",
                "sync_identifier",
                "sync_version",
                "activity_type",
                "start_at",
                "end_at",
                "duration_s",
                "active_energy_kcal",
                "basal_energy_kcal",
                "distance_metres",
                "steps",
                "avg_hr_bpm",
                "max_hr_bpm",
                "avg_cadence_spm",
                "origin",
                "source_bundle",
                "plan_id",
                "session_id",
                "treadmill",
                "treadmill_protocol",
                "treadmill_avg_speed_kmh",
                "treadmill_avg_incline_pct"
            ).joinToString(",")
        val rows =
            workouts.map { w ->
                val notes = w.notes.orEmpty()
                val isTreadmill = notes.startsWith("omnigart.treadmill")
                fun noteField(key: String): String =
                    notes.split('|').firstOrNull { it.startsWith("$key=") }?.removePrefix("$key=").orEmpty()
                listOf(
                    w.id,
                    w.identity.syncIdentifier,
                    w.identity.syncVersion.toString(),
                    w.activityType.name,
                    w.startAt.toString(),
                    w.endAt.toString(),
                    w.durationSeconds().toString(),
                    w.activeEnergyKcal?.toString().orEmpty(),
                    w.basalEnergyKcal?.toString().orEmpty(),
                    w.distanceMetres?.toString().orEmpty(),
                    w.steps?.toString().orEmpty(),
                    w.averageHeartRateBpm?.toString().orEmpty(),
                    w.maxHeartRateBpm?.toString().orEmpty(),
                    w.averageCadenceSpm?.toString().orEmpty(),
                    w.origin.name,
                    w.sourceBundle.orEmpty(),
                    w.planId.orEmpty(),
                    w.sessionId.orEmpty(),
                    if (isTreadmill) "1" else "0",
                    if (isTreadmill) noteField("proto") else "",
                    if (isTreadmill) noteField("spd") else "",
                    if (isTreadmill) noteField("inc") else ""
                ).joinToString(",") { csvEscape(it) }
            }
        return HealthExportArtifact(
            format = HealthExportFormat.CSV_WORKOUTS,
            fileName = "omnigart-workouts.csv",
            mimeType = "text/csv",
            utf8Content = (listOf(header) + rows).joinToString("\n")
        )
    }

    fun exportHeartRateCsv(workouts: List<LocalHealthWorkout>): HealthExportArtifact {
        val header = "workout_id,recorded_at,bpm"
        val rows =
            workouts.flatMap { w ->
                w.heartRateSeries.map { hr ->
                    listOf(w.id, hr.recordedAt.toString(), hr.beatsPerMinute.toString())
                        .joinToString(",") { csvEscape(it) }
                }
            }
        return HealthExportArtifact(
            format = HealthExportFormat.CSV_HEART_RATE,
            fileName = "omnigart-heart-rate.csv",
            mimeType = "text/csv",
            utf8Content = (listOf(header) + rows).joinToString("\n")
        )
    }

    fun exportSamplesCsv(workouts: List<LocalHealthWorkout>): HealthExportArtifact {
        val header = "workout_id,sample_type,recorded_at,end_at,value,unit"
        val rows =
            workouts.flatMap { w ->
                w.samples.map { s ->
                    listOf(
                        w.id,
                        s.type.name,
                        s.recordedAt.toString(),
                        s.endAt?.toString().orEmpty(),
                        s.value.toString(),
                        s.unit
                    ).joinToString(",") { csvEscape(it) }
                }
            }
        return HealthExportArtifact(
            format = HealthExportFormat.CSV_SAMPLES,
            fileName = "omnigart-samples.csv",
            mimeType = "text/csv",
            utf8Content = (listOf(header) + rows).joinToString("\n")
        )
    }

    fun exportGpx(workouts: List<LocalHealthWorkout>): HealthExportArtifact {
        val tracks =
            workouts.filter { it.route.isNotEmpty() }.joinToString("\n") { w ->
                val points =
                    w.route.joinToString("\n") { p ->
                        val elev =
                            p.altitudeMetres?.let { "<ele>$it</ele>" }.orEmpty()
                        """    <trkpt lat="${p.latitude}" lon="${p.longitude}">
      <time>${p.recordedAt}</time>$elev
    </trkpt>"""
                    }
                val treadmillType =
                    if (w.notes?.startsWith("omnigart.treadmill") == true) {
                        "RUNNING_TREADMILL"
                    } else {
                        w.activityType.name
                    }
                """  <trk>
    <name>${xmlEscape(w.id)}</name>
    <type>$treadmillType</type>
    <trkseg>
$points
    </trkseg>
  </trk>"""
            }
        val body =
            """<?xml version="1.0" encoding="UTF-8"?>
<gpx version="1.1" creator="OmniGart" xmlns="http://www.topografix.com/GPX/1/1">
$tracks
</gpx>
"""
        return HealthExportArtifact(
            format = HealthExportFormat.GPX,
            fileName = "omnigart-routes.gpx",
            mimeType = "application/gpx+xml",
            utf8Content = body
        )
    }

    fun exportTcx(workouts: List<LocalHealthWorkout>): HealthExportArtifact {
        val activities =
            workouts.joinToString("\n") { w ->
                val laps =
                    w.events.filter { it.type.name == "LAP" || it.type.name == "SEGMENT" }
                val lapBlocks =
                    if (laps.isEmpty()) {
                        tcxLap(w, w.startAt.toString(), w.endAt.toString())
                    } else {
                        laps.joinToString("\n") { event ->
                            tcxLap(
                                w,
                                event.startAt.toString(),
                                (event.endAt ?: w.endAt).toString()
                            )
                        }
                    }
                val treadmillNotes =
                    w.notes?.takeIf { it.startsWith("omnigart.treadmill") }?.let { n ->
                        val spd =
                            n.split('|').firstOrNull { it.startsWith("spd=") }?.removePrefix("spd=").orEmpty()
                        val inc =
                            n.split('|').firstOrNull { it.startsWith("inc=") }?.removePrefix("inc=").orEmpty()
                        """    <Notes>OmniGart treadmill spd=$spd incline=$inc</Notes>"""
                    }.orEmpty()
                """  <Activity Sport="${tcxSport(w)}">
    <Id>${xmlEscape(w.id)}</Id>
$treadmillNotes
$lapBlocks
  </Activity>"""
            }
        val body =
            """<?xml version="1.0" encoding="UTF-8"?>
<TrainingCenterDatabase xmlns="http://www.garmin.com/xmlschemas/TrainingCenterDatabase/v2">
  <Activities>
$activities
  </Activities>
</TrainingCenterDatabase>
"""
        return HealthExportArtifact(
            format = HealthExportFormat.TCX,
            fileName = "omnigart-workouts.tcx",
            mimeType = "application/vnd.garmin.tcx+xml",
            utf8Content = body
        )
    }

    private fun tcxLap(w: LocalHealthWorkout, start: String, end: String): String {
        val track =
            w.route.joinToString("\n") { p ->
                val alt =
                    p.altitudeMetres?.let { "<AltitudeMeters>$it</AltitudeMeters>" }.orEmpty()
                """      <Trackpoint>
        <Time>${p.recordedAt}</Time>
        <Position>
          <LatitudeDegrees>${p.latitude}</LatitudeDegrees>
          <LongitudeDegrees>${p.longitude}</LongitudeDegrees>
        </Position>$alt
      </Trackpoint>"""
            }
        val hrAvg =
            w.averageHeartRateBpm?.let {
                "<AverageHeartRateBpm><Value>${it.toInt()}</Value></AverageHeartRateBpm>"
            }.orEmpty()
        val calories = ((w.activeEnergyKcal ?: 0.0) + (w.basalEnergyKcal ?: 0.0)).toInt()
        return """    <Lap StartTime="$start">
      <TotalTimeSeconds>${w.durationSeconds()}</TotalTimeSeconds>
      <DistanceMeters>${w.distanceMetres ?: 0.0}</DistanceMeters>
      <Calories>$calories</Calories>
      $hrAvg
      <Intensity>Active</Intensity>
      <TriggerMethod>Manual</TriggerMethod>
      <Track>
$track
      </Track>
    </Lap><!-- end $end -->"""
    }

    private fun tcxSport(w: LocalHealthWorkout): String = when (w.activityType.name) {
        "RUNNING" -> "Running"
        "CYCLING" -> "Biking"
        else -> "Other"
    }

    private fun csvEscape(value: String): String = if (value.contains(',') || value.contains('"') || value.contains('\n')) {
        "\"" + value.replace("\"", "\"\"") + "\""
    } else {
        value
    }

    private fun xmlEscape(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")

    companion object {
        /** Sample types that get a dedicated CSV besides the generic samples file. */
        val dedicatedSampleTypes: Set<HealthSampleType> =
            setOf(HealthSampleType.HEART_RATE)
    }
}
