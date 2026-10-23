package com.omnigart.shared.data

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.omnigart.shared.db.OmniGartDatabase

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver = NativeSqliteDriver(
        schema = OmniGartDatabase.Schema,
        name = "omnigart.db"
    )
}
