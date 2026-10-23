package com.omnigart.shared.data

import app.cash.sqldelight.db.SqlDriver

/**
 * Platform-specific SQLDelight driver factory.
 * Phase 0 uses an unencrypted SQLite database; encrypted SQLCipher is planned
 * (see docs/OPEN_QUESTIONS.md).
 */
expect class DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}
