package com.omnigart.shared.health.store

import com.omnigart.shared.health.WorkoutActivityType
import com.omnigart.shared.health.store.device.DeviceTransferIngestResult
import com.omnigart.shared.health.store.device.DeviceWorkoutTransferCodec
import com.omnigart.shared.health.store.device.DeviceWorkoutTransferDocument
import com.omnigart.shared.health.store.export.HealthExportArtifact
import com.omnigart.shared.health.store.export.HealthExportService
import com.omnigart.shared.health.store.pdf.WorkoutPdfBuilder
import com.omnigart.shared.health.store.pdf.WorkoutPdfDocument
import com.omnigart.shared.health.store.pdf.WorkoutPdfLabelCatalog
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Primitive-friendly façade for Apple/Android shells (avoids constructing nested
 * Kotlin data classes from Swift).
 */
@Suppress("TooManyFunctions")
class LocalHealthBridge(
    private val store: LocalHealthStore,
    private val sync: HealthSyncEngine,
    private val export: HealthExportService,
    private val clock: Clock = Clock.System
) {
    /**
     * Persist a session OmniGart just finished. Always writes locally.
     * Returns syncIdentifier for platform metadata.
     */
    fun recordOmniGartWorkout(
        localId: String,
        activityTypeName: String,
        startEpochMs: Long,
        endEpochMs: Long,
        activeEnergyKcal: Double?,
        basalEnergyKcal: Double?,
        distanceMetres: Double?,
        averageHeartRateBpm: Double?,
        maxHeartRateBpm: Double?,
        sessionId: String?,
        planId: String?,
        heartRateEpochMs: LongArray,
        heartRateBpm: DoubleArray,
        syncVersion: Long = 1
    ): String {
        require(heartRateEpochMs.size == heartRateBpm.size)
        val hr =
            heartRateEpochMs.indices.map { i ->
                LocalHeartRatePoint(
                    recordedAt = Instant.fromEpochMilliseconds(heartRateEpochMs[i]),
                    beatsPerMinute = heartRateBpm[i]
                )
            }
        val activity =
            runCatching { WorkoutActivityType.valueOf(activityTypeName) }
                .getOrDefault(WorkoutActivityType.OTHER)
        val syncId = HealthDataMapper.syncIdentifierFor(localId)
        val workout =
            LocalHealthWorkout(
                id = localId,
                identity = HealthSyncIdentity(syncId, syncVersion),
                activityType = activity,
                startAt = Instant.fromEpochMilliseconds(startEpochMs),
                endAt = Instant.fromEpochMilliseconds(endEpochMs),
                activeEnergyKcal = activeEnergyKcal,
                basalEnergyKcal = basalEnergyKcal,
                distanceMetres = distanceMetres,
                averageHeartRateBpm = averageHeartRateBpm,
                maxHeartRateBpm = maxHeartRateBpm,
                planId = planId,
                sessionId = sessionId,
                origin = HealthRecordOrigin.OMNIGART,
                sourceBundle = HealthDataMapper.SOURCE_BUNDLE_OMNIGART,
                sourceName = "OmniGart",
                updatedAt = clock.now(),
                heartRateSeries = hr
            )
        sync.recordLocally(workout)
        return syncId
    }

    fun importPlatformWorkout(
        localId: String,
        syncIdentifier: String,
        syncVersion: Long,
        activityTypeName: String,
        startEpochMs: Long,
        endEpochMs: Long,
        activeEnergyKcal: Double?,
        basalEnergyKcal: Double?,
        distanceMetres: Double?,
        averageHeartRateBpm: Double?,
        maxHeartRateBpm: Double?,
        platform: String,
        sourceBundle: String?,
        sourceName: String?,
        platformRecordId: String?,
        planId: String?,
        heartRateEpochMs: LongArray,
        heartRateBpm: DoubleArray
    ) {
        require(heartRateEpochMs.size == heartRateBpm.size)
        val hr =
            heartRateEpochMs.indices.map { i ->
                LocalHeartRatePoint(
                    recordedAt = Instant.fromEpochMilliseconds(heartRateEpochMs[i]),
                    beatsPerMinute = heartRateBpm[i]
                )
            }
        val origin =
            when (platform.uppercase()) {
                "APPLE_HEALTH" -> HealthRecordOrigin.APPLE_HEALTH
                "HEALTH_CONNECT" -> HealthRecordOrigin.HEALTH_CONNECT
                else -> HealthRecordOrigin.APPLE_HEALTH
            }
        val syncPlatform =
            when (platform.uppercase()) {
                "HEALTH_CONNECT" -> HealthSyncPlatform.HEALTH_CONNECT
                else -> HealthSyncPlatform.APPLE_HEALTH
            }
        val activity =
            runCatching { WorkoutActivityType.valueOf(activityTypeName) }
                .getOrDefault(WorkoutActivityType.OTHER)
        val workout =
            LocalHealthWorkout(
                id = localId,
                identity = HealthSyncIdentity(syncIdentifier, maxOf(1, syncVersion)),
                activityType = activity,
                startAt = Instant.fromEpochMilliseconds(startEpochMs),
                endAt = Instant.fromEpochMilliseconds(endEpochMs),
                activeEnergyKcal = activeEnergyKcal,
                basalEnergyKcal = basalEnergyKcal,
                distanceMetres = distanceMetres,
                averageHeartRateBpm = averageHeartRateBpm,
                maxHeartRateBpm = maxHeartRateBpm,
                planId = planId,
                origin = origin,
                sourceBundle = sourceBundle,
                sourceName = sourceName,
                platformRecordId = platformRecordId,
                updatedAt = clock.now(),
                heartRateSeries = hr
            )
        sync.importFromPlatform(workout, syncPlatform)
    }

    fun markSyncStatus(platform: String, authStatus: String, changeToken: String?) {
        val syncPlatform =
            when (platform.uppercase()) {
                "HEALTH_CONNECT" -> HealthSyncPlatform.HEALTH_CONNECT
                else -> HealthSyncPlatform.APPLE_HEALTH
            }
        sync.markSynced(syncPlatform, authStatus, changeToken)
    }

    /**
     * Platform deleted a sample identified by HealthKit UUID / HC record id and/or sync id.
     * Keeps the OmniGart copy; never deletes platform data.
     */
    fun markRemovedFromPlatform(platform: String, platformRecordId: String?, syncIdentifier: String?) {
        val syncPlatform =
            when (platform.uppercase()) {
                "HEALTH_CONNECT" -> HealthSyncPlatform.HEALTH_CONNECT
                else -> HealthSyncPlatform.APPLE_HEALTH
            }
        sync.applyPlatformDeletion(
            platform = syncPlatform,
            syncIdentifier = syncIdentifier,
            platformRecordId = platformRecordId
        )
    }

    /** After a successful first-time Apple Health / Health Connect insert. */
    fun markPlatformLinked(localId: String, platformRecordId: String) {
        sync.markPlatformLinked(localId, platformRecordId)
    }

    /**
     * Phone-side ingest of a Watch / Wear transfer JSON payload.
     * Returns `INSERTED`, `ALREADY_PRESENT`, or `REJECTED`.
     */
    fun ingestDeviceTransferJson(utf8: String): String = runCatching {
        val doc = DeviceWorkoutTransferCodec.decode(utf8)
        sync.ingestDeviceTransfer(doc).name
    }.getOrDefault(DeviceTransferIngestResult.REJECTED.name)

    /** Encode a locally recorded workout for device transfer (watch / Wear → phone). */
    fun encodeDeviceTransferJson(localId: String, sourceDevice: String): String? {
        val workout = store.getWorkout(localId) ?: return null
        val doc = DeviceWorkoutTransferCodec.fromLocalWorkout(workout, sourceDevice)
        return DeviceWorkoutTransferCodec.encode(doc)
    }

    fun buildDeviceTransferJson(
        localId: String,
        activityTypeName: String,
        startEpochMs: Long,
        endEpochMs: Long,
        activeEnergyKcal: Double?,
        basalEnergyKcal: Double?,
        distanceMetres: Double?,
        averageHeartRateBpm: Double?,
        maxHeartRateBpm: Double?,
        averageCadenceSpm: Double?,
        heartRateEpochMs: LongArray,
        heartRateBpm: DoubleArray,
        sourceDevice: String
    ): String {
        require(heartRateEpochMs.size == heartRateBpm.size)
        val syncId = HealthDataMapper.syncIdentifierFor(localId)
        val doc =
            DeviceWorkoutTransferDocument(
                localId = localId,
                syncIdentifier = syncId,
                syncVersion = 1,
                activityTypeName = activityTypeName,
                startEpochMs = startEpochMs,
                endEpochMs = endEpochMs,
                activeEnergyKcal = activeEnergyKcal,
                basalEnergyKcal = basalEnergyKcal,
                distanceMetres = distanceMetres,
                averageHeartRateBpm = averageHeartRateBpm,
                maxHeartRateBpm = maxHeartRateBpm,
                averageCadenceSpm = averageCadenceSpm,
                heartRateEpochMs = heartRateEpochMs.toList(),
                heartRateBpm = heartRateBpm.toList(),
                sourceDevice = sourceDevice
            )
        return DeviceWorkoutTransferCodec.encode(doc)
    }

    fun softDeleteWorkout(localId: String) {
        store.softDeleteWorkout(localId)
    }

    fun getBySyncIdentifier(syncIdentifier: String): String? = store.getBySyncIdentifier(syncIdentifier)?.id

    fun removedFromPlatformCount(): Int = sync.workoutsEligibleForRestore().size

    fun removedFromPlatformIds(): List<String> = sync.workoutsEligibleForRestore().map { it.id }

    fun isRemovedFromPlatform(localId: String): Boolean =
        store.getWorkout(localId)?.let { PlatformAppendOnlyPolicy.shouldAttemptPlatformRestore(it) } == true

    /**
     * Bump sync version for a restore insert. Returns false when not eligible.
     * Call before platform exist-check + insert.
     */
    fun prepareRestore(localId: String): Boolean = sync.prepareRestore(localId) != null

    fun restoreSyncIdentifier(localId: String): String? = store.getWorkout(localId)?.identity?.syncIdentifier

    fun restoreSyncVersion(localId: String): Long = store.getWorkout(localId)?.identity?.syncVersion ?: 1L

    fun workoutActivityTypeName(localId: String): String? = store.getWorkout(localId)?.activityType?.name

    fun workoutStartEpochMs(localId: String): Long = store.getWorkout(localId)?.startAt?.toEpochMilliseconds() ?: 0L

    fun workoutEndEpochMs(localId: String): Long = store.getWorkout(localId)?.endAt?.toEpochMilliseconds() ?: 0L

    fun workoutActiveEnergyKcal(localId: String): Double? = store.getWorkout(localId)?.activeEnergyKcal

    fun workoutBasalEnergyKcal(localId: String): Double? = store.getWorkout(localId)?.basalEnergyKcal

    fun workoutDistanceMetres(localId: String): Double? = store.getWorkout(localId)?.distanceMetres

    fun workoutAverageHeartRateBpm(localId: String): Double? = store.getWorkout(localId)?.averageHeartRateBpm

    fun workoutMaxHeartRateBpm(localId: String): Double? = store.getWorkout(localId)?.maxHeartRateBpm

    fun workoutSourceName(localId: String): String? = store.getWorkout(localId)?.sourceName

    fun workoutHeartRateEpochMsList(localId: String): List<Long> =
        store.getWorkout(localId)?.heartRateSeries?.map { it.recordedAt.toEpochMilliseconds() }.orEmpty()

    fun workoutHeartRateBpmList(localId: String): List<Double> =
        store.getWorkout(localId)?.heartRateSeries?.map { it.beatsPerMinute }.orEmpty()

    /** Swift-friendly primitive arrays (KotlinLongArray / KotlinDoubleArray). */
    fun workoutHeartRateEpochMs(localId: String): LongArray = workoutHeartRateEpochMsList(localId).toLongArray()

    fun workoutHeartRateBpm(localId: String): DoubleArray = workoutHeartRateBpmList(localId).toDoubleArray()

    fun workoutEventTypesList(localId: String): List<String> = store.getWorkout(localId)?.events?.map { it.type.name }.orEmpty()

    fun workoutEventStartEpochMsList(localId: String): List<Long> =
        store.getWorkout(localId)?.events?.map { it.startAt.toEpochMilliseconds() }.orEmpty()

    fun workoutEventEndEpochMsList(localId: String): List<Long> = store.getWorkout(localId)?.events?.map {
        it.endAt?.toEpochMilliseconds() ?: it.startAt.toEpochMilliseconds()
    }.orEmpty()

    fun workoutEventLabelsList(localId: String): List<String> = store.getWorkout(localId)?.events?.map { it.label.orEmpty() }.orEmpty()

    fun workoutEventStartEpochMs(localId: String): LongArray = workoutEventStartEpochMsList(localId).toLongArray()

    fun workoutEventEndEpochMs(localId: String): LongArray = workoutEventEndEpochMsList(localId).toLongArray()

    fun workoutDetailTitle(localId: String): String {
        val w = store.getWorkout(localId) ?: return localId
        return "${w.activityType.name} · ${w.startAt}"
    }

    fun workoutDetailSubtitle(localId: String): String {
        val w = store.getWorkout(localId) ?: return ""
        val kcal = w.activeEnergyKcal?.let { "${it.toLong()} kcal" } ?: "—"
        val removed = if (w.removedFromPlatform) "removed" else "local"
        return "$kcal · $removed"
    }

    /** After a successful restore insert. */
    fun completeRestore(localId: String, platformRecordId: String) {
        sync.completeRestore(localId, platformRecordId)
    }

    /** Exist-check found the sync id already on the platform — clear removed flag, skip insert. */
    fun reconcileAlreadyOnPlatform(localId: String, platformRecordId: String?) {
        sync.reconcileAlreadyOnPlatform(localId, platformRecordId)
    }

    fun lastSyncEpochMs(platform: String): Long? {
        val syncPlatform =
            when (platform.uppercase()) {
                "HEALTH_CONNECT" -> HealthSyncPlatform.HEALTH_CONNECT
                else -> HealthSyncPlatform.APPLE_HEALTH
            }
        return store.getSyncStatus(syncPlatform)?.lastSyncAt?.toEpochMilliseconds()
    }

    fun authStatus(platform: String): String {
        val syncPlatform =
            when (platform.uppercase()) {
                "HEALTH_CONNECT" -> HealthSyncPlatform.HEALTH_CONNECT
                else -> HealthSyncPlatform.APPLE_HEALTH
            }
        return store.getSyncStatus(syncPlatform)?.authStatus ?: "NOT_DETERMINED"
    }

    fun openConflictCount(): Int = store.listOpenConflicts().size

    fun resolveConflict(id: String) {
        store.resolveConflict(id)
    }

    fun conflictSummaries(): List<String> = store.listOpenConflicts().map {
        "${it.id}|${it.workoutId}|${it.fieldName}|${it.localValue}|${it.platformValue}|${it.preferredSource}"
    }

    fun exportJsonUtf8(): String = export.exportJson(store.listWorkouts()).utf8Content.orEmpty()

    fun importJsonUtf8(utf8: String): Int = export.importJson(utf8)

    fun exportArtifactNames(): List<String> = export.exportAll().map { it.fileName }

    fun exportArtifactUtf8(fileName: String): String? = export.exportAll().firstOrNull { it.fileName == fileName }?.utf8Content

    fun exportArtifacts(): List<HealthExportArtifact> = export.exportAll()

    /**
     * Build a single-workout PDF layout document (host renders bytes).
     * [strengthJson] optional compact extras: totalKg|exKey=name,exKey2=name2|muscle=kg,muscle2=kg
     */
    fun buildWorkoutPdf(
        workoutId: String,
        locale: String,
        hrMaxBpm: Int = 190,
        hrRestBpm: Int = 60,
        strengthJson: String? = null
    ): WorkoutPdfDocument? {
        val workout = store.getWorkout(workoutId) ?: return null
        val labels = WorkoutPdfLabelCatalog.fromCatalog(locale)
        return WorkoutPdfBuilder(labels).buildSingle(
            workout = workout,
            locale = locale,
            hrMaxBpm = hrMaxBpm,
            hrRestBpm = hrRestBpm,
            strength = parseStrengthExtras(strengthJson)
        )
    }

    /** Date-range PDF layout (inclusive start, exclusive end as epoch ms). */
    fun buildRangePdf(startEpochMs: Long, endEpochMs: Long, locale: String, hrMaxBpm: Int = 190, hrRestBpm: Int = 60): WorkoutPdfDocument {
        val range =
            HealthExportRange(
                start = Instant.fromEpochMilliseconds(startEpochMs),
                endExclusive = Instant.fromEpochMilliseconds(endEpochMs)
            )
        val workouts = store.listWorkouts(range)
        val labels = WorkoutPdfLabelCatalog.fromCatalog(locale)
        return WorkoutPdfBuilder(labels).buildRange(
            workouts = workouts,
            locale = locale,
            hrMaxBpm = hrMaxBpm,
            hrRestBpm = hrRestBpm
        )
    }

    fun latestWorkoutId(): String? = store.listWorkouts().firstOrNull()?.id

    private fun parseStrengthExtras(raw: String?): WorkoutPdfBuilder.StrengthExtras? {
        if (raw.isNullOrBlank()) return null
        val parts = raw.split('|')
        val total = parts.getOrNull(0)?.toDoubleOrNull() ?: 0.0
        val names =
            parts.getOrNull(1).orEmpty().split(',').mapNotNull { pair ->
                val kv = pair.split('=', limit = 2)
                if (kv.size == 2) kv[0] to kv[1] else null
            }.toMap()
        val muscles =
            parts.getOrNull(2).orEmpty().split(',').mapNotNull { pair ->
                val kv = pair.split('=', limit = 2)
                if (kv.size == 2) kv[0] to (kv[1].toDoubleOrNull() ?: return@mapNotNull null) else null
            }.toMap()
        return WorkoutPdfBuilder.StrengthExtras(
            totalWeightLiftedKg = total,
            exerciseNames = names,
            muscleVolumeKg = muscles
        )
    }
}
