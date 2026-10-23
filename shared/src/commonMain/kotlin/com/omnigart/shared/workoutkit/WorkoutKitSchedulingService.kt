package com.omnigart.shared.workoutkit

import com.omnigart.shared.compliance.FeatureFlags
import com.omnigart.shared.health.AuthStatus
import com.omnigart.shared.health.HealthClient
import com.omnigart.shared.health.HealthPermission
import com.omnigart.shared.health.TimeRange

/**
 * Schedules validated plans through a platform [WorkoutKitSchedulerGateway]
 * and optionally imports completed workouts for load/recovery.
 */
class WorkoutKitSchedulingService(private val gateway: WorkoutKitSchedulerGateway, private val health: HealthClient? = null) {
    fun isSchedulingEnabled(): Boolean = FeatureFlags.workoutKitSchedulingEnabled

    suspend fun schedule(plan: ScheduledWorkoutPlan): ScheduleResult {
        if (!FeatureFlags.workoutKitSchedulingEnabled) {
            return ScheduleResult(
                ok = false,
                failure = ScheduleFailureReason.FEATURE_DISABLED,
                detail = "workoutKitSchedulingEnabled is false"
            )
        }
        val validated = ScheduledWorkoutValidator.validate(plan)
        if (!validated.ok) return validated

        if (!gateway.isSupported()) {
            return ScheduleResult(
                ok = false,
                failure = ScheduleFailureReason.UNSUPPORTED_ON_PLATFORM,
                detail = "WorkoutKit unavailable (OS or Personal Team capability gate)"
            )
        }

        val auth = gateway.requestAuthorization()
        if (auth != AuthStatus.AUTHORIZED) {
            return ScheduleResult(
                ok = false,
                failure = ScheduleFailureReason.AUTHORIZATION_DENIED,
                detail = auth.name
            )
        }

        val count = gateway.scheduledCount()
        val cap = ScheduledWorkoutValidator.checkScheduleCap(count)
        if (!cap.ok) return cap

        return gateway.schedule(plan)
    }

    /**
     * Reads completed workouts in [range] and maps them to load contributions.
     * Requires [FeatureFlags.healthWorkoutImportEnabled] (independent of full health sync).
     */
    suspend fun importLoadContributions(
        range: TimeRange,
        restingHrBpm: Double? = null,
        maxHrBpm: Double? = null
    ): List<WorkoutLoadContribution> {
        if (!FeatureFlags.healthWorkoutImportEnabled) return emptyList()
        val client = health ?: return emptyList()
        client.requestAuthorization(setOf(HealthPermission.READ_WORKOUT))
        return client.readWorkouts(range).map {
            CompletedWorkoutLoad.fromHealthWorkout(it, restingHrBpm, maxHrBpm)
        }
    }
}

/** Platform bridge (Swift WorkoutKit on Apple; no-op elsewhere). */
interface WorkoutKitSchedulerGateway {
    fun isSupported(): Boolean

    suspend fun requestAuthorization(): AuthStatus

    suspend fun scheduledCount(): Int

    suspend fun schedule(plan: ScheduledWorkoutPlan): ScheduleResult

    suspend fun removeAllScheduled(): ScheduleResult
}

/** Default gateway for JVM/Android tests and non-Apple targets. */
class UnsupportedWorkoutKitSchedulerGateway : WorkoutKitSchedulerGateway {
    override fun isSupported(): Boolean = false

    override suspend fun requestAuthorization(): AuthStatus = AuthStatus.DENIED

    override suspend fun scheduledCount(): Int = 0

    override suspend fun schedule(plan: ScheduledWorkoutPlan): ScheduleResult =
        ScheduleResult(ok = false, failure = ScheduleFailureReason.UNSUPPORTED_ON_PLATFORM)

    override suspend fun removeAllScheduled(): ScheduleResult =
        ScheduleResult(ok = false, failure = ScheduleFailureReason.UNSUPPORTED_ON_PLATFORM)
}
