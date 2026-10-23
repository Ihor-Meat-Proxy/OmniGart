package com.omnigart.achievements

import kotlin.time.Instant

/**
 * Normalised metric contribution extracted from a [DomainEvent] for rule matching.
 */
data class MetricContribution(
    val metric: String,
    val amount: Double,
    val occurredAt: Instant,
    val timeZoneId: String,
    val attributes: Map<String, String> = emptyMap(),
    /** When true, this day may continue a streak without counting as activity. */
    val isPlannedRest: Boolean = false,
    val isStreakFreeze: Boolean = false,
    val isThresholdCandidate: Boolean = false,
    val isPersonalRecord: Boolean = false,
)

object MetricExtractor {
    fun extract(event: DomainEvent): List<MetricContribution> =
        when (event) {
            is DomainEvent.WorkoutCompleted -> {
                val attrs = mapOf("discipline" to event.discipline)
                buildList {
                    add(
                        MetricContribution(
                            metric = "workout_completed",
                            amount = 1.0,
                            occurredAt = event.occurredAt,
                            timeZoneId = event.timeZoneId,
                            attributes = attrs,
                        ),
                    )
                    if (event.planned) {
                        add(
                            MetricContribution(
                                metric = "planned_session_completed",
                                amount = 1.0,
                                occurredAt = event.occurredAt,
                                timeZoneId = event.timeZoneId,
                                attributes = attrs,
                            ),
                        )
                    }
                    if (event.distanceM > 0) {
                        add(
                            MetricContribution(
                                metric = "distance_m",
                                amount = event.distanceM,
                                occurredAt = event.occurredAt,
                                timeZoneId = event.timeZoneId,
                                attributes = attrs,
                                isThresholdCandidate = true,
                            ),
                        )
                    }
                    if (event.durationMin > 0) {
                        add(
                            MetricContribution(
                                metric = "duration_min",
                                amount = event.durationMin,
                                occurredAt = event.occurredAt,
                                timeZoneId = event.timeZoneId,
                                attributes = attrs,
                                isThresholdCandidate = true,
                            ),
                        )
                    }
                    add(
                        MetricContribution(
                            metric = "training_or_rest_day",
                            amount = 1.0,
                            occurredAt = event.occurredAt,
                            timeZoneId = event.timeZoneId,
                            attributes = attrs,
                        ),
                    )
                    add(
                        MetricContribution(
                            metric = "active_or_rest_day",
                            amount = 1.0,
                            occurredAt = event.occurredAt,
                            timeZoneId = event.timeZoneId,
                        ),
                    )
                    add(
                        MetricContribution(
                            metric = "active_day",
                            amount = 1.0,
                            occurredAt = event.occurredAt,
                            timeZoneId = event.timeZoneId,
                        ),
                    )
                    add(
                        MetricContribution(
                            metric = "distinct_disciplines_week",
                            amount = 1.0,
                            occurredAt = event.occurredAt,
                            timeZoneId = event.timeZoneId,
                            attributes = attrs,
                        ),
                    )
                }
            }

            is DomainEvent.SetLogged -> listOf(
                MetricContribution(
                    metric = "set_logged",
                    amount = 1.0,
                    occurredAt = event.occurredAt,
                    timeZoneId = event.timeZoneId,
                    attributes = mapOf("discipline" to event.discipline),
                ),
                MetricContribution(
                    metric = "load_kg",
                    amount = event.loadKg,
                    occurredAt = event.occurredAt,
                    timeZoneId = event.timeZoneId,
                    attributes = mapOf("discipline" to event.discipline),
                    isThresholdCandidate = true,
                ),
            )

            is DomainEvent.PersonalRecord -> listOf(
                MetricContribution(
                    metric = "personal_record",
                    amount = 1.0,
                    occurredAt = event.occurredAt,
                    timeZoneId = event.timeZoneId,
                    attributes = mapOf("discipline" to event.discipline),
                    isPersonalRecord = true,
                ),
                MetricContribution(
                    metric = event.metric,
                    amount = event.value,
                    occurredAt = event.occurredAt,
                    timeZoneId = event.timeZoneId,
                    attributes = mapOf("discipline" to event.discipline),
                    isPersonalRecord = true,
                ),
            )

            is DomainEvent.WaterLogged -> {
                val goalMet = event.goalMl > 0 && event.dailyTotalMl >= event.goalMl
                buildList {
                    add(
                        MetricContribution(
                            metric = "water_logged",
                            amount = 1.0,
                            occurredAt = event.occurredAt,
                            timeZoneId = event.timeZoneId,
                        ),
                    )
                    if (goalMet) {
                        add(
                            MetricContribution(
                                metric = "water_goal_day",
                                amount = 1.0,
                                occurredAt = event.occurredAt,
                                timeZoneId = event.timeZoneId,
                            ),
                        )
                        add(
                            MetricContribution(
                                metric = "active_or_rest_day",
                                amount = 1.0,
                                occurredAt = event.occurredAt,
                                timeZoneId = event.timeZoneId,
                            ),
                        )
                        add(
                            MetricContribution(
                                metric = "active_day",
                                amount = 1.0,
                                occurredAt = event.occurredAt,
                                timeZoneId = event.timeZoneId,
                            ),
                        )
                    }
                }
            }

            is DomainEvent.MealLogged -> {
                buildList {
                    add(
                        MetricContribution(
                            metric = "meal_logged",
                            amount = 1.0,
                            occurredAt = event.occurredAt,
                            timeZoneId = event.timeZoneId,
                        ),
                    )
                    add(
                        MetricContribution(
                            metric = "nutrition_day",
                            amount = 1.0,
                            occurredAt = event.occurredAt,
                            timeZoneId = event.timeZoneId,
                        ),
                    )
                    add(
                        MetricContribution(
                            metric = "active_or_rest_day",
                            amount = 1.0,
                            occurredAt = event.occurredAt,
                            timeZoneId = event.timeZoneId,
                        ),
                    )
                    add(
                        MetricContribution(
                            metric = "active_day",
                            amount = 1.0,
                            occurredAt = event.occurredAt,
                            timeZoneId = event.timeZoneId,
                        ),
                    )
                    val protein = event.proteinG
                    val target = event.proteinTargetG
                    if (protein != null && target != null && target > 0 && protein >= target) {
                        add(
                            MetricContribution(
                                metric = "protein_target_day",
                                amount = 1.0,
                                occurredAt = event.occurredAt,
                                timeZoneId = event.timeZoneId,
                            ),
                        )
                    }
                }
            }

            is DomainEvent.SleepImported -> listOf(
                MetricContribution(
                    metric = "sleep_imported",
                    amount = 1.0,
                    occurredAt = event.occurredAt,
                    timeZoneId = event.timeZoneId,
                ),
                MetricContribution(
                    metric = "sleep_logged_day",
                    amount = 1.0,
                    occurredAt = event.occurredAt,
                    timeZoneId = event.timeZoneId,
                ),
                MetricContribution(
                    metric = "active_or_rest_day",
                    amount = 1.0,
                    occurredAt = event.occurredAt,
                    timeZoneId = event.timeZoneId,
                ),
                MetricContribution(
                    metric = "active_day",
                    amount = 1.0,
                    occurredAt = event.occurredAt,
                    timeZoneId = event.timeZoneId,
                ),
            )

            is DomainEvent.HrvLogged -> listOf(
                MetricContribution(
                    metric = "hrv_logged",
                    amount = 1.0,
                    occurredAt = event.occurredAt,
                    timeZoneId = event.timeZoneId,
                ),
            )

            is DomainEvent.StressCheckin -> listOf(
                MetricContribution(
                    metric = "stress_checkin",
                    amount = 1.0,
                    occurredAt = event.occurredAt,
                    timeZoneId = event.timeZoneId,
                ),
            )

            is DomainEvent.MeasurementLogged -> listOf(
                MetricContribution(
                    metric = "measurement_logged",
                    amount = 1.0,
                    occurredAt = event.occurredAt,
                    timeZoneId = event.timeZoneId,
                    attributes = mapOf("site" to event.site),
                ),
                MetricContribution(
                    metric = "measurement_week_logged",
                    amount = 1.0,
                    occurredAt = event.occurredAt,
                    timeZoneId = event.timeZoneId,
                ),
                MetricContribution(
                    metric = "measurement_week_day",
                    amount = 1.0,
                    occurredAt = event.occurredAt,
                    timeZoneId = event.timeZoneId,
                ),
                MetricContribution(
                    metric = "distinct_measurement_sites",
                    amount = 1.0,
                    occurredAt = event.occurredAt,
                    timeZoneId = event.timeZoneId,
                    attributes = mapOf("site" to event.site),
                ),
                MetricContribution(
                    metric = "active_or_rest_day",
                    amount = 1.0,
                    occurredAt = event.occurredAt,
                    timeZoneId = event.timeZoneId,
                ),
                MetricContribution(
                    metric = "active_day",
                    amount = 1.0,
                    occurredAt = event.occurredAt,
                    timeZoneId = event.timeZoneId,
                ),
            )

            is DomainEvent.PlannedRestDay -> {
                val attrs = event.discipline?.let { mapOf("discipline" to it) } ?: emptyMap()
                buildList {
                    add(
                        MetricContribution(
                            metric = "training_or_rest_day",
                            amount = 1.0,
                            occurredAt = event.occurredAt,
                            timeZoneId = event.timeZoneId,
                            attributes = attrs,
                            isPlannedRest = true,
                        ),
                    )
                    add(
                        MetricContribution(
                            metric = "active_or_rest_day",
                            amount = 1.0,
                            occurredAt = event.occurredAt,
                            timeZoneId = event.timeZoneId,
                            isPlannedRest = true,
                        ),
                    )
                    if (event.lowReadiness) {
                        add(
                            MetricContribution(
                                metric = "rest_taken_on_low_readiness",
                                amount = 1.0,
                                occurredAt = event.occurredAt,
                                timeZoneId = event.timeZoneId,
                                attributes = attrs,
                            ),
                        )
                    }
                }
            }

            is DomainEvent.StreakFreeze -> listOf(
                MetricContribution(
                    metric = event.metric ?: "active_or_rest_day",
                    amount = 1.0,
                    occurredAt = event.occurredAt,
                    timeZoneId = event.timeZoneId,
                    isStreakFreeze = true,
                ),
                MetricContribution(
                    metric = "active_or_rest_day",
                    amount = 1.0,
                    occurredAt = event.occurredAt,
                    timeZoneId = event.timeZoneId,
                    isStreakFreeze = true,
                ),
            )

            is DomainEvent.ChallengeCompleted -> listOf(
                MetricContribution(
                    metric = "challenge_completed",
                    amount = 1.0,
                    occurredAt = event.occurredAt,
                    timeZoneId = event.timeZoneId,
                    attributes = mapOf("challengeType" to event.challengeType),
                ),
            )

            is DomainEvent.WeeklyGoalMet -> listOf(
                MetricContribution(
                    metric = "weekly_goal_met",
                    amount = 1.0,
                    occurredAt = event.occurredAt,
                    timeZoneId = event.timeZoneId,
                    attributes = mapOf("weekStartIso" to event.weekStartIso),
                ),
            )
        }
}
