package com.omnigart.hydration

import com.omnigart.hydration.health.CaloricDrinkSample
import com.omnigart.hydration.health.CaffeineSample
import com.omnigart.hydration.health.FakeHydrationHealthStore
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
    fun upsertIsIdempotentAndVersioned() = runTest {
        val store = FakeHydrationHealthStore()
        val id = HealthSyncIdentity("drink-1", clientRecordVersion = 1)
        val first =
            store.upsertWater(
                WaterSample(id, volumeLiters = 0.25, startEpochMs = 1_000),
            )
        assertTrue(first.ok)
        val second =
            store.upsertWater(
                WaterSample(id.copy(clientRecordVersion = 2), volumeLiters = 0.33, startEpochMs = 1_000),
            )
        assertTrue(second.ok)
        assertEquals(first.platformRecordId, second.platformRecordId)
        assertEquals(0.33, store.water.getValue("drink-1").sample.volumeLiters)

        val stale =
            store.upsertWater(
                WaterSample(id.copy(clientRecordVersion = 1), volumeLiters = 0.1, startEpochMs = 1_000),
            )
        assertFalse(stale.ok)
    }

    @Test
    fun sourceFilterAvoidsForeignDoubleCount() = runTest {
        val store = FakeHydrationHealthStore()
        store.upsertWater(
            WaterSample(HealthSyncIdentity("a", 1), volumeLiters = 0.5, startEpochMs = 10),
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
    fun caloricDrinkAndCaffeineRoundTrip() = runTest {
        val store = FakeHydrationHealthStore()
        store.upsertCaffeine(
            CaffeineSample(HealthSyncIdentity("caf-1", 1), caffeineMg = 95.0, startEpochMs = 5),
        )
        store.upsertCaloricDrink(
            CaloricDrinkSample(
                identity = HealthSyncIdentity("food-1", 1),
                beverageId = "latte",
                displayName = "Latte",
                volumeMl = 240.0,
                macros = NutritionMacros(108.0, 5.8, 8.4, 5.8),
                caffeineMg = 63.0,
                startEpochMs = 5,
            ),
        )
        assertEquals(95.0, store.readCaffeineMg(0, 100))
        assertTrue(store.deleteByClientRecordId("caf-1", 1).ok)
        assertEquals(0.0, store.readCaffeineMg(0, 100))
        assertTrue(HealthSyncPolicy.WRITE_RAW_VOLUME_RATIONALE_EN_GB.contains("raw", ignoreCase = true))
    }
}
