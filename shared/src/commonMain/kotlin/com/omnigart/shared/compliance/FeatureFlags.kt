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
    /** Health platform sync (HealthKit / Health Connect) UI and workers. */
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
}
