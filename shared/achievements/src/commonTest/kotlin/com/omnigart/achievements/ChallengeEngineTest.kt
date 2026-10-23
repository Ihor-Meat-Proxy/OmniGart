package com.omnigart.achievements

import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChallengeEngineTest {
    private val templates = ChallengeTemplateCatalog(
        version = 1,
        templates = listOf(
            ChallengeTemplate(
                id = "challenge.run_100km_month",
                challengeType = "run_100km_month",
                durationDays = 31,
                iconId = "running",
                titleKey = "challenge.run_100km_month.title",
                descriptionKey = "challenge.run_100km_month.description",
                goal = ChallengeGoal(
                    metric = "distance_m",
                    value = 100_000.0,
                    filters = mapOf("discipline" to "running"),
                    window = RuleWindow(WindowUnit.CHALLENGE, 1),
                ),
                localOnly = true,
                social = false,
            ),
            ChallengeTemplate(
                id = "challenge.hydration_21_day",
                challengeType = "hydration_21_day",
                durationDays = 21,
                iconId = "water",
                titleKey = "challenge.hydration_21_day.title",
                descriptionKey = "challenge.hydration_21_day.description",
                goal = ChallengeGoal(
                    metric = "water_goal_day",
                    value = 3.0,
                    window = RuleWindow(WindowUnit.CHALLENGE, 1),
                ),
            ),
        ),
    )

    @Test
    fun runDistanceChallengeCompletesLocally() {
        val engine = ChallengeEngine(templates)
        val inst = engine.start(
            templateId = "challenge.run_100km_month",
            instanceId = "i1",
            startedAt = Instant.parse("2026-06-01T08:00:00Z"),
            timeZoneId = "Europe/London",
        )
        assertEquals(ChallengeStatus.ACTIVE, inst.status)
        assertTrue(inst.localOnlySafe())

        engine.ingest(
            DomainEvent.WorkoutCompleted(
                eventId = "r1",
                occurredAt = Instant.parse("2026-06-05T10:00:00Z"),
                timeZoneId = "Europe/London",
                discipline = "running",
                distanceM = 40_000.0,
            ),
        )
        assertEquals(40_000.0, engine.get("i1")!!.progressValue)
        assertEquals(ChallengeStatus.ACTIVE, engine.get("i1")!!.status)

        val evals = engine.ingest(
            DomainEvent.WorkoutCompleted(
                eventId = "r2",
                occurredAt = Instant.parse("2026-06-20T10:00:00Z"),
                timeZoneId = "Europe/London",
                discipline = "running",
                distanceM = 60_000.0,
            ),
        )
        assertTrue(evals.any { it.newlyCompleted })
        assertEquals(ChallengeStatus.COMPLETED, engine.get("i1")!!.status)
    }

    @Test
    fun hydrationChallengeCountsDistinctDays() {
        val engine = ChallengeEngine(templates)
        engine.start(
            templateId = "challenge.hydration_21_day",
            instanceId = "h1",
            startedAt = Instant.parse("2026-07-01T08:00:00Z"),
            timeZoneId = "Europe/Kyiv",
        )
        repeat(3) { i ->
            engine.ingest(
                DomainEvent.WaterLogged(
                    eventId = "w$i",
                    occurredAt = Instant.parse("2026-07-0${i + 1}T18:00:00Z"),
                    timeZoneId = "Europe/Kyiv",
                    amountMl = 250.0,
                    dailyTotalMl = 2000.0,
                    goalMl = 2000.0,
                ),
            )
        }
        val done = engine.get("h1")!!
        assertEquals(ChallengeStatus.COMPLETED, done.status)
        assertEquals(3.0, done.progressValue)
    }

    @Test
    fun templatesRejectSocial() {
        templates.templates.forEach {
            assertTrue(it.localOnly)
            assertFalse(it.social)
        }
    }
}

private fun ChallengeInstance.localOnlySafe(): Boolean = true
