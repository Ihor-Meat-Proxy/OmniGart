package com.omnigart.hydration.models

import kotlinx.serialization.json.Json

object BeverageContentPaths {
    const val DATA = "data"
    const val LOCALES = "locales"
    const val SCHEMA = "schema/beverage.schema.json"
}

object BeverageJson {
    val format: Json =
        Json {
            ignoreUnknownKeys = false
            isLenient = false
            explicitNulls = false
        }
}

/**
 * In-memory catalogue. Hosts load JSON from [content/beverages] and pass strings here.
 */
class BeverageCatalogue(beverages: List<Beverage>) {
    val all: List<Beverage> = beverages.sortedBy { it.id }
    private val byId: Map<String, Beverage> = all.associateBy { it.id }

    fun get(id: String): Beverage? = byId[id]

    fun require(id: String): Beverage =
        byId[id] ?: error("Unknown beverage id: $id")

    fun byCategory(category: BeverageCategory): List<Beverage> =
        all.filter { it.category == category }

    companion object {
        fun fromJsonStrings(jsonBodies: List<String>): BeverageCatalogue {
            val items = jsonBodies.map { BeverageJson.format.decodeFromString(Beverage.serializer(), it) }
            val ids = items.map { it.id }
            require(ids.size == ids.toSet().size) { "Duplicate beverage ids in catalogue" }
            return BeverageCatalogue(items)
        }
    }
}
