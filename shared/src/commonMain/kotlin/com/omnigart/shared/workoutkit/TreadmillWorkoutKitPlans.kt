package com.omnigart.shared.workoutkit

import com.omnigart.shared.health.WorkoutActivityType
import com.omnigart.treadmill.program.TreadmillActivityMode
import com.omnigart.treadmill.program.TreadmillProgram
import com.omnigart.treadmill.program.TreadmillProgramCatalog
import com.omnigart.treadmill.program.TreadmillStepGoalKind

/**
 * Maps built-in treadmill programs to [ScheduledWorkoutPlan] for WorkoutKit custom workouts.
 * Indoor location is encoded in JSON as `location=indoor` for the Swift builder.
 */
object TreadmillWorkoutKitPlans {
    fun allPlans(
        hrMax: Int,
        hrRest: Int? = null,
        scheduledStartEpochMillis: Long? = null
    ): List<ScheduledWorkoutPlan> =
        TreadmillProgramCatalog.all.map { toPlan(it, hrMax, hrRest, scheduledStartEpochMillis) }

    fun toPlan(
        program: TreadmillProgram,
        hrMax: Int,
        hrRest: Int? = null,
        scheduledStartEpochMillis: Long? = null
    ): ScheduledWorkoutPlan {
        val activity =
            when (program.mode) {
                TreadmillActivityMode.RUN -> WorkoutActivityType.RUNNING
                TreadmillActivityMode.WALK -> WorkoutActivityType.WALKING
            }
        val blocks =
            program.steps.map { step ->
                val goal =
                    when (step.goalKind) {
                        TreadmillStepGoalKind.DURATION_SECONDS ->
                            WorkoutGoalSpec(WorkoutGoalKind.TIME_SECONDS, step.goalValue)
                        TreadmillStepGoalKind.DISTANCE_METRES ->
                            WorkoutGoalSpec(WorkoutGoalKind.DISTANCE_METRES, step.goalValue)
                    }
                val alerts =
                    step.targetHrZone?.let { zone ->
                        val bands = percentBands(hrMax, hrRest)
                        val idx = (zone - 1).coerceIn(0, bands.lastIndex)
                        listOf(
                            WorkoutAlertSpec(
                                kind = WorkoutAlertKind.HEART_RATE_ZONE,
                                lower = bands[idx].first,
                                upper = bands[idx].second
                            )
                        )
                    } ?: emptyList()
                WorkoutBlockSpec(
                    kind = WorkoutBlockKind.WORK,
                    displayNameKey = step.titleKey,
                    goal = goal,
                    alerts = alerts,
                    repeatCount = 1
                )
            }
        return ScheduledWorkoutPlan(
            id = "treadmill.${program.id}",
            activityType = activity,
            displayNameKey = program.titleKey,
            blocks = blocks,
            scheduledStartEpochMillis = scheduledStartEpochMillis
        )
    }

    /** Five %HRmax bands; Karvonen when resting HR is known. */
    private fun percentBands(hrMax: Int, hrRest: Int?): List<Pair<Double, Double>> {
        val bounds = listOf(0.50, 0.60, 0.70, 0.80, 0.90, 1.00)
        return if (hrRest != null && hrRest in 30 until hrMax) {
            val reserve = (hrMax - hrRest).toDouble()
            (0 until 5).map { i ->
                (hrRest + bounds[i] * reserve) to (hrRest + bounds[i + 1] * reserve)
            }
        } else {
            (0 until 5).map { i ->
                (hrMax * bounds[i]) to (hrMax * bounds[i + 1])
            }
        }
    }
}
