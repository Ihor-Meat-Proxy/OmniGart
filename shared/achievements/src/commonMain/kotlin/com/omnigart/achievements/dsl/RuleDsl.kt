package com.omnigart.achievements.dsl

import com.omnigart.achievements.model.AchievementRule
import com.omnigart.achievements.model.Aggregation
import com.omnigart.achievements.model.WindowKind

/**
 * Lightweight declarative rule DSL checks (parse-time / catalogue-time).
 * Full JSON Schema validation lives in content/achievements/tools and jvm ContentValidator.
 */
object RuleDsl {
    data class Issue(val path: String, val message: String)

    fun validate(rule: AchievementRule, path: String = "rule"): List<Issue> {
        val issues = mutableListOf<Issue>()
        when (rule.window.kind) {
            WindowKind.ROLLING_DAYS ->
                if (rule.window.days == null || rule.window.days < 1) {
                    issues += Issue(path, "rolling_days requires positive days")
                }
            WindowKind.CALENDAR ->
                if (rule.window.unit == null) {
                    issues += Issue(path, "calendar window requires unit")
                }
            WindowKind.STREAK_DAYS -> {
                if (rule.window.qualify == null) {
                    issues += Issue(path, "streak_days requires qualify")
                }
                if (rule.aggregation != Aggregation.STREAK && rule.aggregation != Aggregation.COUNT) {
                    issues += Issue(path, "streak_days usually uses aggregation=streak")
                }
            }
            WindowKind.CHALLENGE_PERIOD ->
                if (rule.window.period == null) {
                    issues += Issue(path, "challenge_period requires period")
                }
            WindowKind.LIFETIME, WindowKind.SINGLE_EVENT -> Unit
        }
        if (rule.aggregation == Aggregation.STREAK && rule.window.kind != WindowKind.STREAK_DAYS) {
            issues += Issue(path, "aggregation streak requires window.kind=streak_days")
        }
        return issues
    }
}
