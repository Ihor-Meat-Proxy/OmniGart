package com.omnigart.achievements

import com.omnigart.achievements.dsl.RuleDsl
import com.omnigart.achievements.model.AchievementRule
import com.omnigart.achievements.model.Aggregation
import com.omnigart.achievements.model.MetricId
import com.omnigart.achievements.model.RuleWindow
import com.omnigart.achievements.model.WindowKind
import kotlin.test.Test
import kotlin.test.assertTrue

class RuleDslTest {
    @Test
    fun streakWithoutQualifyFails() {
        val rule = AchievementRule(
            metric = MetricId.WORKOUTS_COMPLETED,
            aggregation = Aggregation.STREAK,
            window = RuleWindow(kind = WindowKind.STREAK_DAYS),
        )
        assertTrue(RuleDsl.validate(rule).isNotEmpty())
    }

    @Test
    fun rollingRequiresDays() {
        val rule = AchievementRule(
            metric = MetricId.WORKOUTS_COMPLETED,
            aggregation = Aggregation.COUNT,
            window = RuleWindow(kind = WindowKind.ROLLING_DAYS),
        )
        assertTrue(RuleDsl.validate(rule).any { it.message.contains("rolling_days") })
    }
}
