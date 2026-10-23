package com.omnigart.shared.workoutkit

import com.omnigart.activity.hr.Zone2
import com.omnigart.activity.hr.Zone2Method

/**
 * Builds [ScheduledWorkoutPlan] values for Zone 2 continuous runs scheduled via WorkoutKit.
 *
 * HR alert bounds come from [Zone2.profile] / [Zone2.karvonen] so the system Workout app
 * band matches the in-app watch Zone 2 session.
 */
object Zone2WorkoutKitPlans {
    const val DEFAULT_DISPLAY_NAME_KEY: String = "running.session.zone2"

    /**
     * Continuous outdoor run with warm-up / work / cool-down and a Zone 2 HR alert on work.
     *
     * @param hrMax measured or estimated maximal heart rate (bpm)
     * @param hrRest resting heart rate (bpm)
     * @param method Zone 2 model; default Karvonen (%HRR)
     * @param lthrBpm required when [method] is [Zone2Method.LTHR]
     * @param workSeconds work-block duration
     * @param scheduledStartEpochMillis optional schedule time (epoch millis)
     */
    fun continuousRun(
        id: String,
        hrMax: Int,
        hrRest: Int,
        workSeconds: Long,
        scheduledStartEpochMillis: Long? = null,
        method: Zone2Method = Zone2Method.HRR_KARVONEN,
        lthrBpm: Int? = null,
        displayNameKey: String = DEFAULT_DISPLAY_NAME_KEY,
        warmUpSeconds: Long = 300,
        coolDownSeconds: Long = 300
    ): ScheduledWorkoutPlan {
        val profile =
            Zone2.profile(
                method = method,
                hrMax = hrMax,
                hrRest = hrRest,
                lthrBpm = lthrBpm
            )
        return EndurancePlanFactory.continuousRun(
            id = id,
            displayNameKey = displayNameKey,
            workSeconds = workSeconds,
            warmUpSeconds = warmUpSeconds,
            coolDownSeconds = coolDownSeconds,
            hrAlertLowerBpm = profile.lowerBpm.toDouble(),
            hrAlertUpperBpm = profile.upperBpm.toDouble(),
            scheduledStartEpochMillis = scheduledStartEpochMillis
        )
    }
}
