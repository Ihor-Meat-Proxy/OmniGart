package com.omnigart.shared.data

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.omnigart.shared.db.OmniGartDatabase

actual class DatabaseDriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver = AndroidSqliteDriver(
        schema = OmniGartDatabase.Schema,
        context = context,
        name = "omnigart.db"
    )
}
