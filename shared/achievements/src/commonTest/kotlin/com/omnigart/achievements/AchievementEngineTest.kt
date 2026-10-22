package com.omnigart.achievements

import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AchievementEngineTest {
    private val catalog = AchievementCatalog(
        version = 1,
        achievements = listOf(
            AchievementDefinition(
                id = "strength.sessions.5",
                category = AchievementCategory.STRENGTH,
                tier = AchievementTier.Metal(MetalTier.BRONZE),
                familyId = "strength.sessions",
                iconId = "strength",
                titleKey = "achievement.strength_sessions_5.title",
                descriptionKey = "achievement.strength_sessions_5.description",
                rule = AchievementRule(
                    kind = RuleKind.COUNTER,
                    metric = "workout_completed",
                    value = 5.0,
                    filters = mapOf("discipline" to "strength"),
                ),
                sortOrder = 20,
            ),
            AchievementDefinition(
                id = "strength.sessions.15",
                category = AchievementCategory.STRENGTH,
                tier = AchievementTier.Metal(MetalTier.SILVER),
                familyId = "strength.sessions",
                iconId = "strength",
                titleKey = "achievement.strength_sessions_15.title",
                descriptionKey = "achievement.strength_sessions_15.description",
                rule = AchievementRule(
                    kind = RuleKind.COUNTER,
                    metric = "workout_completed",
                    value = 15.0,
                    filters = mapOf("discipline" to "strength"),
                ),
                sortOrder = 30,
            ),
            AchievementDefinition(
                id = "consistency.streak.7",
                category = AchievementCategory.CONSISTENCY,
                tier = AchievementTier.Metal(MetalTier.BRONZE),
                familyId = "consistency.streak",
                iconId = "streak",
                titleKey = "achievement.consistency_streak_7.title",
                descriptionKey = "achievement.consistency_streak_7.description",
                rule = AchievementRule(
                    kind = RuleKind.STREAK,
                    metric = "active_or_rest_day",
                    value = 7.0,
                    streakPolicy = StreakPolicy(
                        allowPlannedRest = true,
                        allowStreakFreeze = true,
                        maxFreezeDaysPer30Days = 2,
                    ),
                ),
            ),
            AchievementDefinition(
                id = "running.single.5k",
                category = AchievementCategory.RUNNING,
                tier = AchievementTier.Metal(MetalTier.BRONZE),
                familyId = "running.distance_single",
                iconId = "running",
                titleKey = "achievement.running_single_5k.title",
                descriptionKey = "achievement.running_single_5k.description",
                rule = AchievementRule(
                    kind = RuleKind.THRESHOLD,
                    metric = "distance_m",
                    value = 5000.0,
                    filters = mapOf("discipline" to "running"),
                ),
            ),
            AchievementDefinition(
                id = "strength.first_pr",
                category = AchievementCategory.STRENGTH,
                tier = AchievementTier.Metal(MetalTier.SILVER),
                familyId = "strength.pr",
                iconId = "trophy",
                titleKey = "achievement.strength_first_pr.title",
                descriptionKey = "achievement.strength_first_pr.description",
                rule = AchievementRule(
                    kind = RuleKind.PERSONAL_RECORD,
                    metric = "load_kg",
                    comparator = ComparatorOp.GT,
                    value = 0.0,
                    filters = mapOf("discipline" to "strength"),
                ),
            ),
            AchievementDefinition(
                id = "water.goal_days.3",
                category = AchievementCategory.WATER,
                tier = AchievementTier.Metal(MetalTier.BRONZE),
                familyId = "water.goal_streak",
                iconId = "water",
                titleKey = "achievement.water_goal_days_3.title",
                descriptionKey = "achievement.water_goal_days_3.description",
                rule = AchievementRule(
                    kind = RuleKind.STREAK,
                    metric = "water_goal_day",
                    value = 3.0,
                    streakPolicy = StreakPolicy(),
                ),
            ),
            AchievementDefinition(
                id = "recovery.rest_when_low.3",
                category = AchievementCategory.RECOVERY,
                tier = AchievementTier.Metal(MetalTier.GOLD),
                familyId = "recovery.wise_rest",
                iconId = "readiness",
                titleKey = "achievement.recovery_rest_when_low_3.title",
                descriptionKey = "achievement.recovery_rest_when_low_3.description",
                rule = AchievementRule(
                    kind = RuleKind.COUNTER,
                    metric = "rest_taken_on_low_readiness",
                    value = 3.0,
                ),
            ),
            AchievementDefinition(
                id = "yoga.challenge.30d_mobility",
                category = AchievementCategory.YOGA,
                tier = AchievementTier.Metal(MetalTier.GOLD),
                familyId = "yoga.challenge",
                iconId = "achievement",
                titleKey = "achievement.yoga_challenge_30d_mobility.title",
                descriptionKey = "achievement.yoga_challenge_30d_mobility.description",
                rule = AchievementRule(
                    kind = RuleKind.CHALLENGE,
                    metric = "challenge_completed",
                    value = 1.0,
                    filters = mapOf("challengeType" to "mobility_30_day"),
                ),
            ),
        ),
    )

    private fun engine() = AchievementEngine(catalog, InMemoryAchievementStore())

    private fun workout(
        id: String,
        iso: String,
        zone: String = "Europe/London",
        discipline: String = "strength",
        distanceM: Double = 0.0,
    ) = DomainEvent.WorkoutCompleted(
        eventId = id,
        occurredAt = Instant.parse(iso),
        timeZoneId = zone,
        discipline = discipline,
        distanceM = distanceM,
    )

    @Test
    fun counterUnlocksAtThreshold() {
        val eng = engine()
        repeat(4) { i ->
            val r = eng.ingest(workout("w$i", "2026-03-0${i + 1}T12:00:00Z"))
            assertTrue(r.unlocks.none { it.achievementId == "strength.sessions.5" })
        }
        val fifth = eng.ingest(workout("w4", "2026-03-05T12:00:00Z"))
        assertTrue(fifth.unlocks.any { it.achievementId == "strength.sessions.5" })
        val snap = eng.progress("strength.sessions.5")!!
        assertTrue(snap.unlocked)
        assertEquals(5.0, snap.currentValue)
        assertEquals("strength.sessions.15", snap.nextInFamily?.achievementId)
    }

    @Test
    fun idempotentEventIds() {
        val eng = engine()
        val e = workout("same", "2026-03-01T12:00:00Z")
        assertFalse(eng.ingest(e).ignoredDuplicate)
        val again = eng.ingest(e)
        assertTrue(again.ignoredDuplicate)
        assertEquals(0, again.unlocks.size)
    }

    @Test
    fun streakCountsPlannedRest() {
        val eng = engine()
        // Days 1-3 active, day 4 planned rest, days 5-7 active → streak 7
        listOf(1, 2, 3).forEach { d ->
            eng.ingest(workout("a$d", "2026-04-0${d}T18:00:00Z"))
        }
        eng.ingest(
            DomainEvent.PlannedRestDay(
                eventId = "rest4",
                occurredAt = Instant.parse("2026-04-04T12:00:00Z"),
                timeZoneId = "Europe/London",
            ),
        )
        listOf(5, 6, 7).forEach { d ->
            eng.ingest(workout("a$d", "2026-04-0${d}T18:00:00Z"))
        }
        val snap = eng.progress("consistency.streak.7")!!
        assertTrue(snap.unlocked)
        assertEquals(7.0, snap.currentValue)
    }

    @Test
    fun streakFreezeBridgesGap() {
        val eng = engine()
        eng.ingest(workout("d1", "2026-05-01T12:00:00Z"))
        eng.ingest(workout("d2", "2026-05-02T12:00:00Z"))
        eng.ingest(
            DomainEvent.StreakFreeze(
                eventId = "fz",
                occurredAt = Instant.parse("2026-05-03T12:00:00Z"),
                timeZoneId = "Europe/London",
                metric = "active_or_rest_day",
            ),
        )
        listOf(4, 5, 6, 7).forEach { d ->
            eng.ingest(workout("d$d", "2026-05-0${d}T12:00:00Z"))
        }
        assertTrue(eng.progress("consistency.streak.7")!!.unlocked)
    }

    @Test
    fun midnightBoundaryUsesLocalZone() {
        val eng = engine()
        // 23:30 in London on 10 Mar, and 00:30 on 11 Mar — two distinct local days
        eng.ingest(workout("late", "2026-03-10T23:30:00Z", zone = "Europe/London"))
        eng.ingest(workout("early", "2026-03-11T00:30:00Z", zone = "Europe/London"))
        // Water goal streak uses day keys — two water goal days
        eng.ingest(
            DomainEvent.WaterLogged(
                eventId = "w1",
                occurredAt = Instant.parse("2026-03-10T23:30:00Z"),
                timeZoneId = "Europe/London",
                amountMl = 500.0,
                dailyTotalMl = 2000.0,
                goalMl = 2000.0,
            ),
        )
        eng.ingest(
            DomainEvent.WaterLogged(
                eventId = "w2",
                occurredAt = Instant.parse("2026-03-11T00:30:00Z"),
                timeZoneId = "Europe/London",
                amountMl = 500.0,
                dailyTotalMl = 2000.0,
                goalMl = 2000.0,
            ),
        )
        eng.ingest(
            DomainEvent.WaterLogged(
                eventId = "w3",
                occurredAt = Instant.parse("2026-03-12T12:00:00Z"),
                timeZoneId = "Europe/London",
                amountMl = 500.0,
                dailyTotalMl = 2000.0,
                goalMl = 2000.0,
            ),
        )
        assertTrue(eng.progress("water.goal_days.3")!!.unlocked)
    }

    @Test
    fun dstSpringForwardStillDistinctDays() {
        // US Pacific DST spring forward 2026-03-08
        val eng = engine()
        eng.ingest(workout("a", "2026-03-07T20:00:00Z", zone = "America/Los_Angeles"))
        eng.ingest(workout("b", "2026-03-08T20:00:00Z", zone = "America/Los_Angeles"))
        eng.ingest(workout("c", "2026-03-09T20:00:00Z", zone = "America/Los_Angeles"))
        val dayA = DayBoundary.dayKey(Instant.parse("2026-03-07T20:00:00Z"), "America/Los_Angeles")
        val dayB = DayBoundary.dayKey(Instant.parse("2026-03-08T20:00:00Z"), "America/Los_Angeles")
        val dayC = DayBoundary.dayKey(Instant.parse("2026-03-09T20:00:00Z"), "America/Los_Angeles")
        assertEquals("2026-03-07", dayA)
        assertEquals("2026-03-08", dayB)
        assertEquals("2026-03-09", dayC)
        assertTrue(dayA != dayB && dayB != dayC)
    }

    @Test
    fun travelKeepsEventLocalDay() {
        val kyivDay = DayBoundary.dayKey(Instant.parse("2026-06-01T22:30:00Z"), "Europe/Kyiv")
        val nyDay = DayBoundary.dayKey(Instant.parse("2026-06-01T22:30:00Z"), "America/New_York")
        assertEquals("2026-06-02", kyivDay)
        assertEquals("2026-06-01", nyDay)
    }

    @Test
    fun thresholdSingleRun() {
        val eng = engine()
        eng.ingest(workout("r1", "2026-07-01T10:00:00Z", discipline = "running", distanceM = 4999.0))
        assertFalse(eng.progress("running.single.5k")!!.unlocked)
        eng.ingest(workout("r2", "2026-07-02T10:00:00Z", discipline = "running", distanceM = 5000.0))
        assertTrue(eng.progress("running.single.5k")!!.unlocked)
    }

    @Test
    fun personalRecordUnlocks() {
        val eng = engine()
        val r = eng.ingest(
            DomainEvent.PersonalRecord(
                eventId = "pr1",
                occurredAt = Instant.parse("2026-08-01T10:00:00Z"),
                timeZoneId = "Europe/London",
                discipline = "strength",
                metric = "load_kg",
                value = 100.0,
            ),
        )
        assertTrue(r.unlocks.any { it.achievementId == "strength.first_pr" })
    }

    @Test
    fun backfillBatchesWithoutDuplicateSpam() {
        val eng = engine()
        val events = (1..5).map { i ->
            workout("bf$i", "2026-09-0${i}T12:00:00Z")
        }
        val result = eng.backfill(events + events) // second copy duplicates
        assertEquals(5, result.processedEvents)
        assertEquals(5, result.skippedDuplicates)
        assertTrue(result.batchSummaryCount >= 1)
        assertTrue(eng.progress("strength.sessions.5")!!.unlocked)
    }

    @Test
    fun wiseRestOnLowReadiness() {
        val eng = engine()
        repeat(3) { i ->
            eng.ingest(
                DomainEvent.PlannedRestDay(
                    eventId = "lr$i",
                    occurredAt = Instant.parse("2026-10-0${i + 1}T12:00:00Z"),
                    timeZoneId = "Europe/London",
                    lowReadiness = true,
                ),
            )
        }
        assertTrue(eng.progress("recovery.rest_when_low.3")!!.unlocked)
    }

    @Test
    fun challengeCompletionUnlocksAchievement() {
        val eng = engine()
        val r = eng.ingest(
            DomainEvent.ChallengeCompleted(
                eventId = "ch1",
                occurredAt = Instant.parse("2026-11-01T12:00:00Z"),
                timeZoneId = "Europe/London",
                challengeType = "mobility_30_day",
                challengeInstanceId = "inst-1",
            ),
        )
        assertTrue(r.unlocks.any { it.achievementId == "yoga.challenge.30d_mobility" })
    }
}
