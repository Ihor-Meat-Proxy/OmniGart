package com.omnigart.shared.health.store

import com.omnigart.shared.health.HealthConnectClient
import com.omnigart.shared.health.TimeRange
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Pulls Health Connect exercise sessions into the OmniGart local store.
 * Does not write foreign-origin records back to Health Connect.
 */
class HealthConnectLocalSync(
    private val bridge: LocalHealthBridge,
    private val client: HealthConnectClient,
    private val clock: Clock = Clock.System
) {
    suspend fun importRecent(daysBack: Long = 30) {
        val end = clock.now()
        val start = Instant.fromEpochMilliseconds(end.toEpochMilliseconds() - daysBack * 86_400_000L)
        val workouts = client.readWorkouts(TimeRange(start, end))
        workouts.forEach { hw ->
            val syncId = "omnigart.workout.${hw.id}"
            bridge.importPlatformWorkout(
                localId = hw.id,
                syncIdentifier = syncId,
                syncVersion = 1,
                activityTypeName = hw.activityType.name,
                startEpochMs = hw.start.toEpochMilliseconds(),
                endEpochMs = hw.end.toEpochMilliseconds(),
                activeEnergyKcal = hw.energyKcal,
                basalEnergyKcal = null,
                distanceMetres = hw.distanceMetres,
                averageHeartRateBpm = hw.averageHeartRateBpm,
                maxHeartRateBpm = null,
                platform = "HEALTH_CONNECT",
                sourceBundle = null,
                sourceName = hw.sourceName,
                platformRecordId = hw.id,
                planId = null,
                heartRateEpochMs = longArrayOf(),
                heartRateBpm = doubleArrayOf()
            )
        }
        val token =
            runCatching {
                client.getOrCreateChangesToken(
                    setOf(com.omnigart.shared.health.HealthDataType.WORKOUT)
                ).value
            }.getOrNull()
        bridge.markSyncStatus(
            platform = "HEALTH_CONNECT",
            authStatus = "AUTHORIZED",
            changeToken = token
        )
    }
}
