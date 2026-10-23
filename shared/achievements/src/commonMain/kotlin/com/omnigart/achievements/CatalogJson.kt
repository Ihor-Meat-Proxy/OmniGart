package com.omnigart.achievements

import kotlinx.serialization.json.Json

val AchievementsJson: Json = Json {
    ignoreUnknownKeys = false
    classDiscriminator = "kind"
    explicitNulls = false
}

fun decodeAchievementCatalog(text: String): AchievementCatalog =
    AchievementsJson.decodeFromString(AchievementCatalog.serializer(), text)

fun decodeChallengeTemplates(text: String): ChallengeTemplateCatalog =
    AchievementsJson.decodeFromString(ChallengeTemplateCatalog.serializer(), text)
