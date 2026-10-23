package com.omnigart.hydration

import com.omnigart.hydration.models.BeverageCatalogue
import com.omnigart.hydration.models.BeverageCategory
import com.omnigart.hydration.models.CountingModeInfo
import com.omnigart.hydration.models.HydrationCountingMode
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class BeverageContentValidatorTest {

    private val root: Path =
        Path.of(
            System.getProperty("omnigart.beverages.root")
                ?: error("omnigart.beverages.root system property missing"),
        )

    @Test
    fun catalogueValidatesAndLoads() {
        val report = BeverageContentValidator(root).validate()
        assertTrue(report.ok, report.issues.joinToString("\n") { "${it.path}: ${it.message}" })
        assertEquals(24, report.beverageCount)

        val bodies =
            Files.list(root.resolve("data")).use { stream ->
                stream.filter { it.toString().endsWith(".json") }
                    .map { it.readText() }
                    .toList()
            }
        val catalogue = BeverageCatalogue.fromJsonStrings(bodies)
        assertEquals(24, catalogue.all.size)
        assertEquals(1.0, catalogue.require("water").hydrationFactor)
        assertEquals(1.50, catalogue.require("whole-milk").hydrationFactor)
        assertEquals(1.58, catalogue.require("skimmed-milk").hydrationFactor)
        assertEquals(BeverageCategory.COFFEE, catalogue.require("espresso").category)
        assertTrue(catalogue.require("oat-milk").hydrationFactorEstimated)
    }

    @Test
    fun localesAreEnGbAndUkOnly() {
        val locales = root.resolve("locales")
        val names = Files.list(locales).use { it.map { p -> p.fileName.toString() }.toList() }
        assertTrue("en-GB.json" in names)
        assertTrue("uk.json" in names)
        assertFalse(names.any { it.startsWith("ru") })
    }

    @Test
    fun defaultCountingModeIsHydrationAdjusted() {
        assertEquals("hydration_adjusted", CountingModeInfo.DEFAULT_MODE)
        assertEquals(HydrationCountingMode.HYDRATION_ADJUSTED, HydrationCountingMode.valueOf("HYDRATION_ADJUSTED"))
        assertTrue(CountingModeInfo.INFO_EN_GB.contains("10.3945/ajcn.115.114769"))
        assertTrue(CountingModeInfo.INFO_UK.contains("BHI"))
    }
}
