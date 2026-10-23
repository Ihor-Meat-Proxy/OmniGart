package com.omnigart.shared.compliance

/**
 * Release defaults for unfinished or gated product surfaces.
 *
 * Rules (enforced by `scripts/check_app_review_compliance.py`):
 * - Every `*Enabled` flag defaults to `false` in release.
 * - Debug tooling is never enabled here; platform shells gate debug UI with
 *   `#if DEBUG` / `BuildConfig.DEBUG` (or debuggable ApplicationInfo).
 *
 * Flip a flag only when the feature is ready for the current build type, or
 * override at runtime in DEBUG builds via platform debug menus.
 */
object FeatureFlags {
    /**
     * Health platform sync (HealthKit / Health Connect / Google Health API) UI and workers.
     * Remains false by default until the health integration layer is product-ready.
     */
    val healthSyncEnabled: Boolean = false

    /** Barcode food logging camera flow. */
    val foodBarcodeEnabled: Boolean = false

    /** Morning readiness check-in surface. */
    val readinessCheckInEnabled: Boolean = false

    /** Wearable Bluetooth accessory pairing. */
    val bluetoothAccessoriesEnabled: Boolean = false

    /** Push / local notification scheduling beyond OS defaults. */
    val notificationsEnabled: Boolean = false

    /** Encrypted cloud backup. */
    val encryptedBackupEnabled: Boolean = false

    /** Achievement gallery / share sheets. */
    val achievementsEnabled: Boolean = false

    /**
     * Schedule OmniGart plans into the system Workout app via WorkoutKit.
     * Stays false until product-ready; Personal Team builds still compile when off.
     */
    val workoutKitSchedulingEnabled: Boolean = false

    /**
     * Import completed HealthKit workouts into load/recovery even when
     * [healthSyncEnabled] is false (read-only path). Default false until validated.
     */
    val healthWorkoutImportEnabled: Boolean = false
}
