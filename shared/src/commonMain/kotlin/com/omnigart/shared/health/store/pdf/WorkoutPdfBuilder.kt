package com.omnigart.shared.health.store.pdf

import com.omnigart.activity.hr.HeartRateZones
import com.omnigart.activity.hr.HrSample
import com.omnigart.activity.hr.ZoneModel
import com.omnigart.shared.health.WorkoutActivityType
import com.omnigart.shared.health.store.HealthEventType
import com.omnigart.shared.health.store.LocalHealthWorkout
import com.omnigart.shared.health.store.LocalWorkoutSet
import kotlin.math.max
import kotlin.math.round
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Builds a [WorkoutPdfDocument] from local workouts + optional strength/muscle extras.
 * Hosts pass already-translated [WorkoutPdfLabels].
 */
class WorkoutPdfBuilder(private val labels: WorkoutPdfLabels, private val clock: Clock = Clock.System) {
    data class StrengthExtras(
        val totalWeightLiftedKg: Double,
        /** exerciseKey → display name */
        val exerciseNames: Map<String, String> = emptyMap(),
        /** muscleKey → volume kg */
        val muscleVolumeKg: Map<String, Double> = emptyMap()
    )

    fun buildSingle(
        workout: LocalHealthWorkout,
        locale: String,
        hrMaxBpm: Int = 190,
        hrRestBpm: Int = 60,
        strength: StrengthExtras? = null
    ): WorkoutPdfDocument = WorkoutPdfDocument(
        locale = locale,
        brandTitle = labels.brandTitle,
        reportTitle = labels.singleReportTitle,
        generatedAtIso = clock.now().toString(),
        disclaimer = labels.disclaimer,
        workouts =
        listOf(
            toPage(workout, hrMaxBpm, hrRestBpm, strength)
        )
    )

    fun buildRange(
        workouts: List<LocalHealthWorkout>,
        locale: String,
        hrMaxBpm: Int = 190,
        hrRestBpm: Int = 60,
        strengthByWorkoutId: Map<String, StrengthExtras> = emptyMap()
    ): WorkoutPdfDocument = WorkoutPdfDocument(
        locale = locale,
        brandTitle = labels.brandTitle,
        reportTitle = labels.rangeReportTitle,
        generatedAtIso = clock.now().toString(),
        disclaimer = labels.disclaimer,
        workouts =
        workouts
            .sortedByDescending { it.startAt }
            .map { toPage(it, hrMaxBpm, hrRestBpm, strengthByWorkoutId[it.id]) }
    )

    private fun toPage(workout: LocalHealthWorkout, hrMaxBpm: Int, hrRestBpm: Int, strength: StrengthExtras?): WorkoutPdfPage {
        val durationSec = workout.durationSeconds().coerceAtLeast(0)
        val active = workout.activeEnergyKcal ?: 0.0
        val basal = workout.basalEnergyKcal ?: 0.0
        val total = active + basal
        return WorkoutPdfPage(
            workoutId = workout.id,
            header =
            WorkoutPdfHeader(
                dateLabel = "${labels.date}: ${formatDate(workout.startAt)}",
                activityLabel =
                "${labels.activity}: ${activityName(workout.activityType)}",
                durationLabel = "${labels.duration}: ${formatDuration(durationSec)}"
            ),
            metrics =
            WorkoutPdfMetrics(
                activeKcalLabel = "${labels.activeKcal}: ${formatOne(active)}",
                totalKcalLabel = "${labels.totalKcal}: ${formatOne(total)}",
                avgHrLabel =
                workout.averageHeartRateBpm?.let {
                    "${labels.avgHr}: ${formatOne(it)}"
                },
                maxHrLabel =
                workout.maxHeartRateBpm?.let {
                    "${labels.maxHr}: ${formatOne(it)}"
                }
            ),
            heartRate = buildHeartRate(workout, hrMaxBpm, hrRestBpm),
            cardio = buildCardio(workout, durationSec),
            strength = buildStrength(workout, strength),
            segments = buildSegments(workout),
            muscleMap = buildMuscleMap(strength)
        )
    }

    private fun buildHeartRate(workout: LocalHealthWorkout, hrMaxBpm: Int, hrRestBpm: Int): WorkoutPdfHeartRate? {
        if (workout.heartRateSeries.isEmpty()) return null
        val zones =
            HeartRateZones.boundsBpm(
                hrMax = hrMaxBpm.coerceAtLeast(100),
                hrRest = hrRestBpm.coerceIn(0, hrMaxBpm - 1),
                model = ZoneModel.PERCENT_HR_MAX
            )
        val samples =
            workout.heartRateSeries.map {
                HrSample(it.recordedAt.toEpochMilliseconds(), it.beatsPerMinute.toInt())
            }
        val timeInZone = HeartRateZones.timeInZone(samples, zones)
        val t0 = samples.minOf { it.epochMillis }.toDouble()
        val t1 = samples.maxOf { it.epochMillis }.toDouble().coerceAtLeast(t0 + 1.0)
        val maxBpm =
            max(
                samples.maxOf { it.bpm }.toDouble(),
                zones.last().upperBpm.toDouble()
            ).coerceAtLeast(1.0)
        val chartPoints =
            samples.sortedBy { it.epochMillis }.map {
                PdfChartPoint(
                    x = ((it.epochMillis - t0) / (t1 - t0)).toFloat(),
                    y = (it.bpm / maxBpm).toFloat().coerceIn(0f, 1f)
                )
            }
        val zoneColors =
            longArrayOf(
                0xFF7CB342, // Z1 green
                0xFF26A69A, // Z2 teal
                0xFFFFB300, // Z3 amber
                0xFFFB8C00, // Z4 orange
                0xFFE53935 // Z5 red
            )
        val bands =
            zones.mapIndexed { index, z ->
                PdfZoneBand(
                    name = z.definition.name,
                    lowerY = (z.lowerBpm / maxBpm).toFloat().coerceIn(0f, 1f),
                    upperY = (z.upperBpm / maxBpm).toFloat().coerceIn(0f, 1f),
                    colorArgb = zoneColors.getOrElse(index) { 0xFF90A4AE }
                )
            }
        val lines =
            timeInZone.map { tiz ->
                val mins = tiz.durationSeconds / 60.0
                "${labels.zonePrefix}${tiz.zone.definition.index} " +
                    "(${tiz.zone.lowerBpm}–${tiz.zone.upperBpm}): " +
                    "${formatOne(mins)} ${labels.minutesSuffix}"
            }
        return WorkoutPdfHeartRate(
            chartPoints = chartPoints,
            zoneBands = bands,
            timeInZoneLines = listOf(labels.timeInZone) + lines
        )
    }

    private fun buildCardio(workout: LocalHealthWorkout, durationSec: Long): WorkoutPdfCardio? {
        val distance = workout.distanceMetres
        val cadence = workout.averageCadenceSpm
        if (distance == null && cadence == null) {
            if (workout.activityType == WorkoutActivityType.STRENGTH ||
                workout.activityType == WorkoutActivityType.YOGA ||
                workout.activityType == WorkoutActivityType.PILATES
            ) {
                return null
            }
        }
        val pace =
            if (distance != null && distance > 0 && durationSec > 0) {
                val secPerKm = durationSec / (distance / 1000.0)
                val mins = (secPerKm / 60).toInt()
                val secs = (secPerKm % 60).toInt()
                "${labels.pace}: $mins:${secs.toString().padStart(2, '0')} /km"
            } else {
                null
            }
        return WorkoutPdfCardio(
            distanceLabel =
            distance?.let {
                "${labels.distance}: ${formatOne(it)} m"
            },
            paceLabel = pace,
            cadenceLabel =
            cadence?.let {
                "${labels.cadence}: ${formatOne(it)} spm"
            }
        )
    }

    private fun buildStrength(workout: LocalHealthWorkout, extras: StrengthExtras?): WorkoutPdfStrength? {
        if (workout.sets.isEmpty() && extras == null) return null
        if (workout.activityType != WorkoutActivityType.STRENGTH &&
            workout.sets.isEmpty()
        ) {
            return null
        }
        val byExercise = workout.sets.groupBy { it.exerciseKey }
        val exercises =
            byExercise.map { (key, sets) ->
                val title = extras?.exerciseNames?.get(key) ?: key
                WorkoutPdfExercise(
                    title = title,
                    setLines =
                    sets.sortedBy { it.setIndex }.map { set ->
                        formatSetLine(set)
                    }
                )
            }
        val total =
            extras?.totalWeightLiftedKg
                ?: workout.sets.sumOf { (it.loadKg ?: 0.0) * (it.reps ?: 0) }
        return WorkoutPdfStrength(
            totalWeightLabel =
            "${labels.totalWeightLifted}: ${formatOne(total)} ${labels.perKg}",
            exercises = exercises
        )
    }

    private fun formatSetLine(set: LocalWorkoutSet): String {
        val parts = mutableListOf<String>()
        parts += "#${set.setIndex + 1}"
        set.reps?.let { parts += "$it×" }
        set.loadKg?.let { parts += "${formatOne(it)} ${labels.perKg}" }
        if (set.isWarmup) parts += "(W)"
        set.rir?.let { parts += "RIR ${formatOne(it)}" }
        return parts.joinToString(" ")
    }

    private fun buildSegments(workout: LocalHealthWorkout): List<WorkoutPdfSegment> {
        val relevant =
            workout.events.filter {
                it.type == HealthEventType.SEGMENT ||
                    it.type == HealthEventType.LAP ||
                    workout.activityType == WorkoutActivityType.YOGA ||
                    workout.activityType == WorkoutActivityType.PILATES
            }
        if (relevant.isEmpty() &&
            (
                workout.activityType == WorkoutActivityType.YOGA ||
                    workout.activityType == WorkoutActivityType.PILATES
                )
        ) {
            return listOf(
                WorkoutPdfSegment(
                    label = labels.segments,
                    detail = formatDuration(workout.durationSeconds())
                )
            )
        }
        return relevant.map { event ->
            val end = event.endAt ?: event.startAt
            val sec =
                ((end.toEpochMilliseconds() - event.startAt.toEpochMilliseconds()) / 1000L)
                    .coerceAtLeast(0)
            WorkoutPdfSegment(
                label = event.label ?: event.type.name,
                detail = formatDuration(sec)
            )
        }
    }

    private fun buildMuscleMap(extras: StrengthExtras?): List<WorkoutPdfMuscleBar> {
        val volumes = extras?.muscleVolumeKg.orEmpty()
        if (volumes.isEmpty()) return emptyList()
        val maxVol = volumes.values.maxOrNull()?.coerceAtLeast(0.001) ?: return emptyList()
        return volumes.entries
            .sortedByDescending { it.value }
            .take(12)
            .map { (key, value) ->
                WorkoutPdfMuscleBar(
                    muscleKey = key,
                    displayName = key.replace('_', ' '),
                    intensity = (value / maxVol).toFloat().coerceIn(0f, 1f)
                )
            }
    }

    private fun activityName(type: WorkoutActivityType): String =
        labels.activityNames[type.name] ?: type.name.lowercase().replaceFirstChar { it.titlecase() }

    private fun formatDate(instant: Instant): String = instant.toString().take(10)

    private fun formatDuration(seconds: Long): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return if (h > 0) {
            "${h}h ${m.toString().padStart(2, '0')}m"
        } else {
            "${m}m ${s.toString().padStart(2, '0')}s"
        }
    }

    private fun formatOne(value: Double): String {
        val rounded = round(value * 10.0) / 10.0
        return if (rounded == rounded.toLong().toDouble()) {
            rounded.toLong().toString()
        } else {
            rounded.toString()
        }
    }
}
