package com.omnigart.shared.health.store

import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Per-record conflict policy when Apple Health and Health Connect (or local vs platform)
 * disagree for the same period.
 *
 * Rules (never silent overwrite):
 * 1. Prefer the original source ([HealthRecordOrigin]).
 * 2. Keep both values with provenance via [HealthConflictRecord].
 * 3. Surface conflicts for user review.
 * 4. Local store always retains the richest merged copy.
 */
object HealthConflictPolicy {
    data class Diff(val fieldName: String, val localValue: String?, val platformValue: String?)

    fun preferredSourceLabel(origin: HealthRecordOrigin): String = when (origin) {
        HealthRecordOrigin.OMNIGART -> "omnigart"
        HealthRecordOrigin.APPLE_HEALTH -> "apple_health"
        HealthRecordOrigin.HEALTH_CONNECT -> "health_connect"
        HealthRecordOrigin.OMNIGART_JSON_IMPORT -> "omnigart_json"
    }

    fun findScalarConflicts(
        local: LocalHealthWorkout,
        platform: LocalHealthWorkout,
        platformKind: HealthSyncPlatform
    ): List<HealthConflictRecord> {
        val diffs = mutableListOf<Diff>()
        if (!HealthDataMapper.nearlyEqual(local.activeEnergyKcal, platform.activeEnergyKcal, 0.05)) {
            diffs += Diff(
                "activeEnergyKcal",
                local.activeEnergyKcal?.toString(),
                platform.activeEnergyKcal?.toString()
            )
        }
        if (!HealthDataMapper.nearlyEqual(local.basalEnergyKcal, platform.basalEnergyKcal, 0.05)) {
            diffs += Diff(
                "basalEnergyKcal",
                local.basalEnergyKcal?.toString(),
                platform.basalEnergyKcal?.toString()
            )
        }
        if (!HealthDataMapper.nearlyEqual(local.distanceMetres, platform.distanceMetres, 0.5)) {
            diffs += Diff(
                "distanceMetres",
                local.distanceMetres?.toString(),
                platform.distanceMetres?.toString()
            )
        }
        if (local.steps != platform.steps && (local.steps != null || platform.steps != null)) {
            diffs += Diff("steps", local.steps?.toString(), platform.steps?.toString())
        }
        if (local.activityType != platform.activityType) {
            diffs += Diff("activityType", local.activityType.name, platform.activityType.name)
        }
        val now = Clock.System.now()
        return diffs.mapIndexed { index, diff ->
            HealthConflictRecord(
                id = "conflict-${local.id}-${platformKind.name}-${diff.fieldName}-$index",
                workoutId = local.id,
                platform = platformKind,
                fieldName = diff.fieldName,
                localValue = diff.localValue,
                platformValue = diff.platformValue,
                preferredSource = preferredSourceLabel(local.origin),
                resolved = false,
                createdAt = now
            )
        }
    }

    /**
     * Apply preference: keep local scalars from original source; still merge richest series.
     */
    fun resolvePreferringOrigin(local: LocalHealthWorkout, platform: LocalHealthWorkout): LocalHealthWorkout {
        val merged = HealthDataMapper.mergeRichest(local, platform)
        return when (local.origin) {
            HealthRecordOrigin.OMNIGART,
            HealthRecordOrigin.OMNIGART_JSON_IMPORT ->
                merged.copy(
                    activeEnergyKcal = local.activeEnergyKcal ?: merged.activeEnergyKcal,
                    basalEnergyKcal = local.basalEnergyKcal ?: merged.basalEnergyKcal,
                    distanceMetres = local.distanceMetres ?: merged.distanceMetres,
                    steps = local.steps ?: merged.steps,
                    activityType = local.activityType
                )
            HealthRecordOrigin.APPLE_HEALTH,
            HealthRecordOrigin.HEALTH_CONNECT ->
                merged.copy(
                    activeEnergyKcal = platform.activeEnergyKcal ?: merged.activeEnergyKcal,
                    basalEnergyKcal = platform.basalEnergyKcal ?: merged.basalEnergyKcal,
                    distanceMetres = platform.distanceMetres ?: merged.distanceMetres,
                    steps = platform.steps ?: merged.steps,
                    activityType = if (platform.activityType !=
                        com.omnigart.shared.health.WorkoutActivityType.OTHER
                    ) {
                        platform.activityType
                    } else {
                        local.activityType
                    }
                )
        }
    }

    fun withTimestamp(record: HealthConflictRecord, at: Instant): HealthConflictRecord = record.copy(createdAt = at)
}
