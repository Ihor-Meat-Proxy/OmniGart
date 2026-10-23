package com.omnigart.hydration

import com.omnigart.hydration.counting.HydrationCounting
import com.omnigart.hydration.models.Beverage
import com.omnigart.hydration.models.BeverageCategory
import com.omnigart.hydration.models.DrinkLogEntry
import com.omnigart.hydration.models.HydrationCountingMode
import com.omnigart.hydration.models.LocalizedNameKeys
import com.omnigart.hydration.models.MacrosPer100ml
import kotlin.test.Test
import kotlin.test.assertEquals

class HydrationCountingTest {

    private val water =
        beverage("water", BeverageCategory.WATER, factor = 1.0, caffeine = 0.0)
    private val milk =
        beverage("whole-milk", BeverageCategory.MILK, factor = 1.50, caffeine = 0.0)
    private val coffee =
        beverage("filter-coffee", BeverageCategory.COFFEE, factor = 1.0, caffeine = 95.0, serving = 240)

    @Test
    fun hydrationAdjustedAppliesBhi() {
        val entry = log("whole-milk", 200.0)
        assertEquals(300.0, HydrationCounting.countedMl(entry, milk, HydrationCountingMode.HYDRATION_ADJUSTED))
    }

    @Test
    fun rawVolumeIgnoresBhi() {
        val entry = log("whole-milk", 200.0)
        assertEquals(200.0, HydrationCounting.countedMl(entry, milk, HydrationCountingMode.RAW_VOLUME))
    }

    @Test
    fun waterOnlyIgnoresMilk() {
        val entries = listOf(log("water", 250.0), log("whole-milk", 200.0))
        val map = mapOf(water.id to water, milk.id to milk)
        assertEquals(
            250.0,
            HydrationCounting.totalCountedMl(entries, map, HydrationCountingMode.WATER_ONLY),
        )
    }

    @Test
    fun caffeineSumsScaledServings() {
        val entries = listOf(log("filter-coffee", 240.0), log("filter-coffee", 120.0))
        val total = HydrationCounting.totalCaffeineMg(entries, mapOf(coffee.id to coffee))
        assertEquals(95.0 + 47.5, total, absoluteTolerance = 1e-9)
    }

    @Test
    fun progressFractionCapsAtLeastZero() {
        assertEquals(0.0, HydrationCounting.progressFraction(-1.0, 1600.0))
        assertEquals(0.5, HydrationCounting.progressFraction(800.0, 1600.0))
    }

    private fun log(beverageId: String, ml: Double) =
        DrinkLogEntry(id = "e-$beverageId-$ml", beverageId = beverageId, loggedAtEpochMs = 0L, volumeMl = ml)

    private fun beverage(
        id: String,
        category: BeverageCategory,
        factor: Double,
        caffeine: Double,
        serving: Int = 200,
    ) = Beverage(
        id = id,
        nameKeys = LocalizedNameKeys("beverage.${id.replace('-', '_')}.name", "beverage.${id.replace('-', '_')}.name"),
        category = category,
        defaultServingMl = serving,
        caffeineMgPerServing = caffeine,
        macrosPer100ml = MacrosPer100ml(0.0, 0.0, 0.0, 0.0),
        hydrationFactor = factor,
    )
}
