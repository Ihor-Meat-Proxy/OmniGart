package com.omnigart.achievements

import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AchievementContentValidatorTest {
    private fun root(): Path {
        val prop = System.getProperty("omnigart.achievements.root")
        return Path.of(prop)
    }

    @Test
    fun catalogValidatesAndCountsEighty() {
        val report = AchievementContentValidator(root()).validate()
        assertTrue(report.ok, report.issues.joinToString("\n") { "${it.path}: ${it.message}" })
        assertEquals(80, report.achievementCount)
        assertEquals(5, report.challengeCount)
    }

    @Test
    fun kotlinDecodeRoundTrip() {
        val text = root().resolve("data/achievements.json").toFile().readText()
        val catalog = decodeAchievementCatalog(text)
        assertEquals(80, catalog.achievements.size)
        assertTrue(catalog.healthyByDesign.forbidCalorieRestrictionRewards)
        assertTrue(catalog.healthyByDesign.streaksAllowPlannedRestAndFreeze)
        val byCat = catalog.achievements.groupingBy { it.category }.eachCount()
        assertEquals(9, byCat[AchievementCategory.STRENGTH])
        assertEquals(8, byCat[AchievementCategory.RUNNING])
        assertEquals(7, byCat[AchievementCategory.SWIMMING])
        assertEquals(7, byCat[AchievementCategory.YOGA])
        assertEquals(7, byCat[AchievementCategory.PILATES])
        assertEquals(8, byCat[AchievementCategory.NUTRITION])
        assertEquals(7, byCat[AchievementCategory.WATER])
        assertEquals(9, byCat[AchievementCategory.RECOVERY])
        assertEquals(6, byCat[AchievementCategory.BODY_MEASUREMENTS])
        assertEquals(12, byCat[AchievementCategory.CONSISTENCY])
    }

    @Test
    fun challengeTemplatesDecode() {
        val text = root().resolve("data/challenge-templates.json").toFile().readText()
        val catalog = decodeChallengeTemplates(text)
        assertEquals(5, catalog.templates.size)
        assertTrue(catalog.templates.all { it.localOnly && !it.social })
    }
}
