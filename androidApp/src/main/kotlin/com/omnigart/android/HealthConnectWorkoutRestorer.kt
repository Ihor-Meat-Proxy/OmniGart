package com.omnigart.android

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import com.omnigart.shared.OmniGartCore
import com.omnigart.shared.health.HealthWorkout
import com.omnigart.shared.health.HeartRateSample
import com.omnigart.shared.health.SdkHealthConnectGateway
import com.omnigart.shared.health.TimeRange
import com.omnigart.shared.health.WorkoutActivityType
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * User-initiated restore of a local workout removed from Health Connect.
 * Append-only: exist-check by stable clientRecordId (EPOCH read) → [insertRecords] only when
 * absent → never update/delete and never bump clientRecordVersion (always 1, same as weigh-ins).
 */
object HealthConnectWorkoutRestorer {
    enum class Outcome {
        RESTORED,
        ALREADY_PRESENT,
        SKIPPED,
        PERMISSION_NEEDED,
        FAILED
    }

    fun writePermissions(): Set<String> = setOf(
        HealthPermission.getWritePermission(ExerciseSessionRecord::class),
        HealthPermission.getWritePermission(HeartRateRecord::class),
        HealthPermission.getWritePermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getWritePermission(DistanceRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class)
    )

    suspend fun hasWriteAccess(context: Context): Boolean = withContext(Dispatchers.IO) {
        if (!SdkHealthConnectGateway.isAvailable(context)) return@withContext false
        val granted =
            HealthConnectClient.getOrCreate(context).permissionController.getGrantedPermissions()
        granted.containsAll(writePermissions())
    }

    @Suppress("ReturnCount", "CyclomaticComplexMethod")
    suspend fun restore(context: Context, core: OmniGartCore, localId: String): Outcome {
        if (!core.localHealth.isRemovedFromPlatform(localId)) return Outcome.SKIPPED
        if (!SdkHealthConnectGateway.isAvailable(context)) return Outcome.FAILED
        if (!hasWriteAccess(context)) return Outcome.PERMISSION_NEEDED
        if (!core.localHealth.prepareRestore(localId)) return Outcome.SKIPPED
        return runCatching { performInsert(context, core, localId) }
            .getOrElse { err ->
                if (err is SecurityException) Outcome.PERMISSION_NEEDED else Outcome.FAILED
            }
    }

    private suspend fun performInsert(context: Context, core: OmniGartCore, localId: String): Outcome {
        return withContext(Dispatchers.IO) {
            val syncId =
                core.localHealth.restoreSyncIdentifier(localId) ?: return@withContext Outcome.SKIPPED
            val start = Instant.fromEpochMilliseconds(core.localHealth.workoutStartEpochMs(localId))
            val end = Instant.fromEpochMilliseconds(core.localHealth.workoutEndEpochMs(localId))
            val gateway = SdkHealthConnectGateway(context)
            if (gateway.clientRecordIdExists(syncId)) {
                core.localHealth.reconcileAlreadyOnPlatform(localId, null)
                return@withContext Outcome.ALREADY_PRESENT
            }
            val workout = buildWorkout(core, localId, syncId, start, end)
            gateway.writeWorkouts(listOf(workout))
            val written =
                gateway.readWorkouts(TimeRange(start, end))
                    .firstOrNull { it.id == workout.id || it.sourceName == workout.sourceName }
            core.localHealth.completeRestore(localId, written?.id ?: syncId)
            Outcome.RESTORED
        }
    }

    private fun buildWorkout(core: OmniGartCore, localId: String, syncId: String, start: Instant, end: Instant): HealthWorkout {
        val platformLocalId = syncId.removePrefix("omnigart.workout.").ifBlank { localId }
        val activity =
            runCatching {
                WorkoutActivityType.valueOf(core.localHealth.workoutActivityTypeName(localId) ?: "OTHER")
            }.getOrDefault(WorkoutActivityType.OTHER)
        val hrEpoch = core.localHealth.workoutHeartRateEpochMsList(localId)
        val hrBpm = core.localHealth.workoutHeartRateBpmList(localId)
        val hr =
            hrEpoch.indices.mapNotNull { i ->
                val bpm = hrBpm.getOrNull(i) ?: return@mapNotNull null
                if (bpm <= 0) return@mapNotNull null
                HeartRateSample(bpm, Instant.fromEpochMilliseconds(hrEpoch[i]))
            }
        return HealthWorkout(
            id = platformLocalId,
            activityType = activity,
            start = start,
            end = end,
            energyKcal = core.localHealth.workoutActiveEnergyKcal(localId),
            basalEnergyKcal = core.localHealth.workoutBasalEnergyKcal(localId),
            distanceMetres = core.localHealth.workoutDistanceMetres(localId),
            averageHeartRateBpm = core.localHealth.workoutAverageHeartRateBpm(localId),
            sourceName = core.localHealth.workoutSourceName(localId) ?: "OmniGart",
            clientRecordVersion = SdkHealthConnectGateway.HEALTH_CONNECT_CLIENT_RECORD_VERSION,
            heartRateSamples = hr
        )
    }

    suspend fun restoreAll(context: Context, core: OmniGartCore): Pair<Int, Int> {
        var ok = 0
        var failed = 0
        for (id in core.localHealth.removedFromPlatformIds()) {
            when (restore(context, core, id)) {
                Outcome.RESTORED, Outcome.ALREADY_PRESENT -> ok += 1
                Outcome.SKIPPED -> Unit
                Outcome.PERMISSION_NEEDED, Outcome.FAILED -> failed += 1
            }
        }
        return ok to failed
    }
}
