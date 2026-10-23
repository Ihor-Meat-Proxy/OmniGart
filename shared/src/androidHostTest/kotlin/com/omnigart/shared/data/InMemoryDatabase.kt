package com.omnigart.shared.data

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.omnigart.shared.db.OmniGartDatabase

fun createInMemoryDatabase(): OmniGartDatabase {
    val driver: SqlDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    OmniGartDatabase.Schema.create(driver)
    return OmniGartDatabase(driver)
}
