package com.omnigart.achievements

import com.omnigart.achievements.model.AchievementDefinition
import kotlinx.serialization.json.Json

object CatalogueJson {
    val json: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    fun parseAchievement(raw: String): AchievementDefinition =
        json.decodeFromString(AchievementDefinition.serializer(), raw)

    fun parseAchievementList(raw: String): List<AchievementDefinition> =
        json.decodeFromString(kotlinx.serialization.builtins.ListSerializer(AchievementDefinition.serializer()), raw)
}
