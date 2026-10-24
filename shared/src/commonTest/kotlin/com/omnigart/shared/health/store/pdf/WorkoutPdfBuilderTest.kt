package com.omnigart.shared.health.store.pdf

import com.omnigart.shared.health.WorkoutActivityType
import com.omnigart.shared.health.store.HealthEventType
import com.omnigart.shared.health.store.HealthRecordOrigin
import com.omnigart.shared.health.store.HealthSyncIdentity
import com.omnigart.shared.health.store.LocalHealthWorkout
import com.omnigart.shared.health.store.LocalHeartRatePoint
import com.omnigart.shared.health.store.LocalWorkoutEvent
import com.omnigart.shared.health.store.LocalWorkoutSet
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class WorkoutPdfBuilderTest {
    private val labels = WorkoutPdfLabelCatalog.fromCatalog("en-GB")

    @Test
    fun singleStrengthPdf_includesSetsMuscleAndZones() {
        val start = Instant.parse("2026-10-22T09:00:00Z")
        val end = Instant.parse("2026-10-22T09:45:00Z")
        val workout =
            LocalHealthWorkout(
                id = "pdf-s1",
                identity = HealthSyncIdentity("omnigart.workout.pdf-s1", 1),
                activityType = WorkoutActivityType.STRENGTH,
                startAt = start,
                endAt = end,
                activeEnergyKcal = 180.0,
                basalEnergyKcal = 35.0,
                averageHeartRateBpm = 125.0,
                maxHeartRateBpm = 155.0,
                origin = HealthRecordOrigin.OMNIGART,
                updatedAt = end,
                heartRateSeries =
                listOf(
                    LocalHeartRatePoint(Instant.parse("2026-10-22T09:05:00Z"), 110.0),
                    LocalHeartRatePoint(Instant.parse("2026-10-22T09:20:00Z"), 140.0),
                    LocalHeartRatePoint(Instant.parse("2026-10-22T09:35:00Z"), 150.0)
                ),
                sets =
                listOf(
                    LocalWorkoutSet(
                        id = "s0",
                        exerciseKey = "bench",
                        setIndex = 0,
                        reps = 8,
                        loadKg = 60.0
                    ),
                    LocalWorkoutSet(
                        id = "s1",
                        exerciseKey = "bench",
                        setIndex = 1,
                        reps = 6,
                        loadKg = 70.0
                    )
                )
            )
        val doc =
            WorkoutPdfBuilder(labels).buildSingle(
                workout = workout,
                locale = "en-GB",
                strength =
                WorkoutPdfBuilder.StrengthExtras(
                    totalWeightLiftedKg = 900.0,
                    exerciseNames = mapOf("bench" to "Barbell bench press"),
                    muscleVolumeKg =
                    mapOf(
                        "pectoralis_major" to 500.0,
                        "triceps_brachii" to 200.0
                    )
                )
            )
        assertEquals("OmniGart", doc.brandTitle)
        assertTrue(doc.disclaimer.isNotBlank())
        assertEquals(1, doc.workouts.size)
        val page = doc.workouts.first()
        assertNotNull(page.heartRate)
        assertTrue(page.heartRate!!.chartPoints.size >= 3)
        assertTrue(page.heartRate!!.timeInZoneLines.size >= 2)
        assertNotNull(page.strength)
        assertEquals(1, page.strength!!.exercises.size)
        assertEquals(2, page.strength!!.exercises.first().setLines.size)
        assertEquals(2, page.muscleMap.size)
        assertEquals("NotoSans", doc.fontSpec.bodyFamily)
    }

    @Test
    fun yogaPdf_includesSegments() {
        val start = Instant.parse("2026-10-22T18:00:00Z")
        val end = Instant.parse("2026-10-22T18:30:00Z")
        val workout =
            LocalHealthWorkout(
                id = "pdf-y1",
                identity = HealthSyncIdentity("omnigart.workout.pdf-y1", 1),
                activityType = WorkoutActivityType.YOGA,
                startAt = start,
                endAt = end,
                activeEnergyKcal = 90.0,
                basalEnergyKcal = 20.0,
                origin = HealthRecordOrigin.OMNIGART,
                updatedAt = end,
                events =
                listOf(
                    LocalWorkoutEvent(
                        id = "e0",
                        type = HealthEventType.SEGMENT,
                        startAt = start,
                        endAt = Instant.parse("2026-10-22T18:15:00Z"),
                        label = "Sun salutation",
                        eventIndex = 0
                    )
                )
            )
        val page =
            WorkoutPdfBuilder(labels).buildSingle(workout, "uk").workouts.first()
        assertEquals(1, page.segments.size)
        assertEquals("Sun salutation", page.segments.first().label)
    }

    @Test
    fun runningPdf_includesPace() {
        val start = Instant.parse("2026-10-22T07:00:00Z")
        val end = Instant.parse("2026-10-22T07:40:00Z")
        val workout =
            LocalHealthWorkout(
                id = "pdf-r1",
                identity = HealthSyncIdentity("omnigart.workout.pdf-r1", 1),
                activityType = WorkoutActivityType.RUNNING,
                startAt = start,
                endAt = end,
                activeEnergyKcal = 400.0,
                distanceMetres = 8000.0,
                averageCadenceSpm = 170.0,
                origin = HealthRecordOrigin.OMNIGART,
                updatedAt = end
            )
        val cardio =
            WorkoutPdfBuilder(labels).buildSingle(workout, "en-GB").workouts.first().cardio
        assertNotNull(cardio)
        assertNotNull(cardio!!.distanceLabel)
        assertNotNull(cardio.paceLabel)
        assertNotNull(cardio.cadenceLabel)
    }

    @Test
    fun fontSpec_forGeorgianAndChinese() {
        assertEquals("NotoSansGeorgian", PdfFontSpec.forLocale("ka").georgianFamily)
        assertTrue(PdfFontSpec.forLocale("zh-Hans").usePlatformCjkCascade)
        assertEquals("NotoSans", PdfFontSpec.forLocale("kk").bodyFamily)
    }

    @Test
    fun rangeReport_ordersNewestFirst() {
        fun w(id: String, start: String): LocalHealthWorkout {
            val s = Instant.parse(start)
            return LocalHealthWorkout(
                id = id,
                identity = HealthSyncIdentity("omnigart.workout.$id", 1),
                activityType = WorkoutActivityType.WALKING,
                startAt = s,
                endAt = Instant.fromEpochMilliseconds(s.toEpochMilliseconds() + 600_000),
                origin = HealthRecordOrigin.OMNIGART,
                updatedAt = s
            )
        }
        val doc =
            WorkoutPdfBuilder(labels).buildRange(
                listOf(w("a", "2026-10-01T10:00:00Z"), w("b", "2026-10-10T10:00:00Z")),
                "en-GB"
            )
        assertEquals(listOf("b", "a"), doc.workouts.map { it.workoutId })
        assertEquals(labels.rangeReportTitle, doc.reportTitle)
    }
}
