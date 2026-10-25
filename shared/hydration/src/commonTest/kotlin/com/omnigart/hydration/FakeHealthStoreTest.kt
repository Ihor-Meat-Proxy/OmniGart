package com.omnigart.hydration

import com.omnigart.hydration.health.CaloricDrinkSample
import com.omnigart.hydration.health.CaffeineSample
import com.omnigart.hydration.health.FakeHydrationHealthStore
import com.omnigart.hydration.health.HYDRATION_CLIENT_RECORD_VERSION
import com.omnigart.hydration.health.HealthReadSourceFilter
import com.omnigart.hydration.health.HealthSyncIdentity
import com.omnigart.hydration.health.HealthSyncPolicy
import com.omnigart.hydration.health.NutritionMacros
import com.omnigart.hydration.health.WaterSample
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class FakeHealthStoreTest {

    @Test
    fun appendWater_isAppendOnly_skipsWhenClientRecordIdPresent() = runTest {
        val store = FakeHydrationHealthStore()
        val id = HealthSyncIdentity("drink-1")
        val first =
            store.appendWater(
                WaterSample(id, volumeLiters = 0.25, startEpochMs = 1_000),
            )
        assertTrue(first.ok)
        assertFalse(first.skippedAlreadyPresent)
        assertEquals(HYDRATION_CLIENT_RECORD_VERSION, id.clientRecordVersion)

        val second =
            store.appendWater(
                WaterSample(id, volumeLiters = 0.33, startEpochMs = 1_000),
            )
        assertTrue(second.ok)
        assertTrue(second.skippedAlreadyPresent)
        assertEquals(first.platformRecordId, second.platformRecordId)
        // Original volume retained — no rewrite.
        assertEquals(0.25, store.water.getValue("drink-1").sample.volumeLiters)
    }

    @Test
    fun sourceFilterAvoidsForeignDoubleCount() = runTest {
        val store = FakeHydrationHealthStore()
        store.appendWater(
            WaterSample(HealthSyncIdentity("a"), volumeLiters = 0.5, startEpochMs = 10),
        )
        store.foreignWaterLiters = 0.75
        assertEquals(
            0.5,
            store.readWaterLiters(0, 100, HealthReadSourceFilter.OMNIGART_ONLY),
        )
        assertEquals(
            1.25,
            store.readWaterLiters(0, 100, HealthReadSourceFilter.ALL_SOURCES),
        )
    }

    @Test
    fun caloricDrinkAndCaffeineAppendOnlyRoundTrip() = runTest {
        val store = FakeHydrationHealthStore()
        val caffeine =
            store.appendCaffeine(
                CaffeineSample(HealthSyncIdentity("caf-1"), caffeineMg = 95.0, startEpochMs = 5),
            )
        assertTrue(caffeine.ok)
        assertFalse(caffeine.skippedAlreadyPresent)
        store.appendCaloricDrink(
            CaloricDrinkSample(
                identity = HealthSyncIdentity("food-1"),
                beverageId = "latte",
                displayName = "Latte",
                volumeMl = 240.0,
                macros = NutritionMacros(108.0, 5.8, 8.4, 5.8),
                caffeineMg = 63.0,
                startEpochMs = 5,
            ),
        )
        assertEquals(95.0, store.readCaffeineMg(0, 100))
        val skip =
            store.appendCaffeine(
                CaffeineSample(HealthSyncIdentity("caf-1"), caffeineMg = 200.0, startEpochMs = 5),
            )
        assertTrue(skip.skippedAlreadyPresent)
        assertEquals(95.0, store.readCaffeineMg(0, 100))
        assertTrue(HealthSyncPolicy.WRITE_RAW_VOLUME_RATIONALE_EN_GB.contains("raw", ignoreCase = true))
    }

    @Test
    fun writerContract_hasNoUpsertOrDeleteMethods() {
        val methods = com.omnigart.hydration.health.HydrationHealthWriter::class.java.methods.map { it.name }
        assertFalse(methods.any { it.startsWith("upsert") })
        assertFalse(methods.any { it.contains("delete", ignoreCase = true) })
        assertTrue(methods.contains("appendWater"))
        assertTrue(methods.contains("appendCaffeine"))
        assertTrue(methods.contains("appendCaloricDrink"))
    }
}
