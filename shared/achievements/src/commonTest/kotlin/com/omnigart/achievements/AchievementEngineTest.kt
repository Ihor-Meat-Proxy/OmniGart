package com.omnigart.achievements

import com.omnigart.achievements.engine.AchievementEngine
import com.omnigart.achievements.engine.DayBoundaries
import com.omnigart.achievements.engine.EvaluationState
import com.omnigart.achievements.events.FoodLogged
import com.omnigart.achievements.events.FreezeTokenUsed
import com.omnigart.achievements.events.MeasurementAdded
import com.omnigart.achievements.events.PlannedRestHonoured
import com.omnigart.achievements.events.SleepImported
import com.omnigart.achievements.events.WaterLogged
import com.omnigart.achievements.events.WorkoutCompleted
import com.omnigart.achievements.model.AchievementCategory
import com.omnigart.achievements.model.AchievementDefinition
import com.omnigart.achievements.model.AchievementRule
import com.omnigart.achievements.model.AchievementTier
import com.omnigart.achievements.model.AchievementType
import com.omnigart.achievements.model.Aggregation
import com.omnigart.achievements.model.MetricId
import com.omnigart.achievements.model.RuleFilters
import com.omnigart.achievements.model.RuleWindow
import com.omnigart.achievements.model.StreakQualify
import com.omnigart.achievements.model.TierId
import com.omnigart.achievements.model.WindowKind
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

class AchievementEngineTest {

    private fun workout(
        id: String,
        day: String,
        zone: String = "Europe/London",
        discipline: String = "strength",
        volumeKg: Double? = 1000.0,
        durationMin: Double = 45.0,
        distanceM: Double? = null,
        sets: Int = 10,
        pr: Boolean = false,
        deleted: Boolean = false,
        strokes: List<String> = emptyList(),
    ): WorkoutCompleted {
        val date = LocalDate.parse(day)
        val instant = LocalDateTime(
            date.year,
            date.month,
            date.day,
            12,
            0,
        ).toInstant(TimeZone.of(zone))
        return WorkoutCompleted(
            eventId = id,
            occurredAt = instant,
            timeZoneId = zone,
            deleted = deleted,
            discipline = discipline,
            durationMin = durationMin,
            distanceM = distanceM,
            volumeKg = volumeKg,
            setsCount = sets,
            swimStrokes = strokes,
            isPersonalRecord = pr,
        )
    }

    private fun firstWorkoutDef() = AchievementDefinition(
        id = "general.first_workout",
        category = AchievementCategory.GENERAL,
        type = AchievementType.DISCOVERY,
        tiers = listOf(AchievementTier(TierId.UNLOCKED, 1.0)),
        rule = AchievementRule(
            metric = MetricId.WORKOUTS_COMPLETED,
            aggregation = Aggregation.COUNT,
            window = RuleWindow(kind = WindowKind.LIFETIME),
            threshold = 1.0,
        ),
        iconId = "workouts",
        titleKey = "achievement.general.first_workout.title",
        descriptionKey = "achievement.general.first_workout.description",
    )

    private fun volumeDef() = AchievementDefinition(
        id = "strength.volume_tonnes",
        category = AchievementCategory.STRENGTH,
        type = AchievementType.MILESTONE,
        tiers = listOf(
            AchievementTier(TierId.BRONZE, 1000.0),
            AchievementTier(TierId.SILVER, 10000.0),
        ),
        rule = AchievementRule(
            metric = MetricId.VOLUME_KG,
            aggregation = Aggregation.SUM,
            window = RuleWindow(kind = WindowKind.LIFETIME),
            filters = RuleFilters(discipline = listOf("strength")),
        ),
        iconId = "weight",
        titleKey = "achievement.strength.volume_tonnes.title",
        descriptionKey = "achievement.strength.volume_tonnes.description",
    )

    private fun streakDef() = AchievementDefinition(
        id = "general.weekly_consistency",
        category = AchievementCategory.GENERAL,
        type = AchievementType.STREAK,
        tiers = listOf(
            AchievementTier(TierId.BRONZE, 2.0),
            AchievementTier(TierId.SILVER, 4.0),
        ),
        rule = AchievementRule(
            metric = MetricId.WORKOUTS_COMPLETED,
            aggregation = Aggregation.STREAK,
            window = RuleWindow(
                kind = WindowKind.STREAK_DAYS,
                qualify = StreakQualify.ACTIVITY_OR_PLANNED_REST,
                restDayTolerancePerWeek = 1,
                freezeTokensAllowed = true,
                maxFreezePerStreak = 2,
            ),
        ),
        iconId = "streak",
        titleKey = "achievement.general.weekly_consistency.title",
        descriptionKey = "achievement.general.weekly_consistency.description",
    )

    @Test
    fun idempotentReplayDoesNotDoubleCount() {
        val engine = AchievementEngine(listOf(firstWorkoutDef(), volumeDef()))
        val e = workout("w1", "2026-03-10", volumeKg = 2000.0)
        val now = Instant.parse("2026-03-10T18:00:00Z")
        val r1 = engine.evaluate(EvaluationState(), listOf(e), now, "Europe/London")
        val r2 = engine.evaluate(r1.state, listOf(e), now, "Europe/London")
        assertEquals(1, r2.summary.skippedDuplicates)
        assertEquals(2000.0, r2.state.progressOf("strength.volume_tonnes").value)
        assertTrue(TierId.BRONZE in r2.state.progressOf("strength.volume_tonnes").unlockedTier)
    }

    @Test
    fun deletedWorkoutRevokesProgress() {
        val engine = AchievementEngine(listOf(volumeDef()))
        val e = workout("w1", "2026-03-10", volumeKg = 5000.0)
        val now = Instant.parse("2026-03-10T18:00:00Z")
        val r1 = engine.evaluate(EvaluationState(), listOf(e), now, "Europe/London")
        assertEquals(5000.0, r1.state.progressOf("strength.volume_tonnes").value)
        val deleted = e.copy(deleted = true)
        val r2 = engine.evaluate(r1.state, listOf(deleted), now, "Europe/London")
        assertEquals(0.0, r2.state.progressOf("strength.volume_tonnes").value)
        assertTrue(r2.state.progressOf("strength.volume_tonnes").unlockedTier.isEmpty())
    }

    @Test
    fun backfillSuppressesIndividualNotifications() {
        val engine = AchievementEngine(listOf(firstWorkoutDef(), volumeDef()))
        val events = listOf(
            workout("w1", "2026-01-01", volumeKg = 1500.0),
            workout("w2", "2026-01-02", volumeKg = 1500.0),
        )
        val now = Instant.parse("2026-01-03T12:00:00Z")
        val r = engine.evaluate(EvaluationState(), events, now, "Europe/London", backfill = true)
        assertTrue(r.summary.unlocks.isEmpty())
        assertTrue(r.summary.backfill)
        assertEquals("achievement.backfill.summary", r.summary.summaryMessageKey)
        assertTrue(r.summary.summaryUnlockCount >= 1)
    }

    @Test
    fun dstSpringForwardLondonStillCountsLocalDay() {
        // 2026-03-29 is the UK spring-forward (01:00 → 02:00).
        val engine = AchievementEngine(listOf(firstWorkoutDef()))
        val before = workout("w1", "2026-03-28", zone = "Europe/London")
        val after = workout("w2", "2026-03-29", zone = "Europe/London")
        val now = Instant.parse("2026-03-29T20:00:00Z")
        val r = engine.evaluate(EvaluationState(), listOf(before, after), now, "Europe/London")
        assertEquals(2.0, r.state.progressOf("general.first_workout").value)
        assertEquals("2026-03-28", DayBoundaries.localDayKey(before.occurredAt, "Europe/London"))
        assertEquals("2026-03-29", DayBoundaries.localDayKey(after.occurredAt, "Europe/London"))
    }

    @Test
    fun travelTimezoneChangeUsesEventLocalZone() {
        val engine = AchievementEngine(listOf(firstWorkoutDef()))
        // Same UTC instant can be different local days — we key off event zone.
        val london = workout("w1", "2026-06-01", zone = "Europe/London")
        val tokyo = workout("w2", "2026-06-02", zone = "Asia/Tokyo")
        val now = Instant.parse("2026-06-02T12:00:00Z")
        val r = engine.evaluate(EvaluationState(), listOf(london, tokyo), now, "Europe/London")
        assertEquals(2.0, r.state.progressOf("general.first_workout").value)
        assertEquals("2026-06-01", DayBoundaries.localDayKey(london.occurredAt, london.timeZoneId))
        assertEquals("2026-06-02", DayBoundaries.localDayKey(tokyo.occurredAt, tokyo.timeZoneId))
    }

    @Test
    fun plannedRestCountsTowardRestTolerantStreak() {
        val engine = AchievementEngine(listOf(streakDef()))
        val w1 = workout("w1", "2026-04-01")
        val rest = PlannedRestHonoured(
            eventId = "r1",
            occurredAt = Instant.parse("2026-04-02T12:00:00Z"),
            timeZoneId = "Europe/London",
            localDay = "2026-04-02",
        )
        val w2 = workout("w2", "2026-04-03")
        val now = Instant.parse("2026-04-03T20:00:00Z")
        val r = engine.evaluate(EvaluationState(), listOf(w1, rest, w2), now, "Europe/London")
        assertTrue(r.state.progressOf("general.weekly_consistency").value >= 3.0)
    }

    @Test
    fun freezeTokenPreservesStreakAcrossMiss() {
        val engine = AchievementEngine(listOf(streakDef()))
        val events = listOf(
            workout("w1", "2026-05-01"),
            workout("w2", "2026-05-02"),
            FreezeTokenUsed(
                eventId = "f1",
                occurredAt = Instant.parse("2026-05-03T12:00:00Z"),
                timeZoneId = "Europe/London",
                localDay = "2026-05-03",
            ),
            workout("w3", "2026-05-04"),
        )
        val now = Instant.parse("2026-05-04T20:00:00Z")
        val r = engine.evaluate(EvaluationState(), events, now, "Europe/London")
        assertTrue(r.state.progressOf("general.weekly_consistency").value >= 4.0)
    }

    @Test
    fun foodWaterSleepMeasurementImportPath() {
        val foodDef = AchievementDefinition(
            id = "nutrition.days_logged",
            category = AchievementCategory.NUTRITION,
            type = AchievementType.MILESTONE,
            tiers = listOf(AchievementTier(TierId.BRONZE, 7.0)),
            rule = AchievementRule(
                metric = MetricId.FOOD_DAYS_LOGGED,
                aggregation = Aggregation.COUNT,
                window = RuleWindow(kind = WindowKind.LIFETIME),
            ),
            iconId = "food",
            titleKey = "achievement.nutrition.days_logged.title",
            descriptionKey = "achievement.nutrition.days_logged.description",
        )
        val waterDef = AchievementDefinition(
            id = "water.days",
            category = AchievementCategory.WATER,
            type = AchievementType.MILESTONE,
            tiers = listOf(AchievementTier(TierId.BRONZE, 7.0)),
            rule = AchievementRule(
                metric = MetricId.WATER_GOAL_DAYS,
                aggregation = Aggregation.COUNT,
                window = RuleWindow(kind = WindowKind.LIFETIME),
            ),
            iconId = "water",
            titleKey = "achievement.water.days.title",
            descriptionKey = "achievement.water.days.description",
        )
        val sleepDef = AchievementDefinition(
            id = "recovery.sleep_days",
            category = AchievementCategory.RECOVERY,
            type = AchievementType.MILESTONE,
            tiers = listOf(AchievementTier(TierId.BRONZE, 7.0)),
            rule = AchievementRule(
                metric = MetricId.SLEEP_CONSISTENT_DAYS,
                aggregation = Aggregation.COUNT,
                window = RuleWindow(kind = WindowKind.LIFETIME),
            ),
            iconId = "sleep",
            titleKey = "achievement.recovery.sleep_days.title",
            descriptionKey = "achievement.recovery.sleep_days.description",
        )
        val bodyDef = AchievementDefinition(
            id = "body.measurement_days",
            category = AchievementCategory.BODY,
            type = AchievementType.MILESTONE,
            tiers = listOf(AchievementTier(TierId.BRONZE, 4.0)),
            rule = AchievementRule(
                metric = MetricId.MEASUREMENT_DAYS,
                aggregation = Aggregation.COUNT,
                window = RuleWindow(kind = WindowKind.LIFETIME),
                filters = RuleFilters(excludeWeightLossRate = true),
            ),
            iconId = "measurements",
            titleKey = "achievement.body.measurement_days.title",
            descriptionKey = "achievement.body.measurement_days.description",
        )
        val engine = AchievementEngine(listOf(foodDef, waterDef, sleepDef, bodyDef))
        val events = (1..7).flatMap { d ->
            val day = "2026-07-${d.toString().padStart(2, '0')}"
            val at = Instant.parse("${day}T12:00:00Z")
            listOf(
                FoodLogged("f$d", at, "Europe/London", localDay = day),
                WaterLogged("wa$d", at, "Europe/London", localDay = day, amountMl = 2000, goalMl = 2000, goalMet = true),
                SleepImported("s$d", at, "Europe/London", localDay = day, midpointVarianceMin = 20.0, consistent = true),
                MeasurementAdded("m$d", at, "Europe/London", localDay = day, kind = "weight"),
            )
        }
        val now = Instant.parse("2026-07-07T20:00:00Z")
        val r = engine.evaluate(EvaluationState(), events, now, "Europe/London", backfill = true)
        assertEquals(7.0, r.state.progressOf("nutrition.days_logged").value)
        assertEquals(7.0, r.state.progressOf("water.days").value)
        assertEquals(7.0, r.state.progressOf("recovery.sleep_days").value)
        assertEquals(7.0, r.state.progressOf("body.measurement_days").value)
        assertTrue(r.summary.backfill)
    }

    @Test
    fun progressTowardNextTier() {
        val engine = AchievementEngine(listOf(volumeDef()))
        val e = workout("w1", "2026-03-10", volumeKg = 5000.0)
        val now = Instant.parse("2026-03-10T18:00:00Z")
        val r = engine.evaluate(EvaluationState(), listOf(e), now, "Europe/London")
        val p = r.state.progressOf("strength.volume_tonnes").progress
        assertEquals(TierId.BRONZE, p.currentTier)
        assertEquals(TierId.SILVER, p.nextTier)
        assertEquals(10000.0, p.nextThreshold)
        assertTrue(p.progressRatio > 0.0 && p.progressRatio < 1.0)
    }

    @Test
    fun swimStrokeVarietyDistinct() {
        val def = AchievementDefinition(
            id = "swimming.stroke_variety",
            category = AchievementCategory.SWIMMING,
            type = AchievementType.DISCOVERY,
            tiers = listOf(
                AchievementTier(TierId.BRONZE, 2.0),
                AchievementTier(TierId.SILVER, 3.0),
            ),
            rule = AchievementRule(
                metric = MetricId.SWIM_STROKES_DISTINCT,
                aggregation = Aggregation.DISTINCT,
                window = RuleWindow(kind = WindowKind.LIFETIME),
                filters = RuleFilters(discipline = listOf("swimming")),
            ),
            iconId = "swimming",
            titleKey = "achievement.swimming.stroke_variety.title",
            descriptionKey = "achievement.swimming.stroke_variety.description",
        )
        val engine = AchievementEngine(listOf(def))
        val events = listOf(
            workout("s1", "2026-08-01", discipline = "swimming", strokes = listOf("freestyle", "backstroke")),
            workout("s2", "2026-08-02", discipline = "swimming", strokes = listOf("breaststroke")),
        )
        val now = Instant.parse("2026-08-02T20:00:00Z")
        val r = engine.evaluate(EvaluationState(), events, now, "Europe/London")
        assertEquals(3.0, r.state.progressOf("swimming.stroke_variety").value)
        assertTrue(TierId.SILVER in r.state.progressOf("swimming.stroke_variety").unlockedTier)
    }
}
