package com.omnigart.shared.health.store.device

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Exact workout selection the phone sends so the watch / Wear app starts the same session
 * (kind, programme / routine, equipment adjustments, planned targets) — not only the generic
 * HealthKit / Health Services activity type.
 *
 * Transport:
 * - Apple: WCSession `applicationContext` / `transferUserInfo` / `sendMessage` (key [PAYLOAD_KEY])
 *   alongside `HKHealthStore.startWatchApp(with:)`
 * - Wear OS: Wearable Data Layer [MessageClient] path [LAUNCH_PATH]
 *
 * Documented in `docs/HEALTH_DATA_MAPPING.md` § Phone → watch / Wear launch.
 */
@Serializable
data class PhoneWorkoutSelection(
    val schema: String = SCHEMA_ID,
    val schemaVersion: Int = SCHEMA_VERSION,
    /** Stable kind id — see [PhoneWorkoutKindIds]. */
    val kind: String,
    val programId: String? = null,
    val routineId: String? = null,
    /** Strength programme day index when [programId] is set. */
    val dayIndex: Int? = null,
    /** Equipment ids enabled for this session (guided strength / catalogue adjustments). */
    val equipmentIds: List<String> = emptyList(),
    /** Optional per-equipment load / enable overrides. */
    val equipmentAdjustments: List<PhoneEquipmentAdjustment> = emptyList(),
    val plannedTargets: PhonePlannedTargets? = null,
    /**
     * Correlates phone + watch / Wear for the single-writer rule: only one recorder writes the
     * platform workout (Apple Health / Health Connect); local store ingest stays append-only by
     * sync id.
     */
    val sessionKey: String? = null,
    /** When true, watch is the HealthKit / Health Services writer; phone mirrors only. */
    val watchIsRecorder: Boolean = true
) {
    init {
        require(schema == SCHEMA_ID) { "Unexpected schema $schema" }
        require(isSupportedSchemaVersion(schemaVersion)) {
            "Unsupported schemaVersion $schemaVersion"
        }
        require(kind.isNotBlank())
        require(PhoneWorkoutKindIds.isKnown(kind)) { "Unknown workout kind $kind" }
    }

    companion object {
        const val SCHEMA_ID = "omnigart.phone.workout.selection"
        const val SCHEMA_VERSION = 1
        const val MIN_SUPPORTED_SCHEMA_VERSION = 1

        /** WCSession dictionary key for the UTF-8 JSON payload. */
        const val PAYLOAD_KEY = "omnigart.workout.selection.json"

        /** Wearable MessageClient path: phone → Wear launch. */
        const val LAUNCH_PATH = "/omnigart/workout/launch"

        fun isSupportedSchemaVersion(version: Int): Boolean =
            version in MIN_SUPPORTED_SCHEMA_VERSION..SCHEMA_VERSION
    }
}

@Serializable
data class PhoneEquipmentAdjustment(
    val equipmentId: String,
    val enabled: Boolean = true,
    /** Optional load multiplier (e.g. 0.5 for lighter band). */
    val loadMultiplier: Double? = null
) {
    init {
        require(equipmentId.isNotBlank())
        loadMultiplier?.let { require(it > 0.0) }
    }
}

@Serializable
data class PhonePlannedTargets(
    val durationSeconds: Int? = null,
    val distanceMetres: Double? = null,
    val activeEnergyKcal: Double? = null,
    val zoneLowerBpm: Int? = null,
    val zoneUpperBpm: Int? = null,
    val targetSets: Int? = null,
    val targetReps: Int? = null,
    val targetWeightKg: Double? = null
) {
    init {
        durationSeconds?.let { require(it >= 0) }
        distanceMetres?.let { require(it >= 0.0) }
        activeEnergyKcal?.let { require(it >= 0.0) }
        zoneLowerBpm?.let { require(it > 0) }
        zoneUpperBpm?.let { require(it > 0) }
        if (zoneLowerBpm != null && zoneUpperBpm != null) {
            require(zoneLowerBpm <= zoneUpperBpm)
        }
        targetSets?.let { require(it >= 0) }
        targetReps?.let { require(it >= 0) }
        targetWeightKg?.let { require(it >= 0.0) }
    }
}

/**
 * Kind ids shared by phone, watchOS (`OmniGartWorkoutKind`), and Wear.
 * Guided strength uses traditional HealthKit strength type but a distinct id so the watch opens
 * the programme path (equipment + slots) instead of free set logging.
 */
object PhoneWorkoutKindIds {
    const val TRADITIONAL_STRENGTH = "traditionalStrength"
    const val FUNCTIONAL_STRENGTH = "functionalStrength"
    const val GUIDED_STRENGTH = "guidedStrength"
    const val YOGA = "yoga"
    const val PILATES = "pilates"
    const val ZONE2_RUN = "zone2Run"
    const val TREADMILL_RUN = "treadmillRun"
    const val TREADMILL_WALK = "treadmillWalk"

    val ALL: Set<String> =
        setOf(
            TRADITIONAL_STRENGTH,
            FUNCTIONAL_STRENGTH,
            GUIDED_STRENGTH,
            YOGA,
            PILATES,
            ZONE2_RUN,
            TREADMILL_RUN,
            TREADMILL_WALK
        )

    fun isKnown(kind: String): Boolean = kind in ALL

    /** HealthKit / Health Services activity family for [kind] (guided → traditional strength). */
    fun platformStrengthFamily(kind: String): String =
        when (kind) {
            GUIDED_STRENGTH, TRADITIONAL_STRENGTH -> TRADITIONAL_STRENGTH
            FUNCTIONAL_STRENGTH -> FUNCTIONAL_STRENGTH
            else -> kind
        }
}

object PhoneWorkoutSelectionCodec {
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    fun encode(selection: PhoneWorkoutSelection): String = json.encodeToString(selection)

    fun decode(utf8: String): PhoneWorkoutSelection = json.decodeFromString(utf8)

    fun decodeOrNull(utf8: String): PhoneWorkoutSelection? = runCatching { decode(utf8) }.getOrNull()
}

/**
 * Swift / ObjC-friendly builders (Kotlin default args and `List` are awkward across the bridge).
 * Prefer these from Apple clients; Kotlin/Android may construct [PhoneWorkoutSelection] directly.
 */
object PhoneWorkoutSelectionFactory {
    fun create(
        kind: String,
        programId: String?,
        routineId: String?,
        dayIndex: Int,
        equipmentIds: Array<String>,
        sessionKey: String?,
        watchIsRecorder: Boolean,
        durationSeconds: Int,
        distanceMetres: Double,
        zoneLowerBpm: Int,
        zoneUpperBpm: Int,
        targetSets: Int,
        targetReps: Int,
        targetWeightKg: Double
    ): PhoneWorkoutSelection {
        val targets =
            if (
                durationSeconds < 0 &&
                distanceMetres < 0.0 &&
                zoneLowerBpm < 0 &&
                zoneUpperBpm < 0 &&
                targetSets < 0 &&
                targetReps < 0 &&
                targetWeightKg < 0.0
            ) {
                null
            } else {
                PhonePlannedTargets(
                    durationSeconds = durationSeconds.takeIf { it >= 0 },
                    distanceMetres = distanceMetres.takeIf { it >= 0.0 },
                    zoneLowerBpm = zoneLowerBpm.takeIf { it > 0 },
                    zoneUpperBpm = zoneUpperBpm.takeIf { it > 0 },
                    targetSets = targetSets.takeIf { it >= 0 },
                    targetReps = targetReps.takeIf { it >= 0 },
                    targetWeightKg = targetWeightKg.takeIf { it >= 0.0 }
                )
            }
        return PhoneWorkoutSelection(
            kind = kind,
            programId = programId?.takeIf { it.isNotBlank() },
            routineId = routineId?.takeIf { it.isNotBlank() },
            dayIndex = dayIndex.takeIf { it >= 0 },
            equipmentIds = equipmentIds.toList(),
            plannedTargets = targets,
            sessionKey = sessionKey?.takeIf { it.isNotBlank() },
            watchIsRecorder = watchIsRecorder
        )
    }

    /** Minimal selection: kind (+ optional session key). Sentinel ints/doubles mean “omit”. */
    fun createSimple(kind: String, sessionKey: String?): PhoneWorkoutSelection =
        create(
            kind = kind,
            programId = null,
            routineId = null,
            dayIndex = -1,
            equipmentIds = emptyArray(),
            sessionKey = sessionKey,
            watchIsRecorder = true,
            durationSeconds = -1,
            distanceMetres = -1.0,
            zoneLowerBpm = -1,
            zoneUpperBpm = -1,
            targetSets = -1,
            targetReps = -1,
            targetWeightKg = -1.0
        )
}
