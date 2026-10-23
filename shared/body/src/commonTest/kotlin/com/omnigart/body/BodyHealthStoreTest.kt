package com.omnigart.body

import com.omnigart.body.health.BodyFatSample
import com.omnigart.body.health.BodyHealthSyncPolicy
import com.omnigart.body.health.BodyMassSample
import com.omnigart.body.health.BodyReadSourceFilter
import com.omnigart.body.health.BodySyncIdentity
import com.omnigart.body.health.FakeBodyHealthStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class BodyHealthStoreTest {

    @Test
    fun upsertVersioningAndSourceFilter() = runTest {
        val store = FakeBodyHealthStore()
        val id = BodySyncIdentity("w1", 1)
        assertTrue(store.upsertBodyMass(BodyMassSample(id, 70.0, 10)).ok)
        assertTrue(
            store.upsertBodyMass(BodyMassSample(id.copy(clientRecordVersion = 2), 71.0, 20)).ok,
        )
        assertFalse(
            store.upsertBodyMass(BodyMassSample(id.copy(clientRecordVersion = 1), 69.0, 15)).ok,
        )
        store.foreignMassKg = 90.0
        assertEquals(71.0, store.readLatestBodyMassKg(0, 100))
        assertEquals(71.0, store.readLatestBodyMassKg(0, 100, BodyReadSourceFilter.ALL_SOURCES))
        assertTrue(BodyHealthSyncPolicy.PHOTOS_LOCAL_ONLY_EN_GB.contains("not", ignoreCase = true))
    }

    @Test
    fun bodyFatUpsertCarriesMethodMetadata() = runTest {
        val store = FakeBodyHealthStore()
        val result =
            store.upsertBodyFat(
                BodyFatSample(
                    identity = BodySyncIdentity("bf1", 1),
                    bodyFatPercent = 22.5,
                    epochMs = 1,
                    methodId = "cun_bae",
                ),
            )
        assertTrue(result.ok)
        assertEquals("cun_bae", store.fat.getValue("bf1").sample.methodId)
    }
}
