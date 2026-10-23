package com.omnigart.shared.health.store

/**
 * Hard product rule: platform health stores are append-only.
 *
 * OmniGart never deletes or rewrites Apple Health / Health Connect objects —
 * not even records it created. Local edits stay in OmniGart. If the platform
 * drops a sample, the local copy is kept and flagged [LocalHealthWorkout.removedFromPlatform].
 *
 * Forbidden platform APIs (enforced by unit/contract tests + CI grep):
 * - HealthKit object deletion APIs
 * - Health Connect record delete / update APIs
 */
object PlatformAppendOnlyPolicy {
    /**
     * HealthKit symbols that must never appear in production OmniGart sources.
     * Built with concatenation so this file itself does not trip the CI grep gate.
     */
    val FORBIDDEN_HEALTHKIT_TOKENS: List<String> =
        listOf(
            "delete" + "Objects(",
            // Instance delete of HK objects (not Keychain/UserDefaults helpers).
            "HKHealthStore).de" + "lete(",
            "healthStore.de" + "lete(",
            "store.de" + "lete(objects",
            "store.de" + "lete(object"
        )

    /**
     * Health Connect symbols that must never appear in production OmniGart sources.
     * Built with concatenation so this file itself does not trip the CI grep gate.
     */
    val FORBIDDEN_HEALTH_CONNECT_TOKENS: List<String> =
        listOf(
            "delete" + "Records(",
            "update" + "Records("
        )

    /**
     * Whether OmniGart may attempt a first-time platform insert for this workout.
     * Skips foreign-origin, soft-deleted, already-linked, and platform-removed rows.
     * Platform writers must still exist-check by sync id / clientRecordId before insert.
     */
    fun shouldAttemptPlatformWrite(workout: LocalHealthWorkout): Boolean = HealthDataMapper.mayWriteToPlatform(workout) &&
        !workout.deleted &&
        !workout.removedFromPlatform &&
        workout.platformRecordId.isNullOrBlank()
}
